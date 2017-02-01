package ch.ethz.inf.vs.talosmodule.communication;

import java.math.BigInteger;

import ch.ethz.inf.vs.talosmodule.communication.messages.MessageFactory;
import ch.ethz.inf.vs.talosmodule.communication.messages.RequestMessage;
import ch.ethz.inf.vs.talosmodule.communication.mopemessages.mOPEClientStepMessage;
import ch.ethz.inf.vs.talosmodule.communication.mopemessages.mOPEMessage;
import ch.ethz.inf.vs.talosmodule.communication.mopemessages.mOPEMsgFactory;
import ch.ethz.inf.vs.talosmodule.communication.mopemessages.mOPEResponseMessage;
import ch.ethz.inf.vs.talosmodule.communication.mopemessages.mOPEResultMessage;
import ch.ethz.inf.vs.talosmodule.crypto.KeyManager;
import ch.ethz.inf.vs.talosmodule.crypto.enclayers.DETBfInt;
import ch.ethz.inf.vs.talosmodule.crypto.enclayers.EncLayer;
import ch.ethz.inf.vs.talosmodule.exceptions.TalosModuleException;
import ch.ethz.inf.vs.talosmodule.main.TalosCommand;
import ch.ethz.inf.vs.talosmodule.main.User;

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
 * Performs mOPE-interactions with the backend
 */
public final class mOPEInteractor {

    private CommunicationHelper helper;
    private KeyManager man = null;
    private String opeResource;

    public mOPEInteractor(CommunicationHelper helper, KeyManager man, String opeResource) {
        this.helper = helper;
        this.man = man;
        this.opeResource = opeResource;
    }

    public mOPEResultMessage startInteraction(User user, TalosCommand cmd) throws TalosModuleException {
        RequestMessage msg = MessageFactory.getCommandRequestMessage(user, cmd);

        String response = helper.sendMessage(msg.toJSON(), opeResource);
        mOPEResponseMessage reponseMsg= mOPEMsgFactory.getResponseMessage(response);

        while (!reponseMsg.isResultMessage()) {
            if(reponseMsg.isClientStep()) {
                mOPEClientStepMessage step = (mOPEClientStepMessage) reponseMsg;
                EncLayer layer = getLayer(step.getDetHash());
                BigInteger refValue = decryptValue(step.getValue(), layer);
                int index = 0;
                boolean eq = false;
                for(String compare : step.getValuesToCompare()) {
                    BigInteger other = decryptValue(compare, layer);
                    if(other.compareTo(refValue) >= 0) {
                        eq = other.compareTo(refValue) == 0;
                        break;
                    }
                    index++;
                }
                mOPEMessage answer = mOPEMsgFactory.getClientAnswerMessage(user, index, eq, step.getSessionID());
                response = helper.sendMessage(answer.toStringMessage(), opeResource);
                reponseMsg= mOPEMsgFactory.getResponseMessage(response);
            } else {
                throw new TalosModuleException("Illegal Message received");
            }
        }

        return (mOPEResultMessage) reponseMsg;
    }

    private BigInteger decryptValue(String val, EncLayer layer) throws TalosModuleException {
        return layer.decrypt(layer.getCipherFromString(val)).getBigInteger();
    }

    private EncLayer getLayer(String hash) throws TalosModuleException {
        return new DETBfInt(man.getKey(hash));
    }
}
