package ch.ethz.inf.vs.talosmodule.crypto.enclayers;

import java.math.BigInteger;

import ch.ethz.inf.vs.talosmodule.crypto.TalosKey;
import ch.ethz.inf.vs.talosmodule.cryptoalg.FastECElGamal;
import ch.ethz.inf.vs.talosmodule.cryptoalg.IPRNG;
import ch.ethz.inf.vs.talosmodule.cryptoalg.NativeECElGamal;
import ch.ethz.inf.vs.talosmodule.cryptoalg.NativeECElGamalCrypto;
import ch.ethz.inf.vs.talosmodule.cryptoalg.PRNGImpl;
import ch.ethz.inf.vs.talosmodule.cryptoalg.PRNGRC4Stream;
import ch.ethz.inf.vs.talosmodule.exceptions.TalosModuleException;
import ch.ethz.inf.vs.talosmodule.main.values.ECPointCipher;
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

public class HOMECElGamal extends HOMLayer {

    private NativeECElGamalCrypto alg;

    public HOMECElGamal(TalosKey key) {
        super();
        IPRNG prng = new PRNGRC4Stream(key.getEncoded());
        alg = new FastECElGamal(NativeECElGamal.generateKeys(prng), new PRNGImpl());
    }

    @Override
    public String getAgrFunc(String arg) {
        StringBuilder sb = new StringBuilder();
        sb.append(CDBUtil.UDF_AGR_FUNC_ELGAMAL).append("( ");
        sb.append(arg).append(")");
        return sb.toString();

    }

    @Override
    public TalosCipher encrypt(TalosValue field) throws TalosModuleException {
        BigInteger plain;
        NativeECElGamalCrypto.NativeECElgamalCipher cipher;
        plain = BigInteger.valueOf(field.getLong());
        try {
            cipher = alg.encrypt(plain);
        } catch (Exception e) {
            throw new TalosModuleException("Encryption failed: "+ e.getMessage());
        }
        return ECPointCipher.createCipher(cipher);
    }

    @Override
    public TalosValue decrypt(TalosCipher field) throws TalosModuleException {
        BigInteger plain;
        NativeECElGamal.NativeECElgamalCipher cipher;
        cipher = new NativeECElGamal.NativeECElgamalCipher(field.getStringRep());
        try {
            //default 32 bit
            plain = alg.decrypt(cipher, 32, true);
        } catch (Exception e) {
            throw new TalosModuleException("Decryption failed: "+ e.getMessage());
        }
        return TalosValue.createTalosValue(plain.longValue());
    }

    @Override
    public TalosCipher getCipherFromString(String in) {
        return ECPointCipher.createCipher(in);
    }
}
