package ch.ethz.inf.vs.talosmodule.communication.messages;

import android.util.Base64;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import ch.ethz.inf.vs.talosmodule.main.SharedUser;
import ch.ethz.inf.vs.talosmodule.main.TalosCipherResultData;
import ch.ethz.inf.vs.talosmodule.main.TalosCipherResultRow;

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

public class MessagesUtils {


    public static final String JSON_ID_TOKEN_KEY = "idtoken";
    public static final String JSON_COMMAND_KEY = "cmd";
    public static final String JSON_ARGS_KEY = "args";
    public static final String JSON_COMMADN_HEAD= "command";

    public static final String JSON_USERS_KEY = "users";
    public static final String JSON_CLIENT_SHARE_KEY = "client_share_id";
    public static final String JSON_SHARE_TOKEN_KEY = "client_share_token";
    public static final String JSON_PK_KEY = "pk";

    public static final String JSON_USER_ID = "id";
    public static final String JSON_USER_MAIL= "mail";

    public static final String RESULT_KEY = "result";
    public static final String DATA_KEY = "data";
    public static final String MESSAGE_KEY = "msg";

    public static final String SUCESS_IDENTIFIER = "success";
    public static final String ERROR_IDENTIFIER = "Error";

    public static ResponseMessage getResponseMessage(String msg) throws JSONException {
        JSONObject jo  = new JSONObject(msg);
        String result = jo.getString(RESULT_KEY);

        if(result.equals(MessagesUtils.ERROR_IDENTIFIER)) {
            return new ErrorResponseMessage(MessagesUtils.ERROR_IDENTIFIER, jo.getString(MESSAGE_KEY));
        } else if(result.equals(MessagesUtils.SUCESS_IDENTIFIER)) {
            if(jo.has(DATA_KEY)) {
                String data = jo.getString(DATA_KEY);
                Gson gson = new Gson();
                return new QueryResponseMessage(new TalosCipherResultData(gson.fromJson(data, TalosCipherResultRow[].class)));
            } else if(jo.has(JSON_USERS_KEY)) {
                JSONArray users = jo.getJSONArray(JSON_USERS_KEY);
                return new UsersRepsonseMessage(extractUsers(users));
            }
            return new SuccessResponseMessage();
        }

        throw new JSONException("Wrong message fromat");
    }

    private static List<SharedUser> extractUsers(JSONArray user) throws JSONException {
        ArrayList<SharedUser> users = new ArrayList<>();
        for(int iter=0; iter<user.length(); iter++) {
            JSONObject jo = user.getJSONObject(iter);
            byte[] pk = Base64.decode(jo.getString(JSON_PK_KEY), Base64.NO_WRAP);
            users.add(new SharedUser(jo.getInt(JSON_USER_ID), jo.getString(JSON_USER_MAIL), pk));
        }
        return users;
    }

}
