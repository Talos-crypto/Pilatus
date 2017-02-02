package ch.ethz.inf.vs.talosfitbitapp.talos;

import android.content.Context;

import java.sql.Date;
import java.sql.Time;
import java.util.ArrayList;

import ch.ethz.inf.vs.talosmodule.communication.TalosServer;
import ch.ethz.inf.vs.talosmodule.exceptions.TalosModuleException;
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

public class TalosModuleFitbit {

    private TalosModuleFactory factory;

    private TalosServer server;

    private final String WEB_APP = "FitBitAppWS";

    public TalosModuleFitbit(Context con, String ip, int port) {
        this.factory = new TalosModuleFactory(con);
        this.server = factory.createServer(ip, port, WEB_APP);
    }

    public TalosModuleFitbit(Context con, String ip, int port, int certResource) {
        this.factory = new TalosModuleFactory(con);
        this.server = factory.createServer(ip, port, certResource, WEB_APP);
    }

    public boolean registerUser(User u) {
        try {
            return server.register(u);
        } catch (TalosModuleException e) {
            return false;
        }
    }

    public boolean insertDataset(User user, Date date, Time time, String datatype, int data) throws TalosModuleException {
        TalosColumn datatypeColDET = new TalosColumn("datatype_DET","datatype_DET");
        TalosColumn dataColRND = new TalosColumn("data_RND","data_RND");
        TalosColumn dataColHOM = new TalosColumn("data_HOM","data_HOM");

        TalosValue datatypeVal = TalosValue.createTalosValue(datatype);
        TalosValue dataVal = TalosValue.createTalosValue(data);

        TalosCipher[] ciphers = new TalosCipher[5];

        TalosEncryptor encryptor = factory.createTalosEncryptor();
        ciphers[0] = new PlainCipher(date.toString());
        ciphers[1] = new PlainCipher(time.toString());;
        ciphers[2] = encryptor.encryptDET(datatypeVal, datatypeColDET);
        ciphers[3] = encryptor.encryptRND(dataVal, dataColRND);
        ciphers[4] = encryptor.encryptHOM(dataVal, dataColHOM);

        TalosCommand cmd = new TalosCommand("insertDataset", ciphers);
        server.execute(user, cmd);
        return true;
    }

    public TalosResult getValuesByDay(User user, Date fromDate, Date toDate, String datatype) throws TalosModuleException{
        TalosColumn dataColHOM = new TalosColumn("data_HOM","data_HOM");
        TalosColumn datatypeColDET = new TalosColumn("datatype_DET","datatype_DET");

        TalosValue datatypeVal = TalosValue.createTalosValue(datatype);

        TalosCipher[] ciphers = new TalosCipher[3];

        TalosEncryptor encryptor = factory.createTalosEncryptor();
        ciphers[0] = new PlainCipher(fromDate.toString());
        ciphers[1] = new PlainCipher(toDate.toString());
        ciphers[2] = encryptor.encryptDET(datatypeVal, datatypeColDET);

        TalosCommand cmd = new TalosCommand("getValuesByDay", ciphers);
        TalosCipherResultData encResult = server.execute(user, cmd);

        TalosDecryptor decryptor = factory.createTalosDecryptor();
        ArrayList<TalosResultSetRow> rows = new ArrayList<>();
        for(TalosCipherResultRow row : encResult) {
            TalosResultSetRow resrow = new TalosResultSetRow();
            TalosValue resSUM = decryptor.decryptHOM(row.get("CRT_GAMAL_SUM(data_HOM)"), TalosDataType.INT_32, dataColHOM);
            resrow.put("COUNT(data)", TalosValue.createTalosValue(row.get("COUNT(data_HOM)")));
            resrow.put("SUM(data)", resSUM);
            resrow.put("date", TalosValue.createTalosValue(row.get("date")));
            rows.add(resrow);
        }
        return new TalosResultSet(rows);
    }

    public TalosResult getValuesDuringDay(User user, Date curdate) throws TalosModuleException{
        TalosColumn dataColHOM = new TalosColumn("data_HOM","data_HOM");
        TalosColumn datatypeColDET = new TalosColumn("datatype_DET","datatype_DET");

        TalosCipher[] ciphers = new TalosCipher[1];

        ciphers[0] = new PlainCipher(curdate.toString());

        TalosCommand cmd = new TalosCommand("getValuesDuringDay", ciphers);
        TalosCipherResultData encResult = server.execute(user, cmd);

        TalosDecryptor decryptor = factory.createTalosDecryptor();
        ArrayList<TalosResultSetRow> rows = new ArrayList<>();
        for(TalosCipherResultRow row : encResult) {
            TalosResultSetRow resrow = new TalosResultSetRow();
            TalosValue resSUM = decryptor.decryptHOM(row.get("CRT_GAMAL_SUM(data_HOM)"), TalosDataType.INT_32, dataColHOM);
            TalosValue resType = decryptor.decryptDET(row.get("datatype_DET"), TalosDataType.STR, datatypeColDET);

            resrow.put("COUNT(data)", TalosValue.createTalosValue(row.get("COUNT(data_HOM)")));
            resrow.put("SUM(data)", resSUM);
            resrow.put("datatype", resType);
            rows.add(resrow);
        }
        return new TalosResultSet(rows);
    }

    public TalosResult agrDailySummary(User user, Date curdate, String datatype, int granularity) throws TalosModuleException{
        TalosColumn dataColHOM = new TalosColumn("data_HOM","data_HOM");
        TalosColumn datatypeColDET = new TalosColumn("datatype_DET","datatype_DET");

        TalosValue datatypeVal = TalosValue.createTalosValue(datatype);

        TalosCipher[] ciphers = new TalosCipher[3];
        TalosEncryptor encryptor = factory.createTalosEncryptor();
        ciphers[0] = new PlainCipher(curdate.toString());
        ciphers[1] = encryptor.encryptDET(datatypeVal, datatypeColDET);
        ciphers[2] = new PlainCipher(String.valueOf(granularity));

        TalosCommand cmd = new TalosCommand("agrDailySummary", ciphers);
        TalosCipherResultData encResult = server.execute(user, cmd);

        TalosDecryptor decryptor = factory.createTalosDecryptor();
        ArrayList<TalosResultSetRow> rows = new ArrayList<>();
        for(TalosCipherResultRow row : encResult) {
            TalosResultSetRow resrow = new TalosResultSetRow();
            TalosValue resSUM = decryptor.decryptHOM(row.get("CRT_GAMAL_SUM(data_HOM)"), TalosDataType.INT_32, dataColHOM);
            resrow.put("COUNT(data)", TalosValue.createTalosValue(row.get("COUNT(data_HOM)")));
            resrow.put("SUM(data)", resSUM);
            resrow.put("agrTime", TalosValue.createTalosValue(row.get("agrTime")));
            rows.add(resrow);
        }
        return new TalosResultSet(rows);
    }

    public TalosResult getMostActualDate(User user) throws TalosModuleException{
        TalosCipher[] ciphers = new TalosCipher[0];

        TalosCommand cmd = new TalosCommand("getMostActualDate", ciphers);
        TalosCipherResultData encResult = server.execute(user, cmd);

        ArrayList<TalosResultSetRow> rows = new ArrayList<>();
        for(TalosCipherResultRow row : encResult) {
            TalosResultSetRow resrow = new TalosResultSetRow();
            resrow.put("date", TalosValue.createTalosValue(row.get("MAX(Dataset.date)")));
            rows.add(resrow);
        }
        return new TalosResultSet(rows);
    }

}
