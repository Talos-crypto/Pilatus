package ch.ethz.inf.vs.talosmodule.communication.mopemessages;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

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
 * Created by lukas on 31.12.15.
 */
public class mOPEClientStepMessage extends mOPEResponseMessage {

    private String value;

    private int sessionID;

    private String detHash;

    private String[] valuesToCompare;

    public mOPEClientStepMessage(String value, int sessionID, String detHash, String[] valuesToCompare) {
        super(mOPEResponseMessage.CLIENTSTEP);
        this.value = value;
        this.sessionID = sessionID;
        this.detHash = detHash;
        this.valuesToCompare = valuesToCompare;
    }

    public String getValue() {
        return value;
    }

    public String[] getValuesToCompare() {
        return Arrays.copyOf(valuesToCompare,valuesToCompare.length);
    }

    public int getSessionID() {
        return sessionID;
    }

    public String getDetHash() {
        return detHash;
    }

    @Override
    public String toStringMessage() {
        JSONObject jo = new JSONObject();
        JSONArray arr = new JSONArray();
        try {
            jo.put(mOPEMessagesUtils.mOPE_TYPE, this.messageID);
            jo.put(mOPEMessagesUtils.mOPE_DETVALUE, value);
            for(String cur : valuesToCompare) {
                arr.put(cur);
            }
            jo.put(mOPEMessagesUtils.mOPE_COMPARETO, arr);
            jo.put(mOPEMessagesUtils.mOPE_SESSIONID, sessionID);
            jo.put(mOPEMessagesUtils.mOPE_DETHASH, detHash);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jo.toString();
    }
}
