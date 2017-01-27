package mopetree;

import java.math.BigInteger;
import java.util.ArrayList;
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
 * According to Knuth's definition, a B-tree of order m is a tree which satisfies the following properties:
 * Every node has at most m children.
 * Every non-leaf node (except root) has at least ⌈m/2⌉ children.
 * The root has at least two children if it is not a leaf node.
 * A non-leaf node with k children contains k−1 keys.
 * All leaves appear in the same level
 */
public class mOPETreeHelper<T> implements Iterable<T> {

    //mOPE Tree Constants
    public static int NUM_KEYS = 4;
    public static int NUM_CHILDREN = NUM_KEYS + 1;
    public static int MIN_KEYS = NUM_KEYS / 2;

    //mOPE Encoding
    public static final int NUM_BITS = numBits();
    public static final BigInteger UNIT_VAL = BigInteger.valueOf(1 << NUM_BITS);
    public static final BigInteger MAX_INDEX = BigInteger.valueOf((1 << NUM_BITS) - 1);
    private static final BigInteger S_MASK = makeMask();

    /**
     * Computes the number of bits needed for encoding a mOPE Node + path
     *
     * @return
     */
    private static int numBits() {
        double temp = (double) NUM_KEYS + 1;
        temp = Math.log(temp) / Math.log(2);
        return (int) Math.ceil(temp);
    }

    private static BigInteger makeMask() {
        BigInteger curMask = BigInteger.ONE;
        for (int i = 1; i < (int) NUM_BITS; i++) {
            curMask = curMask.or(BigInteger.ONE.shiftLeft(i));
        }
        return curMask;
    }

    private static BigInteger computeOpe(BigInteger ope_path, int nbits) {
        return (ope_path.shiftLeft(64 - nbits)).or(S_MASK.shiftLeft(64 - NUM_BITS - nbits));
    }

    /**
     * Transforms a 64-bit mOPE-Encoding to a OPEEncVector data structure
     *
     * @param encoding
     * @return
     * @throws IllegalArgumentException
     */
    private static OPEEncVector encodingToVector(BigInteger encoding) throws IllegalArgumentException {
        OPEEncVector res = new OPEEncVector();
        BigInteger bEncoding = encoding;
        int numZeroPadding = bEncoding.getLowestSetBit();
        int curNonZeroPadding = 64 - numZeroPadding;
        //clear zeros
        bEncoding = bEncoding.shiftRight(numZeroPadding);
        //clear SMAKS
        if (!((bEncoding.and(S_MASK)).compareTo(S_MASK) == 0))
            throw new IllegalArgumentException("Wrong format " + bEncoding.toString(2));
        bEncoding = bEncoding.shiftRight(NUM_BITS);
        curNonZeroPadding -= NUM_BITS;

        while (curNonZeroPadding != 0) {
            BigInteger curNum = bEncoding.and(S_MASK);
            res.addFirst(curNum.intValue());
            bEncoding = bEncoding.shiftRight(NUM_BITS);
            curNonZeroPadding -= NUM_BITS;
        }

        return res;
    }

    /**
     * Transforms a OPEEncVector to an 64-bit mOPE-Encoding
     *
     * @param vec
     * @return
     */
    private static BigInteger vectorToEncoding(OPEEncVector vec) {
        BigInteger res = BigInteger.ZERO;
        int curBits = 0;
        for (int curVal : vec) {
            res = res.or(BigInteger.valueOf(curVal));
            res = res.shiftLeft(NUM_BITS);
            curBits += NUM_BITS;
        }
        res = res.or(S_MASK);
        res = res.shiftLeft(64 - curBits - NUM_BITS);
        return res;
    }

    /**
     * Compute encoding from OPE Path
     * path|opeIndex|S_MASK| Zero-Pad to 64bit
     *
     * @param state
     * @param opeIndex
     * @return
     */
    public static BigInteger computeOpe(mOPEPath state, int opeIndex) {
        BigInteger opePath = state.opepath;
        int nbits = state.nbits;
        opePath = (opePath.shiftLeft(NUM_BITS)).or(BigInteger.valueOf(opeIndex));
        nbits += NUM_BITS;
        return computeOpe(opePath, nbits);
    }

