package ch.ethz.inf.vs.talosmodule.prerelictests;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import ch.ethz.inf.vs.talosmodule.communication.TalosServer;
import ch.ethz.inf.vs.talosmodule.exceptions.TalosModuleException;
import ch.ethz.inf.vs.talosmodule.main.SharedUser;
import ch.ethz.inf.vs.talosmodule.main.TalosCipherResultData;
import ch.ethz.inf.vs.talosmodule.main.TalosCipherResultRow;
import ch.ethz.inf.vs.talosmodule.main.TalosColumn;
import ch.ethz.inf.vs.talosmodule.main.TalosCommand;
import ch.ethz.inf.vs.talosmodule.main.TalosModuleFactory;
import ch.ethz.inf.vs.talosmodule.main.TalosResult;
import ch.ethz.inf.vs.talosmodule.main.TalosResultSet;
import ch.ethz.inf.vs.talosmodule.main.TalosResultSetRow;
import ch.ethz.inf.vs.talosmodule.main.User;
import ch.ethz.inf.vs.talosmodule.main.taloscrypto.TalosDecryptor;
import ch.ethz.inf.vs.talosmodule.main.taloscrypto.TalosEncryptor;
import ch.ethz.inf.vs.talosmodule.main.values.PlainCipher;
import ch.ethz.inf.vs.talosmodule.main.values.TalosCipher;
import ch.ethz.inf.vs.talosmodule.main.values.TalosDataType;
import ch.ethz.inf.vs.talosmodule.main.values.TalosValue;

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

public class PreTalosModule {

    private TalosModuleFactory factory;

    private TalosServer server;

    private final String WEB_APP = "TalosCloud";

    public PreTalosModule(Context con, String ip, int port, User u) {
        this.factory = new TalosModuleFactory(con, u);
        this.server = factory.createServer(ip, port, WEB_APP);
    }

    public PreTalosModule(Context con, String ip, int port, int certResource, User u) {
        this.factory = new TalosModuleFactory(con, u);
        this.server = factory.createServer(ip, port, certResource, WEB_APP);
    }

    public boolean registerUser(User u) {
        try {
            return server.registerShared(u);
        } catch (TalosModuleException e) {
            return false;
        }
    }

    public boolean addSharedUser(User u, SharedUser other) {
        try {
            return server.shareMyData(u, other);
        } catch (TalosModuleException e) {
            return false;
        }
    }

    public List<SharedUser> getAccesses(User u) {
        try {
            return server.getMyAccessableUsers(u);

        } catch (TalosModuleException e) {
            return new ArrayList<>();
        }
    }

    public List<SharedUser> getMyShares(User u) {
        try {
            return server.getMySharedUsers(u);

        } catch (TalosModuleException e) {
            return new ArrayList<>();
        }
    }

    public List<SharedUser> getUsers(User u) {
        try {
            return server.getSharableUsers(u);

        } catch (TalosModuleException e) {
            return new ArrayList<>();
        }
    }

    public boolean insertDataset(User user, int data) throws TalosModuleException {
        TalosColumn dataColHOM = new TalosColumn("data_HOM","data_HOM");

        TalosValue dataVal = TalosValue.createTalosValue(data);

        TalosCipher[] ciphers = new TalosCipher[1];

        TalosEncryptor encryptor = factory.createTalosEncryptor();
        ciphers[0] = encryptor.encryptHOMPRE(dataVal, dataColHOM);

        TalosCommand cmd = new TalosCommand("insertDataset", ciphers);
        server.execute(user, cmd);
        return true;
    }

    public TalosValue getSUM(User user, SharedUser other) throws TalosModuleException{
        TalosColumn dataColHOM = new TalosColumn("data_HOM","data_HOM");


        TalosCipher[] ciphers = new TalosCipher[0];

        TalosCommand cmd = new TalosCommand("getSUM", ciphers);
        TalosCipherResultData encResult;
        if(other==null)
            encResult = server.execute(user, cmd);
        else
            encResult = server.execute(user, cmd, other);

        TalosDecryptor decryptor = factory.createTalosDecryptor();
        ArrayList<TalosResultSetRow> rows = new ArrayList<>();
        for(TalosCipherResultRow row : encResult) {
            TalosResultSetRow resrow = new TalosResultSetRow();
            TalosValue resSUM = decryptor.decryptHOMPRE(row.get("PRE_REL_SUM(data_HOM)"), TalosDataType.INT_32, dataColHOM);
            resrow.put("SUM(data)", resSUM);
            rows.add(resrow);
        }
        return rows.get(0).get("SUM(data)");
    }
}
