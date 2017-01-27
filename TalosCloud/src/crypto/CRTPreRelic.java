package crypto;

import java.io.Serializable;
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
 * Implements the CRT version of proxy-re-encryption with relic
 * Uses the PRERelic implementation.
 */
public class CRTPreRelic {


    /**
     * Solves the crt problem
     * @param nums the input numbers from the decryption
     * @param ds the crt primes for each  crt partition
     * @param d the main crt prime number
     * @return the result for the crt problem
     */
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
     * Generates CRT params
     * @param rand the random number generator
     * @param dBits number of bits wanted per partition
     * @param numD number of partitions
     * @return the CRT params
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
     * Generates a CRT proxy-re-encryption key
     * @param params
     * @return
     */
    public static CRTPreKey generateKeys(CRTParams params) {
        return new CRTPreKey(PRERelic.generatePREKeys().getNativeKey(), params);
    }

    /**
     * Encrypts a value with the corresponding key
     * using proxy-re-encryption
     * @param value plaintext integer
     * @param key
     * @return crt pre cipher
     */
    public static CRTPreCipher encrypt(BigInteger value, CRTPreKey key) {
        PRERelic.PRECipher[] ciphers = new PRERelic.PRECipher[key.getParams().getNumPartitions()];
        for(int iter=0; iter<ciphers.length; iter++) {
            BigInteger mi = value.mod(key.getParams().ds[iter]);
            ciphers[iter] = PRERelic.encrypt(mi.longValue(), key);
        }
        return new CRTPreCipher(ciphers);
    }

    /**
     * Decrypts a pre ciphertext using proxy-re-encryption and maps it
     * to the plaintext integer.
     * @param cipher the crt pre cipher
     * @param key the key
     * @param doBsgs indicates if the baby-step-giant-step algorithm
     *               should be used for the mapping
     * @return the plaintext integer
     */
    public static BigInteger decrypt(CRTPreCipher cipher, CRTPreKey key, boolean doBsgs) {
        BigInteger[] subMessages = new BigInteger[cipher.getNumPartitions()];
        for(int iter=0; iter<subMessages.length; iter++) {
            subMessages[iter] = BigInteger.valueOf(PRERelic.decrypt(cipher.ciphers[iter], key, doBsgs));
        }
        return solveCRT(subMessages, key.getParams().ds, key.getParams().d);
    }

    /**
     * Re-encrypts a CRT Pre Cipher to another CRT Pre Cipher using the provided token
     * @param cipher ciphertext for re-encryption
     * @param token the  re-encryption token
     * @return the re-encrypted ciphertext
     */
    public static CRTPreCipher reEncrypt(CRTPreCipher cipher, PRERelic.PREToken token) {
        PRERelic.PRECipher[] results = new PRERelic.PRECipher[cipher.getNumPartitions()];
        for(int iter=0; iter<results.length; iter++) {
            results[iter] = PRERelic.reEncrypt(cipher.ciphers[iter], token);
        }
        return new CRTPreCipher(results);
    }


    /**
     * Uses the homomorphic property of the scheme and adds two ciphertexts
     * @param cipher1 first ciphertext
     * @param cipher2 second ciphertext
     * @param key the ley
     * @param reRand indicates if re randomization should be allied
     * @return the addition of the two ciphertexts
     */
    public static CRTPreCipher add(CRTPreCipher cipher1, CRTPreCipher cipher2, PRERelic.PREKey key, boolean reRand) {
        PRERelic.PRECipher[] results = new PRERelic.PRECipher[cipher1.getNumPartitions()];
        for(int iter=0; iter<results.length; iter++) {
            results[iter] = PRERelic.addCiphers(cipher1.ciphers[iter], cipher2.ciphers[iter], key, reRand);
        }
        return new CRTPreCipher(results);
    }

    /**
     * Class that represents the crt parameters
     */
    public static final class CRTParams implements Serializable {
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

        public String getStringRep() {
            StringBuilder sb = new StringBuilder();
            String delim = "|";
            sb.append(numbits)
                .append(delim)
                    .append(d).append(delim);
            for(BigInteger b : ds)
                sb.append(b.toString()).append(delim);
            sb.setLength(sb.length()-1);
            return sb.toString();
        }

        public CRTParams fromStringRep(String rep) {
            String delim = "|";
            String[] splits = rep.split(delim);
            int numbits = Integer.valueOf(splits[0]);
            BigInteger d = new BigInteger(splits[1]);
            int numDs = splits.length-2;
            BigInteger[] ds = new BigInteger[numDs];
            for(int iter=0;iter<numDs;iter++) {
                ds[iter] = new BigInteger(splits[2+iter]);
            }
            return new CRTParams(ds, d, numbits);
        }
    }

    /**
     * Class for representing a CRT PRE key
     */
    public static class CRTPreKey extends PRERelic.PREKey {
        private CRTParams params;

        public CRTPreKey(byte[] key, CRTParams params) {
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

    /**
     * Class for representing a CRT PRE Cipher
     */
    public static class CRTPreCipher {
        private  PRERelic.PRECipher[] ciphers;

        public CRTPreCipher(PRERelic.PRECipher[] ciphers) {
            this.ciphers = ciphers;
        }

        private PRERelic.PRECipher[] getCiphers() {
            return ciphers;
        }

        public int getSize() {
            int sum = 0;
            for(PRERelic.PRECipher val : ciphers)
                sum+= val.getSize();
            return sum;
        }

        public int getNumPartitions() {
            return ciphers.length;
        }

        public byte[] encodeCipher() {
            int len = 1, curpos=1;
            for(PRERelic.PRECipher cipher:ciphers){
                len+=cipher.getSize();
            }
            byte[] res = new byte[len];
            res[0] = (byte) ciphers.length;
            for(PRERelic.PRECipher cipher:ciphers){
                byte[] data = cipher.encode();
                System.arraycopy(data, 0, res, curpos, data.length);
                curpos += data.length;
            }
            return res;
        }

        private static int predictLen(byte[] cur, int idx) {
            return (((int) cur[idx] << 8) &0x0000ff00) | (cur[idx+1] &0x000000ff);
        }

        private static int predictFUllLength(byte[] cur, int idx) {
            int lenC1 = predictLen(cur, idx+1);
            int lenC2 = predictLen(cur, idx+lenC1+3);
            return 1 + lenC1 + 2 + lenC2 + 2;
        }

        public static CRTPreCipher decodeCipher(byte[] data) {
            if(data.length<1)
                throw new IllegalArgumentException("Wrong format");
            PRERelic.PRECipher[] ciphers = new PRERelic.PRECipher[data[0]];
            int cur = 1;
            for(int tmp=0; tmp<ciphers.length; tmp++) {
                int length = predictFUllLength(data, cur);
                byte[] temp = new byte[length];
                System.arraycopy(data, cur, temp, 0, temp.length);
                ciphers[tmp] = new PRERelic.PRECipher(temp);
                cur += temp.length;
            }
            return new CRTPreCipher(ciphers);
        }
    }

}
