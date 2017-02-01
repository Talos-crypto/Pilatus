package ch.ethz.inf.vs.talsomoduleapp;

import android.text.format.Time;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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

public class AppUtil {

    public static final int NUM_AFTER_COM = 3;

    public static String getTimeStamp() {
        Time now = new Time();
        now.setToNow();
        String sTime = now.format("%Y_%m_%d_%H_%M_%S");
        return sTime;
    }

    public static String formatTimestamp(String in) {
        SimpleDateFormat formater = new SimpleDateFormat("yyyy_MM_dd_hh_mm_ss");
        Date date = null;
        try {
            date = formater.parse(in);
        } catch (ParseException e) {
            return "";
        }
        SimpleDateFormat sdf =
                new SimpleDateFormat ("dd.MM.yyyy HH:mm:ss");
        return sdf.format(date);
    }

    public static String trimString(String in, int length) {
        if(length>in.length())
            return in;
        return in.substring(0, length);
    }

    public static String calculateAverage(int sum, int count) {
        int temp = sum;
        int countI = count;
        if(temp == 0 || countI == 0)
            return "0";
        temp = temp/ countI;
        return setPoint(String.valueOf(temp));
    }

    public static String setPoint(String in) {
        char[] res;
        char[] cur = in.toCharArray();
        boolean isNeg = in.contains("-");
        int lengthNum;
        if(isNeg)
            lengthNum = in.length()-1;
        else
            lengthNum = in.length();
        int length = in.length() - NUM_AFTER_COM;
        if (lengthNum > NUM_AFTER_COM) {
            res = new char[in.length() + 1];
            res[length] = '.';
            System.arraycopy(cur, 0, res, 0, length);
            System.arraycopy(cur, length, res, length + 1, NUM_AFTER_COM);
        } else {
            int size;
            if(isNeg) {
                length = in.length()-1;
                size = NUM_AFTER_COM + 3;
                res = new char[size];
                res[0] = '-';
                res[1] = '0';
                res[2] = '.';
                System.arraycopy(cur, 1, res, size-length, length);
            } else {
                length = in.length();
                size = NUM_AFTER_COM + 2;
                res = new char[size];
                res[0] = '0';
                res[1] = '.';
                System.arraycopy(cur, 0, res, size-length, length);
            }
            for(int i=0;i<res.length;i++) {
                if(res[i]=='\u0000')
                    res[i]='0';
            }
        }
        return new String(res);
    }

    public static int convertToInteger(String data) {
        BigDecimal num = new BigDecimal(data).setScale(NUM_AFTER_COM, BigDecimal.ROUND_HALF_UP);
        String res = num.toString().replace(".","");
        return Integer.valueOf(res);
    }

}