    @Override
    public Iterator<T> iterator() {
        return getInOrderIterator();
    }

    public mOPEInOrderIterator getInOrderIterator() {
        return new mOPEInOrderIterator();
    }

    protected Iterator<mOPENode<T>> getNodeIterator() {
        return new NodeIterator();
    }

    public mOPETreeWalker<T> getNewWalker() {
        return new mOPETreeWalker<>(rootTracker.getRoot());
    }


    /**
     * Iterates the mOPETree in-order
     */
    public class mOPEInOrderIterator implements Iterator<T> {

        private mOPENode<T> curNode = null;
        private int curIndex = 0;

        private mOPENode<T> getFirst() {
            mOPENode<T> temp = rootTracker.getRoot();
            while (temp.hasChildAtIndex(0)) {
                temp = temp.getNodeAtIndex(0);
            }
            return temp;
        }

        public BigInteger getEncoding() {
            if(curNode==null)
                return BigInteger.ZERO;
            return curNode.computeOPEEncodingForValue(curIndex);
        }

        @Override
        public boolean hasNext() {
            if (curNode == null) {
                mOPENode<T> temp = getFirst();
                return temp.hasKeyAtIndex(0);
            }

            if(curNode.isRoot() && curNode.isLeaf()) {
                return curNode.hasKeyAtIndex(curIndex + 1);
            }

            if (curNode.isLeaf()) {
                if (curNode.hasKeyAtIndex(curIndex + 1)) {
                    return true;
                }
                int tempInd = curNode.parent.getChildIndex(curNode);
                mOPENode<T> temp = curNode.parent;
                ;
                while (!temp.equals(rootTracker.getRoot())) {
                    if (temp.hasKeyAtIndex(tempInd)) {
                        return true;
                    }
                    tempInd = temp.parent.getChildIndex(temp);
                    temp = temp.parent;
                }
                return temp.hasKeyAtIndex(tempInd);

            }

            return true;
        }

        @Override
        public T next() {
            if (!hasNext()) {
                return null;
            }

            if (curNode == null) {
                mOPENode<T> temp = getFirst();
                return setNext(temp, 0);
            }

            if(curNode.isRoot() && curNode.isLeaf()) {
                return setNext(curNode, curIndex + 1);
            }

            if (curNode.isLeaf()) {
                if (curNode.hasKeyAtIndex(curIndex + 1)) {
                    return setNext(curNode, curIndex + 1);
                }
                int tempInd = curNode.parent.getChildIndex(curNode);
                mOPENode<T> temp = curNode.parent;
                while (!temp.equals(rootTracker.getRoot())) {
                    if (temp.hasKeyAtIndex(tempInd)) {
                        return setNext(temp, tempInd);
                    }
                    tempInd = temp.parent.getChildIndex(temp);
                    temp = temp.parent;
                }
                return setNext(temp, tempInd);
            } else {
                mOPENode<T> temp = curNode.getNodeAtIndex(curIndex + 1);
                while (!temp.isLeaf())
                    temp = temp.getNodeAtIndex(0);
                return setNext(temp, 0);
            }
        }

        private T setNext(mOPENode<T> node, int index) {
            this.curNode = node;
            this.curIndex = index;
            return node.getKeyAtIndex(index);
        }

        @Override
        public void remove() {
            // not supported
        }
    }


    private class NodeIterator implements Iterator<mOPENode<T>> {

        private Iterator<mOPENode<T>> curLevel;
        private ArrayList<mOPENode<T>> nextLevel;

        public NodeIterator() {
            ArrayList<mOPENode<T>> temp = new ArrayList<>();
            temp.add(rootTracker.getRoot());
            curLevel = temp.iterator();
            nextLevel = new ArrayList<>();
        }

        @Override
        public boolean hasNext() {
            return curLevel.hasNext() || !nextLevel.isEmpty();
        }

