package mope;

import database.ImOPEDBInterface;
import database.mOPEDBInterface;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
 * Impelments a database uptater for the mOPE interface of the talos cloud.
 * Keeps the mOPE trees in the database upto date with the trees in memory.
 */
public class DBUpdater implements Runnable {

    private static AtomicInteger counter = new AtomicInteger(0);

    private mOPEInteractionState state;

    private ImOPEDBInterface db = mOPEDBInterface.getInstance();

    public DBUpdater(mOPEInteractionState state) {
        this.state = state;
        this.state.aquireDBUpdateLocks();
    }

    @Override
    public void run() {
        try {
            performDBUpdates();
        } catch (Exception e) {
            e.printStackTrace();
        }
        //System.out.println("DB Update Performed " + counter.getAndIncrement());
    }

    private void performDBUpdates() throws Exception {
        try {
            String[] cmdArgs = state.getArgs();
            mOPEJob[] jobs = state.getJobs();
            List<mOPEUpdateTask> updates = getUpdates(jobs, cmdArgs);
            db.performmOPEUpdate(state.getUser(), updates, state.getDesc(), cmdArgs);
        } catch (Exception e) {
            rollbackState();
        } finally {
            state.releaseDBUpdateLocks();
        }
    }

    private List<mOPEUpdateTask> getUpdates(mOPEJob[] jobs, String[] args) {
        ArrayList<mOPEUpdateTask> updates = new ArrayList<>();
        HashMap<Integer, mOPEJob> treeIndToJob = new HashMap<>();
        for(int ind=0; ind < jobs.length; ind++) {
            mOPEJob curJob = jobs[ind];
            if(curJob.getResult().hasUpdateTask()) {
                if(treeIndToJob.containsKey(curJob.tree.getID())) {
                    mOPEUpdateTask last = treeIndToJob.get(curJob.tree.getID()).getResult().getUpdate();
                    if(last.performMergeNext(curJob.getResult().getUpdate())) {
                        args[curJob.getArgIndexInCmd()] = last.getUpdateSummary().getEncodingForValueAndRemove(curJob.getValue()).toString();
                    }
                }
                updates.add(curJob.getResult().getUpdate());
                treeIndToJob.put(curJob.tree.getID(), curJob);
            }
        }
        return updates;
    }

    private void rollbackState() {
        mOPEJob[] jobs = state.getJobs();
        for(mOPEJob job : jobs) {
            job.tree.invalidate();
            mOPEDBInterface.getInstance().refreshCachedTree(state.getUser(), job.tree.getIndex());
        }
    }
}
