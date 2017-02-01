package ch.ethz.inf.vs.talosmodule.main;

import android.content.Context;

import ch.ethz.inf.vs.talosmodule.communication.DebugSSLCommunication;
import ch.ethz.inf.vs.talosmodule.communication.SecureComm;
import ch.ethz.inf.vs.talosmodule.communication.SecureCommSelfSigned;
import ch.ethz.inf.vs.talosmodule.communication.TalosServer;
import ch.ethz.inf.vs.talosmodule.communication.TalosServerCommunicator;
import ch.ethz.inf.vs.talosmodule.crypto.KeyManager;
import ch.ethz.inf.vs.talosmodule.main.taloscrypto.TalosCryptoManager;
import ch.ethz.inf.vs.talosmodule.main.taloscrypto.TalosDecryptor;
import ch.ethz.inf.vs.talosmodule.main.taloscrypto.TalosEncryptor;
import ch.ethz.inf.vs.talosmodule.util.ContextHolder;
import ch.ethz.inf.vs.talosmodule.util.Setting;

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
 * The factory is used for creating the essential objects for interacting with client library.
 * A TalosFactory can be created by providing the Application Context of your app.
 */
public class TalosModuleFactory {

    private Context con;

    private KeyManager keyMan;

    /**
     * Creates a TalosModuleFactory
     * @param con the application android.content.Context of the app
     */
    public TalosModuleFactory(Context con, User u) {
        this.con = con;
        //Bad practice: But table-load Gamal needs context
        ContextHolder.setContext(this.con);
        this.keyMan = new KeyManager(con, u);
    }

    public TalosModuleFactory(Context con) {
        this.con = con;
        //Bad practice: But table-load Gamal needs context
        ContextHolder.setContext(this.con);
        this.keyMan = new KeyManager(con);
    }

    /**
     * Creates a TalosEncryptor for encrypting TalosValues.
     * @return a TalosEncryptor
     */
    public  TalosEncryptor createTalosEncryptor() {
        return TalosCryptoManager.createTalosEncryptor(keyMan);
    }

    /**
     * Creates a TalosDecryptor for decrypting TalosCiphers.
     * @return a TalosDecryptor
     */
    public TalosDecryptor createTalosDecryptor() {
        return TalosCryptoManager.createTalosDecryptor(keyMan);
    }

    /**
     * Creates a TalosServer for interacting with the cloud.
     * Note that this method requires a valid Server-Side certificate. (HTTPS)
     * @param ip the host address of the cloud (EX. www.mycloud.com)
     * @param port the port used for the web service.
     * @return a TalosServer
     */
    public TalosServer createServer(String ip, int port, String webserviceAppliactionPrefix) {
        if(Setting.DEBUG_WITH_HTTP)
            return new TalosServerCommunicator(webserviceAppliactionPrefix, new SecureComm("http", ip, port), keyMan);
        else if(Setting.DEBUG_WITH_HTTPS_NO_CERT_CHECK)
            return new TalosServerCommunicator(webserviceAppliactionPrefix, new DebugSSLCommunication("https",ip,port), keyMan);
        else
            return new TalosServerCommunicator(webserviceAppliactionPrefix, new SecureComm("https", ip, port), keyMan);
    }

    /**
     * Creates a TalosServer for interacting with the cloud.
     * This method assumes a self-signed certificate on the server side.
     * @param ip the host address of the cloud (EX. www.mycloud.com)
     * @param port the port used for the web service. (Ex. 8080)
     * @param selfSignedResourceID the android resource id (Ex. R.raw.mycert) of the self-signed certificate for validating the server.
     * @return a TalosServer
     */
    public TalosServer createServer(String ip, int port, int selfSignedResourceID, String webserviceAppliactionPrefix) {
        return new TalosServerCommunicator(webserviceAppliactionPrefix, new SecureCommSelfSigned(ip, port, con, selfSignedResourceID), keyMan);

    }

}
