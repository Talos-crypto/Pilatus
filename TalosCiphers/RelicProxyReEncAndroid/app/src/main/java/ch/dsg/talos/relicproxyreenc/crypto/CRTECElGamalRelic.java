package ch.dsg.talos.relicproxyreenc.crypto;

import java.math.BigInteger;
import java.util.HashSet;

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
 * Implements CRT EC-ElGamal Relic
 * Faster for larger integers.
 */
public class CRTECElGamalRelic {

    private static BigInteger solveCRT(BigInteger[] nums, BigInteger[] ds, BigInteger d) {
        BigInteger res = BigInteger.ZERO;
        for (int index=0; index<nums.length; index++) {
            BigInteger cur = nums[index];
            BigInteger di = ds[index];
            BigInteger temp = d.divide(di);
            cur = cur.multiply(temp).multiply(temp.modInverse(di));
            res = res.add(cur);
        }
        return res.mod(d);
    }

    /**
     * Gererate CRT params for creating keys.
     * @param rand the random number generator
     * @param dBits the number of bits per Partition
     * @param numD the number of partitions
     * @return the CRT parameters
     */
    public static CRTParams generateCRTParams(IPRNG rand, int dBits, int numD) {
        BigInteger d = BigInteger.ONE;
        BigInteger[] ds = new BigInteger[numD];
        while (dBits*numD > d.bitLength()) {
            HashSet<BigInteger> before = new HashSet<>(numD);
            d = BigInteger.ONE;
            for (int index = 0; index < numD; index++) {
                BigInteger temp;
                do {
                    temp = rand.getRandPrime(dBits);
                } while (before.contains(temp));
                before.add(temp);
                ds[index] = temp;
                d = d.multiply(temp);
            }

        }
        return new CRTParams(ds, d, dBits);
    }

    /**
     * Generate an EC-ElGamal key
     * @param params the CRT parameters
     * @return a fresh EC-ElGamal CRT key
     */
    public static CRTECElGamalKey generateKeys(CRTParams params) {
        return new CRTECElGamalKey(ECElGamalRelic.generateFreshKey().getNativeKey(), params);
    }

    /**
     * Encrypts a given Integer with CRT EC-ElGamal.
     * @param value the plaintext integer
     * @param key the key
     * @return the CRT EC-ElGamal ciphertext
     */
    public static CRTECElGamalCipher encrypt(BigInteger value, CRTECElGamalKey key) {
        ECElGamalRelic.ECElGamalCipher[] ciphers = new ECElGamalRelic.ECElGamalCipher[key.getParams().getNumPartitions()];
        for(int iter=0; iter<ciphers.length; iter++) {
            BigInteger mi = value.mod(key.getParams().ds[iter]);
            ciphers[iter] = ECElGamalRelic.encrypt(mi.longValue(), key);
        }
        return new CRTECElGamalCipher(ciphers);
    }

    /**
     * Decrypts a given ciphertext with CRT EC-ElGamal adn maps it back to the plaintext integer.
     * @param cipher the ciphertext
     * @param key the key for decryption
     * @param doBsgs true if the baby-step-giant-step should be used (recommended) else bruteforce
     * @return the plaintext integer
     */
    public static BigInteger decrypt(CRTECElGamalCipher cipher, CRTECElGamalKey key, boolean doBsgs) {
        BigInteger[] subMessages = new BigInteger[cipher.getNumPartitions()];
        for(int iter=0; iter<subMessages.length; iter++) {
            subMessages[iter] = BigInteger.valueOf(ECElGamalRelic.decrypt(cipher.ciphers[iter], key, doBsgs));
        }
        return solveCRT(subMessages, key.getParams().ds, key.getParams().d);
    }

    /**
     * Adds two CRT EC-ElGamal ciphertext homomorphically
     * @param cipher1 summand 1
     * @param cipher2 summand 2
     * @return the resulting ciphertext that corresponds to the addition of the plaintexts
     */
    public static CRTECElGamalCipher add(CRTECElGamalCipher cipher1, CRTECElGamalCipher cipher2) {
        ECElGamalRelic.ECElGamalCipher[] results = new  ECElGamalRelic.ECElGamalCipher[cipher1.getNumPartitions()];
        for(int iter=0; iter<results.length; iter++) {
            results[iter] = ECElGamalRelic.add(cipher1.ciphers[iter], cipher2.ciphers[iter]);
        }
        return new CRTECElGamalCipher(results);
    }

    /**
     * Precompute BSGS table (optional)
     * @param size the number of entries
     */
    public static void precomputeBSGSTable(int size) {
        ECElGamalRelic.initBsgsTable(size);
    }

    public static final class CRTParams {
        private final BigInteger[] ds;
        private final BigInteger d;
        private final int numbits;

        public CRTParams(BigInteger[] ds, BigInteger d, int numbits) {
            this.ds = ds;
            this.d = d;
            this.numbits = numbits;
        }

        public BigInteger getD() {
            return d;
        }

        public int getNumbits() {
            return numbits;
        }

        public int getNumPartitions() {
            return ds.length;
        }
    }

    public static class CRTECElGamalKey extends ECElGamalRelic.ECElGamalKey {
        private CRTParams params;

        public CRTECElGamalKey(byte[] key, CRTParams params) {
            super(key);
            this.params = params;
        }

        public CRTParams getParams() {
            return params;
        }

        @Override
        public byte[] getEncoded() {
            //TODO
            return super.getEncoded();
        }
    }

    public static class CRTECElGamalCipher {
        private  ECElGamalRelic.ECElGamalCipher[] ciphers;

        public CRTECElGamalCipher(ECElGamalRelic.ECElGamalCipher[] ciphers) {
            this.ciphers = ciphers;
        }

        private ECElGamalRelic.ECElGamalCipher[] getCiphers() {
            return ciphers;
        }

        public int getSize() {
            int sum = 0;
            for(ECElGamalRelic.ECElGamalCipher val : ciphers)
                sum+= val.getSize();
            return sum;
        }

        public int getNumPartitions() {
            return ciphers.length;
        }

        public byte[] encodeCipher() {
            return null;
        }
    }
}
