package mope;

import mopetree.Tuple;
import mopetree.mOPETreeHelper;
import mopetree.mOPETreeWalker;
import util.User;

import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.Semaphore;
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
 * Implements an mOPE interaction tree.
 */
public class mOPEInteractionTree {

    //private final static Logger LOGGER = Logger.getLogger(mOPEInteractionTree.class.getName());

    private static AtomicInteger counter = new AtomicInteger(0);

    private mOPETreeLock treeLock;
    private Semaphore updateSemaphore;
    private int ID = counter.getAndIncrement();

    private User user;
    private mOPETreeIndex index;

    /**
     * The datastructure of the mOPE tree
     */
    private mOPETreeHelper<String> mOPETree;

    private boolean valid = true;

    public mOPEInteractionTree(User user, mOPETreeIndex index, mOPETreeHelper<String> mOPETree) {
        this.user = user;
        this.index = index;
        this.mOPETree = mOPETree;
        this.treeLock = new mOPETreeLock();
        this.updateSemaphore = new Semaphore(1,true);
    }

    public mOPEInteractionTree(User user, mOPETreeIndex index, ResultSet rs) throws SQLException  {
        this.user = user;
        this.index = index;
        this.mOPETree = generateTreeFromDBResult(rs);
        this.treeLock = new mOPETreeLock();
        this.updateSemaphore = new Semaphore(1,true);
    }

    private mOPETreeHelper<String> generateTreeFromDBResult(ResultSet rs) throws SQLException {
        mOPETreeHelper<String> mOPETreeRes = new mOPETreeHelper();
        while (rs.next()) {
            BigInteger encoding = new BigInteger(rs.getString(index.getColumnOPE()));
            String detCipher = rs.getString(index.getColumnDET());
            mOPETreeRes.constructTreeFromEncodingsStep(new Tuple(encoding,detCipher));
        }
        return mOPETreeRes;
    }

    public User getUser() {
        return user;
    }

    public mOPETreeIndex getIndex() {
        return index;
    }

    public mOPETreeWalker<String> startNewWalker() {
        return mOPETree.getNewWalker();
    }

    public int getID() {
        return ID;
    }

    public boolean enterNoUpdate() throws InterruptedException, mOPEException {
         if(!treeLock.aquireReadLock(60)) {
             throw  new mOPEException("Unable to acquire lock within 1 Minute");
         }
        checkNoUpdateOnDB();
        return valid;
    }

    public boolean enterForUpdate() throws InterruptedException, mOPEException {
        if(!treeLock.aquireWirteLock(60)) {
            throw  new mOPEException("Unable to acquire lock within 1 Minute");
        }
        checkNoUpdateOnDB();
        //System.out.println(Thread.currentThread().getName() +" is in Lock " +index.getColumnOPE() +" User:" +user.getLocalid());
        return valid;
    }

    public void checkNoUpdateOnDB() throws InterruptedException {
        this.updateSemaphore.acquire();
        this.updateSemaphore.release();
    }

    public void lockForDBupdate() {
        this.updateSemaphore.drainPermits();
        //System.out.println( "Has Lock " +index.getColumnOPE()+ " DB " + Thread.currentThread().getName()+" User:" +user.getLocalid());
    }
    public void releaseAfterDBupdate() {
        //System.out.println("Release Lock " +index.getColumnOPE()+ " DB " + Thread.currentThread().getName()+" User:" +user.getLocalid());
        this.updateSemaphore.release();
    }


    public void quitNoUpdate() {
        treeLock.releaseReadLock();
    }

    public void quitUpdate() {
        //System.out.println(Thread.currentThread().getName() +" leaves Lock " +index.getColumnOPE()+" User:" +user.getLocalid());
        treeLock.releaseWriteLock();
    }

    public void invalidate() {
        valid = false;
    }

    public String getTreeRepresentation() {
        return mOPETree.getTreeString();
    }


}
