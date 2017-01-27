package resources;

import database.CommandDescription;
import database.DBAccessAPI;
import database.DBAccessAuthenticate;
import database.ProxyReencryptor;
import org.json.JSONArray;
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
 * The REST resource for executing batch insert commands
 */
public class BatchCommandResource extends ServerResource {

    @Override
    protected Representation post(Representation entity) throws ResourceException {
        String command;
        JSONObject content;
        JSONArray argObects;
        JSONArray curArgs;
        User user;
        boolean isSharedCmd ;

        //body = this.getRequest().getEntityAsText();
        try {
            content = (JSONObject) this.getContext().getAttributes().get(SystemUtil.SYSTEM_CONTENT_KEY);
            user = (User) this.getContext().getAttributes().get(SystemUtil.SYSTEM_USER_KEY);
            assert(content!=null && user!=null);
            JSONObject commandObj = MessageUtil.getCommandObj(content);
            command = MessageUtil.getCommand(commandObj);
            argObects = MessageUtil.getArguments(commandObj);
            isSharedCmd = MessageUtil.hasSharedUserID(content);
        } catch (JSONException e) {
            return MessageUtil.getErrorResponse(ErrorResponses.ERROR_WRONG_FORMAT);
        } catch (Exception e) {
            e.printStackTrace();
            return MessageUtil.getErrorResponse(ErrorResponses.ERROR_OCCURED);
        }

        //Check command valid
        CommandDescription desc = DBAccessAuthenticate.getCommandDescription(command);
        if(desc==null) {
            return MessageUtil.getErrorResponse(ErrorResponses.ERROR_QUERY_WRONG);
        }

        //Batch queries are not allowed
        if(desc.isQuery()) {
            return MessageUtil.getErrorResponse(ErrorResponses.ERROR_BATCH);
        }

        //Shared commands are not allowed
        if(isSharedCmd) {
            return MessageUtil.getErrorResponse(ErrorResponses.ERROR_QUERY_WRONG);
        }

        //get the user which we have to re-encrypt the data to (can be empty if no share relations exist)
        List<User> users = DBAccessAuthenticate.getCombinedUsers(user);
        ProxyReencryptor reenc = ProxyReencryptor.getReencryptor();
        for(int iter = 0; iter<argObects.length(); iter ++) {
            try {
                curArgs = argObects.getJSONArray(iter);
            } catch (JSONException e) {
                return MessageUtil.getErrorResponse(ErrorResponses.ERROR_WRONG_FORMAT);
            }
            String[] args = null;

            //Execute Command
            try {
                args = SystemUtil.jsonArrayToStringArray(curArgs);
                DBAccessAPI.excecuteCommand(user, desc, args);
            } catch (JSONException e) {
                return MessageUtil.getErrorResponse(ErrorResponses.ERROR_QUERY_WRONG);
            }

            // perform re-enc, if share relations exist
            if (users != null && !users.isEmpty()) {
                reenc.postReEncTaskAdd(users, desc, args);
            }
        }


        return new StringRepresentation("success", MediaType.APPLICATION_ALL_JSON);
    }

}
