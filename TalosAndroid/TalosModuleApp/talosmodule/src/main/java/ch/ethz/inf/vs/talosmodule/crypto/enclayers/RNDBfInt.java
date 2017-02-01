package ch.ethz.inf.vs.talosmodule.crypto.enclayers;

import ch.ethz.inf.vs.talosmodule.crypto.TalosKey;
import ch.ethz.inf.vs.talosmodule.cryptoalg.BasicCrypto;
import ch.ethz.inf.vs.talosmodule.exceptions.TalosModuleException;
import ch.ethz.inf.vs.talosmodule.main.values.NumberCipher;
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

public class RNDBfInt extends EncLayer {

    private final byte[] key;

    public RNDBfInt(TalosKey key) {
        byte[] bfkey = new byte[BasicCrypto.BF_BLOCK_BYTES];
        System.arraycopy(key.getEncoded(), 0, bfkey, 0, bfkey.length);
        this.key = bfkey;
    }

    @Override
    public TalosCipher encrypt(TalosValue field) throws TalosModuleException {
        byte[] iv, plain, res;
        iv=CDBUtil.generateSalt();
        plain = field.getContent();
        try {
            res = BasicCrypto.encrypt_BLOWFISH_CBC(plain, this.key, iv, false);
        } catch (Exception e) {
            throw new TalosModuleException("Encryption failed " + e.getMessage());
        }
        return NumberCipher.createCipher(CDBUtil.concatenateBytes(iv,res));
    }

    @Override
    public TalosValue decrypt(TalosCipher field) throws TalosModuleException {
        byte[] iv, cipher, res;
        iv = new byte[8];
        cipher = CDBUtil.deconcatenateBytes(field.getByteRep(), iv);
        try {
            res = BasicCrypto.decrypt_BLOWFISH_CBC(cipher, this.key, iv, false);
        } catch (Exception e) {
            throw new TalosModuleException("Decryption failed " + e.getMessage());
        }
        return TalosValue.createTalosValue(res);
    }

    @Override
    public TalosCipher getCipherFromString(String in) {
        return NumberCipher.createCipher(in, BasicCrypto.BF_BLOCK_BYTES * 2);
    }

}
