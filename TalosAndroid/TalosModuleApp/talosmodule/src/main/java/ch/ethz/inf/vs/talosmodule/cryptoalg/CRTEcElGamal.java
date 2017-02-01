package ch.ethz.inf.vs.talosmodule.cryptoalg;

import java.math.BigInteger;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.HashSet;

import ch.ethz.inf.vs.talosmodule.cryptoalg.NativeECElGamalCrypto.NativeECELGamalPrivateKey;
import ch.ethz.inf.vs.talosmodule.cryptoalg.NativeECElGamalCrypto.NativeECELGamalPublicKey;
import ch.ethz.inf.vs.talosmodule.cryptoalg.NativeECElGamalCrypto.NativeECElgamalCipher;

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
 * This class implements additive-homomopric EC-ElGamal combined with
 * the chinese remainder theorem (CRT). Since the ECDLP is faced on decryption,
 * the reduced message space with CRT speeds-up this process. However, the cipher size increases
 * linearly with the number of CRT partitions.
 */
public class CRTEcElGamal {

    /**
     * CRT-EC-ElGamal public key
     */
    private CRTEcElGamalPublicKey pubKey;

    /**
     * additive-homomorphic EC-ElGamal cipher
     */
    private FastECElGamal gamal;

    /**
     * Creates a additive-homomopric EC-ElGamal cipher combined with CRT
     * partitions.
     * @param kp the public/private keypair
     * @param rand
     * @throws IllegalArgumentException
     */
    public CRTEcElGamal(KeyPair kp, IPRNG rand) throws IllegalArgumentException {
        try {
            pubKey = (CRTEcElGamalPublicKey) kp.getPublic();
        } catch (Exception e) {
            throw new IllegalArgumentException("Wrong Key");
        }
        gamal = new FastECElGamal(kp, rand);
    }

    /**
     * Generates a CRT-EC-Elgamal keypair based on the provided parameters
     * @param rand the PRNG for computation
     * @param dBits the number of bits of a CRT partition (2^dBits)
     * @param numD the number of CRT partitions
     * @return  CRT-EC-Elgamal keypair
     */
    public static KeyPair generateKeys(IPRNG rand, int dBits, int numD) {
        NativeECELGamalPrivateKey privKey;
        NativeECELGamalPublicKey publicKey;
        CRTEcElGamalPublicKey pubKey = null;
        KeyPair pair = FastECElGamal.generateKeys(rand);
        publicKey = (NativeECELGamalPublicKey) pair.getPublic();
        privKey = (NativeECELGamalPrivateKey) pair.getPrivate();

        BigInteger d = BigInteger.ONE;
        while (dBits*numD > d.bitLength()) {
            HashSet<BigInteger> before = new HashSet<>(numD);
            BigInteger[] ds = new BigInteger[numD];
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
            pubKey = new CRTEcElGamalPublicKey(publicKey, ds, d, dBits);
        }
        return new KeyPair(pubKey, privKey);
    }

    /**
     * Encrypts an integer to an CRT-EC-ElGamal cipher, which
     * consist of two EC-Points per CRT partition.
     * @param plain the integer to encrypt
     * @return the CRT-EC-ElGamal ciphertext
     * @throws Exception if encryption fails
     */
    public CRTEcElGamalCipher encrypt(BigInteger plain) throws Exception {
        CRTEcElGamalCipher cipher = new CRTEcElGamalCipher(25);
        int numd = pubKey.getNumberOfSubComponents();

        for (int dind=0; dind<numd; dind++) {
            BigInteger mi = plain.mod(pubKey.getDwithIndex(dind));
            NativeECElgamalCipher ciph = gamal.encrypt(mi);
            cipher.addCipher(ciph);
        }
        return cipher;
    }

    /**
     * Decrypts a CRT-EC-ElGamal ciphertext and maps it back to the
     * integer space.
     * @param cipher the CRT-EC-ElGamal ciphertext
     * @param maxPos the MAX integer in the message space, this parameter is used for cumpuing the negative values.
     *               Ex. for java int Integer.MAX_VALUE
     * @return the integer in the message space
     * @throws Exception if decryption fails
     */
    public BigInteger decrypt(CRTEcElGamalCipher cipher, long maxPos) throws Exception {
        ArrayList<BigInteger> submessages = new ArrayList<>();
        int numd = pubKey.getNumberOfSubComponents();
        for (int dind=0; dind<numd; dind++) {
            NativeECElgamalCipher cur = cipher.getCipherWithIndex(dind);
            BigInteger submessage = gamal.decrypt(cur, 32, false);
            submessages.add(submessage);
        }
        BigInteger res = solveCRT(submessages);
        if(res.compareTo(BigInteger.valueOf(maxPos))==1)
            return res.subtract(pubKey.getD());
        return res;
    }

