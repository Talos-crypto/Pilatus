package ch.ethz.inf.vs.talosavaapp.talos;

import java.math.BigDecimal;
import java.math.RoundingMode;

import ch.ethz.inf.vs.talosavaapp.R;

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

public enum Datatype {
    RestingPulseRate,
    SkinTemp,
    AmbTemp,
    Sleep,
    OvalTemp,
    OvalHr;

    public String getDisplayRep() {
        return this.name();
    }

    public static String getSleepDeep() {
        return Sleep.name() + "d";
    }

    public static String getSleepLow() {
        return Sleep.name() + "l";
    }

    private static int compurteAVG(int num, int max) {
        BigDecimal a = BigDecimal.valueOf(num);
        BigDecimal b = BigDecimal.valueOf(max);
        a = a.divide(b, RoundingMode.HALF_UP);
        return a.intValue();
    }

    public static int performAVG(Datatype type, int num, int max) {
        switch (type) {
            case RestingPulseRate:
            case SkinTemp:
            case AmbTemp:
            case OvalTemp:
            case OvalHr:
                return compurteAVG(num, max);
            case Sleep:

            default:
                return num;
        }
    }

    public boolean isOval() {
        switch (this) {
            case OvalTemp:
            case OvalHr:
                return true;
            case SkinTemp:
            case AmbTemp:
            case RestingPulseRate:
            case Sleep:
            default:
                return false;

        }
    }

    public int getImgResource() {
        switch (this) {
            case RestingPulseRate:
                return R.drawable.rpr;
            case SkinTemp:
                return R.drawable.skintemp;
            case AmbTemp:
                return R.drawable.skintemp;
            case Sleep:
                return R.drawable.sleep;
        }
        return R.drawable.login;
    }

    public String formatValue(int value) {
        BigDecimal temp;
        switch (this) {
            case OvalTemp:
            case OvalHr:
            case SkinTemp:
            case AmbTemp:
                temp = BigDecimal.valueOf(value).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                return temp.toString();
            case Sleep:
                temp = BigDecimal.valueOf(value).divide(BigDecimal.valueOf(60*60), 1, RoundingMode.HALF_UP);
                return temp.toString();
            case RestingPulseRate:
            default:
                return String.valueOf(value);
        }
    }

    public String getUnit() {
        switch (this) {
            case SkinTemp:
                return "Celsius";
            case AmbTemp:
                return "Celsius";
            case RestingPulseRate:
                return "bpm";
            case Sleep:
                return  "h";
            default:
                return "";
        }
    }
}
