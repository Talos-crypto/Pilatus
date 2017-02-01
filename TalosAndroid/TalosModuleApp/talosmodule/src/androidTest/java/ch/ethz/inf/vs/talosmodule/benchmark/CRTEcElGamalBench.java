package ch.ethz.inf.vs.talosmodule.benchmark;

import android.os.Debug;
import android.test.InstrumentationTestCase;
import android.util.Log;

import com.google.common.base.Stopwatch;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import ch.ethz.inf.vs.talosmodule.HttpFileTransfer;
import ch.ethz.inf.vs.talosmodule.OutputFile;
import ch.ethz.inf.vs.talosmodule.cryptoalg.CRTEcElGamal;
import ch.ethz.inf.vs.talosmodule.cryptoalg.FastECElGamal;
import ch.ethz.inf.vs.talosmodule.cryptoalg.NativeECElGamalCrypto;
import ch.ethz.inf.vs.talosmodule.cryptoalg.PRNGImpl;
import ch.ethz.inf.vs.talosmodule.cryptoalg.PRNGRC4Stream;
import ch.ethz.inf.vs.talosmodule.cryptoalg.Paillier;
import ch.ethz.inf.vs.talosmodule.cryptoalg.PaillierPriv;
import ch.ethz.inf.vs.talosmodule.util.ContextHolder;
import ch.ethz.inf.vs.talosmodule.util.Setting;

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

public class CRTEcElGamalBench extends InstrumentationTestCase {

    private static String TEST_NAME = "CRTEcElGamalBench";
    private static final int MAX_IT = 10000;

    private static boolean USE_HTTP_TRANFER = true;

    private static final String DELIM = ";";
    private static final String LB = "\n";
    private static final String ENC_TIME_TITLE = "Encryption Time";
    private static final String DEC_TIME_TITLE = "Decryption Time";

    private Random rand = new Random();
    private Stopwatch watch = Stopwatch.createUnstarted();

    public CRTEcElGamalBench() {
        super();
    }

    private static void log(String name) {
        Log.d(TEST_NAME, name);
    }

    private int getRandomInteger() {
        return rand.nextInt();
    }

    private long getRandomLong() {
        return rand.nextLong();
    }
    private byte[] key = new byte[16];

    public void testBenchMain() throws Exception {
        //runBench(16, 1, true, 2 , 10000, TimeUnit.NANOSECONDS);
        //runBench(11, 1, true, 3 , 10000, TimeUnit.NANOSECONDS);
        //runBench(20, 1, true, 1, TimeUnit.NANOSECONDS);
        //runBench(17, 1, true, 2, 10000, TimeUnit.NANOSECONDS);
        //runBench(17, 1, true, 2, 1000, TimeUnit.NANOSECONDS);
        int[] sizes = new int[] {1, 2, 5, 25, 50, 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000};
        //int[] sizes = new int[] {1000};
        //experimentBulkPar(20, 1, false, 3, 10, sizes, 4);
        //experimentBulk(20, 1, false, 3, 10, sizes);
        //bulkExperimentComparePaillier(1024, true, 10,  sizes);
        //bulkExperimentComparePaillier(2048, true, 10,  sizes);
        //comparePaillier(1024, true, 1000);
        //runBench(17, 4, false, 3, 10000, TimeUnit.NANOSECONDS);
        //runBench(16, 1, false, 3, 10000, TimeUnit.NANOSECONDS);
        //runBench(16, 1, false, 4, 10000, TimeUnit.NANOSECONDS);
        //runBench(13, 1, false, 5, 10000, TimeUnit.NANOSECONDS);
        //runBench(16, 2, true, 3, 1000, TimeUnit.NANOSECONDS);
        //runBench(16, 1, false, 2, 1000, TimeUnit.NANOSECONDS);
        //runBench(16, 2, true, 3, 10000, TimeUnit.NANOSECONDS);
        //runBench(16, 4, false, 2, 100, TimeUnit.NANOSECONDS);
        //runBenchSmall(16, 1, 16, 1000, TimeUnit.NANOSECONDS);
        //experimentBitwiseTime(20, 2, 1, 38);
        //experimentBulkParComparePaillier(1024, true, 10, sizes, 4);
        //experimentBulkParComparePaillier(2048, true, 10, sizes, 4);
        //experimentBulkParNoCRT(16, 16, 1, 10, sizes, 4);
        //experimentBulkNoCRT(16 ,16, 1, 10, sizes);
        //testKeyGenGamal(true, 2, 100);
        //testKeyGenGamal(false, 3, 100);
        measureNativeHeap(20);
    }

