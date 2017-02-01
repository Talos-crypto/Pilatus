package ch.ethz.inf.vs.talosmodule.cryptoalg;

import java.math.BigInteger;
import java.security.KeyPair;

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

public class PaillierPrivNative extends PaillierNative {

    static {
        System.loadLibrary("pailliercrypto");
    }

    /* Private key, including g from public part; n=pq */
    private BigInteger p, q;
    private BigInteger a;      /* non-zero for fast mode */

    /* Cached values */
    private boolean fast = true;
    private BigInteger p2, q2;
    private BigInteger two_p, two_q;
    private BigInteger pinv, qinv;
    private BigInteger hp, hq;

    public PaillierPrivNative(KeyPair sk) {
        super(sk.getPublic());
        PaillierPrivKey priv = (PaillierPrivKey) sk.getPrivate();
        p = priv.getP();
        q = priv.getQ();
        a = priv.getA();
        fast = (a.compareTo(BigInteger.ZERO) != 0);
        p2 = p.multiply(p);
        q2 = q.multiply(q);
        BigInteger constTwo = BigInteger.valueOf(2);
        two_p = constTwo.pow(PaillierPriv.numBits(p));
        two_q = constTwo.pow(PaillierPriv.numBits(q));
        pinv = p.modInverse(two_p);
        qinv = q.modInverse(two_q);
        if (fast) {
            hp = PaillierPriv.Lfast((g.mod(p2)).modPow(a, p2), pinv, two_p, p).modInverse(p);
            hq = PaillierPriv.Lfast((g.mod(q2)).modPow(a, q2), qinv, two_q, q).modInverse(q);
        } else {
            hp = PaillierPriv.Lfast((g.mod(p2)).modPow(p.subtract(BigInteger.ONE), p2), pinv, two_p, p).modInverse(p);
            hq = PaillierPriv.Lfast((g.mod(q2)).modPow(p.subtract(BigInteger.ONE), q2), qinv, two_q, q).modInverse(q);
        }
    }

    public BigInteger decrypt(BigInteger ciphertext) {
        BigInteger mp, mq;
        mp = new BigInteger(decryptpart(ciphertext.toString(), p2.toString(), a.toString(), pinv.toString(), two_p.toString(), p.toString(), hp.toString()));
        mq = new BigInteger(decryptpart(ciphertext.toString(), q2.toString(), a.toString(), qinv.toString(), two_q.toString(), q.toString(), hq.toString()));

        BigInteger m, pq;
        m = BigInteger.ZERO;
        pq = BigInteger.ONE;

        BigInteger[] res1 = PaillierPriv.CRT(m, pq, mp, p);
        m = res1[0];
        pq = res1[1];
        BigInteger[] res2 = PaillierPriv.CRT(m, pq, mq, q);
        m = res2[0];
        return m;
    }

    private static native String decryptpart(String j_ciphertext, String j_p2, String j_a, String j_pinv, String j_two_p, String j_p, String j_hp);
}
