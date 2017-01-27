package util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

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
 * Manages the golbal settings of the appliaction
 * By providing a talosexample.properties file the settings can be customized.
 * (In glassfish4 put the file in the /glassfish4/glassfish/domains/domain1/config directory)
 */
public class SystemUtil {

    public static final String COMMAND_RESOURCE = "/executeCmd";
    public static final String BATCH_COMMAND_RESOURCE = "/executeBatchCmd";
    public static final String COMMAND_OPE_RESOURCE = "/executeCmdOPE";
    public static final String REGISTER_RESOURCE = "/register";
    public static final String SHARE_ACCESS = "/myaccess";
    public static final String SHARE_TO_CLIENTS = "/myshares";
    public static final String SHARE_ADD_ACCESS = "/addaccess";
    public static final String SHARE_SHARABLE_USER = "/shareusers";

    public static final String SYSTEM_USER_KEY = "user";
    public static final String SYSTEM_CONTENT_KEY = "body";

    public static String GOOGLE_CLIENT_ID = "894966550665-3ip6bu7lcca5a9eh59qodelsuh89335g.apps.googleusercontent.com";
    public static String IDP_ADDR = "https://accounts.google.com";

    public static final String JSON_ID_TOKEN_KEY = "idtoken";
    public static String APPLICATION_SERVER_DBPOOL_RES = "talosPoolRes";

    public static final boolean SECURITY_OFF_DEBUG = false;
    public static boolean DEBUG_AUTH = false;

    public static final String CONFIG_FILE ="talosexample.properties";

    static {
        File config = new File(CONFIG_FILE);
        if(config.exists()) {
            BufferedInputStream stream = null;

            try {
                Properties properties = new Properties();
                stream = new BufferedInputStream(new FileInputStream(config));
                properties.load(stream);

                GOOGLE_CLIENT_ID = properties.getProperty("googleAuthServerID", "");
                IDP_ADDR = properties.getProperty("identityProviderAddress", IDP_ADDR);
                DEBUG_AUTH = Boolean.valueOf(properties.getProperty("debugAuthentication", "false"));
                APPLICATION_SERVER_DBPOOL_RES = properties.getProperty("dbConnPoolName", APPLICATION_SERVER_DBPOOL_RES);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if(stream!=null)
                    try {
                        stream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
            }
        }
    }


    public static String getIDToken(JSONObject jo) {
        try {
            return jo.getString(JSON_ID_TOKEN_KEY);
        } catch (JSONException e) {
            return null;
        }
    }

    public static String[] jsonArrayToStringArray(JSONArray arr) throws JSONException {
        String[] res = new String[arr.length()];
        for(int i=0; i<res.length; i++) {
            res[i] = arr.getString(i);
        }
        return res;
    }

}
