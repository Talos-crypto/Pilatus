package ch.ethz.inf.vs.talosavaapp.talos;

import android.content.Context;

import java.sql.Date;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.Cipher;

import ch.ethz.inf.vs.talosmodule.communication.TalosServer;
import ch.ethz.inf.vs.talosmodule.exceptions.TalosModuleException;
import ch.ethz.inf.vs.talosmodule.main.SharedUser;
import ch.ethz.inf.vs.talosmodule.main.TalosBatchCommand;
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

public class TalosAvaAPI {

    private TalosModuleFactory factory;

    private TalosServer server;

    private final String WEB_APP = "TalosCloudAva";

    public TalosAvaAPI(Context con, String ip, int port, User u) {
        this.factory = new TalosModuleFactory(con, u);
        this.server = factory.createServer(ip, port, ch.ethz.inf.vs.talosmodule.R.raw.talos, WEB_APP);
    }

    public TalosAvaAPI(Context con, String ip, int port, int certResource, User u) {
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

    public boolean insertDatasetWithSet(User user, Date date, Time time, String datatype, int data, int set) throws TalosModuleException {
        TalosColumn dataColHOM = new TalosColumn("data_HOM","data_HOM");

        TalosValue dataVal = TalosValue.createTalosValue(data);

        TalosCipher[] ciphers = new TalosCipher[5];

        TalosEncryptor encryptor = factory.createTalosEncryptor();
        ciphers[0] = new PlainCipher(date.toString());
        ciphers[1] = new PlainCipher(time.toString());
        ciphers[2] = new PlainCipher(String.valueOf(set));
        ciphers[3] = new PlainCipher(datatype);
        ciphers[4] = encryptor.encryptHOMPRE(dataVal, dataColHOM);

        TalosCommand cmd = new TalosCommand("insertDataset", ciphers);
        server.execute(user, cmd);
        return true;
    }

    public boolean insertDataset(User user, Date date, Time time, String datatype, int data) throws TalosModuleException {
        return insertDatasetWithSet(user, date, time, datatype, data, 0);
    }


    public boolean insertBatchDataset(User user, List<InsertValues> values) throws TalosModuleException {
        TalosColumn dataColHOM = new TalosColumn("data_HOM","data_HOM");

        TalosBatchCommand cmd = new TalosBatchCommand("insertDataset");
        for (InsertValues value : values) {

            TalosValue dataVal = TalosValue.createTalosValue(value.getData());

            TalosCipher[] ciphers = new TalosCipher[5];

            TalosEncryptor encryptor = factory.createTalosEncryptor();
            ciphers[0] = new PlainCipher(value.getDate().toString());
            ciphers[1] = new PlainCipher(value.getTime().toString());
            ciphers[2] = new PlainCipher(String.valueOf(0));
            ciphers[3] = new PlainCipher(value.getDatatype());
            ciphers[4] = encryptor.encryptHOMPRE(dataVal, dataColHOM);
            cmd.addCommand(ciphers);
        }
        server.execute(user, cmd);
        return true;
    }

    public TalosResult getValuesByDay(User user, Date fromDate, Date toDate, String datatype, SharedUser sharedUser) throws TalosModuleException{
        TalosColumn dataColHOM = new TalosColumn("data_HOM","data_HOM");

        TalosCipher[] ciphers = new TalosCipher[3];

        ciphers[0] = new PlainCipher(fromDate.toString());
        ciphers[1] = new PlainCipher(toDate.toString());
        ciphers[2] = new PlainCipher(datatype);

        TalosCommand cmd = new TalosCommand("getValuesByDay", ciphers);
        TalosCipherResultData encResult;
        if(sharedUser==null)
            encResult = server.execute(user, cmd);
        else
            encResult = server.execute(user, cmd, sharedUser);

        TalosDecryptor decryptor = factory.createTalosDecryptor();
        ArrayList<TalosResultSetRow> rows = new ArrayList<>();
        for(TalosCipherResultRow row : encResult) {
            TalosResultSetRow resrow = new TalosResultSetRow();
            TalosValue resSUM = decryptor.decryptHOMPRE(row.get("PRE_REL_SUM(data_HOM)"), TalosDataType.INT_32, dataColHOM);
            resrow.put("COUNT(data)", TalosValue.createTalosValue(row.get("COUNT(data_HOM)")));
            resrow.put("SUM(data)", resSUM);
            resrow.put("date", TalosValue.createTalosValue(row.get("date")));
            rows.add(resrow);
        }
        return new TalosResultSet(rows);
    }

    public TalosResult getValuesDuringDay(User user, Date curdate, SharedUser sharedUser) throws TalosModuleException{
        TalosColumn dataColHOM = new TalosColumn("data_HOM","data_HOM");

        TalosCipher[] ciphers = new TalosCipher[1];

        ciphers[0] = new PlainCipher(curdate.toString());

        TalosCommand cmd = new TalosCommand("getValuesDuringDay", ciphers);
        TalosCipherResultData encResult;
        if(sharedUser==null)
            encResult = server.execute(user, cmd);
        else
            encResult = server.execute(user, cmd, sharedUser);

        TalosDecryptor decryptor = factory.createTalosDecryptor();
        ArrayList<TalosResultSetRow> rows = new ArrayList<>();
        for(TalosCipherResultRow row : encResult) {
            TalosResultSetRow resrow = new TalosResultSetRow();
            TalosValue resSUM = decryptor.decryptHOMPRE(row.get("PRE_REL_SUM(data_HOM)"), TalosDataType.INT_32, dataColHOM);
            resrow.put("COUNT(data)", TalosValue.createTalosValue(row.get("COUNT(data_HOM)")));
            resrow.put("SUM(data)", resSUM);
            resrow.put("datatype", TalosValue.createTalosValue(row.get("datatype_DET")));
            rows.add(resrow);
        }
        return new TalosResultSet(rows);
    }

    public TalosResult agrDailySummary(User user, Date curdate, String datatype, int granularity, SharedUser sharedUser) throws TalosModuleException{
        TalosColumn dataColHOM = new TalosColumn("data_HOM","data_HOM");

        TalosValue datatypeVal = TalosValue.createTalosValue(datatype);

        TalosCipher[] ciphers = new TalosCipher[3];
        TalosEncryptor encryptor = factory.createTalosEncryptor();
        ciphers[0] = new PlainCipher(curdate.toString());
        ciphers[1] = new PlainCipher(datatype);
        ciphers[2] = new PlainCipher(String.valueOf(granularity));

        TalosCommand cmd = new TalosCommand("agrDailySummary", ciphers);
        TalosCipherResultData encResult;
        if(sharedUser==null)
            encResult = server.execute(user, cmd);
        else
            encResult = server.execute(user, cmd, sharedUser);

        TalosDecryptor decryptor = factory.createTalosDecryptor();
        ArrayList<TalosResultSetRow> rows = new ArrayList<>();
        for(TalosCipherResultRow row : encResult) {
            TalosResultSetRow resrow = new TalosResultSetRow();
            TalosValue resSUM = decryptor.decryptHOMPRE(row.get("PRE_REL_SUM(data_HOM)"), TalosDataType.INT_32, dataColHOM);
            resrow.put("COUNT(data)", TalosValue.createTalosValue(row.get("COUNT(data_HOM)")));
            resrow.put("SUM(data)", resSUM);
            resrow.put("agrTime", TalosValue.createTalosValue(row.get("agrTime")));
            rows.add(resrow);
        }
        return new TalosResultSet(rows);
    }

    public TalosResult getMostActualDate(User user, SharedUser sharedUser) throws TalosModuleException{
        TalosCipher[] ciphers = new TalosCipher[0];

        TalosCommand cmd = new TalosCommand("getMostActualDate", ciphers);
        TalosCipherResultData encResult;
        if(sharedUser==null)
            encResult = server.execute(user, cmd);
        else
            encResult = server.execute(user, cmd, sharedUser);

        ArrayList<TalosResultSetRow> rows = new ArrayList<>();
        for(TalosCipherResultRow row : encResult) {
            TalosResultSetRow resrow = new TalosResultSetRow();
            resrow.put("date", TalosValue.createTalosValue(row.get("date")));
            rows.add(resrow);
        }
        return new TalosResultSet(rows);
    }

    public TalosResult getSets(User user, SharedUser sharedUser) throws TalosModuleException{
        TalosCipher[] ciphers = new TalosCipher[0];

        TalosCommand cmd = new TalosCommand("getSets", ciphers);
        TalosCipherResultData encResult;
        if(sharedUser==null)
            encResult = server.execute(user, cmd);
        else
            encResult = server.execute(user, cmd, sharedUser);

        ArrayList<TalosResultSetRow> rows = new ArrayList<>();
        for(TalosCipherResultRow row : encResult) {
            TalosResultSetRow resrow = new TalosResultSetRow();
            resrow.put("setid", TalosValue.createTalosValue(row.get("setid")));
            rows.add(resrow);
        }
        return new TalosResultSet(rows);
    }


    public TalosResult getDataForSet(User user, SharedUser sharedUser, int set) throws TalosModuleException{
        TalosColumn dataColHOM = new TalosColumn("data_HOM","data_HOM");
        TalosCipher[] ciphers = new TalosCipher[1];
        ciphers[0] = new PlainCipher(String.valueOf(set));

        TalosCommand cmd = new TalosCommand("getValuesForSet", ciphers);
        TalosCipherResultData encResult;
        if(sharedUser==null)
            encResult = server.execute(user, cmd);
        else
            encResult = server.execute(user, cmd, sharedUser);

        TalosDecryptor decryptor = factory.createTalosDecryptor();
        ArrayList<TalosResultSetRow> rows = new ArrayList<>();
        for(TalosCipherResultRow row : encResult) {
            TalosResultSetRow resrow = new TalosResultSetRow();
            TalosValue resHom = decryptor.decryptHOMPRE(row.get("data_HOM"), TalosDataType.INT_32, dataColHOM);
            resrow.put("data", resHom);
            resrow.put("datatype", TalosValue.createTalosValue(row.get("datatype_DET")));
            resrow.put("date", TalosValue.createTalosValue(row.get("date")));
            resrow.put("time", TalosValue.createTalosValue(row.get("time")));
            rows.add(resrow);
        }
        return new TalosResultSet(rows);
    }
}
