package ch.ethz.inf.vs.talosmodule.communication.mopemessages;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ch.ethz.inf.vs.talosmodule.communication.messages.MessageFactory;
import ch.ethz.inf.vs.talosmodule.exceptions.CommTalosModuleException;

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
 * Created by lukas on 03.01.16.
 */
public class mOPEMessagesUtils {

    //mOPE
    public static final String mOPE_TREEINDEX = "treeIndex";
    public static final String mOPE_DETVALUE = "detValue";
    public static final String mOPE_DETHASH = "detHash";
    public static final String mOPE_OPTYPE = "typeOp";
    public static final String mOPE_COMPARETO = "compareTo";
    public static final String mOPE_SESSIONID = "sessionID";
    public static final String mOPE_TYPE = "msgID";
    public static final String mOPE_INDEX = "index";
    public static final String mOPE_ISEQUAL = "isEq";
    public static final String mOPE_RESULT = "res";


    static mOPEResponseMessage getmOPEMessage(String msg) throws JSONException, CommTalosModuleException {
        JSONObject jo  = new JSONObject(msg);
        int index = jo.getInt(mOPE_TYPE);

        if (index == mOPEResponseMessage.CLIENTSTEP) {
            JSONArray arr = jo.getJSONArray(mOPE_COMPARETO);
            String[] compareTo = new String[arr.length()];
            for(int ind=0; ind<arr.length(); ind++) {
                compareTo[ind] = arr.getString(ind);
            }
            return new mOPEClientStepMessage(jo.getString(mOPE_DETVALUE),
                    jo.getInt(mOPE_SESSIONID),
                    jo.getString(mOPE_DETHASH),
                    compareTo);
        } else if(index == mOPEResponseMessage.RESULT_RESPONSE) {
            String res = jo.getString(mOPE_RESULT);
            return new mOPEResultMessage(MessageFactory.getResponseMessage(res));
        }
        throw new CommTalosModuleException("Illegal message " + msg);
    }

}
