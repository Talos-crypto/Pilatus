package ch.ethz.inf.vs.talosmodule.prerelictests;

import android.util.Log;

import com.google.common.base.Stopwatch;

import junit.framework.TestCase;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Struct;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import ch.ethz.inf.vs.talosmodule.HttpFileTransfer;
import ch.ethz.inf.vs.talosmodule.cryptoalg.CRTPreRelic;
import ch.ethz.inf.vs.talosmodule.cryptoalg.CRTPreRelic.*;
import ch.ethz.inf.vs.talosmodule.cryptoalg.IPRNG;
import ch.ethz.inf.vs.talosmodule.cryptoalg.PRERelic;
import ch.ethz.inf.vs.talosmodule.cryptoalg.PRERelic.*;
import ch.ethz.inf.vs.talosmodule.cryptoalg.PRNGImpl;

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

public class BenchmarkReEnc extends TestCase {

    private static int NUM_ITERATIONS = 1;
    private static int NUM_ITERATIONS_DEC = 1;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    private static void log(String msg) {
        Log.i("BenchmarkReEnc", msg);
    }

    private static Stopwatch watch = Stopwatch.createUnstarted();

    private static void startBench(String name) {
        log("-----Start Bench " + name + "-----");
        watch.reset();
        watch.start();
    }

    private static void stoptBench(int iterations) {
        BigDecimal total;
        watch.stop();
        total = BigDecimal.valueOf(watch.elapsed(TimeUnit.NANOSECONDS));
        log("Total time for " + iterations + " Iterations: " + total.toString());
        total = total.divide(BigDecimal.valueOf(iterations), BigDecimal.ROUND_HALF_UP);
        log("Average Time: " + total.toString());
    }

    private long[] get_plaintexts(int num_bits) {
        Random rand = new Random();
        long temp;
        long[] res = new long[NUM_ITERATIONS];
        for(int i=0; i<NUM_ITERATIONS; i++) {
            temp = rand.nextLong();
            if(temp<0) {
                temp=-temp;
            }
            res[i] = temp % (1L<<num_bits);
        }
        return res;
    }

    private BigInteger[] get_plaintexts_BN(int num_bits) {
        Random rand = new Random();
        BigInteger[] res = new BigInteger[NUM_ITERATIONS];
        for(int i=0; i<NUM_ITERATIONS; i++) {
            BigInteger temp = new BigInteger(num_bits, rand);
            if(temp.compareTo(BigInteger.ZERO) == -1) {
                temp = temp.negate();
            }
            res[i] = temp;
        }
        return res;
    }

    public boolean check_res(long[] before, long[] after, int size) {
        for(int i=0; i<size; i++) {
           if(before[i] != after[i]) {
                log("Failed Enc/Dec: " + String.valueOf(before[i]));
                return false;
            }
        }
        return true;
    }

    public boolean check_res_BN(BigInteger[] before, BigInteger[] after, int size) {
        for(int i=0; i<size; i++) {
            if(before[i].compareTo(after[i]) != 0) {
                log("Failed Enc/Dec: " + before[i].toString());
                return false;
            }
        }
        return true;
    }

