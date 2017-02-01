package ch.ethz.inf.vs.talosmodule.main.values;

import java.math.BigInteger;

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

public class NumberCipher extends TalosCipher {

    private int size;

    private NumberCipher(int size) {
        this.size = size;
    }

    public static NumberCipher createCipher(byte[] ciph) {
        NumberCipher res = new NumberCipher(ciph.length);
        res.setCipher(ciph);
        return res;
    }

    public static NumberCipher createCipher(String ciph, int size) {
        NumberCipher res = new NumberCipher(size);
        res.setCipher(ciph);
        return res;
    }

    @Override
    public void setCipher(String rep) {
        BigInteger temp = new BigInteger(rep);
        byte[] values = temp.toByteArray();
        if (values.length > size) {
            int offset = values.length - size;
            byte[] res = new byte[size];
            System.arraycopy(values, offset, res, 0, size);
            values = res;
        } else if (values.length < size) {
            byte[] res = new byte[size];
            System.arraycopy(values, 0, res, 0, values.length);
            values = res;
        }
        this.content = values;
    }

    @Override
    public String getStringRep() {
        BigInteger temp = new BigInteger(1,this.content);
        return temp.toString();
    }


}
