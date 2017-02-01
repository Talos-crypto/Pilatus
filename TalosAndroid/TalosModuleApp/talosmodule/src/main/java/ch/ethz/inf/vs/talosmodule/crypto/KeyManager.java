package ch.ethz.inf.vs.talosmodule.crypto;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import ch.ethz.inf.vs.talosmodule.R;
import ch.ethz.inf.vs.talosmodule.cryptoalg.BasicCrypto;
import ch.ethz.inf.vs.talosmodule.cryptoalg.CRTPreRelic;
import ch.ethz.inf.vs.talosmodule.cryptoalg.PRERelic;
import ch.ethz.inf.vs.talosmodule.exceptions.TalosModuleException;
import ch.ethz.inf.vs.talosmodule.main.User;
import ch.ethz.inf.vs.talosmodule.main.values.TalosDataType;
import ch.ethz.inf.vs.talosmodule.util.Setting;

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

public class KeyManager implements IKeyManager {


    //Not safe, just simple to push it in SharedPrefs
    //!Only for non practical purposes!
    private String alias = "cdbkey";
    private String prealias = "prekey";
    private String sharePrefFile = "cdbfile";

    private User u;

    private byte[] mk = null;

    private PRERelic.PREKey preKey = null;
    private CRTPreRelic.CRTParams[] params = null;

    private SharedPreferences pref;
    private Context con;

    public KeyManager(Context con, User u) {
        pref = con.getSharedPreferences(sharePrefFile, Context.MODE_PRIVATE);
        this.con = con;
        alias = "cdbkey" + u.getMail();
        prealias = "prekey" + u.getMail();
        sharePrefFile = "cdbfile" + u.getMail();
        this.u = u;
    }

    public KeyManager(Context con) {
        pref = con.getSharedPreferences(sharePrefFile, Context.MODE_PRIVATE);
        this.con = con;
    }

    public KeyManager(Context con, byte[] predefKey) throws TalosModuleException {
        pref = con.getSharedPreferences(sharePrefFile, Context.MODE_PRIVATE);
        if(predefKey.length!=BasicCrypto.AES_BLOCK_BYTES)
            throw new TalosModuleException("Wrong Masterkey size: "+predefKey.length);
        mk = predefKey;
        u = new User("dummy", "dummy");
    }

    private void loadPRECrtParams() throws IOException, ClassNotFoundException {
        params = new CRTPreRelic.CRTParams[2];
        params[0] = CRTPreRelic.CRTParams.fromStringRep(con.getString(R.string.paramPre32));
        params[1] = CRTPreRelic.CRTParams.fromStringRep(con.getString(R.string.paramPre64));
    }

    private byte[] loadMK() throws TalosModuleException {
        if(Setting.DEBUG_KEY) {
            return new byte[16];
        }

        byte[] key = getKeyFromPref();

        if (key == null) {
            try {
                generateKey();
            } catch (Exception e) {
                throw new TalosModuleException(e.getMessage());
            }
            key = getKeyFromPref();
        }
        return key;
    }

    private void loadPREKey() throws TalosModuleException, IOException, ClassNotFoundException {
        if(Setting.DEBUG_KEY) {
            PRERelic.PREKey key;
            byte[] data;
            if(u.getMail().equals("demo.talos@gmail.com")) {
                data = Base64.decode(con.getString(R.string.debug_pre_key_A), Base64.NO_WRAP);
            } else {
                data = Base64.decode(con.getString(R.string.debug_pre_key_B), Base64.NO_WRAP);
            }
            this.preKey = new PRERelic.PREKey(data);
        } else {
            byte[] key = getKeyFromPref();

            if (key == null) {
                try {
                    generatePREKey();
                } catch (Exception e) {
                    throw new TalosModuleException(e.getMessage());
                }
                key = getKeyFromPref();
            }
            this.preKey = new PRERelic.PREKey(key);
        }
    }

    private void generateKey() throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
        KeyGenerator keyGen = null;
        try {
            keyGen = KeyGenerator.getInstance("AES");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        keyGen.init(128);
        SecretKey secretKey = keyGen.generateKey();

        writeToPref(secretKey.getEncoded());
    }

    private void generatePREKey() {
        writeToPref(PRERelic.generatePREKeys().getEncoded());
    }

    private void writeToPref(byte[] k) {
        String store = Base64.encodeToString(k, Base64.DEFAULT);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(alias, store);
        editor.apply();
    }

    private byte[] getKeyFromPref() {
        String key = pref.getString(alias, null);
        if (key == null)
            return null;

        return Base64.decode(key, Base64.DEFAULT);
    }

    private void writePreToPref(byte[] k) {
        String store = Base64.encodeToString(k, Base64.DEFAULT);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(prealias, store);
        editor.apply();
    }

    private byte[] getPREKeyFromPref() {
        String key = pref.getString(prealias, null);
        if (key == null)
            return null;

        return Base64.decode(key, Base64.DEFAULT);
    }

    @Override
    public TalosKey getKey(String colName) throws TalosModuleException {
        byte[] res = new byte[BasicCrypto.AES_BLOCK_BYTES];
        if (this.mk == null)
            this.mk = loadMK();
        MessageDigest md = null;

        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        }

        md.update(colName.getBytes());
        byte[] shaDig = md.digest();

        System.arraycopy(shaDig, 0, res, 0, BasicCrypto.AES_BLOCK_BYTES);

        TalosKey finalKey = null;
        try {
            finalKey = new TalosKey(BasicCrypto.encrypt_AES(res, mk));
        } catch (Exception e) {
            throw new TalosModuleException(e.getMessage());
        }
        return finalKey;
    }

    @Override
    public PRERelic.PREKey getPREKey() throws TalosModuleException {
        if (this.preKey == null)
            try {
                loadPREKey();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                return null;
            }
        return this.preKey;
    }

    @Override
    public CRTPreRelic.CRTParams getPRECrtParams(TalosDataType type) throws TalosModuleException {
        if(this.params==null)
            try {
                loadPRECrtParams();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }

        switch (type) {
            case INT_32:
                return params[0];
            case INT_64:
                return params[1];
            case STR:
            case UNKNOWN:
                break;
        }
        return null;
    }


}
