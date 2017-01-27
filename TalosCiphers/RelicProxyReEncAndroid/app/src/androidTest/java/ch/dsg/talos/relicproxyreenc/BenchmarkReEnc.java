package ch.dsg.talos.relicproxyreenc;

import android.util.Log;

import com.google.common.base.Stopwatch;

import junit.framework.TestCase;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Struct;
import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import ch.dsg.talos.relicproxyreenc.crypto.CRTECElGamalRelic;
import ch.dsg.talos.relicproxyreenc.crypto.CRTPreRelic;
import ch.dsg.talos.relicproxyreenc.crypto.CRTPreRelic.*;
import ch.dsg.talos.relicproxyreenc.crypto.IPRNG;
import ch.dsg.talos.relicproxyreenc.crypto.PRERelic.*;

import ch.dsg.talos.relicproxyreenc.crypto.PRERelic;
import ch.dsg.talos.relicproxyreenc.crypto.PRNGImpl;

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
 */
public class BenchmarkReEnc extends TestCase {

    private static int NUM_ITERATIONS = 100;
    private static int NUM_ITERATIONS_DEC = 100;

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

    private BigInteger[] get_plaintexts_BN(int num_bits, int num_iters) {
        Random rand = new Random();
        BigInteger[] res = new BigInteger[num_iters];
        for(int i=0; i<num_iters; i++) {
            BigInteger temp = new BigInteger(num_bits, rand);
            if(temp.compareTo(BigInteger.ZERO) == -1) {
                temp = temp.negate();
            }
            res[i] = temp;
        }
        return res;
    }

    private BigInteger[] get_plaintexts_BN(int num_bits) {
        return get_plaintexts_BN(num_bits, NUM_ITERATIONS);
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
        PRECipher[] ciphers = new PRECipher[NUM_ITERATIONS];
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
            log("Key Gen Round " + i);
        }

        plaintexts = get_plaintexts_BN(num_bits);

        //encryption
        for(int i=0; i<NUM_ITERATIONS; i++) {
            watch.reset();
            watch.start();
            ciphers[i] = CRTPreRelic.encrypt(plaintexts[i],keys[i]);
            watch.stop();
            times[1][i] = watch.elapsed(u);
            log("Enc Gen Round " + i);
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
            log("ReEnc Round " + i);
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
            log("DecL1 Round " + i);
        }