        @Override
        public mOPENode<T> next() {
            if(curLevel.hasNext()) {
                mOPENode<T> curNode = curLevel.next();
                nextLevel.addAll(curNode.childs);
                return curNode;
            } else if (!nextLevel.isEmpty()) {
                curLevel = nextLevel.iterator();
                nextLevel = new ArrayList<>();
                return next();
            }
            return null;
        }

        @Override
        public void remove() {

        }
    }

    /**
     * Represents a path in the Tree, the last index is the index of the key in the node.
     */
    private static class OPEEncVector extends ArrayList<Integer> {

        private static final long serialVersionUID = 1L;

        public OPEEncVector() {
            super();
        }

        public OPEEncVector(int size) {
            super(size);
        }

        public void addFirst(int value) {
            this.add(0, value);
        }

        public void increment(int index) {
            int x = this.get(index);
            this.remove(index);
            x++;
            this.add(index, x);
        }

        public void decrement(int index) {
            int x = this.get(index);
            this.remove(index);
            x--;
            this.add(index, x);
        }

        public void removeLast() {
            this.remove(this.size() - 1);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (int i : this) {
                sb.append(i);
                sb.append("->");
            }
            sb.setLength(sb.length() - 2);
            return sb.toString();
        }
    }

    /**
     * Represents a part of the mOPEPath.
     * This data-structure is needed for computing the path recursively from a Node.
     */
    private static class mOPEPath {
        public int nbits;
        public BigInteger opepath;

        public mOPEPath(int nbits, BigInteger opepath) {
            this.nbits = nbits;
            this.opepath = opepath;
        }

        public mOPEPath clone() {
            return new mOPEPath(nbits, opepath);
        }
    }

    //mOPE Helper

    /**
     * Keeps track of the root node.
     */
    private RootTracker rootTracker;

    public mOPETreeHelper() {
        rootTracker = new RootTracker<>();
        rootTracker.setRoot(new mOPENode<T>(rootTracker));
    }

    /**
     * Returns the root of the tree.
     *
     * @return
     */
    public mOPENode<T> getRootOfTree() {
        return rootTracker.getRoot();
    }


    /**
     * This method is used for constructing the tree from set of values with their mOPE encoding.
     * Ex: Restore tree from database
     *
     * @param encodings List of mOPE encodings and their corresponding value
     */
    public void constructTreeFromEncodings(List<Tuple<BigInteger, T>> encodings) {
        for (Tuple<BigInteger, T> curEncoding : encodings) {
            constructTreeFromEncodingsStep(curEncoding);
        }
    }

    public void constructTreeFromEncodingsStep(Tuple<BigInteger, T> curEncoding) {
        BigInteger encoding = curEncoding.a;
        mOPENode<T> curNode = rootTracker.getRoot();
        OPEEncVector curVec = encodingToVector(encoding);
        for (int i = 0; i < curVec.size() - 1; i++) {
            int curIndex = curVec.get(i);
            if (!curNode.hasChildAtIndex(curIndex)) {
                for (int fillInd = curNode.getChildsSize(); fillInd <= curIndex; fillInd++) {
                    mOPENode<T> temp = new mOPENode<T>(curNode, rootTracker);
                    curNode.addChild(temp);
                }
            }
            curNode = curNode.getNodeAtIndex(curIndex);
        }
        int postion = curVec.get(curVec.size() - 1);
        if (!curNode.hasKeyAtIndex(postion)) {
            for (int fillInd = curNode.getKeysSize(); fillInd <= postion; fillInd++) {
                curNode.addKey(null);
            }
        }

        curNode.removeKeyAtIndex(postion);
        curNode.addKeyIndex(curEncoding.b, postion);
    }

    /**
     * Transforms the tree structure to a readable String
     * (Debugging)
     *
     * @return
     */
    public String getTreeString() {
        ArrayList<mOPENode<T>> curNodes = new ArrayList<>();
        ArrayList<mOPENode<T>> belowNNodes = new ArrayList<>();
        curNodes.add(rootTracker.getRoot());
        StringBuilder sb = new StringBuilder();

        while (!curNodes.isEmpty()) {
            for (mOPENode<T> node : curNodes) {
                sb.append(node.getNodeRepresentation());
                sb.append(" ");
                belowNNodes.addAll(node.getChildren());
            }
            sb.append("\n");
            curNodes = belowNNodes;
            belowNNodes = new ArrayList<>();
        }

        return sb.toString();
    }

