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
 * Warpper for JNI
 */
public class PRERelic {
    static {
        System.loadLibrary("relic-proxy-re-enc");
        initPre();
    }

    public static PREKey generatePREKeys() {
        return new PREKey(generateKey());
    }

    public static PRECipher encrypt(long value, PREKey key) {
        return new PRECipher(encrypt(value, key.getNativeKey()));
    }

    public static long decrypt(PRECipher cipher, PREKey key, boolean useBSGS) {
        if(!key.containsPrivateKey())
            throw new IllegalArgumentException("Key must contain private Key");
        return decrypt(cipher.getCipher(), key.getNativeKey(), useBSGS);
    }

    public static PREToken createToken(PREKey from, PREKey to) {
        if(!from.containsPrivateKey())
            throw new IllegalArgumentException("From Key must contain private Key");
        return new PREToken(createReEncToken(from.getNativeKey(), to.getNativeKey()));
    }

    public static PRECipher reEncrypt(PRECipher cipher, PREToken token) {
        return new PRECipher(reApply(cipher.getCipher(), token.token));
    }

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
    }

}
