package mope;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
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
 * Created by lukas on 03.01.16.
 * Implements a read/write lock, which does not restrict
 * a lock/release operation to be perfromed from the same thread.
 *
 */
public class mOPETreeLock {

    private Semaphore updateGate = new Semaphore(1, true);
    private AtomicInteger countRead = new AtomicInteger(0);
    CountDownLatch latch = null;
    private Object mutex = new Object();

    public boolean aquireReadLock(int numSecond) throws InterruptedException {
        return passUpdateGate(numSecond);
    }

    public boolean aquireWirteLock(int numSecond) throws InterruptedException {
        boolean res = updateGate.tryAcquire(numSecond, TimeUnit.SECONDS);
        if(res) {
            synchronized (mutex) {
                latch = new CountDownLatch(countRead.get());
            }
            if(!latch.await(numSecond,TimeUnit.SECONDS)) {
                updateGate.release();
                return false;
            }
        }
        return res;
    }

    public void releaseReadLock() {
        synchronized (mutex) {
            if(latch != null && latch.getCount()>0)
                latch.countDown();
            countRead.decrementAndGet();
        }
    }

    public void releaseWriteLock() {
        updateGate.release();
    }

    private boolean passUpdateGate(int numSecond) throws InterruptedException {
        boolean res = updateGate.tryAcquire(numSecond, TimeUnit.SECONDS);
        if(res) {
            countRead.incrementAndGet();
            updateGate.release();
        }
        return res;
    }

}