    /**
     * Transforms the tree structure to a readable String including the corresponding mOPEEncoding
     * (Debugging)
     *
     * @return
     */
    public String getmOPETreeString() {
        ArrayList<mOPENode<T>> curNodes = new ArrayList<>();
        ArrayList<mOPENode<T>> belowNNodes = new ArrayList<>();
        curNodes.add(rootTracker.getRoot());
        StringBuilder sb = new StringBuilder();

        while (!curNodes.isEmpty()) {
            for (mOPENode<T> node : curNodes) {
                sb.append(node.getmOPENodeRepresentation());
                sb.append(" ");
                belowNNodes.addAll(node.getChildren());
            }
            sb.append("\n");
            curNodes = belowNNodes;
            belowNNodes = new ArrayList<>();
        }

        return sb.toString();
    }

    /**
     * Returns all Encoding/Value pairs in the tree.
     *
     * @return
     */
    public List<Tuple<BigInteger, T>> getEncodingsWithValues() {
        return getEncodingsWithValues(rootTracker.getRoot());
    }

    public List<Tuple<BigInteger, T>> getEncodingsWithValues(mOPENode<T> startNode) {
        List<Tuple<BigInteger, T>> res = new ArrayList<>();
        mOPEInOrderIterator iter = new mOPEInOrderIterator();
        while (iter.hasNext()) {
            T temp = iter.next();
            res.add(new Tuple<BigInteger, T>(iter.getEncoding(), temp));
        }
        return res;
    }

    /**
     * Keeps Track of the mOPE tree root
     *
     * @param <T>
     */
    private static class RootTracker<T> {

        private mOPENode<T> root;

        public RootTracker() {
        }

        public RootTracker(mOPENode<T> root) {
            this.root = root;
        }

        public mOPENode<T> getRoot() {
            return root;
        }

        public void setRoot(mOPENode<T> root) {
            this.root = root;
        }
    }


    /**
     * Represents a Node in the mOPE Tree
     */
    public static class mOPENode<T> {

        /**
         * Parent node
         */
        private mOPENode<T> parent = null;

        /**
         * Keeps track of the root Node
         */
        private RootTracker<T> rootTracker;

        /**
         * Current keys in this node
         */
        private ArrayList<T> keys = new ArrayList<>(NUM_KEYS);

        /**
         * Child nodes of this node.
         * Should contain keys.size()+1 childs, if it is not a leaf
         */
        private ArrayList<mOPENode<T>> childs = new ArrayList<>(NUM_CHILDREN);

        mOPENode(RootTracker<T> rootTracker) {
            this.rootTracker = rootTracker;
        }

        mOPENode(mOPENode<T> parent, RootTracker<T> rootTracker) {
            this.parent = parent;
            this.rootTracker = rootTracker;
        }

        /**
         * Adds the key at a specific index, therefore, it should be called on a leafnode after a mOPE client interaction.
         * Also performs the refinancing, and returns a summary of the changed nodes.
         *
         * @param keyIndex insertion index (0,1,2,3..)
         * @param key      the key to inser
         * @return summary of the updated nodes
         * @throws IllegalArgumentException if the index is invalid
         */
        public ChangeTracker<T> addKeyAtIndex(int keyIndex, T key) throws IllegalArgumentException {
            if (!(checkKeyIndexValid(keyIndex)))
                throw new IllegalArgumentException("Wrong Index: " + keyIndex);
            ChangeTracker<T> change = new ChangeTracker<>();
            keys.add(keyIndex, key);
            if (keys.size() > NUM_KEYS) {
                splitNode(change);
            } else {
                change.pushNode(this, keyIndex);
            }
            return change;
        }

