package ch.dsg.talos.relicproxyreenc.crypto;

import java.security.Key;

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
 * Implements a Java JNI wrapper around the EC-ElGamal jni library
 */
public class ECElGamalRelic {

    static {
        System.loadLibrary("ecelgamal-relic");
        initGamal();
    }

    // native jni functions
    public static native int initGamal();
    public static native int deinitGamal();
    private static native byte[] generateKey();
    private static native byte[] encrypt(long value, byte[] key_oct);
    private static native long decrypt(byte[] ciphertext_oct, byte[] key_oct, boolean use_bsgs);
    public static native int initBsgsTable(int size);
    private static native byte[] homAdd(byte[] ciphertext_1_oct, byte[] ciphertext_2_oct);

    /**
     * Generate an EC-ElGamal Key
     * @return
     */
    public static ECElGamalKey generateFreshKey() {
        return new ECElGamalKey(generateKey());
    }

    /**
     * Encrypt a plaintext integer with the EC-ElGamal Cipher
     * @param value the plaintext integer
     * @param key the EC-ElGamal key
     * @return the EC-ElGamal ciphertext
     */
    public static ECElGamalCipher encrypt(long value, ECElGamalKey key) {
        return new ECElGamalCipher(encrypt(value, key.native_key));
    }

    /**
     * Decrypt an EC-ElGamal ciphertext and maps it back to the plaintext integer
     * @param cipher the ciphertext
     * @param key the EC-ElGamal key
     * @param useBsgs true if the baby-step-giant-step algorithm should be used else false.
     * @return the plaintext integer
     */
    public static long decrypt(ECElGamalCipher cipher, ECElGamalKey key, boolean useBsgs) {
        return decrypt(cipher.cipher, key.native_key, useBsgs);
    }

    /**
     * Homomorphically adds two ciphertexts
     * a+b = dec(enc(a)+enc(b))
     * @param c1 summand 1
     * @param c2 summand 2
     * @return the resulting ciphertext
     */
    public static ECElGamalCipher add(ECElGamalCipher c1, ECElGamalCipher c2) {
        return new ECElGamalCipher(homAdd(c1.cipher, c2.cipher));
    }

    public static class ECElGamalKey implements Key {

        private byte[] native_key;

        public ECElGamalKey(byte[] key) {
            this.native_key = key;
        }

        public boolean containsPrivateKey() {
            return native_key[0]==0;
        }

        public ECElGamalKey getPublicKey() {
            return null;
        }

        protected byte[] getNativeKey() {
            return native_key;
        }

        @Override
        public String getAlgorithm() {
            return null;
        }

        @Override
        public String getFormat() {
            return null;
        }

        @Override
        public byte[] getEncoded() {
            return native_key.clone();
        }
    }

    public static class ECElGamalCipher {

        private byte[] cipher;

        public ECElGamalCipher(byte[] cipher) {
            if(cipher.length<1)
                throw  new IllegalArgumentException("Wrong cipher size");
            this.cipher = cipher;
        }

        private byte[] getCipher() {
            return cipher;
        }

        public int getSize() {
            return cipher.length;
        }

        public byte[] encode() {
            return cipher.clone();
        }
    }
}