    /**
     * Adds two CRT-EC-ElGamal ciphers
     * @param cipherA
     * @param cipherB
     * @return
     */
    public CRTEcElGamalCipher addCiphers(CRTEcElGamalCipher cipherA, CRTEcElGamalCipher cipherB) {
        int numd = pubKey.getNumberOfSubComponents();
        CRTEcElGamalCipher res = new CRTEcElGamalCipher(25);
        for (int dind=0; dind<numd; dind++) {
            res.addCipher(gamal.addCiphers(cipherA.getCipherWithIndex(dind), cipherB.getCipherWithIndex(dind)));
        }
        return res;
    }

    private BigInteger solveCRT(ArrayList<BigInteger> nums) {
        BigInteger res = BigInteger.ZERO;
        BigInteger d = pubKey.getD();
        int numSubs = pubKey.getNumberOfSubComponents();
        for (int index=0; index<numSubs; index++) {
            BigInteger cur = nums.get(index);
            BigInteger di = pubKey.getDwithIndex(index);
            BigInteger temp = d.divide(di);
            cur = cur.multiply(temp).multiply(temp.modInverse(di));
            res = res.add(cur);
        }
        return res.mod(d);
    }


    /**
     * Represents a public key of a CRT-EC-ElGamal cipher
     */
    public static class CRTEcElGamalPublicKey extends NativeECELGamalPublicKey {

        private BigInteger[] ds;
        private BigInteger d;
        private int numbits;

        /**
         * Creates a CRT-EC-ElGamal public key
         * @param curve the id of the curve (OpenSSL)
         * @param Y the public EC-Point
         * @param ds the CRT primes
         * @param d the multiplication of the ds primes
         * @param numbits the number of bits per partition
         */
        public CRTEcElGamalPublicKey(int curve, String Y, BigInteger[] ds, BigInteger d, int numbits) {
            super(curve, Y);
            this.ds = ds;
            this.d = d;
            this.numbits = numbits;
        }

        public CRTEcElGamalPublicKey(NativeECELGamalPublicKey pub, BigInteger[] ds, BigInteger d, int numbits) {
            super(pub.getCurve(), pub.getY());
            this.ds = ds;
            this.d = d;
            this.numbits = numbits;
        }

        /**
         * Return the CRT-Partition prime at the given intex.
         * @param index the index
         * @return the CRT-Partition prime
         * @throws IllegalArgumentException if idndex is not valid
         */
        public BigInteger getDwithIndex(int index) throws IllegalArgumentException {
            if(index<0 || index>=ds.length)
                throw new IllegalArgumentException("Wrong index "+index);
            return ds[index];
        }

        /**
         * Returns the number of CRT partitions.
         * @return the number of CRT partitions
         */
        public int getNumberOfSubComponents() {
            return ds.length;
        }

        /**
         * Returns the number of bits per partition
         * @return the number of bits per partition
         */
        public int getNumbits() {
            return numbits;
        }

        /**
         * Returns the multiplication of all CRT-Partitions
         * @return the number of bits per partition
         */
        public BigInteger getD() {
            return d;
        }
    }

    public static class CRTEcElGamalCipher {

        private final int encodingSize;

        private final ArrayList<NativeECElgamalCipher> ciphers = new ArrayList<>();

        public CRTEcElGamalCipher(int encodingSize, String cipher) throws IllegalArgumentException {
            this.encodingSize = encodingSize;
            int num = encodingSize * 2;
            int cipherSize = num*2;
            char[] hexified = cipher.toCharArray();
            if(hexified.length % cipherSize != 0)
                throw new IllegalArgumentException("Wrong cipher format: "+cipher);
            int numLocalCiphers = hexified.length/(num*2);
            for(int i=0; i<numLocalCiphers; i++) {
                char[] point1 = new char[num];
                char[] point2 = new char[num];
                System.arraycopy(hexified, i*cipherSize, point1, 0, num);
                System.arraycopy(hexified, i*cipherSize+num, point2, 0, num);
                ciphers.add(new NativeECElgamalCipher((new String(point1))+"?"+(new String(point2))));
            }
        }

        public CRTEcElGamalCipher(int encodingSize) {
            this.encodingSize = encodingSize;
        }

        void addCipher(NativeECElgamalCipher cipher) {
            ciphers.add(cipher);
        }

        public NativeECElgamalCipher getCipherWithIndex(int index) throws IllegalArgumentException{
            if(index<0 || index>=ciphers.size())
                throw new IllegalArgumentException("Wrong index "+index);
            return ciphers.get(index);
        }

        public String getCipher() {
            StringBuilder sb = new StringBuilder();
            for(NativeECElgamalCipher cipher : ciphers) {
                sb.append(cipher.getR()).append(cipher.getS());
            }
            return sb.toString();
        }


    }
}
