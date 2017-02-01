package ch.ethz.inf.vs.talosmodule.main.values;

import org.json.JSONException;
import org.json.JSONObject;

import ch.ethz.inf.vs.talosmodule.communication.mopemessages.mOPEMessagesUtils;
import ch.ethz.inf.vs.talosmodule.main.mOPEOperationType;

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

public class mOPEClientCipher extends TalosCipher {

    public final static int OP_TYPE_QUERY = 1;
    public final static int OP_TYPE_INSERT = 2;
    public final static int OP_TYPE_UPDATE = 3;
    public final static int OP_TYPE_DELETE = 4;

    private int treeIndex;

    private String detValue;

    private String detHash;

    private int typeOp;

    public mOPEClientCipher(int treeIndex, String detValue, String detHash, mOPEOperationType typeOp) {
        this.treeIndex = treeIndex;
        this.detValue = detValue;
        this.detHash = detHash;
        this.typeOp = decideOp(typeOp);
    }

    private int decideOp(mOPEOperationType typeOp) {
        switch (typeOp) {
            case INSERT:
                return OP_TYPE_INSERT;
            case UPDATE:
                return OP_TYPE_UPDATE;
            case DELETE:
                return OP_TYPE_DELETE;
            case QUERY:
                return OP_TYPE_QUERY;
        }
        throw new IllegalArgumentException("Not valid");
    }

    @Override
    public void setCipher(String rep) {
        //not neeses
    }

    @Override
    public String getStringRep() {
        JSONObject jo = new JSONObject();
        try {
            jo.put(mOPEMessagesUtils.mOPE_TREEINDEX, this.treeIndex);
            jo.put(mOPEMessagesUtils.mOPE_DETVALUE, this.detValue);
            jo.put(mOPEMessagesUtils.mOPE_DETHASH, this.detHash);
            jo.put(mOPEMessagesUtils.mOPE_OPTYPE, this.typeOp);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jo.toString();
    }

    public int getTreeIndex() {
        return treeIndex;
    }

    public String getDetValue() {
        return detValue;
    }

    public String getDetHash() {
        return detHash;
    }

    public int getTypeOp() {
        return typeOp;
    }
}
