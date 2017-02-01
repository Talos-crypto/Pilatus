package ch.ethz.inf.vs.talosmodule.util;

import org.spongycastle.crypto.digests.MD5Digest;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.List;

import ch.ethz.inf.vs.talosmodule.cryptoalg.BasicCrypto;
import ch.ethz.inf.vs.talosmodule.cryptoalg.IPRNG;
import ch.ethz.inf.vs.talosmodule.cryptoalg.PRNGRC4Stream;

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
 * Multiple useful methods that are used
 * in the module
 */
public class CDBUtil {

    public static final String DELIM = "`";

    public static final String DOT = ".";

    public static final String FUNC_DELIMS = "()";

    public static final String FUNC_TAB_COL_PATTERN = ".*\\(.*\\)";

    public static final String TAB_COL_PATTERN = ".*\\..*";

    public static final String STRING_ANNOTATION = "'";

    public static final String VALID_DBHASH_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public static final String VALID_NUM_CHARS = "0123456789";

    public static final int SIZE_DBHASH = 10;

    public static final String[] SUPPORTED_FUNCS = new String[]{"SUM", "MIN", "MAX", "COUNT"};

    public static final String UDF_AGR_FUNC_PAILLIER = "Paillier_agr";

    public static final String UDF_AGR_FUNC_ELGAMAL = "ECElGamal_Agr";

    public static final String UDF_AGR_PRERELIC = "PRE_REL_SUM";

    public static final String UDF_MIN_FUNC_mOPE= "mOPE_MIN";

    public static final String UDF_MAX_FUNC_mOPE= "mOPE_MAX";

    private static IPRNG rand = null;

    /**
     * Parse ref
     *
     * @param ref form Table.Col
     * @return String[] {Table, Col}
     */
    public static String[] getTableColRef(String ref) throws IllegalArgumentException {
        String[] res = null;
        String dot = "\\" + DOT;
        res = ref.split(dot);
        if (res.length != 2)
            throw new IllegalArgumentException("Wrong ref " + ref);
        return res;
    }


    /**
     * Parse func ref from server
     * @param ref form FUNC(Table.Col)
     * @return String[] {Table, Col, FUNC}
     */
    public static String[] getFuncTableColRef(String ref) throws IllegalArgumentException {
        String[] res = null;
        String regex = "[" + FUNC_DELIMS + "\\" + DOT + "]";
        res = ref.split(regex);
        if (res.length != 3)
            throw new IllegalArgumentException("Wrong ref " + ref);
        String[] temp = new String[3];
        temp[0] = res[1];
        temp[1] = res[2];
        temp[2] = res[0];
        return temp;
    }

    /**
     * Packs name in `name` if name does not contain '`'
     * @param in name
     * @return `name`
     */
    public static String packDelim(String in) {
        if (in.contains(DELIM))
            return in;
        return DELIM + in + DELIM;
    }

    /**
     * Checks if the String representation of a Function
     * is of type SUM
     * @param in Func as String
     * @return true or false
     */
    public static boolean isUDFSUMorSUM(String in) {
        return in.equals(UDF_AGR_FUNC_PAILLIER) || in.equals(UDF_AGR_FUNC_ELGAMAL) || in.toUpperCase().equals(SUPPORTED_FUNCS[0]);
    }

    public static String transformToNormalDBFUNC(String in) {
        if (in.equals(UDF_AGR_FUNC_PAILLIER) || in.equals(UDF_AGR_FUNC_ELGAMAL))
            return SUPPORTED_FUNCS[0];
        if (in.equals(UDF_MIN_FUNC_mOPE))
            return SUPPORTED_FUNCS[1];
        if (in.equals(UDF_MAX_FUNC_mOPE))
            return SUPPORTED_FUNCS[2];
        return in;
    }

