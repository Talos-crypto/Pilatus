package ch.ethz.inf.vs.talosfitbitapp.fitbitapi;

import android.net.Uri;

import com.google.gson.Gson;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.HashMap;

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

public class TokenInfo implements Serializable {

    private Timestamp stamp;

    private String access_token;
    private int expires_in;
    private String scope;
    private String token_type;
    private String user_id;

    private TokenInfo(String access_token, int expires_in, String scope, String token_type, String user_id) {
        this.access_token = access_token;
        this.expires_in = expires_in;
        this.scope = scope;
        this.token_type = token_type;
        this.user_id = user_id;
        this.stamp = new Timestamp(System.currentTimeMillis());
    }

    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public static TokenInfo fromJSON(String in) {
        Gson gson = new Gson();
        return gson.fromJson(in, TokenInfo.class);
    }

    public static TokenInfo fromURI(Uri in) throws Exception {
        String frag = in.getFragment();
        String[] splits = frag.split("&");
        HashMap<String, String> map = new HashMap<>();
        for(String temp : splits) {
            String[] mapping = temp.split("=");
            if(mapping.length!=2)
                throw new IllegalArgumentException("Wrong Format");
            map.put(mapping[0], mapping[1]);
        }

        return new TokenInfo(map.get("access_token"),
                    Integer.valueOf(map.get("expires_in")),
                    map.get("scope"),
                    map.get("token_type"),
                map.get("user_id"));
    }

    public String getAccess_token() {
        return access_token;
    }

    public int getExpires_in() {
        return expires_in;
    }

    public String getScope() {
        return scope;
    }

    public String getToken_type() {
        return token_type;
    }

    public String getUser_id() {
        return user_id;
    }

    public boolean isValid() {
        return System.currentTimeMillis() < stamp.getTime() + (expires_in * 1000);
    }


}
