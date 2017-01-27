package mopetree;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
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
 * Keeps track of the tree changes in the tree with balancing
 */
public class ChangeTracker<T> {

    public static int DELETE = 1;
    public static int ADD = 2;
    public static int CHANGE = 3;

    private ArrayList<mOPETreeHelper.mOPENode<T>> changes = new ArrayList<>();
    private HashMap<mOPETreeHelper.mOPENode<T>, Integer> indexMap = new HashMap<>();

    private HashMap<mOPETreeHelper.mOPENode<T>, Integer> opMap = new HashMap<>();

    public void pushNode(mOPETreeHelper.mOPENode<T> node) {
        changes.add(node);
    }

    public void pushNode(mOPETreeHelper.mOPENode<T> node, int index) {
        pushNode(node);
        indexMap.put(node, index);
    }

    public void pushNode(mOPETreeHelper.mOPENode<T> node, int index, int op) {
        pushNode(node);
        indexMap.put(node, index);
        opMap.put(node,op);
    }

    public mOPETreeHelper.mOPENode<T> getLastChange() {
        if(changes.isEmpty())
            return null;
        return changes.get(changes.size()-1);
    }

    public int getNumChanges() {
        return  changes.size();
    }

    public mOPETreeHelper.mOPENode<T> getNodeAt(int index) {
        return changes.get(index);
    }

    public mOPETreeHelper.mOPENode<T> getHighestNode() {
        mOPETreeHelper.mOPENode<T> res, temp;
        res = null;
        int minHeigt = Integer.MAX_VALUE;

        for(int i=0; i < changes.size(); i++) {
            temp = changes.get(i);
            int tempInt = temp.getHeight();
            if(minHeigt>tempInt && temp.isValid()) {
                res = temp;
                minHeigt = tempInt;
            }
        }
        return res;
    }

    public int getBiggestHeight() {
        int big = -1;
        for(mOPETreeHelper.mOPENode<T> node : changes) {
            if(node.isValid()) {
                int height = node.getHeight();
                if(height>big) {
                    big = height;
                }
            }
        }
        return big;
    }

    public mOPEUpdateSummary<T> getAddUpdateSummary() {
        mOPEUpdateSummary<T> res = new  mOPEUpdateSummary<>();
        mOPETreeHelper.mOPENode<T> node = getHighestNode();
        int index;
        if(indexMap.containsKey(node)) {
            index = indexMap.get(node);
        } else {
            index = 0;
        }

        for (int ind = index; ind < node.getChildsSize(); ind++) {
            addAllUpdates(node.getNodeAtIndex(ind), res);
        }

        for(int ind=index; ind < node.getKeysSize(); ind++) {
            res.pushUpdate(node.getKeyAtIndex(ind), node.computeOPEEncodingForValue(ind));
        }
        return res;
    }

    public mOPEUpdateSummary<T> getDeleteUpdateSummary() {
        mOPEUpdateSummary<T> res = new  mOPEUpdateSummary<>();
        mOPETreeHelper.mOPENode<T> node = getHighestNode();
        int index, op;
        if(indexMap.containsKey(node)) {
            index = indexMap.get(node);
        } else {
            index = 0;
        }
        if(opMap.containsKey(node)) {
            op = opMap.get(node);
        } else  {
            op = ADD;
        }
        int numOccurences = 0;
        for( mOPETreeHelper.mOPENode<T> temp : changes) {
            if(temp.equals(node))
                numOccurences++;
        }

        if(op == DELETE) {
            for (int ind = index; ind < node.getKeysSize(); ind++) {
                res.pushUpdate(node.getKeyAtIndex(ind), node.computeOPEEncodingForValue(ind));
            }
        } else {
            for (int ind = index; ind < node.getChildsSize(); ind++) {
                addAllUpdates(node.getNodeAtIndex(ind), res);
            }

            if(numOccurences>1) {
                index = 0;
            }

            for(int ind=index; ind < node.getKeysSize(); ind++) {
                res.pushUpdate(node.getKeyAtIndex(ind), node.computeOPEEncodingForValue(ind));
            }
        }
        return res;
    }

    private void addAllUpdates(mOPETreeHelper.mOPENode<T> startNode, mOPEUpdateSummary<T> sum) {
        ArrayList<mOPETreeHelper.mOPENode<T>> curNodes = new ArrayList<>();
        ArrayList<mOPETreeHelper.mOPENode<T>> belowNNodes = new ArrayList<>();
        curNodes.add(startNode);
        while (!curNodes.isEmpty()) {
            for (mOPETreeHelper.mOPENode<T> node : curNodes) {
                List<Tuple<BigInteger, T>> temp = node.getEncodingsToValues();
                belowNNodes.addAll(node.getChildren());
                for(Tuple<BigInteger, T> mapping : temp) {
                    sum.pushUpdate(mapping.b, mapping.a);
                }
            }
            curNodes = belowNNodes;
            belowNNodes = new ArrayList<>();
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for(int i = changes.size()-1; i>=0; i--) {
            sb.append(" Change"+i+":");
            sb.append(changes.get(i).getNodeRepresentation());
            if(indexMap.containsKey(changes.get(i))) {
                sb.append("->").append(indexMap.get(changes.get(i)));
            }
        }
        return sb.toString();
    }

}