        /**
         * Helper method for recursively splitting the nodes.
         *
         * @param key
         * @param oldChild
         * @param newleftChild
         * @param newrightChild
         * @param change
         * @throws IllegalArgumentException
         */
        protected void addKeyFromBelow(T key, mOPENode<T> oldChild, mOPENode<T> newleftChild, mOPENode<T> newrightChild, ChangeTracker<T> change) throws IllegalArgumentException {
            int keyIndex = getChildIndex(oldChild);
            keys.add(keyIndex, key);
            childs.remove(oldChild);
            childs.add(keyIndex, newleftChild);
            childs.add(keyIndex + 1, newrightChild);
            if (keys.size() > NUM_KEYS) {
                splitNode(change);
            } else {
                change.pushNode(this, keyIndex);
            }
        }

        /**
         * Deletes the node at the given index and performs the
         * tree rebalancing (rotate right/left, merge right/left)
         * @param index the index of the node to delete
         * @return A summary of the changed nodes
         * @throws IllegalArgumentException
         */
        public ChangeTracker<T> deleteNodetAtIndex(int index) throws IllegalArgumentException {
            if (!(checkKeyIndexValid(index)))
                throw new IllegalArgumentException("Wrong Index: " + index);

            ChangeTracker<T> change = new ChangeTracker<>();
            if (this.isLeaf()) {
                change.pushNode(this, index, ChangeTracker.DELETE);
                this.keys.remove(index);
                this.rebalanceTree(change);
            } else {
                mOPENode<T> curNode = this.childs.get(index+1);
                while (!curNode.isLeaf()) {
                    curNode = curNode.getNodeAtIndex(0);
                }
                keys.remove(index);
                keys.add(index, curNode.getKeyAtIndex(0));
                curNode.keys.remove(0);
                change.pushNode(this, index, ChangeTracker.CHANGE);
                change.pushNode(curNode, 0, ChangeTracker.DELETE);
                curNode.rebalanceTree(change);
            }
            return change;
        }

        /**
         * Returns all the keys in a new List (Attention: Aliasing keys)
         *
         * @return
         */
        public List<T> getKeys() {
            ArrayList<T> res = new ArrayList<>(keys.size());
            for (T t : keys)
                res.add(t);
            return res;
        }

        /**
         * Helper-method for tree construction.
         * Careful can break Invariant
         *
         * @param e
         */
        private void addKey(T e) {
            keys.add(e);
        }

        private void addKeyIndex(T e, int index) {
            keys.add(index, e);
        }

        private void removeKeyAtIndex(int index) {
            keys.remove(index);
        }

        /**
         * Helper-method for tree construction.
         * Careful can break Invariant
         *
         * @param e
         */
        private void addChild(mOPENode<T> e) {
            childs.add(e);
            e.parent = this;
        }

        /**
         * Helper-method for tree construction.
         * Careful can break Invariant
         *
         * @param e
         */
        private void addChild(mOPENode<T> e, int index) {
            childs.add(index, e);
            e.parent = this;
        }

        /**
         * Retruns the child node at the given index.
         *
         * @param index
         * @return Child-Node
         * @throws IllegalArgumentException, if index is invalid
         */
        public mOPENode<T> getNodeAtIndex(int index) throws IllegalArgumentException {
            if (!(hasChildAtIndex(index)))
                throw new IllegalArgumentException("Wrong Index: " + index);
            return childs.get(index);
        }

        public boolean containsKey(T key) {
            return this.keys.contains(key);
        }

        public int getKeyIndex(T key) {
            for(int index=0; index<keys.size(); index++) {
                if(keys.get(index).equals(key))
                    return index;
            }
            return -1;
        }

        public int getHeight() {
            if(isRoot())
                return 0;
            else
                return parent.getHeight()+1;
        }

        /**
         * Returns the key at the given index.
         *
         * @param index
         * @return key
         * @throws IllegalArgumentException, if index is invalid
         */
        public T getKeyAtIndex(int index) throws IllegalArgumentException {
            if (!(hasKeyAtIndex(index)))
                throw new IllegalArgumentException("Wrong Index: " + index);
            return keys.get(index);
        }

        public boolean isValid() {
            return childs!=null;
        }

