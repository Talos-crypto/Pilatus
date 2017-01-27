package testmope;

import mopetree.ChangeTracker;
import mopetree.Tuple;
import mopetree.mOPEDebugClient;
import mopetree.mOPEUpdateSummary;
import org.junit.Test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.fail;

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
 * Created by lukas on 21.12.15.
 */
public class TestmOPETree {

    Random rand = new Random();

    private final static int NUM_VALUES_IN_TREE = 1000;
    private final static int MAX_ITER = 100;
    private final static boolean LOG = true;

    @Test
    public void checkTreeBuilder() {
        for(int rounds = 0; rounds<MAX_ITER; rounds++) {
            mOPEDebugClient<Integer> client = new mOPEDebugClient<>();

            ChangeTracker<Integer> tracker = null;
            for (int i = 1; i <= NUM_VALUES_IN_TREE; i++) {
                client.addKey(rand.nextInt());
            }
            List<Tuple<BigInteger, Integer>> encs = client.getEncodingsWithValues();
            mOPEDebugClient<Integer> client2 = new mOPEDebugClient<>(encs);
            logLineToConsole(client.printEncodingTree());
            logLineToConsole(client2.printEncodingTree());
            assertEquals(client.printEncodingTree(), client2.printEncodingTree());
        }
    }

    @Test
    public void checkTreeDelete() {
        for(int rounds = 0; rounds<MAX_ITER; rounds++) {
            mOPEDebugClient<Integer> client = new mOPEDebugClient<>();
            ChangeTracker<Integer> tracker = null;
            ArrayList<Integer> nums = new ArrayList<>();
            for (int i = 1; i <= NUM_VALUES_IN_TREE; i++) {
                int x = rand.nextInt();
                client.addKey(x);
                nums.add(x);
            }
            logLineToConsole(client.printTree());
            while (nums.size()>0) {
                int curInd = rand.nextInt(nums.size());
                int toRemove = nums.get(curInd);
                nums.remove(curInd);
                logLineToConsole("ToRemove: " + toRemove);
                client.deleteKey(toRemove);
                logLineToConsole(client.printTree());
                assertTrue(client.checkInvariant());
                assertTrue(client.checkTreeValid());
            }

        }
    }

    @Test
    public void inOrderIter() {
        for(int rounds = 0; rounds<MAX_ITER; rounds++) {
            ArrayList<Integer> vals = new ArrayList<>();
            mOPEDebugClient<Integer> client = new mOPEDebugClient<>();
            ChangeTracker<Integer> tracker = null;
            for (int i = 1; i <= NUM_VALUES_IN_TREE; i++) {
                int curRnd = rand.nextInt(100);
                int num = 0;
                vals.add(curRnd);
                client.addKey(curRnd);
                for(Integer tmp : vals)
                    logToConsole(tmp +", ");
                logLineToConsole();

                for(Integer cur : client.getTreeHelper()) {
                    logToConsole(cur +", ");
                    if(!vals.contains(cur)) {
                        logLineToConsole(cur.toString());
                        assertTrue(false);
                    }
                    assertTrue(vals.contains(cur));
                    num++;
                }
                logLineToConsole();
                logLineToConsole(client.printTree());
                assertEquals(vals.size(),num);

            }
        }
    }
    @Test
    public void checkTreeAdd() {
        for(int rounds = 0; rounds<MAX_ITER; rounds++) {
            mOPEDebugClient<Integer> client = new mOPEDebugClient<>();
            ChangeTracker<Integer> tracker = null;
            for (int i = 1; i <= NUM_VALUES_IN_TREE; i++) {
                client.addKey(rand.nextInt());
                //logLineToConsole(client.printEncodingTree());
            }
            assertTrue(client.checkTreeValid());
            assertTrue(client.checkInvariant());
        }
    }

    @Test
    public void checkEncodings() {
        mOPEDebugClient<Integer> client = new mOPEDebugClient<>();
        ChangeTracker<Integer> tracker = null;
        for (int i = 1; i <= NUM_VALUES_IN_TREE; i++) {
            client.addKey(rand.nextInt());
        }
        assertTrue(client.checkEncodingsValid());
    }

    private int getRandFromList(ArrayList<Integer> list) {
        int ind = rand.nextInt(list.size());
        int res = list.get(ind);
        list.remove(ind);
        return res;
    }

