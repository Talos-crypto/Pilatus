package ch.ethz.inf.vs.talosmodule.main.taloscrypto;

import ch.ethz.inf.vs.talosmodule.crypto.KeyManager;
import ch.ethz.inf.vs.talosmodule.crypto.TalosKey;
import ch.ethz.inf.vs.talosmodule.crypto.enclayers.DETAesCMC;
import ch.ethz.inf.vs.talosmodule.crypto.enclayers.DETAesStr;
import ch.ethz.inf.vs.talosmodule.crypto.enclayers.DETBfInt;
import ch.ethz.inf.vs.talosmodule.crypto.enclayers.EncLayer;
import ch.ethz.inf.vs.talosmodule.crypto.enclayers.HOMCrtGamal;
import ch.ethz.inf.vs.talosmodule.crypto.enclayers.HOMECElGamal;
import ch.ethz.inf.vs.talosmodule.crypto.enclayers.HOMPaillier;
import ch.ethz.inf.vs.talosmodule.crypto.enclayers.PREHOMCrtRelic;
import ch.ethz.inf.vs.talosmodule.crypto.enclayers.RNDAesStr;
import ch.ethz.inf.vs.talosmodule.crypto.enclayers.RNDBfInt;
import ch.ethz.inf.vs.talosmodule.exceptions.TalosModuleException;
import ch.ethz.inf.vs.talosmodule.main.TalosColumn;
import ch.ethz.inf.vs.talosmodule.main.mOPEOperationType;
import ch.ethz.inf.vs.talosmodule.main.values.TalosCipher;
import ch.ethz.inf.vs.talosmodule.main.values.TalosDataType;
import ch.ethz.inf.vs.talosmodule.main.values.TalosValue;
import ch.ethz.inf.vs.talosmodule.main.values.mOPEClientCipher;
import ch.ethz.inf.vs.talosmodule.util.Setting;

import static ch.ethz.inf.vs.talosmodule.main.values.TalosDataType.INT_32;
import static ch.ethz.inf.vs.talosmodule.main.values.TalosDataType.INT_64;

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
 * The talos crypto manager is the provider of the encryption
 * and decryption functions for the different encryption layers.
 */
public class TalosCryptoManager implements TalosEncryptor, TalosDecryptor {

    private KeyManager keyManager;

    private TalosCryptoManager(KeyManager keyManager) {
        this.keyManager = keyManager;
    }

    public static TalosEncryptor createTalosEncryptor(KeyManager keyManager) {
        return new TalosCryptoManager(keyManager);
    }

    public static TalosDecryptor createTalosDecryptor(KeyManager keyManager) {
        return new TalosCryptoManager(keyManager);
    }

    private TalosKey getKeyFromVal(String col) throws TalosModuleException {
        return keyManager.getKey(col);
    }

    /**
     * Encrypts a talos value randomly, given the column in the database.
     * The key is derived from the column.
     * @param val the value to encrypt
     * @param column the corresponding column of the value
     * @return returns the encrypted ciphertext
     * @throws TalosModuleException if encryption fails
     */
    @Override
    public TalosCipher encryptRND(TalosValue val, TalosColumn column) throws TalosModuleException {
        EncLayer layer = getLayerRND(val.getType(), getKeyFromVal(column.getToken()));
        return layer.encrypt(val);
    }

    /**
     * Decrypts a randomly encrypted ciphertext, which is given in string format.
     * The type of the ciphertext and the corresponding column is given as an input.
     * @param val the ciphertext
     * @param type the type of the resulting value
     * @param column the column of the value.
     * @return the pliantext value
     * @throws TalosModuleException
     */
    @Override
    public TalosValue decryptRND(String val, TalosDataType type, TalosColumn column) throws TalosModuleException {
        EncLayer layer = getLayerRND(type, getKeyFromVal(column.getToken()));
        TalosCipher ciph = layer.getCipherFromString(val);
        TalosValue res = layer.decrypt(ciph);
        res.setType(type);
        return res;
    }

    private EncLayer getLayerRND(TalosDataType type, TalosKey key) throws TalosModuleException {
        switch(type) {
            case INT_32:
            case INT_64:
                return new RNDBfInt(key);
            case STR:
                return new RNDAesStr(key);
            case UNKNOWN:
                throw new TalosModuleException("Unkown type, cannot decide Layer");
        }
        throw new TalosModuleException("Bad type, cannot decide Layer");
    }

    /**
     * Encrypts a talos value deterministically, given the column in the database.
     * The key is derived from the column.
     * @param val the value to encrypt
     * @param column the corresponding column of the value
     * @return returns the encrypted ciphertext
     * @throws TalosModuleException if encryption fails
     */
    @Override
    public TalosCipher encryptDET(TalosValue val, TalosColumn column) throws TalosModuleException {
        EncLayer layer = getLayerDETEnc(val.getType(), getKeyFromVal(column.getToken()), val.getConentSize());
        return layer.encrypt(val);
    }

    /**
     * Decrypts a deterministically encrypted ciphertext, which is given in string format.
     * The type of the ciphertext and the corresponding column is given as an input.
     * @param val the ciphertext
     * @param type the type of the resulting value
     * @param column the column of the value.
     * @return the pliantext value
     * @throws TalosModuleException
     */
    @Override
    public TalosValue decryptDET(String val, TalosDataType type, TalosColumn column) throws TalosModuleException {
        EncLayer layer = getLayerDETDec(type, getKeyFromVal(column.getToken()), val.length());
        TalosCipher ciph = layer.getCipherFromString(val);
        if(type.equals(TalosDataType.STR))
            layer = getLayerDETDec(type, getKeyFromVal(column.getToken()), ciph.getContentSize());
        TalosValue res = layer.decrypt(ciph);
        res.setType(type);
        return res;
    }