        /**
         * Return true if this node is a leaf node.
         *
         * @return
         */
        public boolean isLeaf() {
            return childs.isEmpty();
        }

        /**
         * Returns true if this node is the root node of the tree.
         *
         * @return
         */
        public boolean isRoot() {
            return parent == null;
        }

        /**
         * Returns the number of keys in this node.
         *
         * @return
         */
        public int getKeysSize() {
            return this.keys.size();
        }

        /**
         * Returns the number of child nodes in this node.
         *
         * @return
         */
        public int getChildsSize() {
            return this.childs.size();
        }

        private boolean checkKeyIndexValid(int index) {
            return index >= 0 && index <= keys.size();
        }

        private boolean hasKeyAtIndex(int index) {
            return (!keys.isEmpty()) && index >= 0 && index < keys.size();
        }

        private boolean checkChildIndexValid(int index) {
            return index >= 0 && index <= childs.size();
        }

        private boolean hasChildAtIndex(int index) {
            return (!childs.isEmpty()) && index >= 0 && index < childs.size();
        }

        private mOPENode<T> hasLeftSibling() {
            if(this.parent==null)
                return null;
            int index = this.parent.getChildIndex(this);
            if(index>0) {
                return this.parent.getNodeAtIndex(index-1);
            }
            return null;
        }

        private mOPENode<T> hasRigthSibling() {
            if(this.parent==null)
                return null;
            int index = this.parent.getChildIndex(this);
            if(this.parent.hasChildAtIndex(index+1)) {
                return this.parent.getNodeAtIndex(index+1);
            }
            return null;
        }

        private void erase() {
            this.keys = null;
            this.childs = null;
            this.parent = null;
        }


        /**
         * Helper method for splitting a node after an overflow in insertion.
         *
         * @param change
         */
        protected void splitNode(ChangeTracker<T> change) {
            int medianIndex = computeMedian(keys.size());
            T median = keys.get(medianIndex);

            mOPENode<T> nodeL, nodeR;

            nodeL = new mOPENode<T>(rootTracker);
            for (int i = 0; i < medianIndex; i++) {
                nodeL.addKey(keys.get(i));
                if (!isLeaf()) {
                    nodeL.addChild(childs.get(i));
                }
            }
            if (!isLeaf()) {
                nodeL.addChild(childs.get(medianIndex));
            }
            nodeL.parent = parent;

            nodeR = new mOPENode<T>(rootTracker);
            for (int i = medianIndex + 1; i < keys.size(); i++) {
                nodeR.addKey(keys.get(i));
                if (!isLeaf()) {
                    nodeR.addChild(childs.get(i));
                }
            }
            if (!isLeaf()) {
                nodeR.addChild(childs.get(keys.size()));
            }
            nodeR.parent = parent;

            change.pushNode(nodeL);
            change.pushNode(nodeR);

            if (isRoot()) {
                mOPENode<T> newRoot = new mOPENode<T>(rootTracker);
                newRoot.addChild(nodeL);
                newRoot.addChild(nodeR);
                newRoot.addKey(median);
                rootTracker.setRoot(newRoot);
                change.pushNode(newRoot, 0);
            } else {
                parent.addKeyFromBelow(median, this, nodeL, nodeR, change);
            }
        }

        protected void rebalanceTree(ChangeTracker<T> tracker) {
            if(!this.isRoot() && this.getKeysSize()<MIN_KEYS) {
                mOPENode<T> left = hasLeftSibling();
                if(left!=null && left.getKeysSize()>MIN_KEYS) {
                    rotateRight(tracker);
                    return;
                }

                mOPENode<T> right = hasRigthSibling();
                if(right!=null && right.getKeysSize()>MIN_KEYS) {
                    rotateLeft(tracker);
                    return;
                }

                if(left!=null) {
                    mergeLeft(tracker);
                    return;
                }

                if(right!=null) {
                    mergeRight(tracker);
                }
            }
        }

