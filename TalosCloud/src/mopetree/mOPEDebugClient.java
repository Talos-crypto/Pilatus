package mopetree;

import java.math.BigInteger;
import java.util.Iterator;
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
 * Created by lukas on 17.12.15.
 * Helper class for testing the mOPE-Tree (B-Tree).
 * Imitates a mOPE client interaction.
 */
public class mOPEDebugClient<T extends Comparable<T>> {

    private mOPETreeHelper<T> treeHelper;

    public mOPEDebugClient() {
        treeHelper = new mOPETreeHelper<>();
    }

    public mOPEDebugClient(List<Tuple<BigInteger, T>> encs) {
        treeHelper = new mOPETreeHelper<>();
        treeHelper.constructTreeFromEncodings(encs);
    }

    public ChangeTracker<T> addKey(T key) {
        mOPETreeHelper.mOPENode<T> curNode = treeHelper.getRootOfTree();
        while (!curNode.isLeaf()) {
            int index = selectNode(curNode, key);
            curNode = curNode.getNodeAtIndex(index);
        }
        return curNode.addKeyAtIndex(selectNode(curNode, key), key);
    }

    public ChangeTracker<T> deleteKey(T key) {
        mOPETreeHelper.mOPENode<T> curNode = treeHelper.getRootOfTree();
        boolean found = false;
        while (!curNode.isLeaf() && !found) {
            if(curNode.containsKey(key)) {
                found=true;
                break;
            }
            int index = selectNode(curNode, key);
            curNode = curNode.getNodeAtIndex(index);
        }
        if(curNode.containsKey(key) || found) {
            return curNode.deleteNodetAtIndex(curNode.getKeyIndex(key));
        } else {
            return null;
        }
    }

    private int selectNode(mOPETreeHelper.mOPENode<T> curNode, T key) {
        List<T> keys = curNode.getKeys();
        int index = 0;
        for (T value : keys) {
            if (value.compareTo(key) >= 0) {
                break;
            }
            index++;
        }
        return index;
    }

    public boolean checkInvariant() {
        Iterator<mOPETreeHelper.mOPENode<T>> iter = treeHelper.getNodeIterator();
        while (iter.hasNext()) {
            mOPETreeHelper.mOPENode<T> curNode = iter.next();
            if(!curNode.checkInvariantHolds())
                return false;
        }
        return true;
    }

    public mOPETreeHelper<T> getTreeHelper() {
        return treeHelper;
    }

    public boolean checkEncodingsValid() {
        List<Tuple<BigInteger, T>> encodigs = treeHelper.getEncodingsWithValues();
        for (Tuple<BigInteger, T> curEnc : encodigs) {
            for (Tuple<BigInteger, T> otherEnc : encodigs) {
                if (curEnc.b.compareTo(otherEnc.b) > 0) {
                    BigInteger cur, other;
                    cur = curEnc.a;
                    other = otherEnc.a;
                    if (cur.compareTo(other) <= 0) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public boolean checkTreeValid() {
        Iterator<T> iter = treeHelper.getInOrderIterator();
        T curKey, nextKey;
        if (iter.hasNext()) {
            curKey = iter.next();
        } else {
            return true;
        }

        while (iter.hasNext()) {
            nextKey = iter.next();
            if (nextKey.compareTo(curKey) < 0) {
                return false;
            }
            curKey = nextKey;
        }
        return true;
    }

    public String getSortedValues() {
        StringBuilder sb = new StringBuilder();
        Iterator<T> iter = treeHelper.getInOrderIterator();
        while (iter.hasNext()) {
            sb.append(iter.next()).append(", ");
        }
        if (sb.length() > 0)
            sb.setLength(sb.length() - 2);
        return sb.toString();
    }

    public String printTree() {
        return treeHelper.getTreeString();
    }

    public String printEncodingTree() {
        return treeHelper.getmOPETreeString();
    }

    public List<Tuple<BigInteger, T>> getEncodingsWithValues() {
        return treeHelper.getEncodingsWithValues();
    }


}