    private void measureNativeHeap(int tableSize) {
        Setting.TABLE_POW_2_SIZE = String.valueOf(tableSize);
        FastECElGamal.TABLE_SIZE_POW = tableSize;
        long before, after, res;
        before = Debug.getNativeHeapAllocatedSize();
        FastECElGamal.loadLookUpTable();
        after = Debug.getNativeHeapAllocatedSize();
        res = after - before;
        Log.i("SPACE",tableSize+" :"+String.valueOf(res));
    }

    private void runBench(int TableSize, int NumThreads, boolean is32bit, int crtPartions, int numITER, TimeUnit logUnit) throws Exception {
        int partSize = computePartionsSize(is32bit, crtPartions);
        int numBits = is32bit ? 32 : 64;
        long maxVal = is32bit ? Integer.MAX_VALUE : Long.MAX_VALUE;

        Setting.TABLE_POW_2_SIZE = String.valueOf(TableSize);
        Setting.NUM_THREADS = NumThreads;
        FastECElGamal.TABLE_SIZE_POW = TableSize;
        log("Run Bench:::TableSize->" + TableSize + ":NumThreads->" + NumThreads + ":NumBits->" + numBits + ":CRT-Partitions->" + crtPartions + ":::NumIterations->" + numITER);

        CRTEcElGamal gamal = createCipher(crtPartions, partSize);
        StringBuilder log = new StringBuilder();
        appendTitle(log);

        //warmup
        CRTEcElGamal.CRTEcElGamalCipher t = gamal.encrypt(BigInteger.valueOf(1));
        gamal.decrypt(t, Integer.MAX_VALUE);

        for(int round=1; round<=numITER; round++) {
            BigInteger res, cur = getRandValue(is32bit);
            watch.reset();
            watch.start();
            CRTEcElGamal.CRTEcElGamalCipher cipher = gamal.encrypt(cur);
            watch.stop();
            log.append(watch.elapsed(logUnit));
            watch.reset();

            log.append(DELIM);
            watch.start();
            res = gamal.decrypt(cipher, maxVal);
            watch.stop();
            log.append(watch.elapsed(logUnit));

            assertEquals(res.longValue(), cur.longValue());
            log.append(LB);

            log("Round " + round + " passed");
        }

        String title = TEST_NAME+"_prime192v1"+"_TBS_"+TableSize+"_Threads_"+NumThreads+"_"+numBits+"_CRT_"+crtPartions+".csv";
        if(USE_HTTP_TRANFER)
            HttpFileTransfer.sendData(title + LB + log.toString());
        else {
            OutputFile benchFile = new OutputFile(title);
            benchFile.writeToFile(log.toString());
        }
    }

    private void runBench(int TableSize, int NumThreads, boolean is32bit, int numITER, TimeUnit logUnit) throws Exception {
        int numBits = is32bit ? 32 : 64;

        log("Run Bench:::TableSize->"+TableSize+":NumThreads->"+NumThreads+":NumBits->"+numBits+":CRT-Partitions->"+1+":::NumIterations->"+numITER);

        FastECElGamal gamal = new FastECElGamal(FastECElGamal.generateKeys(new PRNGRC4Stream(key)),new PRNGImpl());
        Setting.TABLE_POW_2_SIZE = String.valueOf(TableSize);
        Setting.NUM_THREADS = NumThreads;
        StringBuilder log = new StringBuilder();
        appendTitle(log);

        //warmup
        FastECElGamal.NativeECElgamalCipher t = gamal.encrypt(BigInteger.valueOf(1));
        gamal.decrypt(t, numBits, true);

        for(int round=1; round<=numITER; round++) {
            BigInteger res, cur = getRandValue(is32bit);
            watch.reset();
            watch.start();
            FastECElGamal.NativeECElgamalCipher cipher = gamal.encrypt(cur);
            watch.stop();
            log.append(watch.elapsed(logUnit));
            watch.reset();

            log.append(DELIM);
            watch.start();
            res = gamal.decrypt(cipher, numBits, true);
            watch.stop();
            log.append(watch.elapsed(logUnit));

            assertEquals(res.longValue(), cur.longValue());
            log.append(LB);
            log("Round " + round + " passed");
        }


        String title = TEST_NAME+"_TBS_"+TableSize+"_Threads_"+NumThreads+"_"+numBits+"_CRT_"+1+".csv";
        if(USE_HTTP_TRANFER)
            HttpFileTransfer.sendData(title + LB + log.toString());
        else {
            OutputFile benchFile = new OutputFile(title);
            benchFile.writeToFile(log.toString());
        }
    }

