package ch.ethz.inf.vs.talosmodule.cryptoalg;

import org.spongycastle.crypto.BufferedBlockCipher;
import org.spongycastle.crypto.CipherParameters;
import org.spongycastle.crypto.engines.AESEngine;
import org.spongycastle.crypto.engines.BlowfishEngine;
import org.spongycastle.crypto.modes.CBCBlockCipher;
import org.spongycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.crypto.params.ParametersWithIV;

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
 * Basic Cryptographic Block Ciphers, implemented with SpongyCastle
 */
public class BasicCrypto {

    public static final int AES_BLOCK_BYTES = 16;

    public static final int BF_BLOCK_BYTES = 8;


    public static byte[] encrypt_AES(byte[] in, byte[] key) throws IllegalArgumentException {
        if(in.length % AES_BLOCK_BYTES != 0)
            throw new IllegalArgumentException("Wrong Input size");
        byte[] res = new byte[AES_BLOCK_BYTES];
        AESEngine aes = new AESEngine();
        aes.init(true, new KeyParameter(key));
        aes.processBlock(in, 0, res, 0);
        return res;
    }

    public static byte[] decrypt_AES(byte[] in, byte[] key) throws IllegalArgumentException {
        if(in.length % AES_BLOCK_BYTES != 0)
            throw new IllegalArgumentException("Wrong Input size");
        byte[] res = new byte[AES_BLOCK_BYTES];
        AESEngine aes = new AESEngine();
        aes.init(false, new KeyParameter(key));
        aes.processBlock(in, 0, res, 0);
        return res;
    }

    public static byte[] encrypt_BLOWFISH(byte[] in, byte[] key) throws IllegalArgumentException {
        if(in.length % BF_BLOCK_BYTES != 0)
            throw new IllegalArgumentException("Wrong Input size");
        byte[] res = new byte[BF_BLOCK_BYTES];
        BlowfishEngine bf = new BlowfishEngine();
        bf.init(true, new KeyParameter(key));
        bf.processBlock(in, 0, res, 0);
        return res;
    }

    public static byte[] decrypt_BLOWFISH(byte[] in, byte[] key) throws IllegalArgumentException {
        if(in.length % BF_BLOCK_BYTES != 0)
            throw new IllegalArgumentException("Wrong Input size");
        byte[] res = new byte[BF_BLOCK_BYTES];
        BlowfishEngine bf = new BlowfishEngine();
        bf.init(false, new KeyParameter(key));
        bf.processBlock(in, 0, res, 0);
        return res;
    }

    public static byte[] encrypt_BLOWFISH_CBC(byte[] in, byte[] key, byte[] iv, boolean doPad) throws Exception {
        byte[] plain = in;
        BufferedBlockCipher bf;
        boolean isExactSize = plain.length % BF_BLOCK_BYTES == 0;
        if (doPad) {
            bf = new PaddedBufferedBlockCipher(new CBCBlockCipher(new BlowfishEngine()));
        } else {
            if (isExactSize)
                bf = new BufferedBlockCipher(new CBCBlockCipher(new BlowfishEngine()));
            else
                throw new IllegalArgumentException("Blocksize missmatch");
        }
        CipherParameters ivAndKey = new ParametersWithIV(new KeyParameter(key), iv);
        bf.init(true, ivAndKey);
        return cipherData(bf, plain);
    }

    public static byte[] decrypt_BLOWFISH_CBC(byte[] in, byte[] key, byte[] iv, boolean isPad) throws Exception {
        byte[] cipher = in;
        BufferedBlockCipher bf;
        if (isPad)
            bf = new PaddedBufferedBlockCipher(new CBCBlockCipher(new BlowfishEngine()));
        else
            bf = new BufferedBlockCipher(new CBCBlockCipher(new BlowfishEngine()));
        CipherParameters ivAndKey = new ParametersWithIV(new KeyParameter(key), iv);
        bf.init(false, ivAndKey);
        return cipherData(bf, cipher);
    }


    public static byte[] encrypt_AES_CBC(byte[] in, byte[] key, byte[] iv, boolean doPad) throws Exception {
        byte[] plain = in;
        BufferedBlockCipher aes;
        if (!doPad)
            if (plain.length % AES_BLOCK_BYTES == 0)
                aes = new BufferedBlockCipher(new CBCBlockCipher(new AESEngine()));
            else
                throw new IllegalArgumentException("Blocksize missmatch");
        else
            aes = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESEngine()));
        CipherParameters ivAndKey = new ParametersWithIV(new KeyParameter(key), iv);
        aes.init(true, ivAndKey);
        return cipherData(aes, plain);
    }

    public static byte[] decrypt_AES_CBC(byte[] in, byte[] key, byte[] iv, boolean isPad) throws Exception {
        byte[] cipher = in;
        BufferedBlockCipher aes;
        if (isPad)
            aes = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESEngine()));
        else
            aes = new BufferedBlockCipher(new CBCBlockCipher(new AESEngine()));
        CipherParameters ivAndKey = new ParametersWithIV(new KeyParameter(key), iv);
        aes.init(false, ivAndKey);
        return cipherData(aes, cipher);
    }

    private static byte[] cipherData(BufferedBlockCipher cipher, byte[] data) throws Exception {
        int minSize = cipher.getOutputSize(data.length);
        byte[] outBuf = new byte[minSize];
        int length1 = cipher.processBytes(data, 0, data.length, outBuf, 0);
        int length2 = cipher.doFinal(outBuf, length1);
        int actualLength = length1 + length2;
        byte[] result = new byte[actualLength];
        System.arraycopy(outBuf, 0, result, 0, result.length);
        return result;
    }

    public static byte[] encrypt_AES_CMC(byte[] in, byte[] key) throws Exception {
        CMCBlockCipher cipher = new CMCBlockCipher(new AESEngine(),key);
        return cipher.encrypt(in);
    }

    public static byte[] decrypt_AES_CMC(byte[] in, byte[] key) throws Exception {
        CMCBlockCipher cipher = new CMCBlockCipher(new AESEngine(),key);
        return cipher.decrypt(in);
    }

}
