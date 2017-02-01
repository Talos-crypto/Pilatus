package ch.ethz.inf.vs.talosmodule.communication;

import java.util.List;

import ch.ethz.inf.vs.talosmodule.R;
import ch.ethz.inf.vs.talosmodule.communication.messages.ErrorResponseMessage;
import ch.ethz.inf.vs.talosmodule.communication.messages.MessageFactory;
import ch.ethz.inf.vs.talosmodule.communication.messages.QueryResponseMessage;
import ch.ethz.inf.vs.talosmodule.communication.messages.RequestMessage;
import ch.ethz.inf.vs.talosmodule.communication.messages.ResponseMessage;
import ch.ethz.inf.vs.talosmodule.communication.messages.UsersRepsonseMessage;
import ch.ethz.inf.vs.talosmodule.communication.mopemessages.mOPEResultMessage;
import ch.ethz.inf.vs.talosmodule.crypto.KeyManager;
import ch.ethz.inf.vs.talosmodule.exceptions.CommTalosModuleException;
import ch.ethz.inf.vs.talosmodule.exceptions.TalosModuleException;
import ch.ethz.inf.vs.talosmodule.main.SharedUser;
import ch.ethz.inf.vs.talosmodule.main.TalosBatchCommand;
import ch.ethz.inf.vs.talosmodule.main.TalosCipherResultData;
import ch.ethz.inf.vs.talosmodule.main.TalosCommand;
import ch.ethz.inf.vs.talosmodule.main.User;
import ch.ethz.inf.vs.talosmodule.main.values.TalosCipher;
import ch.ethz.inf.vs.talosmodule.main.values.mOPEClientCipher;

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

public class TalosServerCommunicator implements TalosServer {

    private final String COMMAND_RESOURCE;
    private final String BATCH_COMMAND_RESOURCE;
    private final String REGISTER_RESOURCE;
    private final String OPE_COMMAND_RESOURCE;
    private final String SHARE_ACCESS_RESOURCE;
    private final String SHARE_TO_CLIENTS_RESOURCE;
    private final String SHARE_ADD_ACCESS_RESOURCE;
    private final String SHARE_SHARABLE_USER_RESOURCE;

    private CommunicationHelper comm;
    private mOPEInteractor opeInteractor;
    private KeyManager manager;

    public TalosServerCommunicator(String webserviceAppliactionPrefix, CommunicationHelper comm, KeyManager manager) {
        this.COMMAND_RESOURCE = "/"+webserviceAppliactionPrefix +"/executeCmd";
        this.BATCH_COMMAND_RESOURCE = "/"+webserviceAppliactionPrefix +"/executeBatchCmd";
        this.REGISTER_RESOURCE = "/"+ webserviceAppliactionPrefix +"/register";
        this.OPE_COMMAND_RESOURCE = "/"+webserviceAppliactionPrefix +"/executeCmdOPE";
        this.SHARE_ACCESS_RESOURCE = "/"+webserviceAppliactionPrefix +"/myaccess";
        this.SHARE_TO_CLIENTS_RESOURCE  = "/"+webserviceAppliactionPrefix +"/myshares";
        this.SHARE_ADD_ACCESS_RESOURCE  = "/"+webserviceAppliactionPrefix +"/addaccess";
        this.SHARE_SHARABLE_USER_RESOURCE  = "/"+webserviceAppliactionPrefix +"/shareusers";
        this.comm = comm;
        this.opeInteractor = new mOPEInteractor(comm, manager, this.OPE_COMMAND_RESOURCE);
        this.manager = manager;
    }

    @Override
    public TalosCipherResultData execute(User user, TalosCommand cmd) throws TalosModuleException {
        ResponseMessage responseMessage = null;
        if(checkIsmOPECmd(cmd)) {
            responseMessage = handleOPECmd(user, cmd);
        } else {
            RequestMessage msg = MessageFactory.getCommandRequestMessage(user, cmd);
            String response = comm.sendMessage(msg.toJSON(), COMMAND_RESOURCE);
            responseMessage = MessageFactory.getResponseMessage(response);
        }

        checkError(responseMessage);

        if(responseMessage instanceof QueryResponseMessage) {
            return ((QueryResponseMessage)responseMessage).getData();
        }

        return new TalosCipherResultData();
    }

    @Override
    public TalosCipherResultData execute(User user, TalosBatchCommand cmd) throws TalosModuleException {
        ResponseMessage responseMessage = null;
        if(checkIsmOPECmdBatch(cmd)) {
            throw new TalosModuleException("Batch command with mOPE is not allowed");
        } else {
            RequestMessage msg = MessageFactory.getCommandRequestMessage(user, cmd);
            String response = comm.sendMessage(msg.toJSON(), BATCH_COMMAND_RESOURCE);
            responseMessage = MessageFactory.getResponseMessage(response);
        }
        checkError(responseMessage);
        return new TalosCipherResultData();
    }

