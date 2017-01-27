package crypto;

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
 * Implements a wrapper for procxy-re-encryption scheme implemented
 * in C using the relic toolkit. Assumes the shared jni library is compiled and
 * located in /usr/local/lib/librelic-proxy-re-enc.so
 * Allows homomorphic addition and sharing between two parties.
 * Warpper for JNI
 */
public class PRERelic {
    static {
        /**
         * Loads the C code
         */
        System.load("/usr/local/lib/librelic-proxy-re-enc.so");
        initPre();
    }

    /**
     * Generates a proxy-re-encryption key
     * @return a key
     */
    public static PREKey generatePREKeys() {
        return new PREKey(generateKey());
    }

    /**
     * Encrypts a integer with PRE using the provided key
     * @param value the plaintext
     * @param key the key
     * @return the PRE ciphertext
     */
    public static PRECipher encrypt(long value, PREKey key) {
        return new PRECipher(encrypt(value, key.getNativeKey()));
    }

    /**
     * Decrypts a PRE ciphertexts with the pre key and maps it back to
     * the plaintext integer
     * @param cipher the pre ciphertext
     * @param key the pre key
     * @param useBSGS indicates if the baby-step-giant-step algorithm should be
     *                used for the inverse mapping
     * @return the
     */
    public static long decrypt(PRECipher cipher, PREKey key, boolean useBSGS) {
        if(!key.containsPrivateKey())
            throw new IllegalArgumentException("Key must contain private Key");
        return decrypt(cipher.getCipher(), key.getNativeKey(), useBSGS);
    }

    /**
     * Creates a re-encryption token from party A to party B
     * given the two keys.
     * @param from key of party A
     * @param to  key of party B (only public part is needed)
     * @return a PRE token from A to B
     */
    public static PREToken createToken(PREKey from, PREKey to) {
        if(!from.containsPrivateKey())
            throw new IllegalArgumentException("From Key must contain private Key");
        return new PREToken(createReEncToken(from.getNativeKey(), to.getNativeKey()));
    }

    /**
     * Re-Encrypts a PRE ciphertext given the PRE token from A to B.
     * @param cipher the ciphertext
     * @param token the PRE token from A to B
     * @return the re-encrypted ciphertext
     */
    public static PRECipher reEncrypt(PRECipher cipher, PREToken token) {
        return new PRECipher(reApply(cipher.getCipher(), token.token));
    }

    /**
     * Homomorphically adds two ciphertext and returns the new ciphertext
     * @param cipher1 the first ciphertext
     * @param cipher2  the second ciphertext
     * @param key the key
     * @param reRand indicates if re-randomization should be allied
     * @return a PRE ciphertext
     */
    public static PRECipher addCiphers(PRECipher cipher1, PRECipher cipher2, PREKey key, boolean reRand) {
        return new PRECipher(homAdd(cipher1.getCipher(), cipher2.getCipher(), key.getNativeKey(), reRand));
    }

    //native functions
    private static native int initPre();
    public static native int deinitPre();
    private static native byte[] generateKey();
    private static native byte[] encrypt(long value, byte[] key_oct);
    private static native long decrypt(byte[] ciphertext_oct, byte[] key_oct, boolean use_bsgs);
    private static native byte[] createReEncToken(byte[] key_from_oct, byte[] key_to_oct);
    private static native byte[] reApply(byte[] ciphertext_oct, byte[] token_oct);
    private static native byte[] homAdd(byte[] ciphertext_1_oct, byte[] ciphertext_2_oct, byte[] key_oct, boolean re_rand);
    public static native int initBsgsTable(int table_size);

    public static class PREKey implements Key {

        private byte[] native_key;

        public PREKey(byte[] key) {
            this.native_key = key;
        }

        public boolean containsPrivateKey() {
            return Character.toString((char ) native_key[0]).equals("s");
        }

        public PREKey getPublicKey() {
            if(this.containsPrivateKey()) {
                int last_pos = 1;
                for(int i=0; i<5;i++) {
                    int len = (((int) native_key[last_pos] << 8) &0x0000ff00) | (native_key[last_pos + 1] &0x000000ff);
                    last_pos+= 2+len;
                }
                byte[] res = new byte[last_pos];
                System.arraycopy(native_key, 0, res, 0, last_pos);
                int codePublic = (int) 'p';
                res[0] = (byte) (codePublic&0x000000ff);
                return new PREKey(res);
            }
            return new PREKey(native_key.clone());
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

    public static class PRECipher {

        private byte[] cipher;

        public PRECipher(byte[] cipher) {
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

        public boolean isReEncrypted() {
            return Character.toString((char ) cipher[0]).equals("1");
        }

        public byte[] encode() {
            return cipher.clone();
        }
    }

    public static class PREToken {

        private byte[] token;

        public PREToken(byte[] token) {
            this.token = token;
        }

        public byte[] getToken() {
            return token.clone();
        }
    }

}
