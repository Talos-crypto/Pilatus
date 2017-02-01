package ch.ethz.inf.vs.talsomoduleapp.dbmodel;

import android.content.Context;

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
import ch.ethz.inf.vs.talosmodule.main.mOPEOperationType;
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

public class TalosModuleSensor {

    private TalosModuleFactory factory;

    private TalosServer server;

    private static final String WEB_APP_PREF = "SensorAppWS";

    public TalosModuleSensor(Context con, String ip, int port) {
        this.factory = new TalosModuleFactory(con);
        this.server = factory.createServer(ip, port, WEB_APP_PREF);
    }

    public TalosModuleSensor(Context con, String ip, int port, int certResource) {
        this.factory = new TalosModuleFactory(con);
        this.server = factory.createServer(ip, port, certResource, WEB_APP_PREF);
    }

    public boolean registerUser(User u) {
        try {
            return server.register(u);
        } catch (TalosModuleException e) {
            return false;
        }
    }

    public boolean storeMeasurement(User user, String datatype, String sensorID, String timestamp, int data) throws TalosModuleException {
        TalosColumn datatypeColDET = new TalosColumn("datatype_DET","datatype_DET");
        TalosColumn sensorIDColDET = new TalosColumn("sensorID_DET","JOIN1");
        TalosColumn timestampColRND = new TalosColumn("timeStamp_RND","timeStamp_RND");
        TalosColumn dataColDET = new TalosColumn("data_DET","data_DET");
        TalosColumn dataColHOM = new TalosColumn("data_HOM","data_HOM");

        TalosValue datatypeVal = TalosValue.createTalosValue(datatype);
        TalosValue sensorIDVal = TalosValue.createTalosValue(sensorID);
        TalosValue timestampVal = TalosValue.createTalosValue(timestamp);
        TalosValue dataVal = TalosValue.createTalosValue(data);

        TalosCipher[] ciphers = new TalosCipher[6];

        TalosEncryptor encryptor = factory.createTalosEncryptor();
        ciphers[0] = encryptor.encryptDET(datatypeVal,datatypeColDET);
        ciphers[1] = encryptor.encryptDET(sensorIDVal, sensorIDColDET);
        ciphers[2] = encryptor.encryptRND(timestampVal, timestampColRND);
        ciphers[3] = encryptor.encryptDET(dataVal, dataColDET);
        ciphers[4] = encryptor.encryptHOM(dataVal, dataColHOM);
        ciphers[5] = encryptor.encryptOPE(dataVal, dataColDET, 1, mOPEOperationType.INSERT);

        TalosCommand cmd = new TalosCommand("storeMeasurement", ciphers);
        server.execute(user, cmd);
        return true;
    }

    public TalosResult sensorExistsInDB(User user, String nameID) throws TalosModuleException{
        TalosColumn nameIDDET = new TalosColumn("nameID_DET","JOIN1");

        TalosValue nameIDVal = TalosValue.createTalosValue(nameID);

        TalosCipher[] ciphers = new TalosCipher[1];

        TalosEncryptor encryptor = factory.createTalosEncryptor();
        ciphers[0] = encryptor.encryptDET(nameIDVal, nameIDDET);

        TalosCommand cmd = new TalosCommand("sensorExistsInDB", ciphers);
        TalosCipherResultData encResult = server.execute(user, cmd);

        TalosDecryptor decryptor = factory.createTalosDecryptor();
        ArrayList<TalosResultSetRow> rows = new ArrayList<>();
        for(TalosCipherResultRow row : encResult) {
            TalosResultSetRow resrow = new TalosResultSetRow();
            TalosValue resB = decryptor.decryptDET(row.get("nameID_DET"), TalosDataType.STR, nameIDDET);
            resrow.put("nameID", resB);
            rows.add(resrow);
        }
        return new TalosResultSet(rows);
    }

