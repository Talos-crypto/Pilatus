package ch.ethz.inf.vs.talosmodule.cryptoalg;

import java.math.BigInteger;

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
 * A implementation of a AES Pseudo Random Number Generator
 * This code is partly ported from CryptDB
 * http://css.csail.mit.edu/cryptdb/
 */
public class PRNGAesBlock implements IPRNG {

    private byte[] ctr;

    private byte[] key;

    public PRNGAesBlock(byte[] key) {
        ctr = new byte[BasicCrypto.AES_BLOCK_BYTES];
        this.key = key;
    }

    private byte[] randBytes(int nbytes) {
        byte buff[] = new byte[nbytes];
        for (int i = 0; i < nbytes; i += ctr.length) {
            for (int j = 0; j < ctr.length; j++) {
                ctr[j]++;
                if (ctr[j] != 0)
                    break;
            }

            byte[] ct = null;
            byte[] iv = new byte[ctr.length];
            try {
                ct = BasicCrypto.encrypt_AES_CBC(ctr, key, iv, false);
            } catch (Exception e) {
                e.printStackTrace();
            }
            int length = ctr.length;
            if (!(ctr.length < (nbytes - i))) {
                length = (nbytes - i);
            }

            System.arraycopy(ct, 0, buff, i, length);
        }
        return buff;
    }

    public void setCtr(byte[] in) {
        System.arraycopy(in, 0, ctr, 0, ctr.length);
    }

    @Override
    public void nextBytes(byte[] out) {
        byte[] rand = randBytes(out.length);
        System.arraycopy(rand, 0, out, 0, out.length);
    }

    @Override
    public BigInteger getRandPrime(int nbits) {
        int numBytes = (nbits + 7) / 8;
        boolean fits = (numBytes*8) % nbits == 0;
        int offset = (numBytes*8) % nbits;
        int mask = 0;
        for(int i=0; i<8-offset; i++) {
            mask+=1<<(i);
        }
        for (; ; ) {
            byte[] rands = randBytes(numBytes);
            if(!fits)
                rands[0] = (byte) (rands[0]&mask);
            BigInteger probPrime = new BigInteger(1, rands);
            probPrime = probPrime.setBit(nbits - 1);
            if (probPrime.isProbablePrime(25)) {
                return probPrime;
            }
        }
    }

    @Override
    public BigInteger getRandomNumber(int nbits) {
        BigInteger rand;
        int numBytes = (nbits + 7) / 8;
        boolean fits = (numBytes*8) % nbits == 0;
        int offset = (numBytes*8) % nbits;
        int mask = 0;
        for(int i=0; i<8-offset; i++) {
            mask+=1<<(i);
        }
        byte[] rands = randBytes((nbits + 7) / 8);
        if(!fits)
            rands[0] = (byte) (rands[0]&mask);
        rand = new BigInteger(1, rands);
        rand = rand.setBit(nbits - 1);
        return rand;
    }

    @Override
    public BigInteger getRandMod(BigInteger maxIt) {
        byte[] rands = randBytes(maxIt.bitLength() / 8 + 1);
        return new BigInteger(rands).mod(maxIt);
    }

    @Override
    public BigInteger getRandModHGD(BigInteger div) {
        byte[] rands = randBytes(div.bitLength() / 8 + 1);
        return new BigInteger(rands).mod(div);
    }
}