    public void doBenchmark(int num_bits, boolean do_bsgs, int table_size_pow) {
        PRERelic.PREKey[] keys = new PRERelic.PREKey[NUM_ITERATIONS];
        long[] plaintexts;
        long[] res_level1 = new long[NUM_ITERATIONS];
        long[] res_level2= new long[NUM_ITERATIONS];
        PRERelic.PRECipher[] ciphers = new PRECipher[NUM_ITERATIONS];
        PRECipher[] re_ciphers = new PRECipher[NUM_ITERATIONS];
        PREToken[] tokens = new PREToken[NUM_ITERATIONS];
        int max_level1 = 0, max_level2 = 0;

        //key gen
        startBench("Key gen");
        for(int i=0; i<NUM_ITERATIONS; i++) {
            keys[i] = PRERelic.generatePREKeys();
        }
        stoptBench(NUM_ITERATIONS);

        plaintexts = get_plaintexts(num_bits);

        //encryption
        startBench("Encryption");
        for(int i=0; i<NUM_ITERATIONS; i++) {
            ciphers[i] = PRERelic.encrypt(plaintexts[i],keys[i]);
        }
        stoptBench(NUM_ITERATIONS);

        int temp;
        //token gen
        startBench("Token generation");
        for(int i=0; i<NUM_ITERATIONS; i++) {
            if(i==NUM_ITERATIONS-1) {
                temp = 0;
            } else {
                temp = i+1;
            }
            tokens[i] =PRERelic.createToken(keys[i],keys[temp]);
        }
        stoptBench(NUM_ITERATIONS);

        //re-encryption
        startBench("Re-Encryption ");
        for(int i=0; i<NUM_ITERATIONS; i++) {
            re_ciphers[i] = PRERelic.reEncrypt(ciphers[i], tokens[i]);
        }
        stoptBench(NUM_ITERATIONS);

        if(do_bsgs) {
            log("Preload Table ......");
            PRERelic.initBsgsTable(1<<table_size_pow);
        }

        //Decryption Level1
        startBench("Decryption Level1");
        for(int i=0; i<NUM_ITERATIONS_DEC; i++) {
            res_level1[i] = PRERelic.decrypt(ciphers[i], keys[i], do_bsgs);
        }
        stoptBench(NUM_ITERATIONS_DEC);

        assertTrue(check_res(plaintexts, res_level1, NUM_ITERATIONS_DEC));

        //Decryption Level2
        startBench("Decryption Level2");
        for(int i=0; i<NUM_ITERATIONS_DEC; i++) {
            if(i==NUM_ITERATIONS-1) {
                temp = 0;
            } else {
                temp = i+1;
            }
            res_level2[i] = PRERelic.decrypt(re_ciphers[i], keys[temp], do_bsgs);
        }
        stoptBench(NUM_ITERATIONS_DEC);
        assertTrue(check_res(plaintexts, res_level2, NUM_ITERATIONS_DEC));

        for(int i=0; i<NUM_ITERATIONS_DEC; i++) {
            if(ciphers[i].getSize()>max_level1) {
                max_level1 = ciphers[i].getSize();
            }
            if(re_ciphers[i].getSize()>max_level2) {
                max_level2 = re_ciphers[i].getSize();
            }
        }

        log("Max level1 cipher size: " +max_level1 + " Max level2: " + max_level2);

    }

