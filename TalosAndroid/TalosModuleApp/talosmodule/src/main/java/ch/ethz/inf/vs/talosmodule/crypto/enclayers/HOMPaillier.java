package ch.ethz.inf.vs.talosmodule.crypto.enclayers;

import android.util.Log;

import java.math.BigInteger;

import ch.ethz.inf.vs.talosmodule.crypto.TalosKey;
import ch.ethz.inf.vs.talosmodule.cryptoalg.IPRNG;
import ch.ethz.inf.vs.talosmodule.cryptoalg.PRNGRC4Stream;
import ch.ethz.inf.vs.talosmodule.cryptoalg.PaillierPriv;
import ch.ethz.inf.vs.talosmodule.exceptions.TalosModuleException;
import ch.ethz.inf.vs.talosmodule.main.values.HexCipher;
import ch.ethz.inf.vs.talosmodule.main.values.TalosCipher;
import ch.ethz.inf.vs.talosmodule.main.values.TalosValue;
import ch.ethz.inf.vs.talosmodule.util.CDBUtil;

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

public class HOMPaillier extends HOMLayer {

    private static final int N_BITS = 1024;

    private static final int A_BITS = 256;

    private PaillierPriv pail;

    public HOMPaillier(TalosKey key) {
        super();
        IPRNG prng = new PRNGRC4Stream(key.getEncoded());
        this.pail = new PaillierPriv(PaillierPriv.keygen(prng, N_BITS, A_BITS));
    }

    @Override
    public TalosCipher encrypt(TalosValue field) throws TalosModuleException {
        BigInteger plain, res;
        plain = BigInteger.valueOf(field.getLong());
        res = pail.encrypt(plain);
        return HexCipher.createCipher(res.toByteArray());
    }

    @Override
    public TalosValue decrypt(TalosCipher field) throws TalosModuleException {
        BigInteger cipher, res;
        cipher = new BigInteger(field.getByteRep());
        res = pail.decrypt(cipher);
        return TalosValue.createTalosValue(res.longValue());
    }

    @Override
    public TalosCipher getCipherFromString(String in) {
        return HexCipher.createCipher(in);
    }

    @Override
    public String getAgrFunc(String arg) {
        StringBuilder sb = new StringBuilder();
        sb.append(CDBUtil.UDF_AGR_FUNC_PAILLIER).append("( ");
        sb.append(arg).append(", '");
        sb.append(CDBUtil.bytesToHex(pail.hompubkey().toByteArray()));
        Log.d("PAILLIERPUB",CDBUtil.bytesToHex(pail.hompubkey().toByteArray()));
        sb.append("' )");
        return sb.toString();
    }

    public String getPubKey() {
        return CDBUtil.bytesToHex(pail.hompubkey().toByteArray());
    }


}
