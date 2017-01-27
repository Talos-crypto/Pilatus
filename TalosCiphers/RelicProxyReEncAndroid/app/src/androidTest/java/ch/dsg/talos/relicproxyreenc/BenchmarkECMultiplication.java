package ch.dsg.talos.relicproxyreenc;

import android.util.Log;

import com.google.common.base.Stopwatch;

import junit.framework.TestCase;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import ch.dsg.talos.relicproxyreenc.crypto.BenchMulOpenSSL;
import ch.dsg.talos.relicproxyreenc.crypto.BenchMulRelic;

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

public class BenchmarkECMultiplication extends TestCase {

    private static int NUM_ITERATIONS = 1000;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    private static void log(String msg) {
        Log.i("BenchMulOpenSSL", msg);
    }

    Stopwatch watch = Stopwatch.createUnstarted();

    public void benchmark_curve(String curvename, int value, boolean is_relic) {
        BigDecimal total, overhead;
        long temp = 0;
        log("------BenchMulOpenSSL: " + curvename + "------");
        if(is_relic) {
        } else {
            BenchMulOpenSSL.initOpenSSLCurve(value);
        }
        watch.reset();
        watch.start();
        for(int i=0; i<NUM_ITERATIONS; i++) {
            if(is_relic) {
                temp = BenchMulRelic.benchRelicMul();
            } else {
                temp = BenchMulOpenSSL.OpenSSLMul();
            }
        }
        watch.stop();
        total = BigDecimal.valueOf(watch.elapsed(TimeUnit.NANOSECONDS));
        watch.reset();
        watch.start();
        for(int i=0; i<NUM_ITERATIONS; i++) {
            if(is_relic) {
                temp = BenchMulRelic.benchRelicOverhead();
            } else {
                temp = BenchMulOpenSSL.OpenSSLOverhead();
            }
        }
        watch.stop();
        overhead = BigDecimal.valueOf(watch.elapsed(TimeUnit.NANOSECONDS));

        total = total.subtract(overhead);
        log("Total time for " + NUM_ITERATIONS + "Iterations is " + total.toString());
        total = total.divide(BigDecimal.valueOf(NUM_ITERATIONS), BigDecimal.ROUND_HALF_UP);
        log("Average time is " + total.toString());
    }

    public void testBenchmark() {
        BigDecimal total;
        total = BigDecimal.ZERO;

        //Curve RELIC_CURVE
        benchmark_curve("RELIC_CURVE", -1, true);


        //curve OPENSSL_secp160r1
        //benchmark_curve("OPENSSL_secp160r1", BenchMulOpenSSL.OPENSSL_secp160r1, false);
        //curve OPENSSL_secp224k1
        benchmark_curve("OPENSSL_secp224k1", BenchMulOpenSSL.OPENSSL_secp224k1, false);
        //curve OPENSSL_prime192v1
        //benchmark_curve("OPENSSL_prime192v1", BenchMulOpenSSL.OPENSSL_prime192v1, false);

    }
}
