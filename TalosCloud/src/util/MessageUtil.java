package util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import database.ResultSetSerializer;
import mope.messages.mOPEClientAnswer;
import mope.messages.mOPEClientCipher;
import mope.messages.mOPEResponseMessage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.MediaType;
import org.restlet.engine.util.Base64;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;

import java.io.IOException;
import java.io.StringWriter;
import java.sql.ResultSet;
import java.util.List;

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
 * Holds protocol relevant information.
 */
public class MessageUtil {
    public static final String JSON_ID_TOKEN_KEY = "idtoken";
    public static final String JSON_COMMAND_OBJ_KEY = "command";
    public static final String JSON_COMMAND_KEY = "cmd";
    public static final String JSON_ARGS_KEY = "args";

    public static final String JSON_CLIENT_SHARE_KEY = "client_share_id";
    public static final String JSON_SHARE_TOKEN_KEY = "client_share_token";
    public static final String JSON_PK_KEY = "pk";

    public static final String JSON_USERS_KEY = "users";
    public static final String JSON_USER_ID = "id";
    public static final String JSON_USER_MAIL= "mail";

    public static final String RESULT_KEY = "result";
    public static final String DATA_KEY = "data";
    public static final String MESSAGE_KEY = "msg";

    public static final String SUCCESS_MSG_TAG = "success";
    public static final String ERROR_MSG_TAG = "Error";

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



    public static boolean hasPK(JSONObject jo) throws JSONException {
        return jo.has(JSON_PK_KEY);
    }

    public static byte[] getPK(JSONObject jo) throws JSONException {
        try {
            return Base64.decode(jo.getString(JSON_PK_KEY));
        } catch (JSONException e) {
            return null;
        }
    }

    public static byte[] getReEncToken(JSONObject jo) throws JSONException {
        try {
            return Base64.decode(jo.getString(JSON_SHARE_TOKEN_KEY));
        } catch (JSONException e) {
            return null;
        }
    }


    public static String getIDToken(JSONObject jo) throws JSONException {
        try {
            return jo.getString(JSON_ID_TOKEN_KEY);
        } catch (JSONException e) {
            return null;
        }
    }



    public static int getSharedUserID(JSONObject jo) throws JSONException {
        return jo.getInt(JSON_CLIENT_SHARE_KEY);
    }

    public static boolean hasSharedUserID(JSONObject jo) throws JSONException {
        return jo.has(JSON_CLIENT_SHARE_KEY);
    }

    public static JSONArray getArguments(JSONObject jo) throws JSONException {
        return jo.getJSONArray(JSON_ARGS_KEY);
    }

    public static String getCommand(JSONObject jo) throws JSONException {
        return jo.getString(JSON_COMMAND_KEY);
    }

    public static boolean isCommand(JSONObject jo) throws JSONException {
        return jo.has(JSON_COMMAND_KEY);
    }

    public static JSONObject getCommandObj(JSONObject jo) throws JSONException {
        return jo.getJSONObject(JSON_COMMAND_OBJ_KEY);
    }

    public static boolean containsCommand(JSONObject jo) throws JSONException {
        return jo.has(JSON_COMMAND_OBJ_KEY);
    }

    public static String getReponseFromUserQuery(List<User> users) throws JSONException {
        JSONArray jo = new JSONArray();
        JSONObject res = new JSONObject();
        for(User user:users) {
            JSONObject temp = new JSONObject();
            temp.put(JSON_USER_ID, user.getLocalid());
            temp.put(JSON_USER_MAIL, user.getMail());
            temp.put(JSON_PK_KEY, Base64.encode(user.getPk(), false));
            jo.put(temp);
        }
        res.put(RESULT_KEY, SUCCESS_MSG_TAG);
        res.put(JSON_USERS_KEY, jo);
        return res.toString();
    }

    public static String getRepsonseMessageFromDBResult(ResultSet set) throws IOException {
        StringWriter sw = new StringWriter();
        SimpleModule module = new SimpleModule();
        module.addSerializer(new ResultSetSerializer());

        ObjectMapper om = new ObjectMapper();
        om.registerModule(module);

        ObjectNode objectNode = om.createObjectNode();
        objectNode.putPOJO(DATA_KEY, set);
        objectNode.put(RESULT_KEY,SUCCESS_MSG_TAG);

        om.writeValue(sw, objectNode);

        return sw.toString();
    }

    public static Representation getSuccessMsg(String msg) {
        JSONObject resp = new JSONObject();
        try {
            resp.put(RESULT_KEY, SUCCESS_MSG_TAG);
            resp.put(MESSAGE_KEY, msg);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return new StringRepresentation(resp.toString(), MediaType.APPLICATION_ALL_JSON);
    }

    public static String getRepsonseMessageFromUpdtaeDBResult() {
        JSONObject jo = new JSONObject();
        try {
            jo.put(RESULT_KEY,SUCCESS_MSG_TAG);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jo.toString();
    }

    public static Representation getErrorResponse(String msg) {
        return new StringRepresentation(getErrorResponseStr(msg).toString(), MediaType.APPLICATION_ALL_JSON);
    }

    public static String getErrorResponseStr(String msg) {
        JSONObject resp = new JSONObject();
        try {
            resp.put(RESULT_KEY, ERROR_MSG_TAG);
            resp.put(MESSAGE_KEY, msg);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return resp.toString();
    }

    //mOPE
    public static mOPEClientCipher getCipher(JSONObject cipher) throws JSONException {
        return new mOPEClientCipher(cipher.getInt(mOPE_TREEINDEX),
                cipher.getString(mOPE_DETVALUE),
                cipher.getString(mOPE_DETHASH),
                cipher.getInt(mOPE_OPTYPE));
    }

    public static mOPEClientAnswer getAnswer(JSONObject answer) throws JSONException {
        return new mOPEClientAnswer(answer.getInt(mOPE_INDEX),
                answer.getBoolean(mOPE_ISEQUAL),
                answer.getInt(mOPE_SESSIONID));
    }


}
