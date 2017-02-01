package ch.ethz.inf.vs.talosmodule.cryptoalg;
import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.ArrayDeque;
import java.util.Random;

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
 * Implementation of the public part of the Paillier Crypto-System
 * This code is partly ported from CryptDB
 * http://css.csail.mit.edu/cryptdb/
 */
public class Paillier {

    /* Public key */
    BigInteger n, g;

    /* Cached values */
    int nbits;
    BigInteger n2;

    static ArrayDeque<BigInteger> randQueue = new ArrayDeque<BigInteger>();


    public Paillier(PublicKey pubk) {
        PaillierPubKey pk = (PaillierPubKey) pubk;
        n = pk.getN();
        g = pk.getG();
        nbits = n.bitCount();
        n2 = n.multiply(n);
    }

    public void rand_gen(int niter, int nmax) {
        if (randQueue.size() >= nmax)
            niter = 0;
        else
            niter = Math.min(niter, nmax - randQueue.size());

        Random sr = new SecureRandom();
        for (int i = 0; i < niter; i++) {
            BigInteger rand = new BigInteger(nbits, sr);
            BigInteger r = rand.mod(n);
            BigInteger rn = g.modPow(n.multiply(r), n2);
            randQueue.addLast(rn);
        }
    }

    public BigInteger hompubkey() {
        return n2;
    }

    public BigInteger encrypt(BigInteger plaintext) {
        if (!randQueue.isEmpty()) {
            BigInteger rn = randQueue.pollFirst();
            return (g.modPow(plaintext, n2).multiply(rn)).mod(n2);
        } else {
            SecureRandom sr = new SecureRandom();
            BigInteger rand = new BigInteger(nbits, sr);
            BigInteger r = rand.mod(n);
            return g.modPow(plaintext.add(n.multiply(r)), n2);
        }
    }

    public BigInteger add(BigInteger c0, BigInteger c1) {
        return (c0.multiply(c1)).mod(n2);
    }

    public BigInteger mul(BigInteger ciphertext, BigInteger constval) {
        return ciphertext.modPow(constval, n2);
    }

    public static class PaillierPubKey implements PublicKey {

        private final BigInteger n;

        private final BigInteger g;

        public PaillierPubKey(BigInteger n, BigInteger g) {
            this.n = n;
            this.g = g;
        }

        public BigInteger getG() {
            return g;
        }

        public BigInteger getN() {
            return n;
        }

        @Override
        public String getAlgorithm() {
            return "Paillier";
        }

        @Override
        public String getFormat() {
            return null;
        }

        @Override
        public byte[] getEncoded() {
            return new byte[0];
        }
    }

    public static class PaillierPrivKey implements PrivateKey {

        private final BigInteger q;

        private final BigInteger p;

        private final BigInteger a;

        public PaillierPrivKey(BigInteger q, BigInteger p, BigInteger a) {
            this.q = q;
            this.p = p;
            this.a = a;
        }

        public BigInteger getQ() {
            return q;
        }

        public BigInteger getP() {
            return p;
        }

        public BigInteger getA() {
            return a;
        }

        @Override
        public String getAlgorithm() {
            return "Paillier";
        }

        @Override
        public String getFormat() {
            return null;
        }

        @Override
        public byte[] getEncoded() {
            return new byte[0];
        }
    }

}
