package auth;

import database.DBAccessAuthenticate;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Status;
import org.restlet.security.Authenticator;
import util.SystemUtil;
import util.User;

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
 * Implements the restlet authenticator
 * for authenticating users with Google Sign-In tokens
 */
public class TokenIDAuthenticator extends Authenticator {

    public TokenIDAuthenticator(Context context) {
        super(context);
    }

    @Override
    protected boolean authenticate(Request request, Response response) {
        String body = null;
        JSONObject msg = null;
        String idtoken = null;
        User user;
        ServerGoogleAuth auth = ServerGoogleAuth.getInstacne();


        // is it a post?
        if(!request.getMethod().getName().equals("POST")) {
            response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            return false;
        }

        body = request.getEntityAsText();

        if(body==null) {
            response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            return false;
        }

        try {
            msg = new JSONObject(body);
        } catch (JSONException e) {
            response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            return false;
        }

        //get the google id token from the message
        idtoken = SystemUtil.getIDToken(msg);

        if(idtoken==null) {
            response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            return false;
        }

        try {
            //checks if the token is valid
            user = auth.checkIdToken(idtoken);
        } catch (Exception e) {
            response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            return false;
        }

        //Checks if the User is registered and valid
        boolean valid = DBAccessAuthenticate.checkUserValid(user);

        if(!valid) {
            response.setStatus(Status.CLIENT_ERROR_UNAUTHORIZED);
        }

        this.getContext().getAttributes().put(SystemUtil.SYSTEM_USER_KEY, user);
        this.getContext().getAttributes().put(SystemUtil.SYSTEM_CONTENT_KEY, msg);

        return valid;
    }

}
