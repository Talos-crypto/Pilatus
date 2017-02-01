package ch.ethz.inf.vs.talosmodule.cryptoalg;

import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.PublicKey;

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
 * Created by lukas on 16.05.15.
 */
public abstract class NativeECElGamalCrypto {

    public abstract NativeECElgamalCipher encrypt(BigInteger plain) throws Exception;

    public abstract BigInteger decrypt(NativeECElgamalCipher cipher, int sizeNum, boolean doSigned) throws Exception;

    public abstract NativeECElgamalCipher addCiphers(NativeECElgamalCipher cipherA, NativeECElgamalCipher cipherB);

    public static class NativeECElgamalCipher {

        private static final String DELIM = "?";

        private final String R;

        private final String S;

        public NativeECElgamalCipher(String cipher) throws IllegalArgumentException {
            String[] split = cipher.split("\\" + DELIM);
            if (split == null || split.length != 2)
                throw new IllegalArgumentException("Wrong cipher format");
            this.R = split[0];
            this.S = split[1];
        }

        public String getForDB() {
            return R + DELIM + S;
        }

        public String getCipher() {
            return R + DELIM + S;
        }

        public String getR() {
            return R;
        }

        public String getS() {
            return S;
        }

    }

    public static class NativeECELGamalPublicKey implements PublicKey {

        private final int curve;

        private final String Y;

        public NativeECELGamalPublicKey(int curve, String Y) {
            this.curve = curve;
            this.Y = Y;
        }

        public int getCurve() {
            return curve;
        }

        public String getY() {
            return Y;
        }

        @Override
        public String getAlgorithm() {
            return "EC-ElGamal";
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

    public static class NativeECELGamalPrivateKey implements PrivateKey {

        private final BigInteger x;

        public NativeECELGamalPrivateKey(BigInteger x) {
            this.x = x;
        }

        public NativeECELGamalPrivateKey(String x) {
            this.x = new BigInteger(x);
        }

        public BigInteger getX() {
            return x;
        }

        @Override
        public String getAlgorithm() {
            return "EC-ELGamal";
        }

        @Override
        public String getFormat() {
            return null;
        }

        @Override
        public byte[] getEncoded() {
            return x.toByteArray();
        }
    }

}
