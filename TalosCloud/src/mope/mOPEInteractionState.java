package mope;

import database.CommandDescription;
import database.DBAccessAPI;
import database.ImOPEDBInterface;
import database.mOPEDBInterface;
import mope.messages.mOPEMsgFactory;
import mope.messages.mOPEResponseMessage;
import mopetree.mOPETreeWalker;
import mopetree.mOPEUpdateSummary;
import util.MessageUtil;
import util.User;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/*
 * Copyright (c) 2016, Institute for Pervasive Computing, ETH Zurich.
 * All rights reserved.
 *
 * Author:
 *       Lukas Burkhalter <lubu@student.ethz.ch>
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */


/**
 * Implements the interaction logic with the client.
 * Keeps the state in the intercation protocol with the client.
 * The main class, which implements the mOPE logic,.
 */
public final class mOPEInteractionState {

    private static AtomicInteger sessionCounter = new AtomicInteger(0);

    //User
    private User user;
    private int sessionID = sessionCounter.incrementAndGet();

    //Command to be executed on DB
    private CommandDescription desc;
    private String[] args;

    //mOPE values to encrypt in order of encryption
    private mOPEJob[] jobs;

    //currentState
    private int jobIndex = 0;
    private mOPETreeWalker<String> curWalker;

    private AtomicBoolean invalid = new AtomicBoolean(false);

    //DB
    private ImOPEDBInterface db = mOPEDBInterface.getInstance();

    public mOPEInteractionState(User user, String[] args, CommandDescription desc, List<mOPEJob> jobs) throws mOPEException {
        this.user = user;
        this.args = args;
        this.desc = desc;
        loadTreesForJobs(jobs, user);
        handleCreateInteraction(jobs);
    }

    private void loadTreesForJobs(List<mOPEJob> jobs, User u) throws mOPEException {
        for(mOPEJob job : jobs)
            job.loadTree(u, mOPEDBInterface.getInstance());
    }

    private void handleCreateInteraction(List<mOPEJob> jobsList) throws mOPEException {
        try {
            lockTrees(jobsList, getSortedTreeIndexes(jobsList));
        } catch (InterruptedException e) {
            releaseTreeLocks(jobs.length);
            throw new mOPEException("Thread was interupted while acquiring a lock", e);
        }
        if(jobsList.isEmpty())
            throw new mOPEException("Cannot create state without Job");
        jobs = new mOPEJob[jobsList.size()];
        int index = 0;
        for(mOPEJob job : jobsList) {
            jobs[index] = job;
            index++;
        }
        jobIndex = 0;
        curWalker = jobs[0].tree.startNewWalker();
    }

    private List<Integer> getSortedTreeIndexes(List<mOPEJob> jobs) {
        List<Integer> indexes = new ArrayList<>(jobs.size());
        for (mOPEJob job : jobs) {
            indexes.add(job.tree.getID());
        }
        Collections.sort(indexes);
        return indexes;
    }

    private void lockTrees(List<mOPEJob> jobs, List<Integer> sortedTreeID) throws InterruptedException {
        int before = -1;
        for(Integer id : sortedTreeID) {
            if(before == id) {
                continue;
            }
            before = id;
            for(mOPEJob job : jobs) {
                if(job.tree.getID() == id) {
                    aquireLockForTree(job, job.isUpdate());
                }
            }
        }
    }

    private void aquireLockForTree(mOPEJob job, boolean isUpdate) throws InterruptedException {
        if(isUpdate) {
            while (!job.tree.enterForUpdate()) {
                job.tree.quitUpdate();
                job.loadTree(user, db);
            }
        } else {
            while (!job.tree.enterNoUpdate()) {
                job.tree.quitNoUpdate();
                job.loadTree(user, db);
            }
        }
    }

    private void releaseTreeLocks(int length) {
        assert(length <= jobs.length);
        for(int i=0; i < length; i++) {
            if(jobs[i].isUpdate()) {
                jobs[i].tree.quitUpdate();
            } else {
                jobs[i].tree.quitNoUpdate();
            }
        }
        invalid.set(true);
    }

    public mOPEResponseMessage getFirstStep() {
        return handleStepNode(getCurrentJob().getValue(), curWalker);
    }

    /**
     * Handles a protocol step with the client.
     * @param index the index received from the client (the path to take in tree)
     * @param isEqual indicates if the values where equal (if true no further processing is needed)
     * @return the new mOPE response message for the client
     * @throws mOPEException
     */
    public mOPEResponseMessage handleClientStep(int index, boolean isEqual) throws mOPEException {
        try {
            mOPEJob curJob = getCurrentJob();
            // are we at a leaf node?
            if (curWalker.leafNodeReached()) {
                mOPEEncryptionResult res = handleAction(curJob, curWalker, index, isEqual);
                curJob.setResult(res);
                if(switchToNextJob()) {
                    return handleStepNode(getCurrentJob().getValue(), curWalker);
                }
                return handleFinished();
            } else {
                // is the value equal?
                if(isEqual) {
                    mOPEEncryptionResult res = handleAction(curJob, curWalker, index, isEqual);
                    curJob.setResult(res);
                    if(switchToNextJob()) {
                        return handleStepNode(getCurrentJob().getValue(), curWalker);
                    }
                    return handleFinished();
                } else {
                    curWalker.stepToNextNode(index);
                    return handleStepNode(curJob.getValue(), curWalker);
                }
            }
        } catch (Exception e) {
            handleException();
            throw new mOPEException("State Exception:" + e.getMessage() + " -> rollback", e);
        }
    }