    public void doBenchmarkCRT(int num_bits, int dbits, int numPartitions, boolean do_bsgs, int table_size_pow) {
        CRTPreKey[] keys = new CRTPreKey[NUM_ITERATIONS];
        BigInteger[] plaintexts;
        BigInteger[] res_level1 = new BigInteger[NUM_ITERATIONS];
        BigInteger[] res_level2= new BigInteger[NUM_ITERATIONS];
        CRTPreCipher[] ciphers = new CRTPreCipher[NUM_ITERATIONS];
        CRTPreCipher[] re_ciphers = new CRTPreCipher[NUM_ITERATIONS];
        PREToken[] tokens = new PREToken[NUM_ITERATIONS];
        int max_level1 = 0, max_level2 = 0;
        IPRNG prng = new PRNGImpl();
        CRTParams params = CRTPreRelic.generateCRTParams(prng, dbits, numPartitions);

        //key gen
        startBench("Key gen");
        for(int i=0; i<NUM_ITERATIONS; i++) {
            keys[i] = CRTPreRelic.generateKeys(params);
        }
        stoptBench(NUM_ITERATIONS);

        plaintexts = get_plaintexts_BN(num_bits);

        //encryption
        startBench("Encryption");
        for(int i=0; i<NUM_ITERATIONS; i++) {
            ciphers[i] = CRTPreRelic.encrypt(plaintexts[i],keys[i]);
        }
        stoptBench(NUM_ITERATIONS);

        int temp;
        //token gen
        startBench("Token generation");
        for(int i=0; i<NUM_ITERATIONS; i++) {
            if(i==NUM_ITERATIONS-1) {
                temp = 0;
            } else {
                temp = i+1;
            }
            tokens[i] =PRERelic.createToken(keys[i],keys[temp]);
        }
        stoptBench(NUM_ITERATIONS);

        //re-encryption
        startBench("Re-Encryption ");
        for(int i=0; i<NUM_ITERATIONS; i++) {
            re_ciphers[i] = CRTPreRelic.reEncrypt(ciphers[i], tokens[i]);
        }
        stoptBench(NUM_ITERATIONS);

        if(do_bsgs) {
            log("Preload Table ......");
            PRERelic.initBsgsTable(1<<table_size_pow);
        }

        //Decryption Level1
        startBench("Decryption Level1");
        for(int i=0; i<NUM_ITERATIONS_DEC; i++) {
            res_level1[i] = CRTPreRelic.decrypt(ciphers[i], keys[i], do_bsgs);
        }
        stoptBench(NUM_ITERATIONS_DEC);

        assertTrue(check_res_BN(plaintexts, res_level1, NUM_ITERATIONS_DEC));

        //Decryption Level2
        startBench("Decryption Level2");
        //hack
        for(int i=0; i<NUM_ITERATIONS_DEC; i++) {
            if(i==NUM_ITERATIONS-1) {
                temp = 0;
            } else {
                temp = i+1;
            }
            res_level2[i] = CRTPreRelic.decrypt(re_ciphers[i], keys[temp], do_bsgs);
        }
        stoptBench(NUM_ITERATIONS_DEC);
        assertTrue(check_res_BN(plaintexts, res_level2, NUM_ITERATIONS_DEC));

        for(int i=0; i<NUM_ITERATIONS_DEC; i++) {
            if(ciphers[i].getSize()>max_level1) {
                max_level1 = ciphers[i].getSize();
            }
            if(re_ciphers[i].getSize()>max_level2) {
                max_level2 = re_ciphers[i].getSize();
            }
        }

        log("Max level1 cipher size: " +max_level1 + " Max level2: " + max_level2);

    }