    private EncLayer getLayerDETEnc(TalosDataType type, TalosKey key, int size) throws TalosModuleException {
        switch(type) {
            case INT_32:
            case INT_64:
                return new DETBfInt(key);
            case STR:
                if(size<16)
                    return new DETAesStr(key);
                else
                    return new DETAesCMC(key);
            case UNKNOWN:
                throw new TalosModuleException("Unkown type, cannot decide Layer");
        }
        throw new TalosModuleException("Bad type, cannot decide Layer");
    }

    private EncLayer getLayerDETDec(TalosDataType type, TalosKey key, int size) throws TalosModuleException {
        switch(type) {
            case INT_32:
            case INT_64:
                return new DETBfInt(key);
            case STR:
                if(size==16)
                    return new DETAesStr(key);
                else
                    return new DETAesCMC(key);
            case UNKNOWN:
                throw new TalosModuleException("Unkown type, cannot decide Layer");
        }
        throw new TalosModuleException("Bad type, cannot decide Layer");
    }

    /**
     * Encrypts a talos value homomorphically, given the column in the database.
     * The key is derived from the column. Only
     * @param val the value to encrypt
     * @param column the corresponding column of the value
     * @return returns the encrypted ciphertext
     * @throws TalosModuleException if encryption fails
     */
    @Override
    public TalosCipher encryptHOM(TalosValue val, TalosColumn column) throws TalosModuleException {
        EncLayer layer = getLayerHOM(val.getType(), getKeyFromVal(column.getToken()));
        return layer.encrypt(val);
    }

    @Override
    public TalosCipher encryptHOMPRE(TalosValue val, TalosColumn column) throws TalosModuleException {
        EncLayer layer = null;
        switch(val.getType()) {
            case INT_32:
                layer = new PREHOMCrtRelic(keyManager.getPREKey(), keyManager.getPRECrtParams(INT_32), PREHOMCrtRelic.INT_32);
                break;
            case INT_64:
                layer = new PREHOMCrtRelic(keyManager.getPREKey(), keyManager.getPRECrtParams(INT_64), PREHOMCrtRelic.INT_64);
                break;
            case STR:
                throw new TalosModuleException("HOM works only with integers");
            case UNKNOWN:
                throw new TalosModuleException("Unkown type, cannot decide Layer");
            default:
                throw new TalosModuleException("Unkown type, cannot decide Layer");
        }
        return layer.encrypt(val);
    }

    @Override
    public TalosValue decryptHOM(String val, TalosDataType type, TalosColumn column) throws TalosModuleException {
        EncLayer layer = getLayerHOM(type, getKeyFromVal(column.getToken()));
        TalosCipher ciph = layer.getCipherFromString(val);
        TalosValue res = layer.decrypt(ciph);
        res.setType(type);
        return res;
    }

    @Override
    public TalosValue decryptHOMPRE(String val, TalosDataType type, TalosColumn column) throws TalosModuleException {
        EncLayer layer = null;
        switch(type) {
            case INT_32:
                layer = new PREHOMCrtRelic(keyManager.getPREKey(), keyManager.getPRECrtParams(INT_32), PREHOMCrtRelic.INT_32);
                break;
            case INT_64:
                layer = new PREHOMCrtRelic(keyManager.getPREKey(), keyManager.getPRECrtParams(INT_64), PREHOMCrtRelic.INT_64);
                break;
            case STR:
                throw new TalosModuleException("HOM works only with integers");
            case UNKNOWN:
                throw new TalosModuleException("Unkown type, cannot decide Layer");
            default:
                throw new TalosModuleException("Unkown type, cannot decide Layer");
        }
        return layer.decrypt(layer.getCipherFromString(val));
    }

    private EncLayer getLayerHOM(TalosDataType type, TalosKey key) throws TalosModuleException {
        switch(type) {
            case INT_32:
                if(Setting.useCRTEcElGamal)
                    return new HOMCrtGamal(key, HOMCrtGamal.INT_32);
                return new HOMECElGamal(key);
            case INT_64:
                if(Setting.useCRTEcElGamal)
                    return new HOMCrtGamal(key, HOMCrtGamal.INT_64);
                return new HOMPaillier(key);
            case STR:
                throw new TalosModuleException("HOM works only with integers");
            case UNKNOWN:
                throw new TalosModuleException("Unkown type, cannot decide Layer");
        }
        throw new TalosModuleException("Bad type, cannot decide Layer");
    }


    @Override
    public TalosCipher encryptOPE(TalosValue val, TalosColumn detColumn, int indexOfTree, mOPEOperationType type) throws TalosModuleException {
        if(!(val.getType().isInteger()))
            throw new TalosModuleException("Invalid data type for OPE encryption");
        EncLayer layer = new DETBfInt(keyManager.getKey(detColumn.getToken()));
        TalosCipher cipher = layer.encrypt(val);
        return new mOPEClientCipher(indexOfTree, cipher.getStringRep(), detColumn.getToken(), type);
    }



    @Override
    public TalosValue decryptOPE(String val, TalosDataType type, TalosColumn column) throws TalosModuleException {
        throw new TalosModuleException("Not supported with mOPE, use the corresponding det column for decryption");
    }

}
