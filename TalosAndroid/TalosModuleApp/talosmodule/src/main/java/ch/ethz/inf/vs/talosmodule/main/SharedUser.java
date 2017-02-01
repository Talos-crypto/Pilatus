package ch.ethz.inf.vs.talosmodule.main;

import android.util.Base64;

import ch.ethz.inf.vs.talosmodule.crypto.KeyManager;
import ch.ethz.inf.vs.talosmodule.cryptoalg.PRERelic;
import ch.ethz.inf.vs.talosmodule.exceptions.TalosModuleException;

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
 * Represents a User that can be shared with.
 * Can be obtained by querying the Talos Cloud.
 */
public class SharedUser {

    /**
     * The User ID in the database
     */
    private final int localID;

    /**
     * Mail
     */
    private final String mail;

    /**
     * The PRE public key for sharing (creating PRE tokens)
     */
    private final byte[] pk;

    public SharedUser(int localID, String mail, byte[] pk) {
        this.localID = localID;
        this.mail = mail;
        this.pk = pk;
    }

    public int getLocalID() {
        return localID;
    }

    public String getMail() {
        return mail;
    }

    public byte[] getPk() {
        return pk;
    }

    public PRERelic.PREToken generateToken(KeyManager key) {
        try {
            return PRERelic.createToken(key.getPREKey(), new PRERelic.PREKey(pk));
        } catch (TalosModuleException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static final String DELIM = "|";

    public String encodeAsString() {
        StringBuilder sb = new StringBuilder();
        sb.append(localID).append(DELIM).append(mail).append(DELIM).append(Base64.encodeToString(pk, Base64.NO_WRAP));
        return sb.toString();
    }

    public static SharedUser decodeFromString(String str) {
        String[] splits = str.split("\\|");
        if(splits.length!=3)
            throw new IllegalArgumentException("Wromge encoding "+str);
        int localID = Integer.valueOf(splits[0]);
        String mail = splits[1];
        byte[] pk = Base64.decode(splits[2], Base64.NO_WRAP);
        return new SharedUser(localID, mail, pk);
    }
}
