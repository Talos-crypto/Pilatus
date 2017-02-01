package ch.ethz.inf.vs.talosmodule.crypto.enclayers;

import java.math.BigInteger;

import ch.ethz.inf.vs.talosmodule.crypto.TalosKey;
import ch.ethz.inf.vs.talosmodule.cryptoalg.CRTEcElGamal;
import ch.ethz.inf.vs.talosmodule.cryptoalg.IPRNG;
import ch.ethz.inf.vs.talosmodule.cryptoalg.PRNGImpl;
import ch.ethz.inf.vs.talosmodule.cryptoalg.PRNGRC4Stream;
import ch.ethz.inf.vs.talosmodule.exceptions.TalosModuleException;
import ch.ethz.inf.vs.talosmodule.main.values.CrtGamalHexCipher;
import ch.ethz.inf.vs.talosmodule.main.values.TalosCipher;
import ch.ethz.inf.vs.talosmodule.main.values.TalosValue;

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

public class HOMCrtGamal extends EncLayer {

    public static final int INT_64 = 2;
    public static final int INT_32 = 1;

    private CRTEcElGamal gamal;
    private int type;

    public HOMCrtGamal(TalosKey key, int bitID) throws IllegalArgumentException {
        IPRNG prng = new PRNGRC4Stream(key.getEncoded());
        init(prng, bitID);
        type = bitID;
    }

    private void init(IPRNG prng, int bitID) throws IllegalArgumentException {
        if(bitID == INT_64) {
            gamal = new CRTEcElGamal(CRTEcElGamal.generateKeys(prng,22,3), new PRNGImpl());
        } else if(bitID == INT_32) {
            gamal = new CRTEcElGamal(CRTEcElGamal.generateKeys(prng,17,2), new PRNGImpl());
        } else {
            throw new IllegalArgumentException("Not supported type");
        }
    }

    @Override
    public TalosCipher encrypt(TalosValue field) throws TalosModuleException {
        CRTEcElGamal.CRTEcElGamalCipher cipher;
        try {
            cipher = gamal.encrypt(field.getBigInteger());
        } catch (Exception e) {
            throw new TalosModuleException("CRTEcElGamal encryption failed for value: "+field.getBigInteger().toString());
        }
        return new CrtGamalHexCipher(cipher);
    }

    @Override
    public TalosValue decrypt(TalosCipher field) throws TalosModuleException {
        if(!(field instanceof CrtGamalHexCipher))
            throw new TalosModuleException("Wrong input cipher");
        CRTEcElGamal.CRTEcElGamalCipher cipher = ((CrtGamalHexCipher) field).getCrtGamalCipher();
        BigInteger plain;
        try {
            if(type == INT_64) {
                plain = gamal.decrypt(cipher, Long.MAX_VALUE);
            } else if(type == INT_32) {
                plain = gamal.decrypt(cipher, Integer.MAX_VALUE);
            } else {
                throw new IllegalArgumentException("Not supported type");
            }
        } catch (Exception e) {
            throw new TalosModuleException("CRTEcElGamal decryption failed for value: "+ field.getStringRep());
        }
        return TalosValue.createTalosValue(plain.longValue());
    }

    @Override
    public TalosCipher getCipherFromString(String in) {
        CrtGamalHexCipher res = new CrtGamalHexCipher();
        res.setCipher(in);
        return res;
    }

}