    private void runBenchSmall(int TableSize, int NumThreads, int numbits, int numITER, TimeUnit logUnit) throws Exception {
        int numBits = numbits;

        log("Run Bench:::TableSize->"+TableSize+":NumThreads->"+NumThreads+":NumBits->"+numBits+":CRT-Partitions->"+1+":::NumIterations->"+numITER);

        FastECElGamal gamal = new FastECElGamal(FastECElGamal.generateKeys(new PRNGRC4Stream(key)),new PRNGImpl());
        Setting.TABLE_POW_2_SIZE = String.valueOf(TableSize);
        Setting.NUM_THREADS = NumThreads;
        StringBuilder log = new StringBuilder();
        appendTitle(log);

        //warmup
        FastECElGamal.NativeECElgamalCipher t = gamal.encrypt(BigInteger.valueOf(1));
        gamal.decrypt(t, numBits, true);

        for(int round=1; round<=numITER; round++) {
            BigInteger res, cur = new BigInteger(numBits, rand);
            watch.reset();
            watch.start();
            FastECElGamal.NativeECElgamalCipher cipher = gamal.encrypt(cur);
            watch.stop();
            log.append(watch.elapsed(logUnit));
            watch.reset();

            log.append(DELIM);
            watch.start();
            res = gamal.decrypt(cipher, numBits, true);
            watch.stop();
            log.append(watch.elapsed(logUnit));

            assertEquals(res.longValue(), cur.longValue());
            log.append(LB);
            log("Round " + round + " passed");
        }


        String title = TEST_NAME+"_secp224k1_TBS_"+TableSize+"_Threads_"+NumThreads+"_"+numBits+"_CRT_"+1+".csv";
        if(USE_HTTP_TRANFER)
            HttpFileTransfer.sendData(title + LB + log.toString());
        else {
            OutputFile benchFile = new OutputFile(title);
            benchFile.writeToFile(log.toString());
        }
    }

    private void experimentBitwiseTime(int TableSize, int NumThreads, int minBit, int maxBits) throws Exception {
        int numRoundsPerBit = 100;
        Setting.TABLE_POW_2_SIZE = String.valueOf(TableSize);
        Setting.NUM_THREADS = NumThreads;

        FastECElGamal gamal = new FastECElGamal(FastECElGamal.generateKeys(new PRNGRC4Stream(key)),new PRNGImpl());

        StringBuilder log = new StringBuilder();
        log.append("NumBits").append(DELIM).append("AvgDecTime").append(LB);

        //warmup
        FastECElGamal.NativeECElgamalCipher t = gamal.encrypt(BigInteger.valueOf(1));
        gamal.decrypt(t, 2, true);

        for(int numBit=minBit; numBit<=maxBits; numBit++) {
            long timeSum = 0;
            if(numBit<=34)
                numRoundsPerBit = 1000;
            else
                numRoundsPerBit = 100;

            for(int curRound=0; curRound<numRoundsPerBit; curRound++) {
                BigInteger res, cur = new BigInteger(numBit, rand);
                FastECElGamal.NativeECElgamalCipher cipher = gamal.encrypt(cur);
                watch.reset();
                watch.start();
                res = gamal.decrypt(cipher, numBit, false);
                watch.stop();
                timeSum += watch.elapsed(TimeUnit.NANOSECONDS);
                assertEquals(res.longValue(), cur.longValue());
            }

            BigDecimal avg = BigDecimal.valueOf(timeSum)
                    .divide(BigDecimal.valueOf(numRoundsPerBit))
                    .divide(BigDecimal.valueOf(1000000));
            avg = avg.setScale(2, BigDecimal.ROUND_HALF_UP);
            log(numBit + " for " + avg.toString());
            log.append(numBit).append(DELIM).append(avg).append(LB);
        }
        String title = "BitwiseExperiment"+"_TBS_"+TableSize+"_Threads_"+NumThreads+"_"+minBit+"_"+maxBits+".csv";
        if(USE_HTTP_TRANFER)
            HttpFileTransfer.sendData(title + LB + log.toString());
        else {
            OutputFile benchFile = new OutputFile(title);
            benchFile.writeToFile(log.toString());
        }
    }