    @Test
    public void checkChangesDelete() {
        ArrayList<Integer> values = new ArrayList<>(NUM_VALUES_IN_TREE);
        mOPEDebugClient<Integer> client = new mOPEDebugClient<>();
        for(int i=0; i<NUM_VALUES_IN_TREE; i++) {
            client.addKey(i);
            values.add(i);
        }

        ChangeTracker<Integer> tracker = null;
        List<Tuple<BigInteger, Integer>> oldValues, newValues;
        newValues = client.getEncodingsWithValues();
        for (int i = 1; i <= MAX_ITER; i++) {
            int val = getRandFromList(values);
            tracker = client.deleteKey(val);
            if(tracker==null)
                fail("Tracker should not be null");
            oldValues = newValues;
            newValues = client.getEncodingsWithValues();
            logLineToConsole("--------------------------------------------------------------------");
            logLineToConsole("valueDel: "+val);
            logLineToConsole(client.printTree());
            mOPEUpdateSummary sum =  tracker.getDeleteUpdateSummary();
            logLineToConsole("Summary: " + sum.toString());
            List<Integer> diff = changedValuesAdd(oldValues, newValues);
            List<Integer> computedDiff = sum.getChangedKeys();
            if(computedDiff.contains(val))
                computedDiff.remove(val);
            for(Integer cur : diff) {
                logToConsole(cur+", ");
                assertTrue(computedDiff.contains(cur));
            }
            logLineToConsole();
            logLineToConsole("Actual: " +diff.size() +" Computed: "+ (computedDiff.size()-1) + " Diff: "+ (computedDiff.size()-diff.size()-1));
            logLineToConsole(tracker.getHighestNode().getNodeRepresentation());
            assertTrue(diff.size()<=computedDiff.size());
            logLineToConsole("--------------------------------------------------------------------");
        }

    }

    @Test
    public void checkChangesAdd() {
        mOPEDebugClient<Integer> client = new mOPEDebugClient<>();
        ChangeTracker<Integer> tracker = null;
        List<Tuple<BigInteger, Integer>> oldValues, newValues;
        newValues = client.getEncodingsWithValues();
        for (int i = 1; i <= NUM_VALUES_IN_TREE; i++) {
            tracker = client.addKey(rand.nextInt());
            oldValues = newValues;
            newValues = client.getEncodingsWithValues();
            logLineToConsole("--------------------------------------------------------------------");
            logLineToConsole(client.printTree());
            mOPEUpdateSummary sum =  tracker.getAddUpdateSummary();
            logLineToConsole("Summary: " + sum.toString());
            List<Integer> diff = changedValuesAdd(oldValues, newValues);
            List<Integer> computedDiff = sum.getChangedKeys();
            for(Integer cur : diff) {
                logToConsole(cur+", ");
                assertTrue(computedDiff.contains(cur));
            }
            logLineToConsole();
            logLineToConsole("Actual: " +diff.size() +" Computed: "+ (computedDiff.size()-1) + " Diff: "+ (computedDiff.size()-diff.size()-1));
            logLineToConsole(tracker.getHighestNode().getNodeRepresentation());
            assertTrue(diff.size()<=computedDiff.size());
            logLineToConsole("--------------------------------------------------------------------");
        }

    }

    private static List<Integer> changedValuesAdd(List<Tuple<BigInteger, Integer>> oldValues, List<Tuple<BigInteger, Integer>> newValues) {
        if(oldValues.size()>newValues.size()) {
            List<Tuple<BigInteger, Integer>> temp = oldValues;
            oldValues = newValues;
            newValues = temp;
        }
        /*if(oldValues.size()+1 != newValues.size())
            throw new IllegalArgumentException("illegal Size");*/
        List<Integer> res = new ArrayList<>();
        List<Tuple<BigInteger, Integer>> work = new ArrayList<>();
        Iterator<Tuple<BigInteger, Integer>> newIter, oldIter;
        Tuple<BigInteger, Integer> tempOld, tempNew;
        newIter = newValues.iterator();
        oldIter = oldValues.iterator();
        while (oldIter.hasNext()) {
            tempOld = oldIter.next();
            tempNew = newIter.next();
            while (tempOld.b.compareTo(tempNew.b)!=0) {
                if(!newIter.hasNext())
                    break;
                tempNew = newIter.next();
            }

            if(!tempOld.a.equals(tempNew.a)) {
                res.add(tempOld.b);
            }
        }
        return res;
    }

    private static void logToConsole(String log) {
        if(LOG)
            System.out.print(log);
    }

    private static void logLineToConsole(String log) {
        if(LOG)
            System.out.println(log);
    }

    private static void logLineToConsole() {
        if(LOG)
            System.out.println();
    }

}