    /**
     * Implements the action if a leaf node is reached an we found the encoding
     */
    private mOPEEncryptionResult handleAction(mOPEJob curJob, mOPETreeWalker<String> walker, int index, boolean isEqual) throws Exception {
        // what is the type of operation
        switch (curJob.type) {
            case mOPEJob.INSERT:
                if(isEqual) {
                    return new mOPEEncryptionResult(curJob.getValue(), walker.getEncodingOfIndex(index));
                } else {
                    mOPEUpdateSummary<String> summary = walker.insertValue(index, curJob.getValue());
                    BigInteger encoding =  summary.getEncodingForValueAndRemove(curJob.getValue());
                    if(summary.hasUpdates())
                        return new mOPEEncryptionResult(curJob.getValue(),
                            encoding,
                            new mOPEUpdateTask(curJob.tree, summary, curJob.getValue()));
                    else
                        return new mOPEEncryptionResult(curJob.getValue(),
                                encoding);
                }
            case mOPEJob.DELETE:
                if(!isEqual)
                    throw new mOPEException("The tree does not contain the value for delete");
                BigInteger encoding = walker.getEncodingOfIndex(index);
                int numOccurences = db.checkNumOccurences(user, curJob.tree.getIndex(), encoding);
                if(numOccurences<2) {
                    mOPEUpdateSummary<String> summary = walker.deleteValue(index);
                    if(summary.hasUpdates())
                        return new mOPEEncryptionResult(curJob.getValue(),
                                encoding,
                                new mOPEUpdateTask(curJob.tree, summary, curJob.getValue()));
                    else
                        return new mOPEEncryptionResult(curJob.getValue(),
                                encoding);
                } else {
                    return new mOPEEncryptionResult(curJob.getValue(),
                            encoding);
                }
            case mOPEJob.UPDATE:
                throw new mOPEException("Operation UPDATE supported");
            case mOPEJob.QUERY:
                if(isEqual) {
                    return new mOPEEncryptionResult(curJob.getValue(), walker.getEncodingOfIndex(index));
                } else {
                    return new mOPEEncryptionResult(curJob.getValue(), walker.getEncodingForIndexBelow(index));
                }
            default:
                throw new mOPEException("Operation not supported: " + curJob.type);
        }
    }

    private mOPEResponseMessage handleStepNode(String value, mOPETreeWalker<String> walker) {
        List<String> values = walker.getValuesOfCurrentNode();
        String[] valuesAr = new String[values.size()];
        int index = 0;
        for(String cur : values) {
            valuesAr[index] = cur;
            index++;
        }
        return mOPEMsgFactory.createClientStepMsg(value, sessionID, getCurrentJob().getDetHash(), valuesAr);
    }

    /**
     * Handles the case, when the protocol is finished.
     */
    private mOPEResponseMessage handleFinished() throws Exception {
        boolean hasDBUpdate = false;
        boolean isUpdate = false;
        mOPEResponseMessage resultMsg = null;
        for (mOPEJob job : this.jobs) {
            if (!job.hasResult())
                throw new mOPEException("Job not finished");
            if (job.getResult().hasUpdateTask()) {
                hasDBUpdate = true;
            }
            if (job.isUpdate()) {
                isUpdate = true;
            }
        }

        for(int i=0; i<jobs.length; i++) {
            mOPEEncryptionResult res = jobs[i].getResult();
            this.args[jobs[i].getArgIndexInCmd()] = res.getEncoding().toString();
        }

        if (isUpdate) {
            if(hasDBUpdate) {
                DBUpdater updater = new DBUpdater(this);
                (new Thread(updater)).start();
                resultMsg = mOPEMsgFactory.createResultMsg(MessageUtil.getRepsonseMessageFromUpdtaeDBResult());
            } else {
                String res = DBAccessAPI.excecuteCommand(user, desc, args);
                resultMsg = mOPEMsgFactory.createResultMsg(res);
            }
        } else {
            String res = DBAccessAPI.excecuteCommand(user, desc, args);
            resultMsg = mOPEMsgFactory.createResultMsg(res);
        }

        this.releaseTreeLocks(jobs.length);
        return resultMsg;
    }

    public synchronized void rollback() {
        if(!invalid.get()) {
            handleException();
        }
    }

    private void handleException() {
        try {
            for (int i = 0; i <= jobIndex; i++) {
                mOPEJob curJob = jobs[i];
                if (curJob.isUpdate()) {
                    db.refreshCachedTree(user, curJob.tree.getIndex());
                    curJob.tree.invalidate();
                }
            }
        } finally {
            this.releaseTreeLocks(jobIndex+1);
        }
    }

    private boolean switchToNextJob() {
        if(jobIndex < jobs.length-1) {
            jobIndex++;
            curWalker = getCurrentJob().tree.startNewWalker();
            return true;
        } else {
            return false;
        }
    }

    void aquireDBUpdateLocks() {
        HashSet<Integer> perfomedIds = new HashSet<>();
        for(mOPEJob job : jobs) {
            if(!perfomedIds.contains(job.tree.getID())) {
                job.tree.lockForDBupdate();
                perfomedIds.add(job.tree.getID());
            }
        }
    }

    void releaseDBUpdateLocks() {
        HashSet<Integer> perfomedIds = new HashSet<>();
        for(mOPEJob job : jobs) {
            if(!perfomedIds.contains(job.tree.getID())) {
                job.tree.releaseAfterDBupdate();
                perfomedIds.add(job.tree.getID());
            }
        }
    }

    private mOPEJob getCurrentJob() {
        return jobs[jobIndex];
    }

    mOPEJob[] getJobs() {
        return Arrays.copyOf(this.jobs, jobs.length);
    }

    public User getUser() {
        return user;
    }

    public CommandDescription getDesc() {
        return desc;
    }

    public String[] getArgs() {
        return Arrays.copyOf(this.args, args.length);
    }

    public int getSessionID() {
        return sessionID;
    }
}

