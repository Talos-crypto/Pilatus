package ch.ethz.inf.vs.talosmodule.cryptoalg;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
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
 * Implementation of EC-ElGamal with the Baby Step Giant Step algorithm.
 * Look up table lies in native heap.
 * Also uses parallelism for better performance
 * The class calls OpenSSL functions via the JNI, and is implemented for a specific curve.
 * Could later be adapted to the Bouncy Castle design principles.
 */
public class FastECElGamal extends NativeECElGamalCrypto {

    public static final int prime192v1 = 409;
    public static final int prime239v1 = 412;
    public static final int prime256v1 = 415;
    public static final int secp160r1 = 709;
    public static final int secp224k1 = 712;
    public static final int secp384r1 = 715;
    public static final int secp521r1 = 716;

    /**
     * The id of the EC-Curve, default 0 = NIST-192p / prime192v1
     */
    public static final int CURVE_ID = prime192v1;
    // Constants
    public static int TABLE_SIZE_POW = Integer.valueOf(Setting.TABLE_POW_2_SIZE);
    private static final boolean LOAD_TABLE_FROM_FILE = true;

    /**
     * Loads the native library and sets the curve.
     */
    static {
        System.loadLibrary("ecelgamal");
        setUP(CURVE_ID);
    }

    /**
     * Public key
     */
    private final NativeECELGamalPublicKey pubKey;

    /**
     * Private key
     */
    private final NativeECELGamalPrivateKey privKey;

    /**
     * Pseudo random generator use for encryption
     */
    private IPRNG rand;

