package ch.ethz.inf.vs.talosmodule.cryptoalg;

import com.google.common.collect.ImmutableMap;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.math.BigInteger;
import java.security.KeyPair;

import ch.ethz.inf.vs.talosmodule.util.ContextHolder;
import ch.ethz.inf.vs.talosmodule.util.Setting;

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
 * Implementation of the EC-ElGamal Crypto-System
 * Calls native C functions for the ECC operations
 * OLD!!!!!!
 */
public class NativeECElGamal extends NativeECElGamalCrypto {

    private static final int CURVE_ID = 0;

    private static final boolean LOAD_TABLE_FROM_FILE = true;

    static {
        System.loadLibrary("eccrypto");
        setUP(CURVE_ID);
        setUPBSGS(TableLoader.TABLE_SIZE, TableLoader.INV_TABLE_SIZE);
    }

    /**
     * Lookup table for BSGS algorithm
     */
    private static ImmutableMap<String, String> store = null;

    private final NativeECELGamalPublicKey pubKey;

    private final NativeECELGamalPrivateKey privKey;

    private IPRNG rand;

    public NativeECElGamal(KeyPair kp, IPRNG rand) throws IllegalArgumentException {
        NativeECELGamalPublicKey pub = null;
        NativeECELGamalPrivateKey priv = null;
        try {
            pub = (NativeECELGamalPublicKey) kp.getPublic();
            priv = (NativeECELGamalPrivateKey) kp.getPrivate();
        } catch (Exception e) {
            throw new IllegalArgumentException("Wrong Key");
        }
        this.pubKey = pub;
        this.privKey = priv;
        this.rand = rand;
    }

    @Override
    public NativeECElgamalCipher encrypt(BigInteger plain) {
        BigInteger k;
        String cipher;
        k = generateK();
        cipher = encryptNative(plain.toString(), k.toString(), pubKey.getY());
        return new NativeECElgamalCipher(cipher);

    }

    @Override
    public BigInteger decrypt(NativeECElgamalCipher cipher, int sizeNum, boolean doSigned) {
        if (store == null) {
            loadLookUpTable();
        }

        String decryptPoint = decryptNative(cipher.getCipher(), privKey.getX().toString());
        String numStr = solveDiscreteLogBSGS(decryptPoint);

        return new BigInteger(numStr);
    }

    public static KeyPair generateKeys(IPRNG rand, int curve) {
        NativeECELGamalPrivateKey privKey;
        NativeECELGamalPublicKey pubKey;
        BigInteger x;
        String Y;

        setUP(curve);
        BigInteger prime = getCurveOrderJ();
        x = rand.getRandMod(prime);
        privKey = new NativeECELGamalPrivateKey(x);
        Y = computePubKey(x.toString());
        pubKey = new NativeECELGamalPublicKey(curve, Y);
        tearDOWN();

        return new KeyPair(pubKey, privKey);
    }

    public static KeyPair generateKeys(IPRNG rand) {
        return generateKeys(rand, 0);
    }

    public static void preLoadFileTable() {
        if (LOAD_TABLE_FROM_FILE) {
            store = TableLoader.loadTable();
        }
    }

    private void loadLookUpTable() {
        if (LOAD_TABLE_FROM_FILE) {
            store = TableLoader.loadTable();
        } else {
            createBSGSStorage();
        }
    }

    private void createBSGSStorage() {
        String cur;
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        setUPBSGS(TableLoader.TABLE_SIZE, TableLoader.INV_TABLE_SIZE);
        cur = getNextBSGSStorageItem();
        while (!cur.equals("empty")) {
            String[] res = cur.split(",");
            builder.put(res[1], res[0]);
            cur = getNextBSGSStorageItem();
        }
        this.store = builder.build();
    }


    private String solveDiscreteLogBSGS(String point) {
        String curPoint, negPoint, num;
        curPoint = point;
        negPoint = point;

        while (!curPoint.equals("finish")) {
            curPoint = computeBsGsStep(curPoint);
            num = store.get(curPoint);
            if (num != null) {
                return computeBsGsResult(num);
            }

            negPoint = computeBsGsStepNeg(negPoint);
            num = store.get(negPoint);
            if (num != null) {
                return computeBsGsResultNeg(num);
            }
        }
        return null;
    }

    @Override
    public NativeECElgamalCipher addCiphers(NativeECElgamalCipher cipherA, NativeECElgamalCipher cipherB) {
        String res = addCipherNative(cipherA.getCipher(), cipherB.getCipher());
        return new NativeECElgamalCipher(res);
    }

    private BigInteger generateK() {
        BigInteger num = getCurveOrderJ();
        return rand.getRandMod(num);
    }

    private static BigInteger getCurveOrderJ() {
        return new BigInteger(getCurveOrder());
    }

    private static BigInteger getPrimeOfGfJ() {
        return new BigInteger(getPrimeOfGf());
    }

    private static native void setUP(int curveNr);

    private static native void tearDOWN();

    private static native String computePubKey(String secret);

    private static native String getCurveOrder();

    private static native String getPrimeOfGf();

    private static native String encryptNative(String plain, String k, String pubKey);

    private static native String decryptNative(String cipher, String secret);

    private static native String addCipherNative(String cipher1, String cipher2);

    private static native void setUPBSGS(String size, String invSize);

    private static native String getNextBSGSStorageItem();

    private static native String computeBsGsStep(String M);

    private static native String computeBsGsResult(String value);

    private static native String computeBsGsStepNeg(String M);

    private static native String computeBsGsResultNeg(String value);

    private static native String computeGenTimes(String num);

    public static class TableLoader {

        public static final String TABLE_SIZE;
        public static final String INV_TABLE_SIZE;
        private static final String TABLE_NAME = "ectable2pow" + Setting.TABLE_POW_2_SIZE;

        static {
            int exp = Integer.valueOf(Setting.TABLE_POW_2_SIZE);
            int pow = 1 << exp;
            TABLE_SIZE = String.valueOf(pow);
            int invPow = 1 << (Integer.valueOf(32 - exp));
            INV_TABLE_SIZE = String.valueOf(invPow);
        }

        @SuppressWarnings("unchecked")
        public static ImmutableMap<String, String> loadTable() {
            ImmutableMap<String, String> res = null;
            int rawId = ContextHolder.getContext().getResources().getIdentifier(TABLE_NAME, "raw", ContextHolder.getContext().getPackageName());
            InputStream inputS = null;
            ObjectInputStream oInputSt = null;
            try {
                inputS = ContextHolder.getContext().getResources().openRawResource(rawId);
                oInputSt = new ObjectInputStream(inputS);
                res = (ImmutableMap<String, String>) oInputSt.readObject();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (inputS != null)
                        inputS.close();
                    if (oInputSt != null)
                        oInputSt.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return res;
        }

    }

}
