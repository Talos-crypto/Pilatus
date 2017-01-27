package resources;

import auth.ITokenVerifier;
import auth.TokenVerifierFactory;
import database.DBAccessAuthenticate;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.representation.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;
import util.MessageUtil;
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
 * Implements the REST resource for registering a user.
 * The user is added to the databases and can execute talos commands now.
 */
public class RegisterResource extends ServerResource {

    @Override
    protected Representation post(Representation entity) throws ResourceException {
        String body = null;
        JSONObject msg = null;
        String idtoken = null;
        User user;
        ITokenVerifier auth = TokenVerifierFactory.getVerifier();

        body = this.getRequest().getEntityAsText();

        if(body==null) {
            return MessageUtil.getErrorResponse("No Body");
        }

        try {
            msg = new JSONObject(body);
        } catch (JSONException e) {
            return MessageUtil.getErrorResponse("Non JSON message received");
        }

        //retrieve google sign in token
        idtoken = SystemUtil.getIDToken(msg);

        if(idtoken==null) {
            return MessageUtil.getErrorResponse("IDToken field in JSON message missing");
        }

        try {
            // is token valid?
            user = auth.checkIdToken(idtoken);
        } catch (Exception e) {
            return MessageUtil.getErrorResponse("Google TokenID verification failed: " + e.getMessage());
        }

        try {
            // does the user provide a pk?
            if(MessageUtil.hasPK(msg)) {
                user.setPk(MessageUtil.getPK(msg));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // register user and add the user to the Users table in the db
        if(!DBAccessAuthenticate.registerUser(user)) {
            return MessageUtil.getSuccessMsg("Client already registered");
        }


        return MessageUtil.getSuccessMsg("User "+user.getMail()+ " with ID "+user.getUserid()+ " registered");
    }
}
