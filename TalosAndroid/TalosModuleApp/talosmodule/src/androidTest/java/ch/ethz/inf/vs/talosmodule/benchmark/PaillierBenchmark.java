package ch.ethz.inf.vs.talosmodule.benchmark;

import android.util.Log;

import com.google.common.base.Stopwatch;

import junit.framework.TestCase;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.KeyPair;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import ch.ethz.inf.vs.talosmodule.HttpFileTransfer;
import ch.ethz.inf.vs.talosmodule.cryptoalg.IPRNG;
import ch.ethz.inf.vs.talosmodule.cryptoalg.PRNGRC4Stream;
import ch.ethz.inf.vs.talosmodule.cryptoalg.PaillierPriv;

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

public class PaillierBenchmark extends TestCase {


    private static int NUM_ITERATIONS = 1000;
    private static int NUM_ITERATIONS_KEY = 10;


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

    public void benchmarkPaillier(String name, int numPrimeBits, int integerBits) {
        IPRNG prng = new PRNGRC4Stream(new byte[16]);
        PaillierPriv[] pailliers = new PaillierPriv[NUM_ITERATIONS_KEY];
        KeyPair[] keyPairs = new KeyPair[NUM_ITERATIONS_KEY];

        long[][] times = new long[3][NUM_ITERATIONS];
        String[] colNames = new String[] {"Key gen", "Encryption", "Decryption"};
        Stopwatch watch = Stopwatch.createUnstarted();

        TimeUnit u = TimeUnit.NANOSECONDS;

        //key gen
        for(int i=0; i<NUM_ITERATIONS_KEY; i++) {
            watch.reset();
            watch.start();
            keyPairs[i] = PaillierPriv.keygen(prng, numPrimeBits, numPrimeBits/4);
            watch.stop();
            times[0][i] = watch.elapsed(TimeUnit.NANOSECONDS);
            log("Key Gen Round " + i);
            pailliers[i] = new PaillierPriv(keyPairs[i]);
        }

        BigInteger[] plaintexts = get_plaintexts_BN(integerBits);
        BigInteger[] ciphers = new BigInteger[NUM_ITERATIONS];

        //encryption
        for(int i=0; i<NUM_ITERATIONS; i++) {
            watch.reset();
            watch.start();
            ciphers[i] = pailliers[i % NUM_ITERATIONS_KEY].encrypt(plaintexts[i]);
            watch.stop();
            times[1][i] = watch.elapsed(TimeUnit.NANOSECONDS);
            log("Enc Round " + i);
        }

        BigInteger[] res_dec = new BigInteger[NUM_ITERATIONS];

        //Decryption
        for(int i=0; i<NUM_ITERATIONS; i++) {
            watch.reset();
            watch.start();
            res_dec[i] =pailliers[i % NUM_ITERATIONS_KEY].decrypt(ciphers[i]);
            watch.stop();
            times[2][i] = watch.elapsed(TimeUnit.NANOSECONDS);
            log("Dec Round " + i);
        }

        assertTrue(check_res_BN(plaintexts, res_dec, NUM_ITERATIONS));


        try {
            HttpFileTransfer.sendData(name + "\n" + createCSVFile(colNames, times, ";"));
        } catch (IOException e) {
            e.printStackTrace();
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

    public void testBenchmarkPaillier() {
        benchmarkPaillier("paillier-SEC128.csv", 3072, 64);
    }
}
