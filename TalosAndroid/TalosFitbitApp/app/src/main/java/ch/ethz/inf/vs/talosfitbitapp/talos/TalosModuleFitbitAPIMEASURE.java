package ch.ethz.inf.vs.talosfitbitapp.talos;

import android.content.Context;
import android.util.Log;

import com.google.common.base.Stopwatch;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import ch.ethz.inf.vs.talosfitbitapp.activities.CloudSelectActivity;
import ch.ethz.inf.vs.talosfitbitapp.fitbitapi.model.CaloriesQuery;
import ch.ethz.inf.vs.talosfitbitapp.fitbitapi.model.DistQuery;
import ch.ethz.inf.vs.talosfitbitapp.fitbitapi.model.FloorQuery;
import ch.ethz.inf.vs.talosfitbitapp.fitbitapi.model.StepsQuery;
import ch.ethz.inf.vs.talosfitbitapp.talos.model.DataEntryAgrDate;
import ch.ethz.inf.vs.talosfitbitapp.talos.model.DataEntryAgrTime;
import ch.ethz.inf.vs.talosmodule.exceptions.TalosModuleException;
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

public class TalosModuleFitbitAPIMEASURE extends TalosModuleFitbitAPI {

    private Stopwatch watch = Stopwatch.createUnstarted();
    private static final String MEASURE_TAG = "MEASURE";

    public TalosModuleFitbitAPIMEASURE(Context conn) {
        super(conn);
        //Log.i(MEASURE_TAG, "#----------------*Start Measuring*----------------#");
    }

    private void startWatch() {
        watch.reset();
        watch.start();
    }

    private void stopWatchLog(String methodName) {
        watch.stop();
        long time = watch.elapsed(TimeUnit.NANOSECONDS);
        Log.i(MEASURE_TAG, methodName + ";" + time);
    }

    private void stopWatchLog(String methodName, int numInserts) {
        watch.stop();
        long time = watch.elapsed(TimeUnit.NANOSECONDS);
        BigDecimal temp = BigDecimal.valueOf(time);
        if(numInserts!=0) {
            temp = temp.divide(BigDecimal.valueOf(numInserts), 2, RoundingMode.HALF_UP);
            Log.i(MEASURE_TAG, methodName + ";"+temp.toString());
        }
    }

    @Override
    public void storeData(User u, StepsQuery steps) throws TalosModuleException {
        int numInserts = steps.activitiesStepsIntraday.getDataset().size();
        startWatch();
        super.storeData(u, steps);
        stopWatchLog("storeData", numInserts);
    }

    @Override
    public void storeData(User u, FloorQuery floors) throws TalosModuleException {
        int numInserts = floors.activitiesFloorsIntraday.getDataset().size();
        startWatch();
        super.storeData(u, floors);
        stopWatchLog("storeData", numInserts);
    }

    @Override
    public void storeData(User u, CaloriesQuery calories) throws TalosModuleException {
        int numInserts = calories.activitiesCaloriesIntraday.getDataset().size();
        startWatch();
        super.storeData(u, calories);
        stopWatchLog("storeData", numInserts);
    }

    @Override
    public void storeData(User u, DistQuery dist) throws TalosModuleException {
        int numInserts = dist.activitiesDistanceIntraday.getDataset().size();
        startWatch();
        super.storeData(u, dist);
        stopWatchLog("storeData", numInserts);
    }

    @Override
    public ArrayList<DataEntryAgrDate> getAgrDataPerDate(User u, Date from, Date to, Datatype type) throws TalosModuleException {
        startWatch();
        ArrayList<DataEntryAgrDate> res = super.getAgrDataPerDate(u, from, to, type);
        stopWatchLog("getAgrDataPerDate");
        return res;
    }

    @Override
    public ArrayList<DataEntryAgrTime> getAgrDataForDate(User u, Date curDate, Datatype type) throws TalosModuleException {
        startWatch();
        ArrayList<DataEntryAgrTime> res = super.getAgrDataForDate(u, curDate, type);
        stopWatchLog("getAgrDataForDate");
        return  res;
    }

    @Override
    public ArrayList<CloudSelectActivity.CloudListItem> getCloudListItems(User u, Date today) throws TalosModuleException {
        startWatch();
        ArrayList<CloudSelectActivity.CloudListItem> res = super.getCloudListItems(u, today);
        stopWatchLog("getCloudListItems");
        return res;
    }
}