    @Override
    public TalosCipherResultData execute(User user, TalosCommand cmd, SharedUser shareUser) throws TalosModuleException {
        ResponseMessage responseMessage = null;
        if(checkIsmOPECmd(cmd)) {
            responseMessage = handleOPECmd(user, cmd);
        } else {
            RequestMessage msg = MessageFactory.getShareCommandRequestMesssage(user, cmd, shareUser);
            String response = comm.sendMessage(msg.toJSON(), COMMAND_RESOURCE);
            responseMessage = MessageFactory.getResponseMessage(response);
        }

        checkError(responseMessage);

        if(responseMessage instanceof QueryResponseMessage) {
            return ((QueryResponseMessage)responseMessage).getData();
        }

        return new TalosCipherResultData();
    }

    @Override
    public boolean register(User user) throws CommTalosModuleException {
        RequestMessage msg = MessageFactory.getEmptyRequestMessage(user);
        String response = comm.sendMessage(msg.toJSON(), REGISTER_RESOURCE);
        ResponseMessage responseMessage = MessageFactory.getResponseMessage(response);
        checkError(responseMessage);
        return true;

    }

    @Override
    public boolean registerShared(User user) throws TalosModuleException {
        RequestMessage msg = MessageFactory.getSharedRegisterRequest(user, manager.getPREKey().getPublicKey().getEncoded());
        String response = comm.sendMessage(msg.toJSON(), REGISTER_RESOURCE);
        ResponseMessage responseMessage = MessageFactory.getResponseMessage(response);
        checkError(responseMessage);
        return true;
    }

    private List<SharedUser> runUserQuery(User u, String resource) throws TalosModuleException {
        RequestMessage msg = MessageFactory.getEmptyRequestMessage(u);
        String response = comm.sendMessage(msg.toJSON(), resource);
        ResponseMessage responseMessage = MessageFactory.getResponseMessage(response);
        checkError(responseMessage);

        if(responseMessage instanceof UsersRepsonseMessage) {
            UsersRepsonseMessage userResponse = (UsersRepsonseMessage) responseMessage;
            return userResponse.getUsers();
        } else {
            throw new TalosModuleException("Wrong Message received: " + response);
        }
    }

    @Override
    public List<SharedUser> getSharableUsers(User user) throws TalosModuleException {
        return runUserQuery(user, SHARE_SHARABLE_USER_RESOURCE);
    }

    @Override
    public List<SharedUser> getMySharedUsers(User user) throws TalosModuleException {
        return runUserQuery(user, SHARE_TO_CLIENTS_RESOURCE);
    }

    @Override
    public List<SharedUser> getMyAccessableUsers(User user) throws TalosModuleException {
        return runUserQuery(user, SHARE_ACCESS_RESOURCE);
    }

    @Override
    public boolean shareMyData(User user, SharedUser shareUser) throws TalosModuleException {
        RequestMessage msg = MessageFactory.getAddAccessRequestMessage(user, shareUser, shareUser.generateToken(manager));
        String response = comm.sendMessage(msg.toJSON(), SHARE_ADD_ACCESS_RESOURCE);
        ResponseMessage responseMessage = MessageFactory.getResponseMessage(response);
        checkError(responseMessage);
        return true;
    }

    private boolean checkIsmOPECmd(TalosCommand cmd) {
        for(int i=0; i<cmd.numCiphers(); i++) {
            if(cmd.getCipherAtIndex(i) instanceof mOPEClientCipher) {
                return true;
            }
        }
        return false;
    }

    private boolean checkIsmOPECmdBatch(TalosBatchCommand cmd) {
        for(int i=0; i<cmd.numCiphers(); i++) {
            boolean temp = false;
            TalosCipher[] ciphers = cmd.getArgsAtIndex(i);

            for(int index=0; index < ciphers.length; index++) {
                if(cmd.getCommandAtIndex(i, index) instanceof mOPEClientCipher) {
                    temp = true;
                }
            }

            if (temp) {
                return true;
            }
        }
        return false;
    }

    private ResponseMessage handleOPECmd(User user, TalosCommand cmd) throws TalosModuleException {
        mOPEResultMessage msg = opeInteractor.startInteraction(user, cmd);
        return msg.getResult();
    }

    private void checkError(ResponseMessage response) throws CommTalosModuleException {
        if(response instanceof ErrorResponseMessage) {
            ErrorResponseMessage error = (ErrorResponseMessage) response;
            throw new CommTalosModuleException(error.getMsg());
        }
    }
}
