package resources;

import crypto.PRERelic;
import database.DBAccessAuthenticate;
import database.ProxyReencryptor;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;
import util.ErrorResponses;
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
 * Implements the REST resource for creating a share relation
 */
public class ClientShareResource extends ServerResource {

    @Override
    protected Representation post(Representation entity) throws ResourceException {
        JSONObject content;
        User user;
        try {
            content = (JSONObject) this.getContext().getAttributes().get(SystemUtil.SYSTEM_CONTENT_KEY);
            user = (User) this.getContext().getAttributes().get(SystemUtil.SYSTEM_USER_KEY);
            //the id the user wants to share to
            int share_id = MessageUtil.getSharedUserID(content);
            //the provided re-encryption token
            byte[] token = MessageUtil.getReEncToken(content);

            //add share relation to db
            User sharedUser = DBAccessAuthenticate.addAccess(user, share_id, token);
            //post re-encrypt job (re-encrypt existing data)
            ProxyReencryptor reEncryptor = ProxyReencryptor.getReencryptor();
            reEncryptor.postReEncTask(user.getLocalid(), sharedUser.getLocalid(), new PRERelic.PREToken(token));
        } catch (JSONException e) {
            return MessageUtil.getErrorResponse(ErrorResponses.ERROR_WRONG_FORMAT);
        } catch (Exception e) {
            e.printStackTrace();
            return MessageUtil.getErrorResponse(ErrorResponses.ERROR_OCCURED);
        }
        return MessageUtil.getSuccessMsg("SUCCESS");
    }

}
