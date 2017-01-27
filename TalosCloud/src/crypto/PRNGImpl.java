package crypto;

import java.math.BigInteger;
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
 * Created by lukas on 12.03.15.
 * Pseudo Random Number Generator with Androids SecureRandom
 */
public class PRNGImpl implements IPRNG {

    private SecureRandom sn = new SecureRandom();

    @Override
    public BigInteger getRandPrime(int nbits) {
        reseed();
        return BigInteger.probablePrime(nbits, sn);
    }

    @Override
    public BigInteger getRandomNumber(int nbits) {
        reseed();
        return (new BigInteger(nbits, sn)).setBit(nbits - 1);
    }

    @Override
    public BigInteger getRandMod(BigInteger num) {
        reseed();
        byte[] buffer = new byte[num.bitLength() / 8 + 1];
        sn.nextBytes(buffer);
        BigInteger temp = new BigInteger(buffer);
        return temp.mod(num);
    }

    @Override
    public BigInteger getRandModHGD(BigInteger max) {
        reseed();
        byte[] buffer = new byte[max.bitLength() / 8 + 1];
        sn.nextBytes(buffer);
        BigInteger num = new BigInteger(buffer);
        return num.mod(max);
    }

    @Override
    public void nextBytes(byte[] out) {
        sn.nextBytes(out);
    }

    private void reseed() {
        sn = new SecureRandom();
    }
}
