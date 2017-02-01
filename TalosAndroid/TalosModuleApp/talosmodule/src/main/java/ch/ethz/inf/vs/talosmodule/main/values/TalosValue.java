package ch.ethz.inf.vs.talosmodule.main.values;

import com.google.common.primitives.Longs;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

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
 *  A TalosValue is a abstract representation for values that should be stored in the cloud.
 *  TalosValues can be transformed to TalosCiphers by encrypting them and vice-versa.
 */
public class TalosValue {

    private TalosDataType type;
    private byte[] content;

    private TalosValue(byte[] in, TalosDataType type) {
        this.content = in;
        this.type = type;
    }

    /**
     * Creates a TalosValue from an integer.
     * @param val the value in form of an integer
     * @return the TalosValue of the input value
     */
    public static TalosValue createTalosValue(int val) {
        return new TalosValue(Longs.toByteArray((long) val), TalosDataType.INT_32);
    }

    /**
     * Creates a TalosValue from a byte array.
     * @param res the value in form of a byte array
     * @return the TalosValue of the input value
     */
    public static TalosValue createTalosValue(byte[] res) {
        return new TalosValue(res, TalosDataType.UNKNOWN);
    }

    /**
     * Creates a TalosValue from an long.
     * @param val the value in form of a long.
     * @return the TalosValue of the input value.
     */
    public static TalosValue createTalosValue(long val) {
        return new TalosValue(Longs.toByteArray(val), TalosDataType.INT_64);
    }

    /**
     * Creates a TalosValue from a java.util.Date.
     * @param val the value in form of a java.util.Date.
     * @return the TalosValue of the input value.
     */
    public static TalosValue createTalosValue(Date val) {
        long time = val.getTime() / 1000L;
        return new TalosValue(Longs.toByteArray(time), TalosDataType.INT_32);
    }

    /**
     * Creates a Talos Value from a float, encodes the float into a long.
     * Therefore, precision can be lost.
     * @param val the value to encode
     * @param precision the number of digits after the comma (1.324234, 3) -> 1324
     * @return  the TalosValue of the input value.
     * @throws IllegalArgumentException if encoding does not fit into a long
     */
    public static TalosValue createTalosValue(float val, int precision) throws IllegalArgumentException {
        BigInteger encoding;
        BigDecimal temp = BigDecimal.valueOf(val);
        temp = temp.movePointRight(precision);
        encoding = temp.toBigInteger();
        if(encoding.compareTo(BigInteger.valueOf(Long.MAX_VALUE))>0 ||
                encoding.compareTo(BigInteger.valueOf(Long.MIN_VALUE)) < 0)
            throw  new IllegalArgumentException("Does not fit into encoding " + encoding);
        return new TalosValue(Longs.toByteArray(encoding.longValue()), TalosDataType.INT_64);
    }

    /**
     * Creates a Talos Value from a float, encodes the double into a long.
     * Therefore, precision can be lost or the double might not fit into a 64-bit integer dependant on the precision.
     * @param val the value to encode
     * @param precision the number of digits after the comma (1.324234, 3) -> 1324
     * @return  the TalosValue of the input value.
     * @throws IllegalArgumentException if encoding does not fit into a long
     */
    public static TalosValue createTalosValue(double val, int precision) throws IllegalArgumentException {
        BigInteger encoding;
        BigDecimal temp = BigDecimal.valueOf(val);
        temp = temp.movePointRight(precision);
        encoding = temp.toBigInteger();
        if(encoding.compareTo(BigInteger.valueOf(Long.MAX_VALUE))>0 ||
                encoding.compareTo(BigInteger.valueOf(Long.MIN_VALUE)) < 0)
            throw  new IllegalArgumentException("Does not fit into encoding " + encoding);
        return new TalosValue(Longs.toByteArray(encoding.longValue()), TalosDataType.INT_64);
    }

    /**
     * Creates a TalosValue from a String.
     * @param in the value in form of a String.
     * @return the TalosValue of the input value.
     */
    public static TalosValue createTalosValue(String in) {
        try {
            return new TalosValue(in.getBytes("UTF-8"), TalosDataType.STR);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     *
     * @return the type of the Value
     */
    public TalosDataType getType() {
        return type;
    }

    /**
     *
     * @return the value represented in a byte array
     */
    public byte[] getContent() {
        return content;
    }

    /**
     *
     * @return the value represented as an integer
     */
    public int getInt() {
        return (int) getLong();
    }

    /**
     *
     * @return the value represented as a long,
     */
    public long getLong() {
        return Longs.fromByteArray(content);
    }

    /**
     *
     * @return the value represented as a java.util.Date,
     */
    public Date getDate() {
        return new Date(this.getLong() * 1000L);
    }

    /**
     *
     * @return the value represented as a BigInteger.
     */
    public BigInteger getBigInteger() {
       if(this.getType().equals(TalosDataType.INT_32)) {
           return BigInteger.valueOf(this.getInt());
       } else {
           return BigInteger.valueOf(this.getLong());
       }
    }

    /**
     *
     * @return the value represented as a String.
     */
    public String getString() {
        try {
            return new String(content,"UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Sets the type of this value.
     * @param type the datatype of the value
     */
    public void setType(TalosDataType type) {
        this.type = type;
    }

    /**
     *
     * @return the size of the byte array of this value.
     */
    public int getConentSize() {
        return this.content.length;
    }

}
