package ch.ethz.inf.vs.talosmodule.ciphers;

import android.test.InstrumentationTestCase;
import android.util.Log;

import com.google.common.base.Stopwatch;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import ch.ethz.inf.vs.talosmodule.RandomString;
import ch.ethz.inf.vs.talosmodule.cryptoalg.CRTEcElGamal;
import ch.ethz.inf.vs.talosmodule.cryptoalg.FastECElGamal;
import ch.ethz.inf.vs.talosmodule.cryptoalg.IPRNG;
import ch.ethz.inf.vs.talosmodule.cryptoalg.PRNGImpl;
import ch.ethz.inf.vs.talosmodule.cryptoalg.PRNGRC4Stream;
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

public class CRTEcElGamalTest extends InstrumentationTestCase {

    private static String TEST_NAME = "CRTEcElGamalTest";

    private byte[] key = null;

    private static final int MAX_IT = 1000;

    private Random rand = new Random();

    private RandomString ransStr = new RandomString();

    private Stopwatch watch = Stopwatch.createUnstarted();

    public CRTEcElGamalTest() {
        super();
    }

    private void log(String name) {
        Log.d(TEST_NAME, name);
    }

    private int getRandomInteger() {
        return rand.nextInt();
    }

    private double enctime = 0;
    private double dectime = 0;

    private long getRandomLong() {
        return rand.nextLong();
    }

    private String getRandomString(int maxLength) {
        return ransStr.nextString(maxLength);
    }

    private CRTEcElGamal gamal;

    public void testDefault() throws Exception {
        gamal = new CRTEcElGamal(CRTEcElGamal.generateKeys(new PRNGRC4Stream(key),17,2),new PRNGImpl());
        //warmup
        CRTEcElGamal.CRTEcElGamalCipher t = gamal.encrypt(BigInteger.valueOf(1));
        gamal.decrypt(t, Integer.MAX_VALUE);

        for(int round=1; round<=MAX_IT; round++) {
            int in = rand.nextInt();
            watch.reset();
            watch.start();
            CRTEcElGamal.CRTEcElGamalCipher cipher = gamal.encrypt(BigInteger.valueOf(in));
            watch.stop();
            enctime += watch.elapsed(TimeUnit.MILLISECONDS);
            watch.reset();
            watch.start();
            BigInteger res = gamal.decrypt(cipher, Integer.MAX_VALUE);
            watch.stop();
            dectime += watch.elapsed(TimeUnit.MILLISECONDS);
            //log("Expected: "+in +" Result: "+ res.toString());
            assertEquals(res.longValue(), in);
        }
        log("Enctime: " + String.valueOf(enctime/MAX_IT) + " Dectime: "+String.valueOf(dectime/MAX_IT));
    }

    public void testNegative() throws Exception {
        int in = -1;
        int in2 = -2;
        CRTEcElGamal.CRTEcElGamalCipher cipher = gamal.encrypt(BigInteger.valueOf(in));
        CRTEcElGamal.CRTEcElGamalCipher cipher2 = gamal.encrypt(BigInteger.valueOf(in2));
        CRTEcElGamal.CRTEcElGamalCipher cipher3 = gamal.addCiphers(cipher, cipher2);
        BigInteger res = gamal.decrypt(cipher3, Integer.MAX_VALUE);
        log("Expected: " + -3 + " Result: " + res.toString());
        //assertEquals(realRes.longValue(), in);
    }

    private long getUnsignedInteger() {
        long in = rand.nextLong();
        if(in<0) in=-in;
        in = in % 4294967295L;
        return in;
    }

    public void test64Bit() throws Exception {
        gamal = new CRTEcElGamal(CRTEcElGamal.generateKeys(new PRNGRC4Stream(key),23,3),new PRNGImpl());
        CRTEcElGamal.CRTEcElGamalCipher t = gamal.encrypt(BigInteger.valueOf(1));
        gamal.decrypt(t, Integer.MAX_VALUE);
        Setting.NUM_THREADS=4;

        for(int round=1; round<=MAX_IT; round++) {
            long in = getRandomLong();
            watch.reset();
            watch.start();
            CRTEcElGamal.CRTEcElGamalCipher cipher = gamal.encrypt(BigInteger.valueOf(in));
            watch.stop();
            enctime += watch.elapsed(TimeUnit.MILLISECONDS);
            watch.reset();
            watch.start();
            BigInteger res = gamal.decrypt(cipher, Long.MAX_VALUE);
            watch.stop();
            dectime += watch.elapsed(TimeUnit.MILLISECONDS);
            //log("Expected: "+in +" Result: "+ res.toString());
            assertEquals(res.longValue(), in);
            log("Enctime: " + String.valueOf(enctime / round) + " Dectime: " + String.valueOf(dectime / round));
        }
        log("Enctime: " + String.valueOf(enctime / MAX_IT) + " Dectime: " + String.valueOf(dectime / MAX_IT));
    }

    public void testSums() throws Exception {
        gamal = new CRTEcElGamal(CRTEcElGamal.generateKeys(new PRNGRC4Stream(key),17,2),new PRNGImpl());
        for(int round=1; round<MAX_IT/10; round++) {
            int a = rand.nextInt(10000000);
            int b = rand.nextInt(10000000);
            CRTEcElGamal.CRTEcElGamalCipher cipherA = gamal.encrypt(BigInteger.valueOf(a));
            CRTEcElGamal.CRTEcElGamalCipher cipherB = gamal.encrypt(BigInteger.valueOf(b));
            CRTEcElGamal.CRTEcElGamalCipher cipherAB = gamal.addCiphers(cipherA, cipherB);
            BigInteger res = gamal.decrypt(cipherAB, Integer.MAX_VALUE);
            //log("Expected: " + String.valueOf(a+b) + " Result: " + res.toString());
            assertEquals(res.longValue(), a + b);
        }
    }

    public void testPRNG() {
        key = generateKey();
        IPRNG prng = new PRNGRC4Stream(key);

        for(int round=1; round<MAX_IT; round++) {
            int before, after;
            before = rand.nextInt(54);
            before+=10;
            after = prng.getRandPrime(before).bitLength();
            assertEquals(before, after);
        }
    }


    private byte[] generateKey() {
        KeyGenerator keyGen = null;
        try {
            keyGen = KeyGenerator.getInstance("AES");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        keyGen.init(128);
        SecretKey secretKey = keyGen.generateKey();
        return secretKey.getEncoded();
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        this.key = generateKey();
        ContextHolder.setContext(this.getInstrumentation().getContext());
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        FastECElGamal.TableLoader.freeTables();
    }

}
