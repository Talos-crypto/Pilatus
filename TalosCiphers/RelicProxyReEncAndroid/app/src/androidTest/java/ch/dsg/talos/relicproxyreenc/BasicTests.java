package ch.dsg.talos.relicproxyreenc;

import android.util.Log;

import junit.framework.TestCase;

import ch.dsg.talos.relicproxyreenc.crypto.ECElGamalRelic;
import ch.dsg.talos.relicproxyreenc.crypto.PRERelic;
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

public class BasicTests extends TestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testPCB() {
        /*
        byte[] key_alice = PRERelic.generateKey();
        PRERelic.initBsgsTable(200);
        byte[] key_bob = PRERelic.generateKey();
        byte[] cipher_alice = PRERelic.encrypt(11, key_alice);
        long res1 = PRERelic.decrypt(cipher_alice, key_alice, true);
        byte[] token_to_bob = PRERelic.createReEncToken(key_alice, key_bob);
        byte[] cipher_bob= PRERelic.reApply(cipher_alice, token_to_bob);
        long res_bob = PRERelic.decrypt(cipher_bob, key_bob, true);
        Log.d("Test", String.valueOf(res_bob));
        Log.d("Test", "BliBlaBlu");*/

    }

    public void testPRERel1() {
        /*
        byte[] key = PRERelic.generateKey();
        PRERelicCipher.PREKey key_pre = new PRERelicCipher.PREKey(key);
        boolean test = key_pre.containsPrivateKey();
        assertTrue(test);
        byte[] pub = key_pre.getPublicKeyEncoded();
        PRERelicCipher.PREKey key_pre = new PRERelicCipher.PREKey(key);*/
    }

    public void testGAMAL() {
        ECElGamalRelic.ECElGamalKey key = ECElGamalRelic.generateFreshKey();
        ECElGamalRelic.ECElGamalCipher cipher = ECElGamalRelic.encrypt(2, key);
        long res = ECElGamalRelic.decrypt(cipher, key, false);
        Log.d("TESTGAMAL", "Res is" + String.valueOf(res));
    }

}
