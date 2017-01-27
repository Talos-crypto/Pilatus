package ch.dsg.talos.relicproxyreenc;

import android.util.Log;

import com.google.common.base.Stopwatch;

import junit.framework.TestCase;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import ch.dsg.talos.relicproxyreenc.crypto.CRTECElGamalRelic;
import ch.dsg.talos.relicproxyreenc.crypto.CRTECElGamalRelic.*;
import ch.dsg.talos.relicproxyreenc.crypto.ECElGamalRelic;
import ch.dsg.talos.relicproxyreenc.crypto.IPRNG;
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

public class BenchmarkECElGamalRelic extends TestCase {

    private static int NUM_ITERATIONS = 1000;
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
        Log.i("BECELGAMALR", msg);
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

    public void doBenchmarkCRT(int num_bits, int dbits, int numPartitions, boolean do_bsgs, int table_size_pow) {
        CRTECElGamalKey[] keys = new CRTECElGamalKey[NUM_ITERATIONS];
        BigInteger[] plaintexts;
        BigInteger[] res_dec = new BigInteger[NUM_ITERATIONS];
        CRTECElGamalCipher[] ciphers = new CRTECElGamalCipher[NUM_ITERATIONS];
        IPRNG prng = new PRNGImpl();
        CRTParams params = CRTECElGamalRelic.generateCRTParams(prng, dbits, numPartitions);
        int max = -1;

        //key gen
        startBench("Key gen");
        for(int i=0; i<NUM_ITERATIONS; i++) {
            keys[i] = CRTECElGamalRelic.generateKeys(params);
        }
        stoptBench(NUM_ITERATIONS);

        plaintexts = get_plaintexts_BN(num_bits);

        //encryption
        startBench("Encryption");
        for(int i=0; i<NUM_ITERATIONS; i++) {
            ciphers[i] = CRTECElGamalRelic.encrypt(plaintexts[i],keys[i]);
        }
        stoptBench(NUM_ITERATIONS);

        if(do_bsgs) {
            log("Preload Table ......");
            CRTECElGamalRelic.precomputeBSGSTable(1<<table_size_pow);
        }

        //Decryption
        startBench("Decryption");
        for(int i=0; i<NUM_ITERATIONS_DEC; i++) {
            res_dec[i] = CRTECElGamalRelic.decrypt(ciphers[i], keys[i], do_bsgs);
        }
        stoptBench(NUM_ITERATIONS_DEC);

        assertTrue(check_res_BN(plaintexts, res_dec, NUM_ITERATIONS_DEC));


        for(int i=0; i<NUM_ITERATIONS_DEC; i++) {
            if(ciphers[i].getSize()>max) {
                max = ciphers[i].getSize();
            }
        }

        log("Max cipher size: " +max);

    }

