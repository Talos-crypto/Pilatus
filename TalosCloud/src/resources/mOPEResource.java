package resources;

import com.google.common.cache.*;
import database.CommandDescription;
import database.DBAccessAuthenticate;
import mope.mOPEException;
import mope.mOPEInteractionState;
import mope.mOPEJob;
import mope.messages.mOPEClientAnswer;
import mope.messages.mOPEClientCipher;
import mope.messages.mOPEMsgFactory;
import mope.messages.mOPEResponseMessage;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
 * REST resource for executing a talos command containing mOPE operations.
 */
public class mOPEResource extends ServerResource {

    private static final int STATE_LIVENESS_SECONDS = 1000;

    private static Cache<String, mOPEInteractionState> stateStorage = CacheBuilder.newBuilder()
            .removalListener(new StateRemovalListener())
            .expireAfterWrite(STATE_LIVENESS_SECONDS, TimeUnit.SECONDS)
            .build();

    private static class StateRemovalListener implements RemovalListener<String,mOPEInteractionState> {
        @Override
        public void onRemoval(RemovalNotification<String, mOPEInteractionState> removalNotification) {
            if(removalNotification.wasEvicted()) {
                mOPEInteractionState state = removalNotification.getValue();
                if(state!=null)
                    state.rollback();
            }
        }
    }

    @Override
    protected Representation post(Representation entity) throws ResourceException {
        JSONObject content;
        User user;

        try {
            content = (JSONObject) this.getContext().getAttributes().get(SystemUtil.SYSTEM_CONTENT_KEY);
            user = (User) this.getContext().getAttributes().get(SystemUtil.SYSTEM_USER_KEY);
            assert(content!=null && user!=null);
            if(MessageUtil.containsCommand(content)) {
                return handleCommand(content, user);
            } else {
                return handleClientInteraction(content, user);
            }
        } catch (JSONException e) {
            return handleError(ErrorResponses.ERROR_WRONG_FORMAT);
        } catch (Exception e) {
            e.printStackTrace();
            return handleError(ErrorResponses.ERROR_OCCURED);
        }

    }

    private Representation handleCommand(JSONObject content, User user) {
        String command;
        JSONArray arguments;
        mOPEResponseMessage response = null;

        try {
            JSONObject commandObj = MessageUtil.getCommandObj(content);
            command = MessageUtil.getCommand(commandObj);
            arguments = MessageUtil.getArguments(commandObj);
        } catch (JSONException e) {
            return handleError(ErrorResponses.ERROR_WRONG_FORMAT);
        } catch (Exception e) {
            e.printStackTrace();
            return handleError(ErrorResponses.ERROR_OCCURED);
        }

        //Check command valid
        CommandDescription desc = DBAccessAuthenticate.getCommandDescription(command);
        if(desc==null) {
            return handleError(ErrorResponses.ERROR_QUERY_WRONG);
        }

        List<mOPEJob> jobs = new ArrayList<>();
        String[] strArguments = new String[arguments.length()];

        try {
            for (int index = 0; index < arguments.length(); index++) {
                String curArg = arguments.getString(index);
                if(checkIsmOPECipher(curArg)) {
                    JSONObject jo = new JSONObject(curArg);
                    mOPEClientCipher ciph = MessageUtil.getCipher(jo);
                    jobs.add(new mOPEJob(ciph.getTypeOp(), ciph.getDetValue(), ciph.getDetHash(), index, ciph.getTreeIndex()));
                }
                strArguments[index] = curArg;
            }

            if(jobs.isEmpty()) {
                return handleError(ErrorResponses.NON_mOPE_QUERY);
            }

            mOPEInteractionState state = new mOPEInteractionState(user, strArguments, desc, jobs);
            stateStorage.put(key(state.getSessionID(),user), state);
            response = state.getFirstStep();

        } catch (JSONException e) {
            return handleError(ErrorResponses.ERROR_WRONG_FORMAT);
        } catch (mOPEException e) {
            return handleError(ErrorResponses.mOPE_INTERACTION_ERROR + " " + e.getMessage());
        }

        return new StringRepresentation(response.toStringMessage(), MediaType.APPLICATION_ALL_JSON);
    }

    private static String key(int sessionID, User u) {
        return u.getLocalid()+"//"+ sessionID;
    }

    private boolean checkIsmOPECipher(String s) {
        if(s.isEmpty())
            return false;
        return s.charAt(0) == '{';
    }


    private Representation handleClientInteraction(JSONObject content, User user) {
        mOPEClientAnswer answer = null;
        mOPEInteractionState state = null;
        mOPEResponseMessage msg = null;

        try {
            answer = MessageUtil.getAnswer(content);
            state = stateStorage.getIfPresent(key(answer.getSessionID(), user));

            if(state==null) {
                return handleError(ErrorResponses.mOPE_NO_SESSION_ERROR);
            }

            msg = state.handleClientStep(answer.getIndex(), answer.isEq());

            if(msg.isResultMessage()) {
                stateStorage.invalidate(key(answer.getSessionID(), user));
            }

        } catch (JSONException e) {
            return handleError(ErrorResponses.ERROR_WRONG_FORMAT);
        } catch (mOPEException e) {
            return handleError(ErrorResponses.mOPE_INTERACTION_ERROR + " " + e.getMessage());
        }

        return new StringRepresentation(msg.toStringMessage(), MediaType.APPLICATION_ALL_JSON);
    }

    private Representation handleError(String msg) {
        return new StringRepresentation(mOPEMsgFactory.createResultMsg(MessageUtil.getErrorResponseStr(msg)).toStringMessage()
                , MediaType.APPLICATION_ALL_JSON);
    }

}