    private void experimentBulk(int TableSize, int NumThreads, boolean is32bit, int crtPartions, int numIterations, int[] bulkSizes) throws Exception {
        Setting.TABLE_POW_2_SIZE = String.valueOf(TableSize);
        Setting.NUM_THREADS = NumThreads;
        TimeUnit unit = TimeUnit.NANOSECONDS;



        int partSize = computePartionsSize(is32bit, crtPartions);
        int numBits = is32bit ? 32 : 64;
        long maxVal = is32bit ? Integer.MAX_VALUE : Long.MAX_VALUE;

        CRTEcElGamal gamal = createCipher(crtPartions, partSize);
        StringBuilder log = new StringBuilder();
        for(int i=0; i < bulkSizes.length; i++) {
            log.append(bulkSizes[i]);
            if(i==bulkSizes.length-1)
                log.append(LB);
            else
                log.append(DELIM);
        }

        //warmup
        CRTEcElGamal.CRTEcElGamalCipher t = gamal.encrypt(BigInteger.valueOf(1));
        gamal.decrypt(t, Integer.MAX_VALUE);

        for(int round=1; round<=numIterations; round++) {
            log("=================================================");
            for(int bulkSize=0; bulkSize<bulkSizes.length; bulkSize++) {
                ArrayList<CRTEcElGamal.CRTEcElGamalCipher> ciphers = new ArrayList<>();
                for(int subRound=0; subRound<bulkSizes[bulkSize]; subRound++) {
                    BigInteger cur = getRandValue(is32bit);
                    CRTEcElGamal.CRTEcElGamalCipher cipher = gamal.encrypt(cur);
                    ciphers.add(cipher);
                }

                watch.reset();
                watch.start();
                for(CRTEcElGamal.CRTEcElGamalCipher cipher : ciphers)
                    gamal.decrypt(cipher, maxVal);
                watch.stop();
                log("Bulk with size " + bulkSizes[bulkSize] + " passed");
                log.append(watch.elapsed(unit));
                if(bulkSize==bulkSizes.length-1)
                    log.append(LB);
                else
                    log.append(DELIM);
            }
            log("Round " + round + " passed");
            log("=================================================");

        }

        String title = "BulkExperiment"+"_secp160r1"+"_TBS_"+TableSize+"_Threads_"+NumThreads+"_"+numBits+"_CRT_"+crtPartions+".csv";
        if(USE_HTTP_TRANFER) {
            HttpFileTransfer.sendData(title + LB + log.toString());
            OutputFile benchFile = new OutputFile(title);
            benchFile.writeToFile(log.toString());
        }
    }

    private void experimentBulkNoCRT(int bits ,int TableSize, int NumThreads, int numIterations, int[] bulkSizes) throws Exception {
        Setting.TABLE_POW_2_SIZE = String.valueOf(TableSize);
        Setting.NUM_THREADS = NumThreads;
        TimeUnit unit = TimeUnit.NANOSECONDS;

        int numBits = bits;

        FastECElGamal gamal = new FastECElGamal(FastECElGamal.generateKeys(new PRNGRC4Stream(key)),new PRNGImpl());
        StringBuilder log = new StringBuilder();
        for(int i=0; i < bulkSizes.length; i++) {
            log.append(bulkSizes[i]);
            if(i==bulkSizes.length-1)
                log.append(LB);
            else
                log.append(DELIM);
        }

        //warmup
        NativeECElGamalCrypto.NativeECElgamalCipher t = gamal.encrypt(BigInteger.valueOf(1));
        gamal.decrypt(t, 16, false);

        for(int round=1; round<=numIterations; round++) {
            log("=================================================");
            for(int bulkSize=0; bulkSize<bulkSizes.length; bulkSize++) {
                ArrayList<NativeECElGamalCrypto.NativeECElgamalCipher> ciphers = new ArrayList<>();
                for(int subRound=0; subRound<bulkSizes[bulkSize]; subRound++) {
                    BigInteger cur = new BigInteger(numBits,rand);
                    NativeECElGamalCrypto.NativeECElgamalCipher cipher = gamal.encrypt(cur);
                    ciphers.add(cipher);
                }

                watch.reset();
                watch.start();
                for(NativeECElGamalCrypto.NativeECElgamalCipher cipher : ciphers)
                    gamal.decrypt(cipher, numBits+1, false);
                watch.stop();
                log("Bulk with size " + bulkSizes[bulkSize] + " passed");
                log.append(watch.elapsed(unit));
                if(bulkSize==bulkSizes.length-1)
                    log.append(LB);
                else
                    log.append(DELIM);
            }
            log("Round " + round + " passed");
            log("=================================================");

        }

        String title = "BulkExperiment"+"_secp224k1"+"_TBS_"+TableSize+"_Threads_"+NumThreads+"_"+numBits+"_CRT_"+1+".csv";
        if(USE_HTTP_TRANFER) {
            HttpFileTransfer.sendData(title + LB + log.toString());
            OutputFile benchFile = new OutputFile(title);
            benchFile.writeToFile(log.toString());
        }
    }