        protected void rotateLeft(ChangeTracker<T> tracker) throws RuntimeException {
            mOPENode<T> right = hasRigthSibling();
            if(right==null || !(right.getKeysSize()>MIN_KEYS))
                throw new RuntimeException("Rotate Left breaks invariant");
            int index = parent.getChildIndex(this);
            this.addKey(parent.getKeyAtIndex(index));
            parent.keys.remove(index);
            parent.keys.add(index,right.getKeyAtIndex(0));
            right.keys.remove(0);
            tracker.pushNode(right,0,ChangeTracker.DELETE);
            tracker.pushNode(parent,index,ChangeTracker.CHANGE);
            if(!isLeaf()) {
                mOPENode<T> change = right.getNodeAtIndex(0);
                this.childs.add(change);
                change.parent = this;
                right.childs.remove(0);
            }
        }

        protected void rotateRight(ChangeTracker<T> tracker) throws RuntimeException {
            mOPENode<T> left = hasLeftSibling();
            if(left==null || !(left.getKeysSize()>MIN_KEYS))
                throw new RuntimeException("Rotate Right breaks invariant");
            int index = parent.getChildIndex(left);
            this.keys.add(0,parent.getKeyAtIndex(index));
            parent.keys.remove(index);
            int removeInd = left.getKeysSize()-1;
            parent.keys.add(index, left.getKeyAtIndex(removeInd));
            left.keys.remove(removeInd);
            tracker.pushNode(left,removeInd,ChangeTracker.DELETE);
            tracker.pushNode(parent,index,ChangeTracker.CHANGE);
            if(!isLeaf()) {
                mOPENode<T> change = left.getNodeAtIndex(left.getChildsSize() - 1);
                this.childs.add(0, change);
                change.parent = this;
                left.childs.remove(left.getChildsSize() - 1);
            }
        }

        protected void mergeLeft(ChangeTracker<T> tracker) throws RuntimeException {
            mOPENode<T> mergeNode = hasLeftSibling();
            if(mergeNode==null || parent==null)
                throw new RuntimeException("Merge breaks invariant");
            int index = parent.getChildIndex(mergeNode);
            mergeNode.addKey(parent.getKeyAtIndex(index));
            mergeNode.keys.addAll(this.keys);
            mergeNode.childs.addAll(this.childs);
            for(mOPENode<T> child : this.childs) {
                child.parent = mergeNode;
            }
            parent.keys.remove(index);
            parent.childs.remove(index+1);
            tracker.pushNode(mergeNode,0,ChangeTracker.ADD);
            if(parent.isRoot() && parent.getKeysSize()<1) {
                rootTracker.setRoot(mergeNode);
                mergeNode.parent = null;
            } else {
                tracker.pushNode(parent,index,ChangeTracker.ADD);
                parent.rebalanceTree(tracker);
            }
            this.erase();
        }

        protected void mergeRight(ChangeTracker<T> tracker) throws RuntimeException {
            mOPENode<T> mergeNode = hasRigthSibling();
            if(mergeNode==null || parent==null)
                throw new RuntimeException("Merge breaks invariant");
            int index = parent.getChildIndex(this);
            this.addKey(parent.getKeyAtIndex(index));
            this.keys.addAll(mergeNode.keys);
            this.childs.addAll(mergeNode.childs);
            for(mOPENode<T> child : mergeNode.childs) {
                child.parent = this;
            }
            parent.keys.remove(index);
            parent.childs.remove(index+1);
            tracker.pushNode(this,0,ChangeTracker.ADD);
            if(parent.isRoot() && parent.getKeysSize()<1) {
                rootTracker.setRoot(this);
                this.parent = null;
            } else {
                tracker.pushNode(parent,index,ChangeTracker.ADD);
                parent.rebalanceTree(tracker);
            }
            mergeNode.erase();
        }

        /**
         * Helper method for identifing the child index of the given node.
         *
         * @param node
         * @return the index, if node is a valid child, else -1
         */
        private int getChildIndex(mOPENode<T> node) {
            int index = 0;
            for (mOPENode<T> n : childs) {
                if (n.equals(node))
                    return index;
                index++;
            }
            assert (false);
            return -1;
        }

