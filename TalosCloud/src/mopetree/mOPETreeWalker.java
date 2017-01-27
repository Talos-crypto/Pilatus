package mopetree;

import java.math.BigInteger;
import java.util.List;

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
 * A helper for traversing the tree
 */
public class mOPETreeWalker<T> {

    private mOPETreeHelper.mOPENode<T> curNode;

    public mOPETreeWalker(mOPETreeHelper.mOPENode<T> startNode) {
        this.curNode = startNode;
    }

    public void stepToNextNode(int index) throws IllegalArgumentException {
        curNode = curNode.getNodeAtIndex(index);
    }

    public List<T> getValuesOfCurrentNode() {
        return curNode.getKeys();
    }

    public boolean leafNodeReached() {
        return curNode.isLeaf();
    }

    public BigInteger getEncodingOfIndex(int index) throws IllegalArgumentException {
        return curNode.computeOPEEncodingForValue(index);
    }

    public BigInteger getEncodingForIndexBelow(int index) throws IllegalArgumentException {
        return curNode.computeOPEEncodingBelowValue(index);
    }

    public mOPEUpdateSummary<T> insertValue(int index, T value) throws IllegalArgumentException {
        ChangeTracker<T> change = curNode.addKeyAtIndex(index, value);
        return change.getAddUpdateSummary();
    }

    public mOPEUpdateSummary<T> deleteValue(int index) throws IllegalArgumentException {
        ChangeTracker<T> change = curNode.deleteNodetAtIndex(index);
        return change.getDeleteUpdateSummary();
    }
}
