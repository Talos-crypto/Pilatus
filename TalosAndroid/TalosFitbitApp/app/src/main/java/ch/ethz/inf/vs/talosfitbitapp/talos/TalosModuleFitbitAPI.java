package ch.ethz.inf.vs.talosfitbitapp.talos;

import android.content.Context;

import java.sql.Date;
import java.sql.Time;
import java.util.ArrayList;
import java.util.HashMap;

import ch.ethz.inf.vs.talosfitbitapp.R;
import ch.ethz.inf.vs.talosfitbitapp.activities.CloudSelectActivity;
import ch.ethz.inf.vs.talosfitbitapp.fitbitapi.model.CaloriesQuery;
import ch.ethz.inf.vs.talosfitbitapp.fitbitapi.model.Dataset;
import ch.ethz.inf.vs.talosfitbitapp.fitbitapi.model.DistQuery;
import ch.ethz.inf.vs.talosfitbitapp.fitbitapi.model.DoubleDataSet;
import ch.ethz.inf.vs.talosfitbitapp.fitbitapi.model.FloorQuery;
import ch.ethz.inf.vs.talosfitbitapp.fitbitapi.model.HeartQuery;
import ch.ethz.inf.vs.talosfitbitapp.fitbitapi.model.StepsQuery;
import ch.ethz.inf.vs.talosfitbitapp.talos.model.DataEntry;
import ch.ethz.inf.vs.talosfitbitapp.talos.model.DataEntryAgrDate;
import ch.ethz.inf.vs.talosfitbitapp.talos.model.DataEntryAgrTime;
import ch.ethz.inf.vs.talosfitbitapp.util.AppUtil;
import ch.ethz.inf.vs.talosmodule.exceptions.TalosModuleException;
import ch.ethz.inf.vs.talosmodule.main.TalosResult;
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

public class TalosModuleFitbitAPI {

    private TalosModuleFitbit module;

    public TalosModuleFitbitAPI(Context conn) {
        this.module = new TalosModuleFitbit(conn, conn.getString(R.string.TalosCloudAddr),
                Integer.valueOf(conn.getString(R.string.TalosCloudPort)));
    }

    public void storeData(User u, StepsQuery steps) throws TalosModuleException {
        String date = steps.activitiesSteps.iterator().next().getDateTime();
        java.sql.Date datesql = java.sql.Date.valueOf(date);
        for(Dataset dataSet : steps.activitiesStepsIntraday.getDataset()) {
            Time time = Time.valueOf(dataSet.getTime());
            if(dataSet.getValue()!=0) {
                module.insertDataset(u, datesql, time, Datatype.STEPS.name(), dataSet.getValue());
            }
        }
    }

    public void storeData(User u, FloorQuery floors) throws TalosModuleException {
        String date = floors.activitiesFloors.iterator().next().getDateTime();
        java.sql.Date datesql = java.sql.Date.valueOf(date);
        for(Dataset dataSet : floors.activitiesFloorsIntraday.getDataset()) {
            Time time = Time.valueOf(dataSet.getTime());
            if(dataSet.getValue()!=0) {
                module.insertDataset(u, datesql, time, Datatype.FLOORS.name(), dataSet.getValue());
            }
        }
    }

    public void storeData(User u, CaloriesQuery calories) throws TalosModuleException {
        String date = calories.activitiesCalories.iterator().next().getDateTime();
        java.sql.Date datesql = java.sql.Date.valueOf(date);
        for(DoubleDataSet dataSet : calories.activitiesCaloriesIntraday.getDataset()) {
            Time time = Time.valueOf(dataSet.getTime());
            if(dataSet.getValue()!=0) {
                int data = AppUtil.transformToInt( dataSet.getValue(), CaloriesQuery.CAL_RAD);
                module.insertDataset(u, datesql, time, Datatype.CALORIES.name(), data);
            }
        }
    }

    public void storeData(User u, DistQuery dist) throws TalosModuleException {
        String date = dist.activitiesDistance.iterator().next().getDateTime();
        java.sql.Date datesql = java.sql.Date.valueOf(date);
        for(DoubleDataSet dataSet : dist.activitiesDistanceIntraday.getDataset()) {
            Time time = Time.valueOf(dataSet.getTime());
            if(dataSet.getValue()!=0) {
                int data = AppUtil.transformToInt( dataSet.getValue(), DistQuery.DIST_RAD);
                module.insertDataset(u, datesql, time, Datatype.DISTANCE.name(), data);
            }
        }
    }

    public void storeData(User u, HeartQuery heartQuery) throws TalosModuleException {
        String date = heartQuery.getDateTime();
        java.sql.Date datesql = java.sql.Date.valueOf(date);
        for(DoubleDataSet dataSet : heartQuery.getActivitiesHeartIntraday().getDataset()) {
            Time time = Time.valueOf(dataSet.getTime());
            if(dataSet.getValue()!=0) {
                int data = AppUtil.transformToInt( dataSet.getValue(), HeartQuery.DIST_RAD);
                module.insertDataset(u, datesql, time, Datatype.HEARTRATE.name(), data);
            }
        }
    }

    public ArrayList<DataEntryAgrDate> getAgrDataPerDate(User u, Date from, Date to, Datatype type) throws TalosModuleException {
        TalosResult res = module.getValuesByDay(u, from, to, type.getDisplayRep());
        ArrayList<DataEntryAgrDate> data = new ArrayList<>();
        while (res.next()) {
            data.add(DataEntry.createFromTalosResult(res, type));
        }
        return data;
    }

    public ArrayList<DataEntryAgrTime> getAgrDataForDate(User u, Date curDate, Datatype type) throws TalosModuleException {
        TalosResult res = module.agrDailySummary(u, curDate, type.getDisplayRep(), 30);
        ArrayList<DataEntryAgrTime> data = new ArrayList<>();
        while (res.next()) {
            data.add(DataEntry.createFromTalosResultTime(res, type));
        }
        return data;
    }

    public ArrayList<CloudSelectActivity.CloudListItem> getCloudListItems(User u, Date today) throws TalosModuleException {
        ArrayList<CloudSelectActivity.CloudListItem> items = new ArrayList<>();
        HashMap<Datatype, CloudSelectActivity.CloudListItem> mappings = new HashMap<>();
        TalosResult res = module.getValuesDuringDay(u, today);
        while (res.next()) {
            Datatype type = Datatype.valueOf(res.getString("datatype"));
            int numVals = res.getInt("SUM(data)");
            int max= res.getInt("COUNT(data)");
            numVals = Datatype.performAVG(type, numVals, max);
            mappings.put(type, new CloudSelectActivity.CloudListItem(type, type.formatValue(numVals)));
        }

        for(Datatype in : Datatype.values()) {
            if(mappings.containsKey(in)) {
                items.add(mappings.get(in));
            } else {
                items.add(new CloudSelectActivity.CloudListItem(in, in.formatValue(0)));
            }
        }
        return items;
    }

    public boolean registerUser(User u) throws TalosModuleException {
        return module.registerUser(u);
    }

    public java.util.Date getMostActualDate(User u) throws TalosModuleException {
        TalosResult res = module.getMostActualDate(u);
        if(res.next()) {
            String strDate = res.getString("date");
            return Date.valueOf(strDate);
        }
        return null;
    }
}