        private int computeMedian(int size) {
            return size / 2;
        }

        public boolean checkInvariantHolds() {
            if(isRoot())
                return (keys.isEmpty() || isLeaf() || keys.size()+1 == childs.size()) && rootTracker.getRoot().equals(this);
            return (isLeaf() || keys.size()+1 == childs.size()) && keys.size()>=MIN_KEYS && keys.size()<=NUM_KEYS;
        }

        /**
         * Attention leaking (Aliasing)!!
         * Returns the children list, no copy
         *
         * @return
         */
        ArrayList<mOPENode<T>> getChildren() {
            return childs;
        }

        public String getNodeRepresentation() {
            StringBuilder sb = new StringBuilder();
            String delim = "|";
            sb.append(delim);
            for (T t : keys) {
                sb.append(t)
                        .append(delim);
            }
            if (keys.isEmpty())
                sb.append("-").append(delim);
            return sb.toString();
        }

        /**
         * Transforms the node to a readable string.
         *
         * @return
         */
        public String getmOPENodeRepresentation() {
            StringBuilder sb = new StringBuilder();
            String delim = "|";
            sb.append(delim);
            mOPEPath path = computeOPEPath();

            for (int index = 0; index < keys.size(); index++) {
                BigInteger mOPEEc = computeOpe(path, index);
                sb.append(keys.get(index)).append("::").append(mOPEEc.toString(10)).append("::").append(encodingToVector(mOPEEc))
                        .append(delim);
            }
            if (keys.isEmpty())
                sb.append("-").append(delim);
            return sb.toString();
        }

        /**
         * Returns all the keys with their encoding in this node
         *
         * @return
         */
        public List<Tuple<BigInteger, T>> getEncodingsToValues() {
            List<Tuple<BigInteger, T>> res = new ArrayList<>();
            mOPEPath myPath = computeOPEPath();
            for (int keyInd = 0; keyInd < keys.size(); keyInd++) {
                res.add(new Tuple<>(computeOpe(myPath, keyInd), keys.get(keyInd)));
            }
            return res;
        }

        //mOPE Encoding
        private void getOpePath(mOPENode<T> n, mOPEPath state) {
            if (n.parent != null) {
                getOpePath(n.parent, state);
                state.nbits = state.nbits + NUM_BITS;
                state.opepath = (state.opepath.shiftLeft(NUM_BITS)).add(BigInteger.valueOf(n.parent.getChildIndex(n)));
            } else {
                state.nbits = 0;
                state.opepath = BigInteger.ZERO;
            }
        }

        /**
         * Computes the mOPEPath for reaching this node.
         *
         * @return
         */
        public mOPEPath computeOPEPath() {
            mOPEPath state = new mOPEPath(0, BigInteger.ZERO);
            getOpePath(this, state);
            return state;
        }

        /**
         * Computes the mOPEEncodig for the value at the index
         *
         * @return
         */
        public BigInteger computeOPEEncodingForValue(int index) throws IllegalArgumentException {
            if(!hasKeyAtIndex(index))
                throw new IllegalArgumentException("Wrong Index: "+index);
            return computeOpe(computeOPEPath(), index);
        }

        public BigInteger computeOPEEncodingBelowValue(int index) throws IllegalArgumentException {
            if(index<0 || index >= keys.size()+1)
                throw new IllegalArgumentException("Wrong Index: "+index);
            mOPEPath path = computeOPEPath();
            path.nbits = path.nbits + NUM_BITS;
            path.opepath = (path.opepath.shiftLeft(NUM_BITS)).add(BigInteger.valueOf(index));
            return computeOpe(path, 0);
        }

        /**
         * Computes the mOPEPath for reaching this node,
         * based on the previous computed path of the parent.
         *
         * @return
         */
        public mOPEPath updateOPEPath(mOPEPath parentPath) {
            mOPEPath res = parentPath.clone();
            res.nbits += NUM_BITS;
            res.opepath = (res.opepath.shiftLeft(NUM_BITS))
                    .add(BigInteger.valueOf(this.parent.getChildIndex(this)));
            return res;
        }


    }

}
