package main;

import auth.DebugAuthenticator;
import auth.TokenIDAuthenticator;
import org.restlet.security.Authenticator;
import resources.*;
import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.routing.Router;
import util.SystemUtil;

import java.io.File;
import java.io.IOException;

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
 * The root Restlet Application for the Talos Cloud.
 * Defines the REST API and the corresponding java classes that are called upon
 * requests.
 */
public class TalosApplication extends Application {

    /**
     * Creates a root Restlet that will receive all incoming calls.
     */
    @Override
    public synchronized Restlet createInboundRoot() {
        Router startRouter = new Router(getContext());
        Router protectedRouter = new Router(getContext());
        Authenticator auth;

        if(SystemUtil.DEBUG_AUTH) {
            auth = new DebugAuthenticator(getContext());
            startRouter.attach(SystemUtil.REGISTER_RESOURCE, DebugRegisterResource.class);
        } else {
            auth = new TokenIDAuthenticator(getContext());
            startRouter.attach(SystemUtil.REGISTER_RESOURCE, RegisterResource.class);
        }
        startRouter.attachDefault(auth);
        protectedRouter.attach(SystemUtil.COMMAND_RESOURCE, CommandResource.class);
        protectedRouter.attach(SystemUtil.BATCH_COMMAND_RESOURCE, BatchCommandResource.class);
        protectedRouter.attach(SystemUtil.COMMAND_OPE_RESOURCE, mOPEResource.class);
        protectedRouter.attach(SystemUtil.SHARE_ACCESS, SharedAccessUsersResource.class);
        protectedRouter.attach(SystemUtil.SHARE_ADD_ACCESS, ClientShareResource.class);
        protectedRouter.attach(SystemUtil.SHARE_TO_CLIENTS, SharedUsersResource.class);
        protectedRouter.attach(SystemUtil.SHARE_SHARABLE_USER, SharableUsersResource.class);
        auth.setNext(protectedRouter);

        if(SystemUtil.SECURITY_OFF_DEBUG)
            return protectedRouter;
        else
            return startRouter;
    }


}
