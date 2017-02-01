package ch.ethz.inf.vs.talosmodule.ciphers;

import android.test.InstrumentationTestCase;
import android.util.Log;

import java.security.NoSuchAlgorithmException;
import java.util.Random;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import ch.ethz.inf.vs.talosmodule.RandomString;
import ch.ethz.inf.vs.talosmodule.crypto.TalosKey;
import ch.ethz.inf.vs.talosmodule.crypto.enclayers.DETAesStr;
import ch.ethz.inf.vs.talosmodule.crypto.enclayers.DETBfInt;
import ch.ethz.inf.vs.talosmodule.crypto.enclayers.EncLayer;
import ch.ethz.inf.vs.talosmodule.crypto.enclayers.HOMCrtGamal;
import ch.ethz.inf.vs.talosmodule.crypto.enclayers.HOMECElGamal;
import ch.ethz.inf.vs.talosmodule.crypto.enclayers.HOMPaillier;
import ch.ethz.inf.vs.talosmodule.crypto.enclayers.RNDAesStr;
import ch.ethz.inf.vs.talosmodule.crypto.enclayers.RNDBfInt;
import ch.ethz.inf.vs.talosmodule.exceptions.TalosModuleException;
import ch.ethz.inf.vs.talosmodule.main.values.TalosCipher;
import ch.ethz.inf.vs.talosmodule.main.values.TalosValue;
import ch.ethz.inf.vs.talosmodule.util.ContextHolder;

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

public class LayersTest extends InstrumentationTestCase {

    private static String TEST_NAME = "LayersTest";

    private byte[] key = null;

    private static final int MAX_IT = 100;

    private Random rand = new Random();

    private RandomString ransStr = new RandomString();

    public LayersTest() {
        super();
    }

    private void log(String name) {
        Log.d(TEST_NAME, name);
    }

    private int getRandomInteger() {
        return rand.nextInt();
    }

    private long getRandomLong() {
        return rand.nextLong();
    }

    private String getRandomString(int maxLength) {
        return ransStr.nextString(maxLength);
    }


    public void testRNDint() throws Exception {
        RNDBfInt layer = new RNDBfInt(new TalosKey(key));
        for(int rounds = 1; rounds<= MAX_IT; rounds++) {
            int rndInt = getRandomInteger();
            TalosValue val = TalosValue.createTalosValue(rndInt);
            TalosValue res = partRunPartTest(layer, val);
            assertEquals(rndInt,res.getInt());
        }
    }

    public void testRNDint64() throws Exception {
        RNDBfInt layer = new RNDBfInt(new TalosKey(key));
        for(int rounds = 1; rounds<= MAX_IT; rounds++) {
            long rndlong = getRandomLong();
            TalosValue val = TalosValue.createTalosValue(rndlong);
            TalosValue res = partRunPartTest(layer, val);
            assertEquals(rndlong,res.getLong());
        }
    }

    public void testRNDstr() throws Exception {
        RNDAesStr layer = new RNDAesStr(new TalosKey(key));
        for(int rounds = 1; rounds<= MAX_IT; rounds++) {
            String before = getRandomString(500);
            TalosValue val = TalosValue.createTalosValue(before);
            TalosValue res = partRunPartTest(layer, val);
            assertEquals(before,res.getString());
        }
    }

    public void testDETint() throws Exception {
        DETBfInt layer = new DETBfInt(new TalosKey(key));
        for(int rounds = 1; rounds<= MAX_IT; rounds++) {
            int rndInt = getRandomInteger();
            TalosValue val = TalosValue.createTalosValue(rndInt);
            TalosValue res = partRunPartTest(layer, val);
            assertEquals(rndInt,res.getInt());
        }
    }

    public void testDETint64() throws Exception {
        DETBfInt layer = new DETBfInt(new TalosKey(key));
        for(int rounds = 1; rounds<= MAX_IT; rounds++) {
            long rndlong = getRandomLong();
            TalosValue val = TalosValue.createTalosValue(rndlong);
            TalosValue res = partRunPartTest(layer, val);
            assertEquals(rndlong,res.getLong());
        }
    }

    public void testDETstr() throws Exception {
        DETAesStr layer = new DETAesStr(new TalosKey(key));
        for(int rounds = 1; rounds<= MAX_IT; rounds++) {
            String before = getRandomString(500);
            TalosValue val = TalosValue.createTalosValue(before);
            TalosValue res = partRunPartTest(layer, val);
            assertEquals(before,res.getString());
        }
    }

    public void testHOMint32() throws Exception {
        HOMECElGamal layer = new HOMECElGamal(new TalosKey(key));
        for(int rounds = 1; rounds<= MAX_IT; rounds++) {
            int rndInt = getRandomInteger();
            TalosValue val = TalosValue.createTalosValue(rndInt);
            TalosValue res = partRunPartTest(layer, val);
            assertEquals(rndInt,res.getInt());
        }
    }

    public void testHOMint32_CRT() throws Exception {
        HOMCrtGamal layer = new HOMCrtGamal(new TalosKey(key), HOMCrtGamal.INT_32);
        for(int rounds = 1; rounds<= MAX_IT; rounds++) {
            int rndInt = getRandomInteger();
            TalosValue val = TalosValue.createTalosValue(rndInt);
            TalosValue res = partRunPartTest(layer, val);
            assertEquals(rndInt,res.getInt());
        }
    }

    public void testHOMint64() throws Exception {
        HOMPaillier layer = new HOMPaillier(new TalosKey(key));
        for(int rounds = 1; rounds<= MAX_IT; rounds++) {
            long rndlong = getRandomLong();
            TalosValue val = TalosValue.createTalosValue(rndlong);
            TalosValue res = partRunPartTest(layer, val);
            assertEquals(rndlong,res.getLong());
        }
    }

    public void testHOMint64_CRT() throws Exception {
        HOMCrtGamal layer = new HOMCrtGamal(new TalosKey(key), HOMCrtGamal.INT_64);
        for(int rounds = 1; rounds<= MAX_IT; rounds++) {
            int rndInt = getRandomInteger();
            TalosValue val = TalosValue.createTalosValue(rndInt);
            TalosValue res = partRunPartTest(layer, val);
            assertEquals(rndInt,res.getInt());
        }
    }

    private TalosValue partRunPartTest(EncLayer layer, TalosValue value) throws TalosModuleException {
        TalosValue before;
        TalosCipher cipher;
        before = value;
        cipher = layer.encrypt(before);
        log("Cipher in database: " + cipher.getStringRep());
        return layer.decrypt(cipher);
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
    }
}
