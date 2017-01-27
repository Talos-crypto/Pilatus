package auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import util.SystemUtil;
import util.User;

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
 * Implements the token verfier for
 * Google Sign-in
 * https://developers.google.com/identity/sign-in/android/backend-auth
 */
public class ServerGoogleAuth implements ITokenVerifier {

    private static ServerGoogleAuth auth = null;

    private final JsonFactory jsonFactory;
    private final GoogleIdTokenVerifier verifier;

    private ServerGoogleAuth() {
        NetHttpTransport transport = new NetHttpTransport();
        jsonFactory = JacksonFactory.getDefaultInstance();
        verifier = new GoogleIdTokenVerifier.Builder(transport, jsonFactory)
                .setAudience(Arrays.asList(SystemUtil.GOOGLE_CLIENT_ID))
                .setIssuer(SystemUtil.IDP_ADDR)
                .build();
    }

    public static ServerGoogleAuth getInstacne() {
        if(auth==null) {
            auth = new ServerGoogleAuth();
        }
        return auth;
    }

    public User checkIdToken(String token) throws Exception {
        GoogleIdToken.Payload payload;
        User res;

        GoogleIdToken idToken = verifier.verify(token);
        if (idToken != null) {
            payload = idToken.getPayload();
        } else {
            throw new RuntimeException("Invalid idtoken");
        }

        res = new User(payload.getSubject(), payload.getEmail());

        return res;
    }



}