    /**
     * Transformts the byte array to a
     * hexadecimal String
     * @param in byte array
     * @return hexstring
     */
    public static String bytesToHex(byte[] in) {
        final StringBuilder builder = new StringBuilder();
        for (byte b : in) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

    /**
     * Merges a function and a column to
     * a valid function expression
     * @param func function
     * @param col column
     * @return FUNC(col)
     */
    public static String functionize(String func, String col) {
        return func + "(" + col + ")";
    }

    public static byte[] HexToBytes(String in) {
        int len = in.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(in.charAt(i), 16) << 4)
                    + Character.digit(in.charAt(i + 1), 16));
        }
        return data;
    }

    /**
     * Transforms a byte array to a
     * positive number string
     * @param in byte array
     * @return positive number string
     */
    public static String bytesToPositiveNumString(byte[] in) {
        BigInteger num = new BigInteger(1, in);
        return num.toString();
    }

    /**
     * Transforms a byte array to a signed
     * number String
     * @param in byte array
     * @return signed num String
     */
    public static String bytesToSignedNumString(byte[] in) {
        BigInteger num = new BigInteger(in);
        return num.toString();
    }

    /**
     * Transforms a number String to a byte array
     * @param in number String
     * @return
     */
    public static byte[] NumStringToBytes(String in) {
        BigInteger num = new BigInteger(in);
        byte[] bytes = num.toByteArray();
        if (bytes.length > 0) {
            if (bytes[0] == 0) {
                byte[] cuttedByte = new byte[bytes.length - 1];
                System.arraycopy(bytes, 1, cuttedByte, 0, bytes.length - 1);
                return cuttedByte;
            }

        }
        return num.toByteArray();
    }

    /**
     * Takes a String an outputs a
     * @param in
     * @return
     */
    public static String stringify(String in) {
        return STRING_ANNOTATION + in + STRING_ANNOTATION;
    }

    public static String deStringify(String in) {
        return in.replaceAll(STRING_ANNOTATION, "");
    }

    public static boolean isInStringRep(String in) {
        return in.contains(STRING_ANNOTATION);
    }

    public static String encodeBigInteger(BigInteger v) {
        return bytesToHex(v.toByteArray());
    }

    /**
     * Checks if the function is supported
     * @param func function in String format
     * @return
     */
    public static boolean isValidAgrFunction(String func) {
        String upperFunc = func.toUpperCase();
        for (String agrFunc : SUPPORTED_FUNCS) {
            if (upperFunc.equals(agrFunc))
                return true;
        }
        return false;
    }

    public static boolean isSUM(String func) {
        return func.toUpperCase().equals(SUPPORTED_FUNCS[0]);
    }

    public static boolean isMAXorMIN(String func) {
        return isMAX(func) || isMIN(func);
    }

    public static boolean isMAX(String func) {
        return func.toUpperCase().equals(SUPPORTED_FUNCS[1]);
    }

    public static boolean isMIN(String func) {
        return func.toUpperCase().equals(SUPPORTED_FUNCS[2]);
    }

    public static boolean ismOPEudfMinMax(String func) {
        return func.equals(UDF_MAX_FUNC_mOPE) || func.equals(UDF_MIN_FUNC_mOPE);
    }

    public static byte[] generateSalt() {
        if (rand == null)
            rand = new PRNGRC4Stream(generateSecureBytes(16));
        byte[] iv = new byte[8];
        rand.nextBytes(iv);
        iv[0] |= (1 << 7);
        return iv;
    }

    public static byte[] generateSecureBytes(int size) {
        SecureRandom sr = new SecureRandom();
        byte[] bytes = new byte[size];
        sr.nextBytes(bytes);
        return bytes;
    }

    public static byte[] toAESIV(byte[] in) {
        byte[] out = new byte[BasicCrypto.AES_BLOCK_BYTES];
        MD5Digest md = new MD5Digest();
        md.reset();
        md.update(in, 0, in.length);
        md.doFinal(out, 0);
        return out;
    }

    /**
     * Generates a common hashname for joined
     * columns
     * @param hashes List of column hashes
     * @return common hash
     */
    public static String createCommonName(List<String> hashes) {
        int[] nums = new int[SIZE_DBHASH];
        char[] valids = VALID_DBHASH_CHARS.toCharArray();
        StringBuilder sb = new StringBuilder();

        for (String hash : hashes) {
            char[] cur = hash.toCharArray();
            for (int i = 0; i < SIZE_DBHASH; i++)
                nums[i] += cur[i];
        }

        for (int num : nums)
            sb.append(valids[num % valids.length]);

        return sb.toString();
    }

    public static byte[] concatenateBytes(byte[] iv, byte[] cipher) {
        byte[] res = new byte[iv.length+cipher.length];
        System.arraycopy(iv,0,res,0,iv.length);
        System.arraycopy(cipher,0,res,iv.length,cipher.length);
        return res;
    }

    public static byte[] deconcatenateBytes(byte[] input, byte[] iv) {
        byte[] res = new byte[input.length-iv.length];
        System.arraycopy(input,0,iv,0,iv.length);
        System.arraycopy(input,iv.length,res,0,res.length);
        return res;
    }

    private static int mOPE_ID = 0;
    private static String mOPE_TAG = "OPEENC_";

    public static String getmOPETag() {
        char[] ids = VALID_DBHASH_CHARS.toCharArray();
        mOPE_ID++;
        return mOPE_TAG + ids[mOPE_ID % ids.length];
    }

    public static int getID() {
        mOPE_ID++;
        return mOPE_ID;
    }


}
