package ch.ethz.inf.vs.talosmodule.cryptoalg;

import org.spongycastle.crypto.BlockCipher;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.crypto.paddings.PKCS7Padding;
import org.spongycastle.crypto.params.KeyParameter;

import java.security.SecureRandom;

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
 * Based on  CryptDB's C++ CMC mode implementation
 * This code is partly ported from CryptDB
 * http://css.csail.mit.edu/cryptdb/
 */
public class CMCBlockCipher {

    private BlockCipher cipher;
    private PKCS7Padding padding = new PKCS7Padding();
    private byte[] key;
    private int blocksize;

    /**
     * Init a CMCBlockCipher
     * @param cipher Ciphertext
     * @param key key length should be equal to the block-size of the cipher
     * @throws IllegalArgumentException
     */
    public CMCBlockCipher(BlockCipher cipher, byte[] key) throws IllegalArgumentException{
        this.cipher = cipher;
        this.key = key;
        padding.init(new SecureRandom());
        blocksize = cipher.getBlockSize();
        if(key.length!=blocksize)
            throw new IllegalArgumentException("Wrong key size");

    }

    /**
     * Encrypts a byte[] array and applies padding.
     * @param value plaintext
     * @return ciphertext
     * @throws RuntimeException
     */
    public byte[] encrypt(byte[] value) throws RuntimeException {
        int offset = computeOffset(value);
        byte[] plaintext = padInput(value, offset);
        final byte[] ciphertext = new byte[plaintext.length];
        this.cipher.init(true, new KeyParameter(key));


        byte[] x = new byte[blocksize];
        for (int i = 0; i < plaintext.length; i += blocksize) {
            byte[] y = new byte[blocksize];
            xorBlock(y, 0, plaintext, i, x, 0, blocksize);
            cipher.processBlock(y, 0, ciphertext, i);
            System.arraycopy(ciphertext, i, x, 0, blocksize);
        }

        byte[] m = new byte[blocksize];
        int carry = 0;
        for (int j = blocksize; j != 0; j--) {
            int a = ciphertext[j - 1] ^
                    ciphertext[j - 1 + plaintext.length - blocksize];
            m[j - 1] = (byte) (carry |  (a << 1));
            carry = a >> 7;
        }
        m[blocksize-1] = (byte) (m[blocksize-1] | carry);

        for (int i = 0; i < plaintext.length; i += blocksize)
            for (int j = 0; j < blocksize; j++)
               ciphertext[i+j] = (byte) (ciphertext[i+j] ^ m[j]);

        x = new byte[blocksize];
        for (int i = plaintext.length; i != 0; i -= blocksize) {
            byte[] y = new byte[blocksize];
            byte[] z = new byte[blocksize];
            cipher.processBlock(ciphertext,i - blocksize, y,0);
            xorBlock(z, 0, y, 0, x, 0, blocksize);
            System.arraycopy(ciphertext, i-blocksize, x , 0, blocksize);
            System.arraycopy(z, 0, ciphertext, i-blocksize, blocksize);
        }

        this.cipher.reset();
        return ciphertext;
    }

    private void xorBlock(byte[] res, int startR, byte[] a, int startA, byte[] b, int startB, int length) {
        for(int i=0; i<length; i++) {
            res[startR + i] = (byte) (a[startA+i]^b[startB+i]);
        }
    }

    private int computeOffset(byte[] value) {
        return value.length % blocksize;
    }

    private byte[] padInput(byte[] value, int offset) {
        int length = value.length-offset+blocksize;
        byte[] res = new byte[length];
        System.arraycopy(value,0,res,0,value.length);
        padding.addPadding(res,value.length);
        return res;
    }


    /**
     * Decrypts ciphertext
     * @param value ciphertext
     * @return plaintext
     * @throws RuntimeException
     */
    public byte[] decrypt(byte[] value) throws RuntimeException {
        byte[] ciphertext = value.clone();
        byte[] plaintext  = new byte[ciphertext.length];
        this.cipher.init(false, new KeyParameter(key));

        if(value.length % blocksize != 0)
            throw new RuntimeException("Invalid length of ciphertext");

        byte[] x = new byte[blocksize];
        for (int i = ciphertext.length; i != 0; i -= blocksize) {
            byte[] y = new byte[blocksize];
            xorBlock(y, 0, ciphertext, i-blocksize, x, 0, blocksize);
            cipher.processBlock(y, 0, plaintext, i-blocksize);
            System.arraycopy(plaintext, i-blocksize, x, 0, blocksize);
        }

        byte[] m = new byte[blocksize];
        int carry = 0;
        for (int j = blocksize; j != 0; j--) {
            int a = plaintext[j - 1] ^
                    plaintext[j - 1 + ciphertext.length - blocksize];
            m[j - 1] = (byte) (carry |  (a << 1));
            carry = a >> 7;
        }
        m[blocksize-1] = (byte) (m[blocksize-1] | carry);

        for (int i = 0; i < plaintext.length; i += blocksize)
            for (int j = 0; j < blocksize; j++)
                plaintext[i+j] = (byte) (plaintext[i+j] ^ m[j]);

        x = new byte[blocksize];
        for (int i = 0; i < ciphertext.length; i += blocksize) {
            byte[] y = new byte[blocksize];
            byte[] z = new byte[blocksize];
            cipher.processBlock(plaintext,i, y,0);
            xorBlock(z, 0, y, 0, x, 0,blocksize);
            System.arraycopy(plaintext, i, x , 0, blocksize);
            System.arraycopy(z, 0, plaintext, i, blocksize);
        }

        int off = 0;
        try {
            off = padding.padCount(plaintext);
        } catch (InvalidCipherTextException e) {
            throw new RuntimeException(e.getMessage());
        }
        int newLength = plaintext.length - off;
        byte[] res = new byte[newLength];
        System.arraycopy(plaintext,0,res,0,newLength);

        cipher.reset();
        return res;
    }

}
