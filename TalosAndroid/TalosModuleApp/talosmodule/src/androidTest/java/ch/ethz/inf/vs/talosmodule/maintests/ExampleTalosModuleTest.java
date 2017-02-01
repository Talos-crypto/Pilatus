package ch.ethz.inf.vs.talosmodule.maintests;

import android.content.Context;
import android.test.InstrumentationTestCase;
import android.util.Log;

import com.google.common.base.Stopwatch;

import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import ch.ethz.inf.vs.talosmodule.R;
import ch.ethz.inf.vs.talosmodule.RandomString;
import ch.ethz.inf.vs.talosmodule.main.ExampleTalosModule;
import ch.ethz.inf.vs.talosmodule.main.TalosResult;
import ch.ethz.inf.vs.talosmodule.main.User;

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

public class ExampleTalosModuleTest extends InstrumentationTestCase {

    private static String TEST_NAME = "LayersTest";
    private static int TEST_PORT = 8181;
    private static String TEST_SERVER_ADR = "lubudevelop.com";

    private Random rand = new Random();

    private RandomString ransStr = new RandomString();

    public ExampleTalosModuleTest() {
        super();
    }

    private ExampleTalosModule module = null;


    /*public void testSum() throws Exception {
        TalosResult sumRes = module.computeSum(new User("1"));
    }*/

    public void testDefault() throws Exception {
        module.insertTest(new User("1"), 1,2,"Hello");
        TalosResult res = module.searchTest(new User("1"), 2);
        module.insertTest(new User("1"), 2,3,"BLABLA");
        TalosResult sumRes = module.computeSum(new User("1"));

        if(res.next()) {
            int a = res.getInt("ColumnA");
            long b = res.getLong("ColumnB");
            String c = res.getString("ColumnC");
            assertTrue(a == 1 && b == 2 && c.equals("Hello"));
        } else {
            assertTrue(false);
        }

        if(sumRes.next()) {
            int a = sumRes.getInt("SUM(ColumnA)");
            assertTrue(a==3);
        } else {
            assertTrue(false);
        }
    }


    public void testOPE() throws Exception {
        Stopwatch watch = Stopwatch.createUnstarted();
        double avgtime = 0;
        User user = new User("1");
        int numValues = 200;
        for(int i=0; i<numValues; i++) {
            watch.start();
            module.insertOPE(user, i);
            watch.stop();

        }
        avgtime =((double) watch.elapsed(TimeUnit.MILLISECONDS)) / numValues;
        Log.d("AVG_OPE","Avg Time: "+avgtime);

        for(int round=0; round<100; round++) {
            int from, to;
            from = rand.nextInt(numValues);
            to = rand.nextInt(numValues);
            if(from>to) {
                int temp = from;
                from = to;
                to = temp;
            }
            TalosResult res = module.queryOPE(user,from,to);
            HashSet<Integer> temp = new HashSet<>();

            while (res.next()) {
                temp.add(res.getInt("VAL"));
            }
            for(int i=from; i<=to; i++) {
                assertTrue(temp.contains(i));
            }
        }

    }


    @Override
    public void setUp() throws Exception {
        super.setUp();
        Context con = this.getInstrumentation().getContext();
        module = new ExampleTalosModule(con, TEST_SERVER_ADR, TEST_PORT, R.raw.talos);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

}