    public void doBenchmarkDETAILED(String name, int num_bits, boolean do_bsgs, int table_size_pow) {
        PRERelic.PREKey[] keys = new PRERelic.PREKey[NUM_ITERATIONS];
        long[] plaintexts;
        long[] res_level1 = new long[NUM_ITERATIONS];
        long[] res_level2= new long[NUM_ITERATIONS];
        PRECipher[] ciphers = new PRECipher[NUM_ITERATIONS];
        PRECipher[] re_ciphers = new PRECipher[NUM_ITERATIONS];
        PREToken[] tokens = new PREToken[NUM_ITERATIONS];
        int max_level1 = 0, max_level2 = 0;

        long[][] times = new long[6][NUM_ITERATIONS];
        String[] colNames = new String[] {"Key Generation", "Encryption", "Token Generation", "Re-Encryption", "Decryption Level 1", "Decryption Level 2"};
        Stopwatch watch = Stopwatch.createUnstarted();
        TimeUnit u = TimeUnit.NANOSECONDS;


        //key gen
        for(int i=0; i<NUM_ITERATIONS; i++) {
            watch.reset();
            watch.start();
            keys[i] = PRERelic.generatePREKeys();
            watch.stop();
            times[0][i] = watch.elapsed(u);
        }
        plaintexts = get_plaintexts(num_bits);

        //encryption
        for(int i=0; i<NUM_ITERATIONS; i++) {
            watch.reset();
            watch.start();
            ciphers[i] = PRERelic.encrypt(plaintexts[i],keys[i]);
            watch.stop();
            times[1][i] = watch.elapsed(u);
        }

        int temp;
        //token gen
        for(int i=0; i<NUM_ITERATIONS; i++) {
            if(i==NUM_ITERATIONS-1) {
                temp = 0;
            } else {
                temp = i+1;
            }
            watch.reset();
            watch.start();
            tokens[i] =PRERelic.createToken(keys[i],keys[temp]);
            watch.stop();
            times[2][i] = watch.elapsed(u);
        }

        //re-encryption
        for(int i=0; i<NUM_ITERATIONS; i++) {
            watch.reset();
            watch.start();
            re_ciphers[i] = PRERelic.reEncrypt(ciphers[i], tokens[i]);
            watch.stop();
            times[3][i] = watch.elapsed(u);
        }

        if(do_bsgs) {
            log("Preload Table ......");
            PRERelic.initBsgsTable(1<<table_size_pow);
        }

        //Decryption Level1
        for(int i=0; i<NUM_ITERATIONS_DEC; i++) {
            watch.reset();
            watch.start();
            res_level1[i] = PRERelic.decrypt(ciphers[i], keys[i], do_bsgs);
            watch.stop();
            times[4][i] = watch.elapsed(u);
        }

        assertTrue(check_res(plaintexts, res_level1, NUM_ITERATIONS_DEC));

        //Decryption Level2
        for(int i=0; i<NUM_ITERATIONS_DEC; i++) {
            if(i==NUM_ITERATIONS-1) {
                temp = 0;
            } else {
                temp = i+1;
            }
            watch.reset();
            watch.start();
            res_level2[i] = PRERelic.decrypt(re_ciphers[i], keys[temp], do_bsgs);
            watch.stop();
            times[5][i] = watch.elapsed(u);

        }
        assertTrue(check_res(plaintexts, res_level2, NUM_ITERATIONS_DEC));

        for(int i=0; i<NUM_ITERATIONS_DEC; i++) {
            if(ciphers[i].getSize()>max_level1) {
                max_level1 = ciphers[i].getSize();
            }
            if(re_ciphers[i].getSize()>max_level2) {
                max_level2 = re_ciphers[i].getSize();
            }
        }

        log("Max level1 cipher size: " +max_level1 + " Max level2: " + max_level2);
        try {
            HttpFileTransfer.sendData(name + "\n" + createCSVFile(colNames, times, ";"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void doBenchmarkCRTDETAILED(String name, int num_bits, int dbits, int numPartitions, boolean do_bsgs, int table_size_pow) {
        CRTPreKey[] keys = new CRTPreKey[NUM_ITERATIONS];
        BigInteger[] plaintexts;
        BigInteger[] res_level1 = new BigInteger[NUM_ITERATIONS];
        BigInteger[] res_level2= new BigInteger[NUM_ITERATIONS];
        CRTPreCipher[] ciphers = new CRTPreCipher[NUM_ITERATIONS];
        CRTPreCipher[] re_ciphers = new CRTPreCipher[NUM_ITERATIONS];
        PREToken[] tokens = new PREToken[NUM_ITERATIONS];
        int max_level1 = 0, max_level2 = 0;
        IPRNG prng = new PRNGImpl();
        CRTParams params = CRTPreRelic.generateCRTParams(prng, dbits, numPartitions);

        long[][] times = new long[6][NUM_ITERATIONS];
        String[] colNames = new String[] {"Key Generation", "Encryption", "Token Generation", "Re-Encryption", "Decryption Level 1", "Decryption Level 2"};

        Stopwatch watch = Stopwatch.createUnstarted();
        TimeUnit u = TimeUnit.NANOSECONDS;

        //key gen
        for(int i=0; i<NUM_ITERATIONS; i++) {
            watch.reset();
            watch.start();
            keys[i] = CRTPreRelic.generateKeys(params);
            watch.stop();
            times[0][i] = watch.elapsed(u);
        }

        plaintexts = get_plaintexts_BN(num_bits);

        //encryption
        for(int i=0; i<NUM_ITERATIONS; i++) {
            watch.reset();
            watch.start();
            ciphers[i] = CRTPreRelic.encrypt(plaintexts[i],keys[i]);
            watch.stop();
            times[1][i] = watch.elapsed(u);
        }

        int temp;
        //token gen
        for(int i=0; i<NUM_ITERATIONS; i++) {
            if(i==NUM_ITERATIONS-1) {
                temp = 0;
            } else {
                temp = i+1;
            }
            watch.reset();
            watch.start();
            tokens[i] =PRERelic.createToken(keys[i],keys[temp]);
            watch.stop();
            times[2][i] = watch.elapsed(u);
        }

        //re-encryption
        for(int i=0; i<NUM_ITERATIONS; i++) {
            watch.reset();
            watch.start();
            re_ciphers[i] = CRTPreRelic.reEncrypt(ciphers[i], tokens[i]);
            watch.stop();
            times[3][i] = watch.elapsed(u);
        }

        if(do_bsgs) {
            log("Preload Table ......");
            PRERelic.initBsgsTable(1<<table_size_pow);
        }

        //Decryption Level1
        for(int i=0; i<NUM_ITERATIONS_DEC; i++) {
            watch.reset();
            watch.start();
            res_level1[i] = CRTPreRelic.decrypt(ciphers[i], keys[i], do_bsgs);
            watch.stop();
            times[4][i] = watch.elapsed(u);
        }

        assertTrue(check_res_BN(plaintexts, res_level1, NUM_ITERATIONS_DEC));

        //Decryption Level2
        //hack
        for(int i=0; i<NUM_ITERATIONS_DEC; i++) {
            if(i==NUM_ITERATIONS-1) {
                temp = 0;
            } else {
                temp = i+1;
            }
            watch.reset();
            watch.start();
            res_level2[i] = CRTPreRelic.decrypt(re_ciphers[i], keys[temp], do_bsgs);
            watch.stop();
            times[5][i] = watch.elapsed(u);
        }
        assertTrue(check_res_BN(plaintexts, res_level2, NUM_ITERATIONS_DEC));

        for(int i=0; i<NUM_ITERATIONS_DEC; i++) {
            if(ciphers[i].getSize()>max_level1) {
                max_level1 = ciphers[i].getSize();
            }
            if(re_ciphers[i].getSize()>max_level2) {
                max_level2 = re_ciphers[i].getSize();
            }
        }

        log("Max level1 cipher size: " +max_level1 + " Max level2: " + max_level2);
        try {
            HttpFileTransfer.sendData(name + "\n" + createCSVFile(colNames, times, ";"));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public String createCSVFile(String[] colnames, long[][] data, String delim) {
        StringBuilder sb = new StringBuilder();
        for(String name: colnames) {
            sb.append(name).append(delim);
        }
        sb.setLength(sb.length()-1);
        sb.append("\n");
        for (int row=0; row<data[0].length; row++) {
            for (int colId = 0; colId < data.length; colId++) {
                sb.append(data[colId][row]).append(delim);
            }
            sb.setLength(sb.length()-1);
            sb.append("\n");
        }

        return sb.toString();
    }

    public void testMain() {
        //doBenchmark(22, true, 19);
        doBenchmarkCRT(32, 17, 2, true, 17);
        //doBenchmarkCRT(64, 23, 3, true, 19);
        //doBenchmarkCRTDETAILED("and-pre-rel-32-SEC_128-TAB17-CRT_2_17.csv",32, 17, 2, true, 17);
        //doBenchmarkCRTDETAILED("and-pre-rel-64-SEC_80-TAB19-CRT_3_23.csv",64, 23, 3, true, 19);
        //doBenchmarkDETAILED("and-pre-rel-16-SEC_80-TAB16.csv",16, true, 16);
    }
}