    public boolean storeSensorInDB(User user, String nameID, String name, String belongsTo, String vendor, String description) throws TalosModuleException {
        TalosColumn nameIDColDET = new TalosColumn("nameID_DET","JOIN1");
        TalosColumn nameColRND = new TalosColumn("name_RND","name_RND");
        TalosColumn belongsToColRND = new TalosColumn("belongsTo_RND","belongsTo_RND");
        TalosColumn vendorColRND = new TalosColumn("vendor_RND","vendor_RND");
        TalosColumn descriptionColRND = new TalosColumn("description_RND","description_RND");

        TalosValue nameIDVal = TalosValue.createTalosValue(nameID);
        TalosValue nameVal = TalosValue.createTalosValue(name);
        TalosValue belongsToVal = TalosValue.createTalosValue(belongsTo);
        TalosValue vendorVal = TalosValue.createTalosValue(vendor);
        TalosValue descriptionVal = TalosValue.createTalosValue(description);

        TalosCipher[] ciphers = new TalosCipher[5];

        TalosEncryptor encryptor = factory.createTalosEncryptor();
        ciphers[0] = encryptor.encryptDET(nameIDVal,nameIDColDET);
        ciphers[1] = encryptor.encryptRND(nameVal, nameColRND);
        ciphers[2] = encryptor.encryptRND(belongsToVal, belongsToColRND);
        ciphers[3] = encryptor.encryptRND(vendorVal, vendorColRND);
        ciphers[4] = encryptor.encryptRND(descriptionVal, descriptionColRND);

        TalosCommand cmd = new TalosCommand("storeSensorInDB", ciphers);
        server.execute(user, cmd);
        return true;
    }


    public TalosResult loadSensors(User user) throws TalosModuleException{
        TalosColumn nameIDColDET = new TalosColumn("nameID_DET","JOIN1");
        TalosColumn nameColRND = new TalosColumn("name_RND","name_RND");
        TalosColumn belongsToColRND = new TalosColumn("belongsTo_RND","belongsTo_RND");
        TalosColumn vendorColRND = new TalosColumn("vendor_RND","vendor_RND");
        TalosColumn descriptionColRND = new TalosColumn("description_RND","description_RND");

        TalosCipher[] ciphers = new TalosCipher[0];
        TalosCommand cmd = new TalosCommand("loadSensors", ciphers);
        TalosCipherResultData encResult = server.execute(user, cmd);

        TalosDecryptor decryptor = factory.createTalosDecryptor();
        ArrayList<TalosResultSetRow> rows = new ArrayList<>();
        for(TalosCipherResultRow row : encResult) {
            TalosResultSetRow resrow = new TalosResultSetRow();
            TalosValue resA = decryptor.decryptDET(row.get("nameID_DET"), TalosDataType.STR, nameIDColDET);
            TalosValue resB = decryptor.decryptRND(row.get("name_RND"), TalosDataType.STR, nameColRND);
            TalosValue resC = decryptor.decryptRND(row.get("belongsTo_RND"), TalosDataType.STR, belongsToColRND);
            TalosValue resD = decryptor.decryptRND(row.get("vendor_RND"), TalosDataType.STR, vendorColRND);
            TalosValue resE = decryptor.decryptRND(row.get("description_RND"), TalosDataType.STR, descriptionColRND);
            resrow.put("nameID", resA);
            resrow.put("name", resB);
            resrow.put("belongsTo", resC);
            resrow.put("vendor", resD);
            resrow.put("description", resE);
            rows.add(resrow);
        }
        return new TalosResultSet(rows);
    }

    public TalosResult loadMeasurementsWithTag(User user, String nameID, String datatype, int limit) throws TalosModuleException{
        TalosColumn datatypeColDET = new TalosColumn("datatype_DET","datatype_DET");
        TalosColumn sensorIDColDET = new TalosColumn("sensorID_DET","JOIN1");
        TalosColumn timestampColRND = new TalosColumn("timeStamp_RND","timeStamp_RND");
        TalosColumn dataColDET = new TalosColumn("data_DET","data_DET");

        TalosValue nameIDVal = TalosValue.createTalosValue(nameID);
        TalosValue datatypeVal = TalosValue.createTalosValue(datatype);


        TalosEncryptor encryptor = factory.createTalosEncryptor();
        TalosCipher[] ciphers = new TalosCipher[3];
        ciphers[0] = encryptor.encryptDET(nameIDVal, sensorIDColDET);
        ciphers[1] = encryptor.encryptDET(datatypeVal, datatypeColDET);
        ciphers[2] = new PlainCipher(String.valueOf(limit));
        TalosCommand cmd = new TalosCommand("loadMeasurementsWithTag", ciphers);
        TalosCipherResultData encResult = server.execute(user, cmd);

        TalosDecryptor decryptor = factory.createTalosDecryptor();
        ArrayList<TalosResultSetRow> rows = new ArrayList<>();
        for(TalosCipherResultRow row : encResult) {
            TalosResultSetRow resrow = new TalosResultSetRow();
            TalosValue resA = TalosValue.createTalosValue(Integer.valueOf(row.get("id_PLAIN")));
            TalosValue resB = decryptor.decryptDET(row.get("datatype_DET"), TalosDataType.STR, datatypeColDET);
            TalosValue resC = decryptor.decryptDET(row.get("sensorID_DET"), TalosDataType.STR, sensorIDColDET);
            TalosValue resD = decryptor.decryptRND(row.get("timeStamp_RND"), TalosDataType.STR, timestampColRND);
            TalosValue resE = decryptor.decryptDET(row.get("data_DET"), TalosDataType.INT_32, dataColDET);
            resrow.put("id", resA);
            resrow.put("datatype", resB);
            resrow.put("sensorID", resC);
            resrow.put("timeStamp", resD);
            resrow.put("data", resE);
            rows.add(resrow);
        }
        return new TalosResultSet(rows);
    }