        //assertTrue(check_res_BN(plaintexts, res_level1, NUM_ITERATIONS_DEC));

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
            log("DecL2 Round " + i);
        }
        //assertTrue(check_res_BN(plaintexts, res_level2, NUM_ITERATIONS_DEC));

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

    public void doBenchmarkDETAILEDBulk(int numThreads, int[] batchsizes, int numIters, String name, int num_bits, final boolean do_bsgs, int table_size_pow) {
        if(do_bsgs) {
            log("Preload Table ......");
            PRERelic.initBsgsTable(1<<table_size_pow);
        }

        for(int batchsize : batchsizes) {
            log("Batch Size " + batchsize);

            final BigInteger[] res_level1 = new BigInteger[batchsize];
            final BigInteger[] res_level2 = new BigInteger[batchsize];
            final PRECipher[] ciphers = new PRECipher[batchsize];
            final PRECipher[] re_ciphers = new PRECipher[batchsize];
            final int processingSize = batchsize/numThreads;

            int max_level1 = 0, max_level2 = 0;
            IPRNG prng = new PRNGImpl();

            long[][] times = new long[4][numIters];
            String[] colNames = new String[]{"Encryption", "Re-Encryption", "Decryption Level 1", "Decryption Level 2"};

            Stopwatch watch = Stopwatch.createUnstarted();
            TimeUnit u = TimeUnit.NANOSECONDS;

            for(int iteration=0; iteration<numIters; iteration++) {

                final PREKey key = PRERelic.generatePREKeys();
                final PREKey key2 = PRERelic.generatePREKeys();

                final BigInteger[] plaintexts = get_plaintexts_BN(num_bits);

                //encryption
                watch.reset();
                watch.start();
                final CyclicBarrier barrier1 = new CyclicBarrier(numThreads);
                for(int t_id=0; t_id<numThreads-1; t_id++) {
                    final int cur_id = t_id;
                    (new Thread(new Runnable() {
                        @Override
                        public void run() {
                            for (int i = cur_id*processingSize; i < (cur_id+1) * processingSize; i++) {
                                ciphers[i] = PRERelic.encrypt(plaintexts[i].longValue(), key);
                            }
                            try {
                                barrier1.await();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            } catch (BrokenBarrierException e) {
                                e.printStackTrace();
                            }
                        }
                    })).start();
                }
                for (int i = (numThreads-1)*processingSize; i < batchsize; i++) {
                    ciphers[i] = PRERelic.encrypt(plaintexts[i].longValue(), key);
                }
                try {
                    barrier1.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (BrokenBarrierException e) {
                    e.printStackTrace();
                }
                watch.stop();
                times[0][iteration] = watch.elapsed(u);
                log("Enc Gen Round " + iteration);


                final PREToken token = PRERelic.createToken(key, key2);


                //re-encryption
                watch.reset();
                watch.start();
                final CyclicBarrier barrier2 = new CyclicBarrier(numThreads);
                for(int t_id=0; t_id<numThreads-1; t_id++) {
                    final int cur_id = t_id;
                    (new Thread(new Runnable() {
                        @Override
                        public void run() {
                            for (int i = cur_id*processingSize; i < (cur_id+1) * processingSize; i++) {
                                re_ciphers[i] = PRERelic.reEncrypt(ciphers[i], token);
                            }
                            try {
                                barrier2.await();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            } catch (BrokenBarrierException e) {
                                e.printStackTrace();
                            }
                        }
                    })).start();
                }
                for (int i = (numThreads-1)*processingSize; i < batchsize; i++) {
                    re_ciphers[i] = PRERelic.reEncrypt(ciphers[i], token);
                }
                try {
                    barrier2.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (BrokenBarrierException e) {
                    e.printStackTrace();
                }
                watch.stop();
                times[1][iteration] = watch.elapsed(u);
                log("ReEnc Round " + iteration);


                //Decryption Level1
                watch.reset();
                watch.start();
                final CyclicBarrier barrier3 = new CyclicBarrier(numThreads);
                for(int t_id=0; t_id<numThreads-1; t_id++) {
                    final int cur_id = t_id;
                    (new Thread(new Runnable() {
                        @Override
                        public void run() {
                            for (int i = cur_id*processingSize; i < (cur_id+1) * processingSize; i++) {
                                res_level1[i] = BigInteger.valueOf(PRERelic.decrypt(ciphers[i], key, do_bsgs));
                            }
                            try {
                                barrier3.await();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            } catch (BrokenBarrierException e) {
                                e.printStackTrace();
                            }
                        }
                    })).start();
                }
                for (int i = (numThreads-1)*processingSize; i < batchsize; i++) {
                    res_level1[i] = BigInteger.valueOf(PRERelic.decrypt(ciphers[i], key, do_bsgs));
                }
                try {
                    barrier3.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (BrokenBarrierException e) {
                    e.printStackTrace();
                }
                watch.stop();
                times[2][iteration] = watch.elapsed(u);
                log("DecL1 Round " + iteration);

                assertTrue(check_res_BN(plaintexts, res_level1, batchsize));

                //Decryption Level2
                watch.reset();
                watch.start();
                final CyclicBarrier barrier4 = new CyclicBarrier(numThreads);
                for(int t_id=0; t_id<numThreads-1; t_id++) {
                    final int cur_id = t_id;
                    (new Thread(new Runnable() {
                        @Override
                        public void run() {
                            for (int i = cur_id*processingSize; i < (cur_id+1) * processingSize; i++) {
                                res_level2[i] = BigInteger.valueOf(PRERelic.decrypt(re_ciphers[i], key2, do_bsgs));
                            }
                            try {
                                barrier4.await();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            } catch (BrokenBarrierException e) {
                                e.printStackTrace();
                            }
                        }
                    })).start();
                }
                for (int i = (numThreads-1)*processingSize; i < batchsize; i++) {
                    res_level2[i] = BigInteger.valueOf(PRERelic.decrypt(re_ciphers[i], key2, do_bsgs));
                }
                try {
                    barrier4.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (BrokenBarrierException e) {
                    e.printStackTrace();
                }
                watch.stop();
                times[3][iteration] = watch.elapsed(u);
                log("DecL2 Round " + iteration);
                assertTrue(check_res_BN(plaintexts, res_level2, batchsize));
            }
            String fullname = name + "_BATCH_"+ batchsize+".csv";
            try {
                HttpFileTransfer.sendData(fullname + "\n" + createCSVFile(colNames, times, ";"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public void doBenchmarkCRTDETAILEDBulk(int numThreads, int[] batchsizes, int numIters, String name, int num_bits, int dbits, int numPartitions, final boolean do_bsgs, int table_size_pow) {
        if (do_bsgs) {
            log("Preload Table ......");
            PRERelic.initBsgsTable(1 << table_size_pow);
        }

        for (int batchsize : batchsizes) {
            log("Batch Size " + batchsize);

            final BigInteger[] res_level1 = new BigInteger[batchsize];
            final BigInteger[] res_level2 = new BigInteger[batchsize];
            final CRTPreCipher[] ciphers = new CRTPreCipher[batchsize];
            final CRTPreCipher[] re_ciphers = new CRTPreCipher[batchsize];
            final int processingSize = batchsize / numThreads;

            int max_level1 = 0, max_level2 = 0;
            IPRNG prng = new PRNGImpl();
            CRTParams params = CRTPreRelic.generateCRTParams(prng, dbits, numPartitions);

            long[][] times = new long[4][numIters];
            String[] colNames = new String[]{"Encryption", "Re-Encryption", "Decryption Level 1", "Decryption Level 2"};

            Stopwatch watch = Stopwatch.createUnstarted();
            TimeUnit u = TimeUnit.NANOSECONDS;

            for (int iteration = 0; iteration < numIters; iteration++) {

                final CRTPreKey key = CRTPreRelic.generateKeys(params);
                final CRTPreKey key2 = CRTPreRelic.generateKeys(params);

                final BigInteger[] plaintexts = get_plaintexts_BN(num_bits);

                //encryption
                watch.reset();
                watch.start();
                final CyclicBarrier barrier1 = new CyclicBarrier(numThreads);
                for (int t_id = 0; t_id < numThreads - 1; t_id++) {
                    final int cur_id = t_id;
                    (new Thread(new Runnable() {
                        @Override
                        public void run() {
                            for (int i = cur_id * processingSize; i < (cur_id + 1) * processingSize; i++) {
                                ciphers[i] = CRTPreRelic.encrypt(plaintexts[i], key);
                            }
                            try {
                                barrier1.await();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            } catch (BrokenBarrierException e) {
                                e.printStackTrace();
                            }
                        }
                    })).start();
                }
                for (int i = (numThreads - 1) * processingSize; i < batchsize; i++) {
                    ciphers[i] = CRTPreRelic.encrypt(plaintexts[i], key);
                }
                try {
                    barrier1.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (BrokenBarrierException e) {
                    e.printStackTrace();
                }
                watch.stop();
                times[0][iteration] = watch.elapsed(u);
                log("Enc Gen Round " + iteration);


                final PREToken token = PRERelic.createToken(key, key2);


                //re-encryption
                watch.reset();
                watch.start();
                final CyclicBarrier barrier2 = new CyclicBarrier(numThreads);
                for (int t_id = 0; t_id < numThreads - 1; t_id++) {
                    final int cur_id = t_id;
                    (new Thread(new Runnable() {
                        @Override
                        public void run() {
                            for (int i = cur_id * processingSize; i < (cur_id + 1) * processingSize; i++) {
                                re_ciphers[i] = CRTPreRelic.reEncrypt(ciphers[i], token);
                            }
                            try {
                                barrier2.await();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            } catch (BrokenBarrierException e) {
                                e.printStackTrace();
                            }
                        }
                    })).start();
                }
                for (int i = (numThreads - 1) * processingSize; i < batchsize; i++) {
                    re_ciphers[i] = CRTPreRelic.reEncrypt(ciphers[i], token);
                }
                try {
                    barrier2.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (BrokenBarrierException e) {
                    e.printStackTrace();
                }
                watch.stop();
                times[1][iteration] = watch.elapsed(u);
                log("ReEnc Round " + iteration);


                //Decryption Level1
                watch.reset();
                watch.start();
                final CyclicBarrier barrier3 = new CyclicBarrier(numThreads);
                for (int t_id = 0; t_id < numThreads - 1; t_id++) {
                    final int cur_id = t_id;
                    (new Thread(new Runnable() {
                        @Override
                        public void run() {
                            for (int i = cur_id * processingSize; i < (cur_id + 1) * processingSize; i++) {
                                res_level1[i] = CRTPreRelic.decrypt(ciphers[i], key, do_bsgs);
                            }
                            try {
                                barrier3.await();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            } catch (BrokenBarrierException e) {
                                e.printStackTrace();
                            }
                        }
                    })).start();
                }
                for (int i = (numThreads - 1) * processingSize; i < batchsize; i++) {
                    res_level1[i] = CRTPreRelic.decrypt(ciphers[i], key, do_bsgs);
                }
                try {
                    barrier3.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (BrokenBarrierException e) {
                    e.printStackTrace();
                }
                watch.stop();
                times[2][iteration] = watch.elapsed(u);
                log("DecL1 Round " + iteration);

                assertTrue(check_res_BN(plaintexts, res_level1, batchsize));

                //Decryption Level2
                watch.reset();
                watch.start();
                final CyclicBarrier barrier4 = new CyclicBarrier(numThreads);
                for (int t_id = 0; t_id < numThreads - 1; t_id++) {
                    final int cur_id = t_id;
                    (new Thread(new Runnable() {
                        @Override
                        public void run() {
                            for (int i = cur_id * processingSize; i < (cur_id + 1) * processingSize; i++) {
                                res_level2[i] = CRTPreRelic.decrypt(re_ciphers[i], key2, do_bsgs);
                            }
                            try {
                                barrier4.await();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            } catch (BrokenBarrierException e) {
                                e.printStackTrace();
                            }
                        }
                    })).start();
                }
                for (int i = (numThreads - 1) * processingSize; i < batchsize; i++) {
                    res_level2[i] = CRTPreRelic.decrypt(re_ciphers[i], key2, do_bsgs);
                }
                try {
                    barrier4.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (BrokenBarrierException e) {
                    e.printStackTrace();
                }
                watch.stop();
                times[3][iteration] = watch.elapsed(u);
                log("DecL2 Round " + iteration);
                assertTrue(check_res_BN(plaintexts, res_level2, batchsize));
            }
            String fullname = name + "_BATCH_" + batchsize + ".csv";
            try {
                HttpFileTransfer.sendData(fullname + "\n" + createCSVFile(colNames, times, ";"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public void doBenchmarkCRTDETAILEDBulkToken(int numThreads, int[] batchsizes, int numIters, String name) {
        for(int batchsize : batchsizes) {
            log("Batch Size " + batchsize);

            final int processingSize = batchsize/numThreads;

            IPRNG prng = new PRNGImpl();
            CRTParams params = CRTPreRelic.generateCRTParams(prng, 17, 2);
            final CRTPreKey keyA[] = new CRTPreKey[batchsize];
            final CRTPreKey keyB[] = new CRTPreKey[batchsize];

            long[][] times = new long[1][numIters];
            String[] colNames = new String[]{"Token Generation"};

            Stopwatch watch = Stopwatch.createUnstarted();
            TimeUnit u = TimeUnit.NANOSECONDS;

            for(int count=0; count<batchsize; count++) {
                keyA[count] = CRTPreRelic.generateKeys(params);
                keyB[count] = CRTPreRelic.generateKeys(params);
            }

            for(int iteration=0; iteration<numIters; iteration++) {
                //token gen
                watch.reset();
                watch.start();
                final CyclicBarrier barrier1 = new CyclicBarrier(numThreads);
                for(int t_id=0; t_id<numThreads-1; t_id++) {
                    final int cur_id = t_id;
                    (new Thread(new Runnable() {
                        @Override
                        public void run() {
                            for (int i = cur_id*processingSize; i < (cur_id+1) * processingSize; i++) {
                                PREToken token =  PRERelic.createToken(keyA[i], keyB[i]);
                            }
                            try {
                                barrier1.await();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            } catch (BrokenBarrierException e) {
                                e.printStackTrace();
                            }
                        }
                    })).start();
                }
                for (int i = (numThreads-1)*processingSize; i < batchsize; i++) {
                    PREToken token =  PRERelic.createToken(keyA[i], keyB[i]);
                }
                try {
                    barrier1.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (BrokenBarrierException e) {
                    e.printStackTrace();
                }
                watch.stop();
                times[0][iteration] = watch.elapsed(u);
                log("Token Gen Round " + iteration);

            }
            String fullname = name + "_BATCH_"+ batchsize+".csv";
            try {
                HttpFileTransfer.sendData(fullname + "\n" + createCSVFile(colNames, times, ";"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public static String createCSVFile(String[] colnames, long[][] data, String delim) {
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
        doBenchmark(16,true,16);
        //doBenchmark(22, true, 19);
        doBenchmarkCRT(32, 17, 2, true, 17);
        doBenchmarkCRT(64, 23, 3, true, 19);

        /*
        doBenchmarkDETAILED("and-pre-rel-16-SEC_80-TAB16-CRT_1_16.csv",16, true, 16);
        doBenchmarkCRTDETAILED("and-pre-rel-32-SEC_80-TAB17-CRT_2_17.csv",32, 17, 2, true, 17);
        doBenchmarkCRTDETAILED("and-pre-rel-32-SEC_80-TAB11-CRT_3_11.csv",32, 11, 3, true, 11);
        doBenchmarkCRTDETAILED("and-pre-rel-32-SEC_80-TAB9-CRT_4_9.csv",32, 9, 4, true, 9);
        doBenchmarkCRTDETAILED("and-pre-rel-32-SEC_80-TAB7-CRT_5_7.csv",32, 7, 5, true, 7);*/

        //doBenchmarkCRTDETAILED("and-pre-rel-64-SEC_80-TAB19-CRT_2_32.csv",64, 33, 2, true, 19);
        //doBenchmarkCRTDETAILED("and-pre-rel-64-SEC_80-TAB19-CRT_3_23.csv",64, 23, 3, true, 19);
        //doBenchmarkCRTDETAILED("and-pre-rel-64-SEC_80-TAB17-CRT_4_17.csv",64, 17, 4, true, 17);
        //doBenchmarkCRTDETAILED("and-pre-rel-64-SEC_80-TAB13-CRT_5_13.csv",64, 13, 5, true, 13);
        //doBenchmarkDETAILED("and-pre-rel-16-SEC_80-TAB16.csv",16, true, 16);

        /*
        doBenchmarkDETAILED("and-pre-rel-16-SEC_128-TAB16-CRT_1_16.csv",16, true, 16);
        doBenchmarkCRTDETAILED("and-pre-rel-32-SEC_128-TAB17-CRT_2_17.csv",32, 17, 2, true, 17);
        doBenchmarkCRTDETAILED("and-pre-rel-32-SEC_128-TAB11-CRT_3_11.csv",32, 11, 3, true, 11);
        doBenchmarkCRTDETAILED("and-pre-rel-32-SEC_128-TAB9-CRT_4_9.csv",32, 9, 4, true, 9);
        doBenchmarkCRTDETAILED("and-pre-rel-32-SEC-128-TAB7-CRT_5_7.csv",32, 7, 5, true, 7);*/

        //doBenchmarkCRTDETAILED("and-pre-rel-64-SEC_128-TAB19-CRT_2_32.csv",64, 33, 2, true, 19);
        /*doBenchmarkCRTDETAILED("and-pre-rel-64-SEC_128-TAB19-CRT_3_23.csv",64, 23, 3, true, 19);
        doBenchmarkCRTDETAILED("and-pre-rel-64-SEC_128-TAB17-CRT_4_17.csv",64, 17, 4, true, 17);
        doBenchmarkCRTDETAILED("and-pre-rel-64-SEC_128-TAB13-CRT_5_13.csv",64, 13, 5, true, 13);*/
    }

    public void testBulk() {
        int[] batchSizes = new int[] {25, 50, 75, 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000};
        doBenchmarkDETAILEDBulk(4, batchSizes, 10, "and-pre-rel-16-SEC_80-TAB16-CRT_1_16.csv",16, true, 16);
        doBenchmarkCRTDETAILEDBulk(4, batchSizes, 10, "and-pre-rel-32-SEC_80-TAB17-CRT_2_17.csv",32, 17, 2, true, 17);
        doBenchmarkCRTDETAILEDBulk(4, batchSizes, 10, "and-pre-rel-64-SEC_80-TAB19-CRT_3_23.csv",64, 23, 3, true, 19);
    }

    public void testTokenGen() {
        int[] batchSizes = new int[] {1, 5, 25, 50, 75, 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000};
        doBenchmarkCRTDETAILEDBulkToken(4, batchSizes, 100, "pre-rel-token-gen-SEC_80");
    }

}

