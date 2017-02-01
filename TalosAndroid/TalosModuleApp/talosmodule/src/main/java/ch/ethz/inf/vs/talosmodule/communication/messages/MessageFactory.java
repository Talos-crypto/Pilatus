package ch.ethz.inf.vs.talosmodule.communication.messages;

import org.json.JSONException;

import ch.ethz.inf.vs.talosmodule.cryptoalg.PRERelic;
import ch.ethz.inf.vs.talosmodule.main.SharedUser;
import ch.ethz.inf.vs.talosmodule.main.TalosBatchCommand;
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

public class MessageFactory {

    public static RequestMessage getCommandRequestMessage(User user, TalosCommand command) {
        return new CommandRequestMessage(user.getIdToken(), command);
    }

    public static RequestMessage getCommandRequestMessage(User user, TalosBatchCommand command) {
        return new BatchCommandMessage(user.getIdToken(), command);
    }

    public static RequestMessage getEmptyRequestMessage(User user) {
        return new EmptyRequestMessage(user.getIdToken());
    }

    public static RequestMessage getSharedRegisterRequest(User user, byte[] pk) {
        return new SharedRegisterRequestMessage(user.getIdToken(), pk);
    }

    public static RequestMessage getAddAccessRequestMessage(User user, SharedUser user1, PRERelic.PREToken shareToken1) {
        return new AddAccessRequestMessage(user.getIdToken(), user1, shareToken1);
    }

    public static RequestMessage getShareCommandRequestMesssage(User user, TalosCommand command, SharedUser shareuser) {
        return new ShareCommandRequestMessage(user.getIdToken(), command, shareuser);
    }

    public static ResponseMessage getResponseMessage(String msg) {
        try {
            return MessagesUtils.getResponseMessage(msg);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
}