    public TalosResult loadValueTypesAndAverage(User user, String nameID) throws TalosModuleException{
        TalosColumn sensorIDColDET = new TalosColumn("sensorID_DET","JOIN1");
        TalosColumn datatypeColDET = new TalosColumn("datatype_DET","datatype_DET");
        TalosColumn dataColHOM = new TalosColumn("data_HOM","data_HOM");

        TalosValue nameIDVal = TalosValue.createTalosValue(nameID);

        TalosEncryptor encryptor = factory.createTalosEncryptor();
        TalosCipher[] ciphers = new TalosCipher[1];
        ciphers[0] = encryptor.encryptDET(nameIDVal, sensorIDColDET);
        TalosCommand cmd = new TalosCommand("loadValueTypesAndAverage", ciphers);
        TalosCipherResultData encResult = server.execute(user, cmd);

        TalosDecryptor decryptor = factory.createTalosDecryptor();
        ArrayList<TalosResultSetRow> rows = new ArrayList<>();
        for(TalosCipherResultRow row : encResult) {
            TalosResultSetRow resrow = new TalosResultSetRow();
            TalosValue resA = decryptor.decryptDET(row.get("datatype_DET"), TalosDataType.STR, datatypeColDET);
            TalosValue resB = decryptor.decryptHOM(row.get("CRT_GAMAL_SUM(Measurement.data_HOM)"), TalosDataType.INT_32, dataColHOM);
            TalosValue resC = TalosValue.createTalosValue(Integer.valueOf(row.get("COUNT(Measurement.data_HOM)")));
            resrow.put("datatype", resA);
            resrow.put("SUM(data)", resB);
            resrow.put("COUNT(data)", resC);
            rows.add(resrow);
        }
        return new TalosResultSet(rows);
    }

    public TalosResult loadMetaOfMeasurementsWithTag(User user, String nameID, String datatype) throws TalosModuleException{
        TalosColumn datatypeColDET = new TalosColumn("datatype_DET","datatype_DET");
        TalosColumn sensorIDColDET = new TalosColumn("sensorID_DET","JOIN1");
        TalosColumn dataColDET = new TalosColumn("data_DET","data_DET");

        TalosValue nameIDVal = TalosValue.createTalosValue(nameID);
        TalosValue datatypeVal = TalosValue.createTalosValue(datatype);


        TalosEncryptor encryptor = factory.createTalosEncryptor();
        TalosCipher[] ciphers = new TalosCipher[2];
        ciphers[0] = encryptor.encryptDET(nameIDVal, sensorIDColDET);
        ciphers[1] = encryptor.encryptDET(datatypeVal, datatypeColDET);
        TalosCommand cmd = new TalosCommand("loadMetaOfMeasurementsWithTag", ciphers);
        TalosCipherResultData encResult = server.execute(user, cmd);

        TalosDecryptor decryptor = factory.createTalosDecryptor();
        ArrayList<TalosResultSetRow> rows = new ArrayList<>();
        for(TalosCipherResultRow row : encResult) {
            TalosResultSetRow resrow = new TalosResultSetRow();
            TalosValue resA = TalosValue.createTalosValue(Integer.valueOf(row.get("COUNT(Measurement.id_PLAIN)")));
            TalosValue resB = decryptor.decryptDET(row.get("mOPE_MIN(Measurement.data_DET, Measurement.data_OPE)"), TalosDataType.INT_32, dataColDET);
            TalosValue resC = decryptor.decryptDET(row.get("mOPE_MAX(Measurement.data_DET, Measurement.data_OPE)"), TalosDataType.INT_32, dataColDET);
            resrow.put("COUNT(id)", resA);
            resrow.put("MIN(data)", resB);
            resrow.put("MAX(data)", resC);
            rows.add(resrow);
        }
        return new TalosResultSet(rows);
    }

}