    private void experimentBulkPar(int TableSize, int NumThreads, boolean is32bit, int crtPartions, int numIterations, int[] bulkSizes, int poolSize) throws Exception {
        Setting.TABLE_POW_2_SIZE = String.valueOf(TableSize);
        Setting.NUM_THREADS = NumThreads;
        TimeUnit unit = TimeUnit.NANOSECONDS;
        ExecutorService pool = Executors.newFixedThreadPool(poolSize);

        int partSize = computePartionsSize(is32bit, crtPartions);
        int numBits = is32bit ? 32 : 64;
        final long maxVal = is32bit ? Integer.MAX_VALUE : Long.MAX_VALUE;

        final CRTEcElGamal gamal = createCipher(crtPartions, partSize);
        StringBuilder log = new StringBuilder();
        for(int i=0; i < bulkSizes.length; i++) {
            log.append(bulkSizes[i]);
            if(i==bulkSizes.length-1)
                log.append(LB);
            else
                log.append(DELIM);
        }

        //warmup
        CRTEcElGamal.CRTEcElGamalCipher t = gamal.encrypt(BigInteger.valueOf(1));
        gamal.decrypt(t, Integer.MAX_VALUE);

        for(int round=1; round<=numIterations; round++) {
            log("=================================================");
            for(int bulkSize=0; bulkSize<bulkSizes.length; bulkSize++) {
                ArrayList<CRTEcElGamal.CRTEcElGamalCipher> ciphers = new ArrayList<>();
                for(int subRound=0; subRound<bulkSizes[bulkSize]; subRound++) {
                    BigInteger cur = getRandValue(is32bit);
                    CRTEcElGamal.CRTEcElGamalCipher cipher = gamal.encrypt(cur);
                    ciphers.add(cipher);
                }

                ArrayList<ArrayList<CRTEcElGamal.CRTEcElGamalCipher>> parts = new ArrayList<>();
                Semaphore sem;
                if(ciphers.size()<poolSize) {
                    parts.add(ciphers);
                    sem = new Semaphore(0);
                } else {
                    int num = bulkSizes[bulkSize] / poolSize, cur = num, part = 1;
                    ArrayList<CRTEcElGamal.CRTEcElGamalCipher> curPart = new ArrayList<>();
                    for(int i=0; i<ciphers.size(); i++) {
                        if(i>=cur) {
                            if(part == poolSize-1)
                                cur = ciphers.size();
                            else
                                cur += num;
                            parts.add(curPart);
                            curPart = new ArrayList<>();
                            part++;
                        }
                        curPart.add(ciphers.get(i));
                    }
                    parts.add(curPart);
                    sem = new Semaphore(-poolSize+1);
                }

                final Semaphore counter = sem;
                watch.reset();
                watch.start();
                for(final ArrayList<CRTEcElGamal.CRTEcElGamalCipher> curPart : parts) {
                    log("Partsize = "+curPart.size());
                    pool.execute(new Runnable() {
                        @Override
                        public void run() {
                            for(CRTEcElGamal.CRTEcElGamalCipher ciph : curPart)
                                try {
                                    gamal.decrypt(ciph, maxVal);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            log("Released " + Thread.currentThread().getName());
                            counter.release();
                        }
                    });
                }
                counter.acquire();
                watch.stop();
                log("Bulk with size " + bulkSizes[bulkSize] + " passed");
                log.append(watch.elapsed(unit));
                if(bulkSize==bulkSizes.length-1)
                    log.append(LB);
                else
                    log.append(DELIM);
            }
            log("Round " + round + " passed");
            log("=================================================");

        }

        String title = "BulkExperiment"+"_secp160r1"+"_TBS_"+TableSize+"_Threads_"+NumThreads+"_"+numBits+"_CRT_"+crtPartions+"_PAR_"+poolSize+".csv";
        if(USE_HTTP_TRANFER) {
            HttpFileTransfer.sendData(title + LB + log.toString());
            OutputFile benchFile = new OutputFile(title);
            benchFile.writeToFile(log.toString());
        }

        pool.shutdown();
    }

    private void experimentBulkParNoCRT(int bits, int TableSize, int NumThreads, int numIterations, int[] bulkSizes, int poolSize) throws Exception {
        Setting.TABLE_POW_2_SIZE = String.valueOf(TableSize);
        Setting.NUM_THREADS = NumThreads;
        TimeUnit unit = TimeUnit.NANOSECONDS;
        ExecutorService pool = Executors.newFixedThreadPool(poolSize);
        final int numBits = bits;

        final  FastECElGamal gamal = new FastECElGamal(FastECElGamal.generateKeys(new PRNGRC4Stream(key)),new PRNGImpl());
        StringBuilder log = new StringBuilder();
        for(int i=0; i < bulkSizes.length; i++) {
            log.append(bulkSizes[i]);
            if(i==bulkSizes.length-1)
                log.append(LB);
            else
                log.append(DELIM);
        }

        //warmup
        NativeECElGamalCrypto.NativeECElgamalCipher t = gamal.encrypt(BigInteger.valueOf(1));
        gamal.decrypt(t, 32, true);

        for(int round=1; round<=numIterations; round++) {
            log("=================================================");
            for(int bulkSize=0; bulkSize<bulkSizes.length; bulkSize++) {
                ArrayList<NativeECElGamalCrypto.NativeECElgamalCipher> ciphers = new ArrayList<>();
                for(int subRound=0; subRound<bulkSizes[bulkSize]; subRound++) {
                    BigInteger cur = new BigInteger(numBits, rand);
                    NativeECElGamalCrypto.NativeECElgamalCipher cipher = gamal.encrypt(cur);
                    ciphers.add(cipher);
                }

                ArrayList<ArrayList<NativeECElGamalCrypto.NativeECElgamalCipher>> parts = new ArrayList<>();
                Semaphore sem;
                if(ciphers.size()<poolSize) {
                    parts.add(ciphers);
                    sem = new Semaphore(0);
                } else {
                    int num = bulkSizes[bulkSize] / poolSize, cur = num, part = 1;
                    ArrayList<NativeECElGamalCrypto.NativeECElgamalCipher> curPart = new ArrayList<>();
                    for(int i=0; i<ciphers.size(); i++) {
                        if(i>=cur) {
                            if(part == poolSize-1)
                                cur = ciphers.size();
                            else
                                cur += num;
                            parts.add(curPart);
                            curPart = new ArrayList<>();
                            part++;
                        }
                        curPart.add(ciphers.get(i));
                    }
                    parts.add(curPart);
                    sem = new Semaphore(-poolSize+1);
                }

                final Semaphore counter = sem;
                watch.reset();
                watch.start();
                for(final ArrayList<NativeECElGamalCrypto.NativeECElgamalCipher> curPart : parts) {
                    log("Partsize = "+curPart.size());
                    pool.execute(new Runnable() {
                        @Override
                        public void run() {
                            for(NativeECElGamalCrypto.NativeECElgamalCipher ciph : curPart)
                                try {
                                    gamal.decrypt(ciph, numBits + 1, false);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            log("Released " + Thread.currentThread().getName());
                            counter.release();
                        }
                    });
                }
                counter.acquire();
                watch.stop();
                log("Bulk with size " + bulkSizes[bulkSize] + " passed");
                log.append(watch.elapsed(unit));
                if(bulkSize==bulkSizes.length-1)
                    log.append(LB);
                else
                    log.append(DELIM);
            }
            log("Round " + round + " passed");
            log("=================================================");

        }

        String title = "BulkExperiment"+"_secp224k1"+"_TBS_"+TableSize+"_Threads_"+NumThreads+"_"+numBits+"_CRT_"+1+"_PAR_"+poolSize+".csv";
        if(USE_HTTP_TRANFER) {
            HttpFileTransfer.sendData(title + LB + log.toString());
            OutputFile benchFile = new OutputFile(title);
            benchFile.writeToFile(log.toString());
        }

        pool.shutdown();
    }

    private void experimentBulkParComparePaillier(int paillierBits, boolean is32bit, int numIterations, int[] bulkSizes, int poolSize) throws Exception {
        TimeUnit unit = TimeUnit.NANOSECONDS;
        ExecutorService pool = Executors.newFixedThreadPool(poolSize);

        int numBits = is32bit ? 32 : 64;
        final long maxVal = is32bit ? Integer.MAX_VALUE : Long.MAX_VALUE;

        KeyPair pair = PaillierPriv.keygen(new PRNGRC4Stream(key),paillierBits, paillierBits/4);
        final PaillierPriv pailDec= new PaillierPriv(pair);
        Paillier pailEnc = pailDec;

        StringBuilder log = new StringBuilder();
        for(int i=0; i < bulkSizes.length; i++) {
            log.append(bulkSizes[i]);
            if(i==bulkSizes.length-1)
                log.append(LB);
            else
                log.append(DELIM);
        }

        //warmup
        BigInteger t = pailEnc.encrypt(BigInteger.valueOf(1));
        pailDec.decrypt(t);

        for(int round=1; round<=numIterations; round++) {
            log("=================================================");
            for(int bulkSize=0; bulkSize<bulkSizes.length; bulkSize++) {
                ArrayList<BigInteger> ciphers = new ArrayList<>();
                for(int subRound=0; subRound<bulkSizes[bulkSize]; subRound++) {
                    BigInteger cur = getRandValue(is32bit);
                    BigInteger cipher = pailDec.encrypt(cur);
                    ciphers.add(cipher);
                }

                ArrayList<ArrayList<BigInteger>> parts = new ArrayList<>();
                Semaphore sem;
                if(ciphers.size()<poolSize) {
                    parts.add(ciphers);
                    sem = new Semaphore(0);
                } else {
                    int num = bulkSizes[bulkSize] / poolSize, cur = num, part = 1;
                    ArrayList<BigInteger> curPart = new ArrayList<>();
                    for(int i=0; i<ciphers.size(); i++) {
                        if(i>=cur) {
                            if(part == poolSize-1)
                                cur = ciphers.size();
                            else
                                cur += num;
                            parts.add(curPart);
                            curPart = new ArrayList<>();
                            part++;
                        }
                        curPart.add(ciphers.get(i));
                    }
                    parts.add(curPart);
                    sem = new Semaphore(-poolSize+1);
                }

                final Semaphore counter = sem;
                watch.reset();
                watch.start();
                for(final ArrayList<BigInteger> curPart : parts) {
                    log("Partsize = "+curPart.size());
                    pool.execute(new Runnable() {
                        @Override
                        public void run() {
                            for(BigInteger ciph : curPart)
                                try {
                                    pailDec.decrypt(ciph);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            log("Released " + Thread.currentThread().getName());
                            counter.release();
                        }
                    });
                }
                counter.acquire();
                watch.stop();
                log("Bulk with size " + bulkSizes[bulkSize] + " passed");
                log.append(watch.elapsed(unit));
                if(bulkSize==bulkSizes.length-1)
                    log.append(LB);
                else
                    log.append(DELIM);
            }
            log("Round " + round + " passed");
            log("=================================================");

        }

        String title = "BulkExperiment"+"_SecParam_"+paillierBits+"_PAR_"+poolSize+".csv";
        if(USE_HTTP_TRANFER) {
            HttpFileTransfer.sendData(title + LB + log.toString());
            OutputFile benchFile = new OutputFile(title);
            benchFile.writeToFile(log.toString());
        }

        pool.shutdown();
    }

    private void bulkExperimentComparePaillier(int paillierBits, boolean is32bit, int numIterations, int[] bulkSizes) throws Exception {
        TimeUnit unit = TimeUnit.NANOSECONDS;
        int numBits = is32bit ? 32 : 64;
        long maxVal = is32bit ? Integer.MAX_VALUE : Long.MAX_VALUE;

        KeyPair pair = PaillierPriv.keygen(new PRNGRC4Stream(key),paillierBits, paillierBits/4);
        PaillierPriv pailDec= new PaillierPriv(pair);
        Paillier pailEnc = pailDec;

        StringBuilder log = new StringBuilder();
        for(int i=0; i < bulkSizes.length; i++) {
            log.append(bulkSizes[i]);
            if(i==bulkSizes.length-1)
                log.append(LB);
            else
                log.append(DELIM);
        }

        //warmup
        BigInteger t = pailEnc.encrypt(BigInteger.valueOf(1));
        pailDec.decrypt(t);

        for(int round=1; round<=numIterations; round++) {
            log("=================================================");
            for(int bulkSize=0; bulkSize<bulkSizes.length; bulkSize++) {
                ArrayList<BigInteger> ciphers = new ArrayList<>();
                for(int subRound=0; subRound<bulkSizes[bulkSize]; subRound++) {
                    BigInteger cur = getRandValue(is32bit);
                    BigInteger cipher = pailEnc.encrypt(cur);
                    ciphers.add(cipher);
                }

                watch.reset();
                watch.start();
                for(BigInteger cipher : ciphers)
                    pailDec.decrypt(cipher);
                watch.stop();
                log("Bulk with size " + bulkSizes[bulkSize] + " passed");
                log.append(watch.elapsed(unit));
                if(bulkSize==bulkSizes.length-1)
                    log.append(LB);
                else
                    log.append(DELIM);
            }
            log("Round " + round + " passed");
            log("=================================================");

        }

        String title = "BulkExperiment_SecParam_"+paillierBits+"_"+numBits+"2.csv";
        if(USE_HTTP_TRANFER)
            HttpFileTransfer.sendData(title + LB + log.toString());
        else {
            OutputFile benchFile = new OutputFile(title);
            benchFile.writeToFile(log.toString());
        }
    }

    private void comparePaillier(int paillierBits, boolean is32bit, int numITER) throws Exception {
        KeyPair pair = PaillierPriv.keygen(new PRNGRC4Stream(key),paillierBits, paillierBits/4);
        PaillierPriv pailDec= new PaillierPriv(pair);
        Paillier pailEnc = pailDec;
        StringBuilder log = new StringBuilder();
        int numBits = is32bit ? 32 : 64;
        appendTitle(log);

        for(int round=1; round<=numITER; round++) {
            BigInteger temp, res, cur = getRandValue(is32bit);
            watch.reset();
            watch.start();
            temp = pailEnc.encrypt(cur);
            watch.stop();
            log.append(watch.elapsed(TimeUnit.NANOSECONDS));
            watch.reset();

            log.append(DELIM);
            watch.start();
            res = pailDec.decrypt(temp);
            watch.stop();
            log.append(watch.elapsed(TimeUnit.NANOSECONDS));

            assertEquals(res.longValue(), cur.longValue());
            log.append(LB);
            log("Round " + round + " passed");
        }

        String title = "PaillierBench"+"_SecParam_"+paillierBits+"_"+numBits+".csv";
        if(USE_HTTP_TRANFER)
            HttpFileTransfer.sendData(title + LB + log.toString());
        else {
            OutputFile benchFile = new OutputFile(title);
            benchFile.writeToFile(log.toString());
        }
    }

    public void testKeyGenPaillier(int numBits, int numITER) throws Exception{
        StringBuilder log = new StringBuilder();
        log.append("KeyGen Time").append(LB);
        for(int round=1; round<=numITER; round++) {
            watch.reset();
            rand.nextBytes(key);
            watch.start();
            KeyPair pair = PaillierPriv.keygen(new PRNGRC4Stream(key),numBits, numBits/4);
            watch.stop();
            log.append(watch.elapsed(TimeUnit.NANOSECONDS)).append(LB);
        }
        String title = "PaillierKeyGen"+"_SecParam_"+numBits+".csv";
        if(USE_HTTP_TRANFER)
            HttpFileTransfer.sendData(title + LB + log.toString());
        else {
            OutputFile benchFile = new OutputFile(title);
            benchFile.writeToFile(log.toString());
        }
    }

    public void testKeyGenGamal(boolean is32bit, int crtPartions, int numITER) throws Exception{
        StringBuilder log = new StringBuilder();
        log.append("KeyGen Time").append(LB);
        int numBits = is32bit ? 32 : 64;
        for(int round=1; round<=numITER; round++) {
            watch.reset();
            rand.nextBytes(key);
            watch.start();
            CRTEcElGamal.generateKeys(new PRNGRC4Stream(key), computePartionsSize(is32bit, crtPartions), crtPartions);
            watch.stop();
            log.append(watch.elapsed(TimeUnit.NANOSECONDS)).append(LB);
        }
        String title = "EcElGamalKeyGen_secp224k1"+"_CRT_"+crtPartions+"_BITS_"+numBits+".csv";
        if(USE_HTTP_TRANFER)
            HttpFileTransfer.sendData(title + LB + log.toString());
        else {
            OutputFile benchFile = new OutputFile(title);
            benchFile.writeToFile(log.toString());
        }
    }

    private BigInteger getRandValue(boolean is32bit) {
        if(is32bit) {
            return BigInteger.valueOf(getRandomInteger());
        } else {
            return BigInteger.valueOf(getRandomLong());
        }
    }

    private static void appendTitle(StringBuilder sb) {
        sb.append(ENC_TIME_TITLE).append(DELIM).append(DEC_TIME_TITLE).append(LB);
    }

    private CRTEcElGamal createCipher(int crtPartions, int partSize) {
        return new CRTEcElGamal(CRTEcElGamal.generateKeys(new PRNGRC4Stream(key),partSize,crtPartions),new PRNGImpl());
    }

    private int computePartionsSize(boolean is32bit, int crtPartions) {
        if(is32bit) {
            return (int) ((32.0/((double)crtPartions)) +1);
        } else {
            return (int) ((64.0/((double)crtPartions)) +1);
        }
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        ContextHolder.setContext(this.getInstrumentation().getContext());
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        FastECElGamal.TableLoader.freeTables();
    }





}
