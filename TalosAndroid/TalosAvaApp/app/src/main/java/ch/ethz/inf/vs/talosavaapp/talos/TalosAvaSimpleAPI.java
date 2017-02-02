package ch.ethz.inf.vs.talosavaapp.talos;

import android.content.Context;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import ch.ethz.inf.vs.talosavaapp.R;
import ch.ethz.inf.vs.talosavaapp.activities.CloudActivityOverview;
import ch.ethz.inf.vs.talosavaapp.avadata.AvaDataEntry;
import ch.ethz.inf.vs.talosavaapp.avadata.AvaOvalDataEntry;
import ch.ethz.inf.vs.talosavaapp.talos.model.DataEntry;
import ch.ethz.inf.vs.talosavaapp.talos.model.DataEntryAgrDate;
import ch.ethz.inf.vs.talosavaapp.talos.model.DataEntryAgrTime;
import ch.ethz.inf.vs.talosavaapp.talos.model.DataOval;
import ch.ethz.inf.vs.talosmodule.exceptions.TalosModuleException;
import ch.ethz.inf.vs.talosmodule.main.SharedUser;
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

public class TalosAvaSimpleAPI {
    private TalosAvaAPI api;

    public TalosAvaSimpleAPI(Context con, User u) {
        api = new TalosAvaAPI(con, con.getString(R.string.TalosCloudAddr),
                Integer.valueOf(con.getString(R.string.TalosCloudPORT)), u);
    }

    public TalosAvaAPI getApi() {
        return api;
    }

    public boolean insertDataset(User u, AvaDataEntry entry) throws TalosModuleException {
        Calendar mydate = Calendar.getInstance();
        mydate.setTimeInMillis(((long) entry.time_stamp)*1000);
        java.util.Date utilDate = mydate.getTime();
        Date curDate = new java.sql.Date(utilDate.getTime());
        Time time = new Time(curDate.getTime());
        boolean a = api.insertDataset(u, curDate, time, Datatype.AmbTemp.name(), entry.temp_amb);
        boolean b = api.insertDataset(u, curDate, time, Datatype.SkinTemp.name(), entry.temp_skin);
        boolean c = api.insertDataset(u, curDate, time, Datatype.RestingPulseRate.name(), entry.avg_bpm);
        boolean d = api.insertDataset(u, curDate, time, Datatype.getSleepDeep(), entry.sleep_state_deep);
        boolean e = api.insertDataset(u, curDate, time, Datatype.getSleepLow(), entry.sleep_state_low);
        return a || b || c || d || e;
    }

    public boolean insertBatchDataset(User u, List<AvaDataEntry> entries) throws TalosModuleException {
        List<InsertValues> values = new ArrayList<>();
        for(AvaDataEntry entry : entries) {
            Calendar mydate = Calendar.getInstance();
            mydate.setTimeInMillis(((long) entry.time_stamp) * 1000);
            java.util.Date utilDate = mydate.getTime();
            Date curDate = new java.sql.Date(utilDate.getTime());
            Time time = new Time(curDate.getTime());
            values.add(new InsertValues(curDate, time, Datatype.AmbTemp.name(), entry.temp_amb));
            values.add(new InsertValues(curDate, time, Datatype.SkinTemp.name(), entry.temp_skin));
            values.add(new InsertValues(curDate, time, Datatype.RestingPulseRate.name(), entry.avg_bpm));
            values.add(new InsertValues(curDate, time, Datatype.getSleepDeep(), entry.sleep_state_deep));
            values.add(new InsertValues(curDate, time, Datatype.getSleepLow(), entry.sleep_state_low));
        }
        return api.insertBatchDataset(u, values);
    }

    public boolean insertDatasetOval(User u, AvaOvalDataEntry entry) throws TalosModuleException {
        Date curDate = new java.sql.Date(entry.time.getTime());
        Time time = new Time(entry.time.getTime());
        boolean a = api.insertDatasetWithSet(u, curDate, time, Datatype.OvalHr.name(), entry.hr, entry.setID);
        boolean b = api.insertDatasetWithSet(u, curDate, time, Datatype.OvalTemp.name(), entry.skinTemp, entry.setID);
        return a || b;
    }