    /**
     * Creates a additive-homomorphic EC-ElGamal cipher with the give keys and a PRNG
     * @param kp teh public and private key for the cipher
     * @param rand the PRNG for encryption
     * @throws IllegalArgumentException if wrong key format
     */
    public FastECElGamal(KeyPair kp, IPRNG rand) throws IllegalArgumentException {
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

    /**
     * Encrypts the given integer to a additive-homomorphic cipher.
     * First, the integer is mapped to the EC-Curve and, afterwards, encrypted with EC-ElGamal.
     * @param plain the integer to encrypt
     * @return an EC-Elgamal cipher consiting of two EC-Points encoded as hex string
     */
    @Override
    public NativeECElgamalCipher encrypt(BigInteger plain) {
        BigInteger k;
        String cipher;
        k = generateK();
        cipher = encryptNative(plain.toString(), k.toString(), this.pubKey.getY());
        return new NativeECElgamalCipher(cipher);

    }

    /**
     * Decrypts the give cipher to the initial integer. Therefore, first the EC-Elgamal decryption is performed,
     * followed by the inverse mapping to the integer space.
     * @param cipher the cipher, which should be decrypted
     * @param sizeNumBits the size of the integer space for the inverse mapping (2^sizeNumBits)
     * @param doSigned indicates if the can be signed
     * @return the plaintext integer
     * @throws Exception if decryption failed
     */
    @Override
    public BigInteger decrypt(NativeECElgamalCipher cipher, int sizeNumBits, boolean doSigned) throws Exception {
        if (!TableLoader.isLoaded()) {
            loadLookUpTable();
        }

        String decryptPoint = decryptNative(cipher.getCipher(), privKey.getX().toString());
        long numStr = solveDiscreteLogBSGS(decryptPoint,sizeNumBits, doSigned);

        return BigInteger.valueOf(numStr);
    }


    /**
     * Generates a EC-ElGamal Key-Pair with the provided PRNG.
     * @param rand the PRNG for computing the keys.
     * @return
     */
    public static KeyPair generateKeys(IPRNG rand) {
        NativeECELGamalPrivateKey privKey;
        NativeECELGamalPublicKey pubKey;
        BigInteger x;
        String Y;

        BigInteger prime = getCurveOrderJ();
        x = rand.getRandMod(prime);
        privKey = new NativeECELGamalPrivateKey(x);
        Y = computePubKey(x.toString());
        pubKey = new NativeECELGamalPublicKey(CURVE_ID, Y);

        return new KeyPair(pubKey, privKey);
    }

    /**
     * Loads/computes the lookup-table to the native heap for the defined curve.
     */
    public static void loadLookUpTable() {
        if (LOAD_TABLE_FROM_FILE) {
            TableLoader.loadTableFast(1 << TABLE_SIZE_POW);
        } else {
            initTable();
            computeTable(1 << TABLE_SIZE_POW);
            TableLoader.tableFastLoaded = true;
        }
    }


    private long solveDiscreteLogBSGS(String point, int sizeBits, boolean doSigned) throws Exception {
        if(Setting.NUM_THREADS>1) {
            return ParECDLPSolver.solveECDLP(point,sizeBits,TABLE_SIZE_POW, doSigned);
        } else {
            long size;
            int exp =  sizeBits-TABLE_SIZE_POW;
            if(exp<=0)
                size = 1L;
            else
                size = (1L << exp) + 1;
            return solveECDLPBsGs(point, size, doSigned);
        }
    }

    /**
     * Adds two Ciphers
     * @param cipherA the first cipher
     * @param cipherB the second cipher
     * @return the resulting cipher
     */
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

    /*
    Native CODE
    EC-POINTS are represented in hexadecimal Strings (compressed encoding)
    BigIntegers are passed as decimal Strings
     */

    public static native void initTable();
    public static  native void destroyTable();
    public static native int putInTable(String key, int value);
    public static native int getFromTable(String key);
    public static native void computeTable(int size);
    public static native void setTableSizeConstants(int size);

    public static native long solveECDLPBsGs(String m, long maxIt, boolean doSigned);

    private native static void setUP(int curveNr);
    public native static void tearDOWN();

    private native static String computePubKey(String secret);

    private static native String getCurveOrder();
    private static native String getPrimeOfGf();

    private static native String encryptNative(String plain, String k, String pubKey);
    private static native String decryptNative(String cipher,String secret);
    private static native String addCipherNative(String cipher1,String cipher2);

    public static native String computeGenTimes(String num);

    // Par Stuff
    private static class ResultState {
        public int found = 0;
        public long result = 0;
    }
    private static native long solveECDLPBsGsPos(ResultState state, String M, long starti, long endI) throws Exception;
    private static native long solveECDLPBsGsNeg(ResultState state, String M, long starti, long endI) throws Exception;

    // Classes for Keys and Cipher, Loader

    /**
     * Parallel ECDLP-Solver, which also depends on the statically defined curve and calls
     * JNI functions.
     */
    private static class ParECDLPSolver {

        private static final int NUM_THREADS_PERSIDE = Setting.NUM_THREADS / 2;
        private static final long MAX_WAIT_TIME_BEFORE_ERROR = 10000;

        /**
         * Solves the ECDLP with the defined input parameters
         * @param M the message EC-POINT compressed encoding in hexadecimal
         * @param sizePow the power of the integer ex 32 for int
         * @param tableSize the size of the table (2^tableSize)
         * @param doSigned indicates if also negative values are accepted
         * @return the result x for xG=M
         * @throws Exception
         */
        public static long solveECDLP(final String M, final int sizePow, int tableSize, boolean doSigned) throws Exception {
            ResultState state = new ResultState();
            Thread[] threads = new Thread[NUM_THREADS_PERSIDE*2 - 1];
            long sideRange = 1L<<(sizePow-tableSize-1) +1;
            long maxIt = BigDecimal.valueOf(sideRange).divide(BigDecimal.valueOf(NUM_THREADS_PERSIDE),BigDecimal.ROUND_UP).longValue();
            long curStart = 0;
            long curEnd= maxIt;

            if(doSigned) {
                for (int i = 0; i < NUM_THREADS_PERSIDE; i++) {
                    threads[i] = createThread(state, M, curStart, curEnd, true);
                    if (i != 0) {
                        threads[i + NUM_THREADS_PERSIDE - 1] = createThread(state, M, curStart, curEnd, false);
                    }
                    curStart += maxIt;
                    curEnd += maxIt;
                }
            } else {
                curStart += maxIt;
                curEnd += maxIt;
                for (int i = 1; i < NUM_THREADS_PERSIDE*2; i++) {
                    threads[i-1] = createThread(state, M, curStart, curEnd, false);
                    curStart += maxIt;
                    curEnd += maxIt;
                }
            }

            for(Thread t : threads) {
                t.start();
            }

            long res = 0;
            boolean error = false;
            try {
                res = solveECDLPBsGsPos(state, M, 0, maxIt);
            } catch(Exception e) {
                error = true;
            }
            if(!error) {
                state.found=1;
                state.result = res;
            } else {
                long startime = System.currentTimeMillis();
                while(!(state.found==1)) {
                    if(System.currentTimeMillis()-startime>MAX_WAIT_TIME_BEFORE_ERROR) {
                        throw new RuntimeException("Result not found");
                    }
                    Thread.sleep(100);
                }
            }

            return state.result;
        }

        private static Thread createThread(final ResultState state, final String M, final long from, final long to, final boolean isNeg) {
            return new Thread() {

                private boolean error = false;

                @Override
                public void run() {
                    long res = 0;
                    try {
                        if(isNeg)
                            res = solveECDLPBsGsNeg(state, M, from, to);
                        else
                            res = solveECDLPBsGsPos(state, M, from, to);
                    } catch(Exception e) {
                        error = true;
                    }
                    if(!error) {
                        state.found = 1;
                        state.result = res;
                    }
                    super.run();
                }

            };
        }
    }

    /**
     * Implements the table loader, which loads the EC-Lookuptable from a file,
     * At the moments this class depends on the Android Context (ContextHolder) for accessing the files.
     * The EC-Table file is located in the raw resources (Android).
     */
    public static class TableLoader {

        private static final String TABLE_NAME_FAST = "table2pow19";
        private static final String TABLE_NAME_FAST_EXT = "table2pow19_20";

        private static boolean tableFastLoaded = false;

        /**
         * Loads the table up to the specified size from the files.
         * @param size the size of the table.
         */
        public static void loadTableFast(int size) {
            int rawId = ContextHolder.getContext().getResources().getIdentifier(TABLE_NAME_FAST, "raw", ContextHolder.getContext().getPackageName());
            int rawIdext = ContextHolder.getContext().getResources().getIdentifier(TABLE_NAME_FAST_EXT, "raw", ContextHolder.getContext().getPackageName());
            InputStream inputS= null;
            InputStreamReader buffreader= null;
            BufferedReader reader = null;
            BufferedReader readerExt = null;
            FastECElGamal.initTable();
            try {
                boolean needExt = false;
                if(Setting.TABLE_POW_2_SIZE.equals("20")) {
                    needExt = true;
                    size = 1<<19;
                }
                inputS = ContextHolder.getContext().getResources().openRawResource(rawId);
                buffreader = new InputStreamReader(inputS);
                reader = new BufferedReader(buffreader);
                String line;
                int count = 0;
                while((line=reader.readLine())!=null && count <= size) {
                    FastECElGamal.putInTable(line, count);
                    count++;
                }
                if(needExt) {
                    readerExt = new BufferedReader(new InputStreamReader(ContextHolder.getContext().getResources().openRawResource(rawIdext)));

                    while((line=readerExt.readLine())!=null && count <= 1<<20) {
                        FastECElGamal.putInTable(line, count);
                        count++;
                    }

                    FastECElGamal.setTableSizeConstants(1<<20);
                } else {
                    FastECElGamal.setTableSizeConstants(size);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if(inputS!=null)
                        inputS.close();
                    if(buffreader!=null)
                        buffreader.close();
                    if(reader!=null)
                        reader.close();
                    if(readerExt!=null)
                        readerExt.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            tableFastLoaded = true;
        }

        public static boolean isLoaded() {
            return tableFastLoaded;
        }

        public static void freeTables() {
            if(isLoaded())
                FastECElGamal.destroyTable();
            tableFastLoaded = false;
        }
    }
}