    public void doBenchmarkDETAILED(String name, int num_bits, boolean do_bsgs, int table_size_pow) {
        ECElGamalRelic.ECElGamalKey[] keys = new ECElGamalRelic.ECElGamalKey[NUM_ITERATIONS];
        long[] plaintexts;
        long[] res_dec = new long[NUM_ITERATIONS];
        ECElGamalRelic.ECElGamalCipher[] ciphers = new ECElGamalRelic.ECElGamalCipher[NUM_ITERATIONS];
        int max = -1;

        long[][] times = new long[3][NUM_ITERATIONS];
        String[] colNames = new String[] {"Key Generation", "Encryption", "Decryption"};
        Stopwatch watch = Stopwatch.createUnstarted();
        TimeUnit u = TimeUnit.NANOSECONDS;

        //key gen
        for(int i=0; i<NUM_ITERATIONS; i++) {
            watch.reset();
            watch.start();
            keys[i] = ECElGamalRelic.generateFreshKey();
            watch.stop();
            times[0][i] = watch.elapsed(TimeUnit.NANOSECONDS);
        }

        plaintexts = get_plaintexts(num_bits);

        //encryption
        for(int i=0; i<NUM_ITERATIONS; i++) {
            watch.reset();
            watch.start();
            ciphers[i] = ECElGamalRelic.encrypt(plaintexts[i],keys[i]);
            watch.stop();
            times[1][i] = watch.elapsed(TimeUnit.NANOSECONDS);
        }

        if(do_bsgs) {
            log("Preload Table ......");
            CRTECElGamalRelic.precomputeBSGSTable(1<<table_size_pow);
        }

        //Decryption
        for(int i=0; i<NUM_ITERATIONS_DEC; i++) {
            watch.reset();
            watch.start();
            res_dec[i] = ECElGamalRelic.decrypt(ciphers[i], keys[i], do_bsgs);
            watch.stop();
            times[2][i] = watch.elapsed(TimeUnit.NANOSECONDS);
        }

        assertTrue(check_res(plaintexts, res_dec, NUM_ITERATIONS_DEC));


        for(int i=0; i<NUM_ITERATIONS_DEC; i++) {
            if(ciphers[i].getSize()>max) {
                max = ciphers[i].getSize();
            }
        }

        log("Max cipher size: " +max);

        try {
            HttpFileTransfer.sendData(name + "\n" + BenchmarkReEnc.createCSVFile(colNames, times, ";"));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void test33() {
        CRTECElGamalKey key;
        IPRNG prng = new PRNGImpl();
        CRTParams params = CRTECElGamalRelic.generateCRTParams(prng, 33, 2);
        CRTECElGamalCipher cipher;
        key = CRTECElGamalRelic.generateKeys(params);
        log("Preload Table ......");
        CRTECElGamalRelic.precomputeBSGSTable(1<<19);

        cipher = CRTECElGamalRelic.encrypt(BigInteger.ONE.shiftLeft(64), key);

        BigInteger res = CRTECElGamalRelic.decrypt(cipher, key, true);

        assertEquals(BigInteger.ONE.shiftLeft(64), res);

    }

    public void doBenchmarkCRTDETAILED(String name, int num_bits, int dbits, int numPartitions, boolean do_bsgs, int table_size_pow) {
        CRTECElGamalKey[] keys = new CRTECElGamalKey[NUM_ITERATIONS];
        BigInteger[] plaintexts;
        BigInteger[] res_dec = new BigInteger[NUM_ITERATIONS];
        CRTECElGamalCipher[] ciphers = new CRTECElGamalCipher[NUM_ITERATIONS];
        IPRNG prng = new PRNGImpl();
        CRTParams params = CRTECElGamalRelic.generateCRTParams(prng, dbits, numPartitions);
        int max = -1;

        long[][] times = new long[3][NUM_ITERATIONS];
        String[] colNames = new String[] {"Key Generation", "Encryption", "Decryption"};
        Stopwatch watch = Stopwatch.createUnstarted();
        TimeUnit u = TimeUnit.NANOSECONDS;

        //key gen
        for(int i=0; i<NUM_ITERATIONS; i++) {
            watch.reset();
            watch.start();
            keys[i] = CRTECElGamalRelic.generateKeys(params);
            watch.stop();
            times[0][i] = watch.elapsed(TimeUnit.NANOSECONDS);
            log("Key Gen Round " + i);
        }

        plaintexts = get_plaintexts_BN(num_bits);

        //encryption
        for(int i=0; i<NUM_ITERATIONS; i++) {
            watch.reset();
            watch.start();
            ciphers[i] = CRTECElGamalRelic.encrypt(plaintexts[i],keys[i]);
            watch.stop();
            times[1][i] = watch.elapsed(TimeUnit.NANOSECONDS);
            log("Enc Round " + i);
        }

        if(do_bsgs) {
            log("Preload Table ......");
            CRTECElGamalRelic.precomputeBSGSTable(1<<table_size_pow);
        }

        //Decryption
        for(int i=0; i<NUM_ITERATIONS_DEC; i++) {
            watch.reset();
            watch.start();
            res_dec[i] = CRTECElGamalRelic.decrypt(ciphers[i], keys[i], do_bsgs);
            watch.stop();
            times[2][i] = watch.elapsed(TimeUnit.NANOSECONDS);
            log("Dec Round " + i);
        }

        //assertTrue(check_res_BN(plaintexts, res_dec, NUM_ITERATIONS_DEC));


        for(int i=0; i<NUM_ITERATIONS_DEC; i++) {
            if(ciphers[i].getSize()>max) {
                max = ciphers[i].getSize();
            }
        }

        log("Max cipher size: " +max);

        try {
            HttpFileTransfer.sendData(name + "\n" + BenchmarkReEnc.createCSVFile(colNames, times, ";"));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public void doBenchmarkDETAILEDBulk(int numThreads, int[] batchsizes, int numIters, String name, int num_bits, final boolean do_bsgs, int table_size_pow) {
        if (do_bsgs) {
            log("Preload Table ......");
            CRTECElGamalRelic.precomputeBSGSTable(1 << table_size_pow);
        }

        for(int batchsize : batchsizes) {
            log("Batch Size " + batchsize);

            final BigInteger[] res_dec = new BigInteger[batchsize];
            final ECElGamalRelic.ECElGamalCipher[] ciphers = new ECElGamalRelic.ECElGamalCipher[batchsize];

            final int processingSize = batchsize/numThreads;

            IPRNG prng = new PRNGImpl();

            long[][] times = new long[2][numIters];
            String[] colNames = new String[] {"Encryption", "Decryption"};
            Stopwatch watch = Stopwatch.createUnstarted();

            for(int iteration=0; iteration<numIters; iteration++) {
                final ECElGamalRelic.ECElGamalKey key = ECElGamalRelic.generateFreshKey();
                final BigInteger[] plaintexts = get_plaintexts_BN(num_bits, batchsize);

                //encryption
                watch.reset();
                watch.start();
                final CyclicBarrier barrier = new CyclicBarrier(numThreads);
                for(int t_id=0; t_id<numThreads-1; t_id++) {
                    final int cur_id = t_id;
                    (new Thread(new Runnable() {
                        @Override
                        public void run() {
                            for (int i = cur_id*processingSize; i < (cur_id+1) * processingSize; i++) {
                                ciphers[i] = ECElGamalRelic.encrypt(plaintexts[i].longValue(), key);
                            }
                            try {
                                barrier.await();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            } catch (BrokenBarrierException e) {
                                e.printStackTrace();
                            }
                        }
                    })).start();
                }
                for (int i = (numThreads-1)*processingSize; i < batchsize; i++) {
                    ciphers[i] = ECElGamalRelic.encrypt(plaintexts[i].longValue(), key);
                }
                try {
                    barrier.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (BrokenBarrierException e) {
                    e.printStackTrace();
                }
                watch.stop();
                times[0][iteration] = watch.elapsed(TimeUnit.NANOSECONDS);
                log("Enc Round " + iteration);

                //Decryption
                watch.reset();
                watch.start();
                final CyclicBarrier barrier2 = new CyclicBarrier(numThreads);
                for(int t_id=0; t_id<numThreads-1; t_id++) {
                    final int cur_id = t_id;
                    (new Thread(new Runnable() {
                        @Override
                        public void run() {
                            for (int i = cur_id*processingSize; i < (cur_id+1) * processingSize; i++) {
                                res_dec[i] = BigInteger.valueOf(ECElGamalRelic.decrypt(ciphers[i], key, do_bsgs));
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
                    res_dec[i] = BigInteger.valueOf(ECElGamalRelic.decrypt(ciphers[i], key, do_bsgs));
                }
                try {
                    barrier2.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (BrokenBarrierException e) {
                    e.printStackTrace();
                }
                watch.stop();
                times[1][iteration] = watch.elapsed(TimeUnit.NANOSECONDS);
                log("Dec Round " + iteration);

                assertTrue(check_res_BN(plaintexts, res_dec, batchsize));
            }
            String fullname = name + "_BATCH_"+ batchsize+".csv";
            try {
                HttpFileTransfer.sendData(fullname + "\n" + BenchmarkReEnc.createCSVFile(colNames, times, ";"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }


    public void doBenchmarkCRTDETAILEDBulk(int numThreads, int[] batchsizes, int numIters, String name, int num_bits, int dbits, int numPartitions, final boolean do_bsgs, int table_size_pow) {
        if (do_bsgs) {
            log("Preload Table ......");
            CRTECElGamalRelic.precomputeBSGSTable(1 << table_size_pow);
        }

        for(int batchsize : batchsizes) {
            log("Batch Size " + batchsize);

            final BigInteger[] res_dec = new BigInteger[batchsize];
            final CRTECElGamalCipher[] ciphers = new CRTECElGamalCipher[batchsize];

            final int processingSize = batchsize/numThreads;

            IPRNG prng = new PRNGImpl();
            CRTParams params = CRTECElGamalRelic.generateCRTParams(prng, dbits, numPartitions);

            long[][] times = new long[2][numIters];
            String[] colNames = new String[] {"Encryption", "Decryption"};
            Stopwatch watch = Stopwatch.createUnstarted();

            for(int iteration=0; iteration<numIters; iteration++) {
                final CRTECElGamalKey key = CRTECElGamalRelic.generateKeys(params);
                final BigInteger[] plaintexts = get_plaintexts_BN(num_bits, batchsize);

                //encryption
                watch.reset();
                watch.start();
                final CyclicBarrier barrier = new CyclicBarrier(numThreads);
                for(int t_id=0; t_id<numThreads-1; t_id++) {
                    final int cur_id = t_id;
                    (new Thread(new Runnable() {
                        @Override
                        public void run() {
                            for (int i = cur_id*processingSize; i < (cur_id+1) * processingSize; i++) {
                                ciphers[i] = CRTECElGamalRelic.encrypt(plaintexts[i], key);
                            }
                            try {
                                barrier.await();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            } catch (BrokenBarrierException e) {
                                e.printStackTrace();
                            }
                        }
                    })).start();
                }
                for (int i = (numThreads-1)*processingSize; i < batchsize; i++) {
                    ciphers[i] = CRTECElGamalRelic.encrypt(plaintexts[i], key);
                }
                try {
                    barrier.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (BrokenBarrierException e) {
                    e.printStackTrace();
                }
                watch.stop();
                times[0][iteration] = watch.elapsed(TimeUnit.NANOSECONDS);
                log("Enc Round " + iteration);

                //Decryption
                watch.reset();
                watch.start();
                final CyclicBarrier barrier2 = new CyclicBarrier(numThreads);
                for(int t_id=0; t_id<numThreads-1; t_id++) {
                    final int cur_id = t_id;
                    (new Thread(new Runnable() {
                        @Override
                        public void run() {
                            for (int i = cur_id*processingSize; i < (cur_id+1) * processingSize; i++) {
                                res_dec[i] = CRTECElGamalRelic.decrypt(ciphers[i], key, do_bsgs);
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
                    res_dec[i] = CRTECElGamalRelic.decrypt(ciphers[i], key, do_bsgs);
                }
                try {
                    barrier2.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (BrokenBarrierException e) {
                    e.printStackTrace();
                }
                watch.stop();
                times[1][iteration] = watch.elapsed(TimeUnit.NANOSECONDS);
                log("Dec Round " + iteration);

                assertTrue(check_res_BN(plaintexts, res_dec, batchsize));
            }
            String fullname = name + "_BATCH_"+ batchsize+".csv";
            try {
                HttpFileTransfer.sendData(fullname + "\n" + BenchmarkReEnc.createCSVFile(colNames, times, ";"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public void testMain() {
        //doBenchmark(22, true, 19);
        //doBenchmarkCRT(32, 17, 2, true, 17);
        //doBenchmarkCRT(64, 23, 3, true, 19);
        //doBenchmarkDETAILED("ecelgamal-relic-64-SEC_80-TAB16-CRT_1_16.csv",16, true, 16);
        /*
        doBenchmarkCRTDETAILED("ecelgamal-relic-32-SEC_80-TAB17-CRT_2_17.csv",32, 17, 2, true, 17);
        doBenchmarkCRTDETAILED("ecelgamal-relic-32-SEC_80-TAB11-CRT_3_11.csv",32, 11, 3, true, 11);
        doBenchmarkCRTDETAILED("ecelgamal-relic-32-SEC_80-TAB9-CRT_4_9.csv",32, 9, 4, true, 9);
        doBenchmarkCRTDETAILED("ecelgamal-relic-32-SEC_80-TAB7-CRT_5_7.csv",32, 7, 5, true, 7);*/

        //doBenchmarkCRTDETAILED("ecelgamal-relic-64-SEC_80-TAB19-CRT_2_33.csv",64, 33, 2, true, 19);
        //doBenchmarkCRTDETAILED("ecelgamal-relic-64-SEC_80-TAB19-CRT_3_23.csv",64, 23, 3, true, 19);
        //doBenchmarkCRTDETAILED("ecelgamal-relic-64-SEC_80-TAB17-CRT_4_17.csv",64, 17, 4, true, 17);
        //doBenchmarkCRTDETAILED("ecelgamal-relic-64-SEC_80-TAB13-CRT_5_13.csv",64, 13, 5, true, 13);


         /*doBenchmarkDETAILED("ecelgamal-relic-16-SEC_128-TAB16-CRT_1_16.csv",16, true, 16);

        doBenchmarkCRTDETAILED("ecelgamal-relic-32-SEC_128-TAB17-CRT_2_17.csv",32, 17, 2, true, 17);
        doBenchmarkCRTDETAILED("ecelgamal-relic-32-SEC_128-TAB11-CRT_3_11.csv",32, 11, 3, true, 11);
        doBenchmarkCRTDETAILED("ecelgamal-relic-32-SEC_128-TAB9-CRT_4_9.csv",32, 9, 4, true, 9);
        doBenchmarkCRTDETAILED("ecelgamal-relic-32-SEC_128-TAB7-CRT_5_7.csv",32, 7, 5, true, 7);*/

        //doBenchmarkCRTDETAILED("ecelgamal-relic-64-SEC_128-TAB19-CRT_2_33.csv",64, 33, 2, true, 19);
        /*doBenchmarkCRTDETAILED("ecelgamal-relic-64-SEC_128-TAB19-CRT_3_23.csv",64, 23, 3, true, 19);
        doBenchmarkCRTDETAILED("ecelgamal-relic-64-SEC_128-TAB17-CRT_4_17.csv",64, 17, 4, true, 17);
        doBenchmarkCRTDETAILED("ecelgamal-relic-64-SEC_128-TAB13-CRT_5_13.csv",64, 13, 5, true, 13);*/
    }

    public void testBatch() {
        int[] batchSizes = new int[] {25, 50, 75, 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000};
        doBenchmarkDETAILEDBulk(4, batchSizes, 10, "ecelgamal-relic-32-SEC_80-TAB16-CRT_1_16",16, true, 16);
        doBenchmarkCRTDETAILEDBulk(4, batchSizes, 10, "ecelgamal-relic-32-SEC_80-TAB17-CRT_2_17",32, 17, 2, true, 17);
        doBenchmarkCRTDETAILEDBulk(4, batchSizes, 10,"ecelgamal-relic-64-SEC_80-TAB19-CRT_3_23",64, 23, 3, true, 19);
    }


}
