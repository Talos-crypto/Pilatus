package ch.ethz.inf.vs.talsomoduleapp.dbmodel;

import android.util.Pair;

import java.util.ArrayList;
import java.util.List;

import ch.ethz.inf.vs.talosmodule.exceptions.TalosModuleException;
import ch.ethz.inf.vs.talosmodule.main.TalosResult;
import ch.ethz.inf.vs.talosmodule.main.User;
import ch.ethz.inf.vs.talsomoduleapp.AppUtil;
import ch.ethz.inf.vs.talsomoduleapp.data.ValuesActivity;

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

public class DBStoreInterface {

    TalosModuleSensor module;

    public DBStoreInterface(TalosModuleSensor module) {
        this.module = module;
    }

    public boolean storeMeasurement(User u, SensorMeasurement measure) throws TalosModuleException {
        if(!sensorExistsInDB(u,measure.getBelongsTo()))
            storeSensorInDB(u,measure.getBelongsTo());
        return module.storeMeasurement(u,measure.getDataType(), measure.getBelongsTo().getNameID(), measure.getTimeStamp(), measure.getData());
    }

    public boolean registerUser(User u) throws TalosModuleException {
        return module.registerUser(u);
    }

    public boolean sensorExistsInDB(User u, PhoneSensor sens) throws TalosModuleException {
        TalosResult res = module.sensorExistsInDB(u, sens.getNameID());
        return res.next();
    }

    public boolean storeSensorInDB(User u, PhoneSensor sens) throws TalosModuleException{
        return module.storeSensorInDB(u, sens.getNameID(), sens.getName(), sens.getBelongsTo(), sens.getVendor(), sens.getDescription());
    }

    public List<PhoneSensor> loadSensors(User u, int max) throws TalosModuleException {
        TalosResult res = module.loadSensors(u);
        ArrayList<PhoneSensor> sensors = new ArrayList<>();
        while (res.next()) {
            PhoneSensor temp = new PhoneSensor(
                    res.getString("nameID"),
                    res.getString("name"),
                    res.getString("belongsTo"),
                    res.getString("vendor"),
                    res.getString("description"));
            sensors.add(temp);
        }
        return sensors;
    }

    public List<Pair<String,String>> loadValueTypesAndAverage(User u, PhoneSensor sens) throws TalosModuleException {
        TalosResult res = module.loadValueTypesAndAverage(u, sens.getNameID());
        ArrayList<Pair<String,String>> avges = new ArrayList<>();
        while (res.next()) {
            int sum = res.getInt("SUM(data)");
            int count = res. getInt("COUNT(data)");
            String avg = AppUtil.calculateAverage(sum, count);
            avges.add(new Pair<>(res.getString("datatype"), avg));
        }
        return avges;
    }

    public List<SensorMeasurement> loadMeasurementsWithTag(User u, PhoneSensor sens, String tag, int maxMeasurements) throws TalosModuleException {
        TalosResult res = module.loadMeasurementsWithTag(u, sens.getNameID(), tag, maxMeasurements);
        ArrayList<SensorMeasurement> measures = new ArrayList<>();
        while (res.next()) {
            SensorMeasurement temp = new SensorMeasurement(
                    res.getInt("id"),
                    res.getString("datatype"),
                    res.getString("sensorID"),
                    res.getString("timeStamp"),
                    res.getInt("data"));
            measures.add(temp);
        }
        return measures;
    }

    public ValuesActivity.ValuesMeta loadMetaOfMeasurementsWithTag(User u, PhoneSensor sens, String tag) throws TalosModuleException {
        ValuesActivity.ValuesMeta meta;
        TalosResult res = module.loadMetaOfMeasurementsWithTag(u, sens.getNameID(), tag);
        if(res.next()) {
            meta = new  ValuesActivity.ValuesMeta(res.getInt("MIN(data)"),res.getInt("MAX(data)"),res.getInt("COUNT(id)"));
            return meta;
        }
        return null;
    }


}