    public List<Date> getDates(User u, SharedUser sharedUser) throws TalosModuleException {
        TalosResult res = api.getMostActualDate(u, sharedUser);
        ArrayList<Date> data = new ArrayList<>();
        while (res.next()) {
            data.add(Date.valueOf(res.getString("date")));
        }
        return data;
    }

    public ArrayList<DataEntryAgrDate> getAgrDataPerDate(User u, Date from, Date to, Datatype type, SharedUser sharedUser) throws TalosModuleException {
        TalosResult res = api.getValuesByDay(u, from, to, type.getDisplayRep(), sharedUser);
        ArrayList<DataEntryAgrDate> data = new ArrayList<>();
        while (res.next()) {
            data.add(DataEntry.createFromTalosResult(res, type));
        }
        return data;
    }

    public ArrayList<DataEntryAgrTime> getAgrDataForDate(User u, Date curDate, Datatype type, SharedUser sharedUser) throws TalosModuleException {
        TalosResult res = api.agrDailySummary(u, curDate, type.getDisplayRep(), 30, sharedUser);
        ArrayList<DataEntryAgrTime> data = new ArrayList<>();
        while (res.next()) {
            data.add(DataEntry.createFromTalosResultTime(res, type));
        }
        return data;
    }

    public ArrayList<CloudActivityOverview.CloudListItem> getCloudListItems(User u, Date today, SharedUser sharedUser) throws TalosModuleException {
        ArrayList<CloudActivityOverview.CloudListItem> items = new ArrayList<>();
        HashMap<Datatype, CloudActivityOverview.CloudListItem> mappings = new HashMap<>();
        TalosResult res = api.getValuesDuringDay(u, today, sharedUser);
        List<Integer> sleepSum= new ArrayList<>();
        while (res.next()) {
            String datatypeStr = res.getString("datatype");
            int numVals = res.getInt("SUM(data)");
            int max= res.getInt("COUNT(data)");
            if(datatypeStr.equals(Datatype.getSleepDeep()) || datatypeStr.equals(Datatype.getSleepLow())) {
                sleepSum.add(numVals);
            } else {
                Datatype type = Datatype.valueOf(datatypeStr);
                if(type.isOval())
                    continue;
                numVals = Datatype.performAVG(type, numVals, max);
                mappings.put(type, new CloudActivityOverview.CloudListItem(type, type.formatValue(numVals)));
            }
        }

        if(!sleepSum.isEmpty()) {
            int sum = 0;
            for(Integer sleep : sleepSum) {
                sum += sleep;
            }
            sum *= 10;
            mappings.put(Datatype.Sleep, new CloudActivityOverview.CloudListItem(Datatype.Sleep, Datatype.Sleep.formatValue(sum)));
        }

        for(Datatype in : Datatype.values()) {
            if(in.isOval())
                continue;
            if(mappings.containsKey(in)) {
                items.add(mappings.get(in));
            } else {
                items.add(new CloudActivityOverview.CloudListItem(in, in.formatValue(0)));
            }
        }
        return items;
    }

    public ArrayList<DataOval> getDataForSet(User u, int setID, SharedUser sharedUser) throws TalosModuleException {
        TalosResult res = api.getDataForSet(u, sharedUser, setID);
        ArrayList<DataOval> data = new ArrayList<>();
        while (res.next()) {
            data.add(DataEntry.createFromTalosResultTimeOval(res));
        }
        return data;
    }

    public List<Integer> getAvailableSets(User u, SharedUser sharedUser) throws TalosModuleException {
        TalosResult res = api.getSets(u, sharedUser);
        ArrayList<Integer> data = new ArrayList<>();
        while (res.next()) {
            if(!(res.getInt("setid")==0))
                data.add(res.getInt("setid"));
        }
        return data;
    }

}
