package ch.ethz.inf.vs.talosmodule.ciphers;

import android.util.Log;

import junit.framework.TestCase;

import org.spongycastle.crypto.engines.AESEngine;

import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Random;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import ch.ethz.inf.vs.talosmodule.cryptoalg.CMCBlockCipher;

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

public class CMCTest extends TestCase {

    private static String TEST_NAME = "CMCTest";

    private byte[] key = null;

    private static final int MAX_IT = 1000;

    private Random rand = new Random();

    public CMCTest() {
        super(TEST_NAME);
    }

    private void log(String name) {
        Log.d(TEST_NAME, name);
    }


    public void testTest1() throws Exception {
        for(int round = 0; round< MAX_IT; round ++) {
            partRun(round+1);
        }
    }

    public void testTest2() throws Exception {
        byte[] ctext, ptextE;
        byte[] ptextS = new byte[128/8];
        rand.nextBytes(ptextS);
        CMCBlockCipher cipher = new CMCBlockCipher(new AESEngine(), key);
        ctext = cipher.encrypt(ptextS);
        ptextE = cipher.decrypt(ctext);
        checkArrayEqual(ptextS, ptextE);
    }

    private void partRun(int iteration) {
        byte[] ctext, ptextE;
        byte[] ptextS = new byte[rand.nextInt(1000)];
        rand.nextBytes(ptextS);
        CMCBlockCipher cipher = new CMCBlockCipher(new AESEngine(), key);
        ctext = cipher.encrypt(ptextS);
        ptextE = cipher.decrypt(ctext);
        log("Round: "+iteration+"\np1: "+ Arrays.toString(ptextS)+ "\np2: "+ Arrays.toString(ptextE));
        checkArrayEqual(ptextS, ptextE);
    }

    private void checkArrayEqual(byte[] a, byte[] b) {
        assertEquals(a.length,b.length);
        for (int i = 0; i < a.length; i++) {
            assertEquals(a[i], b[i]);
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
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }
}
