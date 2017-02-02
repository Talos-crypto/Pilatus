package ch.ethz.inf.vs.talosavaapp.activities;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.DatePicker;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.YAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;


import ch.ethz.inf.vs.talosavaapp.R;
import ch.ethz.inf.vs.talosavaapp.talos.Datatype;
import ch.ethz.inf.vs.talosavaapp.talos.TalosAvaSimpleAPI;
import ch.ethz.inf.vs.talosavaapp.talos.model.DataEntryAgrDate;
import ch.ethz.inf.vs.talosmodule.exceptions.TalosModuleException;
import ch.ethz.inf.vs.talosmodule.main.SharedUser;
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

public class DataWeeklyActivity extends AppCompatActivity {

    private BarChart mChart;
    private Calendar currentCalendar;

    private TextView fromDate;
    private TextView toDate;

    private Datatype type;

    private SharedUser shareU = null;

    private ArrayList<Date> dates = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_chart);
        Intent creator = getIntent();
        type = Datatype.valueOf(creator.getStringExtra(ActivitiesUtil.DATATYPE_KEY));
        Date fixDate;
        if(creator.getExtras().containsKey(ActivitiesUtil.DETAIL_DATE_KEY)) {
            try {
                fixDate = ActivitiesUtil.titleFormat.parse(creator.getStringExtra(ActivitiesUtil.DETAIL_DATE_KEY));
            } catch (ParseException e) {
                fixDate = Calendar.getInstance().getTime();
            }
        } else {
            fixDate = Calendar.getInstance().getTime();
        }


        getSupportActionBar().setTitle(type.getDisplayRep());

        fromDate = (TextView) findViewById(R.id.fromdatainput);
        toDate = (TextView) findViewById(R.id.todateinput);

        mChart = (BarChart) findViewById(R.id.chart);
        mChart.getXAxis().setTextSize(10f);
        mChart.getXAxis().setDrawGridLines(false);
        mChart.getAxisRight().setDrawGridLines(false);
        mChart.getAxisRight().setDrawLabels(false);
        //mChart.getAxisLeft().setDrawGridLines(false);
        mChart.getAxisLeft().setValueFormatter(new YAxisStepsFormat());
        mChart.setDescription("");
        mChart.getLegend().setEnabled(false);
        mChart.setGridBackgroundColor(getResources().getColor(R.color.heavierBG));

        Intent intent = getIntent();
        if(intent.hasExtra(ActivitiesUtil.SHARED_USER_KEY)) {
            this.shareU = SharedUser.decodeFromString(intent.getStringExtra(ActivitiesUtil.SHARED_USER_KEY));
        } else if(savedInstanceState!=null && savedInstanceState.getString(ActivitiesUtil.SHARED_USER_KEY) != null) {
            this.shareU = SharedUser.decodeFromString(savedInstanceState.getString(ActivitiesUtil.SHARED_USER_KEY));
        }

        mChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, int dataSetIndex, Highlight h) {
                if(!dates.isEmpty()) {
                    Date selectedDate = dates.get(e.getXIndex());
                    Intent intent = new Intent(getApplicationContext(), DataDailyActivity.class);
                    intent.putExtra(ActivitiesUtil.DETAIL_DATE_KEY, ActivitiesUtil.titleFormat.format(selectedDate));
                    intent.putExtra(ActivitiesUtil.DATATYPE_KEY, type.name());
                    if (shareU != null) {
                        intent.putExtra(ActivitiesUtil.SHARED_USER_KEY, shareU.encodeAsString());
                    }
                    startActivity(intent);
                }

            }

            @Override
            public void onNothingSelected() {

            }

        });

        currentCalendar = Calendar.getInstance();
        currentCalendar.setTime(fixDate);
        currentCalendar.add(Calendar.DATE, -3);
        Calendar cal = Calendar.getInstance();
        final Date to = cal.getTime();
        cal.add(Calendar.DATE, -6);
        final Date from = cal.getTime();
        setDataEmpty(from, to);

        fromDate.setText(ActivitiesUtil.titleFormat.format(from));
        toDate.setText(ActivitiesUtil.titleFormat.format(to));

        loadData(this.shareU);
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if(shareU!=null)
            outState.putString(ActivitiesUtil.SHARED_USER_KEY, shareU.encodeAsString());
        super.onSaveInstanceState(outState);
    }

    private void setDataEmpty(Date from, Date to) {
        Calendar fromCal = Calendar.getInstance();
        fromCal.setTime(from);
        Calendar toCal = Calendar.getInstance();
        toCal.setTime(to);
        toCal.add(Calendar.DATE, 1);

        ArrayList<String> xVals = new ArrayList<>();
        ArrayList<BarEntry> yVals1 = new ArrayList<>();


        int index = 0;
        for (fromCal.setTime(from);fromCal.getTime().before(toCal.getTime());fromCal.add(Calendar.DATE,1)) {
            xVals.add(ActivitiesUtil.dateFormat.format(fromCal.getTime()));
            yVals1.add(new BarEntry(0, index));
            index++;
        }


        BarDataSet set1 = new BarDataSet(yVals1, "Steps");
        set1.setBarSpacePercent(35f);

        ArrayList<BarDataSet> dataSets = new ArrayList<BarDataSet>();
        set1.setColor(getResources().getColor(R.color.lightBG));
        dataSets.add(set1);

        BarData bla = new BarData(xVals, dataSets);
        bla.setValueTextSize(10f);

        mChart.setData(bla);
        mChart.invalidate();
    }

    public void onSelectDate(View v) {
        DialogFragment newFragment = new StepsActivityDateFragment();
        newFragment.show(this.getFragmentManager(), "tag1");
    }

    private void onDataSet(Date date) {
        currentCalendar.setTime(date);
        loadData(this.shareU);
    }

    private void setData(ArrayList<DataEntryAgrDate> data, Date from, Date to) {
        ArrayList<String> xVals = new ArrayList<>();
        ArrayList<BarEntry> yVals1 = new ArrayList<>();
        HashMap<String, DataEntryAgrDate> entries = new HashMap<>();
        Calendar fromCal = Calendar.getInstance();
        Calendar toCal = Calendar.getInstance();
        toCal.setTime(to);
        toCal.add(Calendar.DATE, 1);

        for (DataEntryAgrDate entry : data) {
            entries.put(ActivitiesUtil.titleFormat.format(entry.getDate()), entry);
        }


        dates = new ArrayList<>();
        int index = 0;
        for (fromCal.setTime(from); fromCal.getTime().before(toCal.getTime()); fromCal.add(Calendar.DATE,1)) {
            String key = ActivitiesUtil.titleFormat.format(fromCal.getTime());
            if(entries.containsKey(key)) {
                xVals.add(ActivitiesUtil.dateFormat.format(entries.get(key).getDate()));
                String val = type.formatValue(entries.get(key).getValue());
                yVals1.add(new BarEntry(Float.valueOf(val), index));
            } else  {
                xVals.add(ActivitiesUtil.dateFormat.format(fromCal.getTime()));
                yVals1.add(new BarEntry(0, index));
            }
            dates.add(fromCal.getTime());
            index++;
        }


        BarDataSet set1 = new BarDataSet(yVals1, "Steps");
        set1.setBarSpacePercent(35f);

        ArrayList<BarDataSet> dataSets = new ArrayList<BarDataSet>();
        set1.setColor(getResources().getColor(R.color.lightBG));
        dataSets.add(set1);

        BarData bla = new BarData(xVals, dataSets);
        bla.setValueTextSize(10f);

        mChart.setData(bla);
        mChart.invalidate();
        mChart.animateY(2000);
    }

    private void loadData(final SharedUser sharedUser) {
        Calendar cal = Calendar.getInstance();
        final Date dateCurMedian = currentCalendar.getTime();
        cal.setTime(dateCurMedian);
        cal.add(Calendar.DATE, 3);
        final Date to = cal.getTime();
        cal.setTime(dateCurMedian);
        cal.add(Calendar.DATE, -3);
        final Date from = cal.getTime();
        final Context con = this;
        (new AsyncTask<Void, Void, ArrayList<DataEntryAgrDate>>() {
            @Override
            protected ArrayList<DataEntryAgrDate> doInBackground(Void... params) {
                User u = MainActivity.getLoggedInUser();
                TalosAvaSimpleAPI api = new TalosAvaSimpleAPI(con, u);
                try {
                    return api.getAgrDataPerDate(u, new java.sql.Date(from.getTime()), new java.sql.Date(to.getTime()), type, sharedUser);
                } catch (TalosModuleException e) {
                    e.printStackTrace();
                   return null;
                }
            }

            @Override
            protected void onPostExecute(ArrayList<DataEntryAgrDate> s) {
                super.onPostExecute(s);
                if(s!=null) {
                    setData(s, from, to);
                    fromDate.setText(ActivitiesUtil.titleFormat.format(from));
                    toDate.setText(ActivitiesUtil.titleFormat.format(to));
                }
            }
        }).execute();
    }


    class YAxisStepsFormat implements YAxisValueFormatter {

        @Override
        public String getFormattedValue(float value, YAxis yAxis) {
            return String.valueOf((int) value);
        }
    }

    public static class StepsActivityDateFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {

        private DataWeeklyActivity attached;

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            attached = (DataWeeklyActivity) activity;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Calendar c = Calendar.getInstance();
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);
            int year = c.get(Calendar.YEAR);

            return new DatePickerDialog(getActivity(),this,year,month,day);
        }

        @Override
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            Calendar cur = Calendar.getInstance();
            cur.set(year, monthOfYear, dayOfMonth);
            Date date = cur.getTime();
            attached.onDataSet(date);
        }
    }
}
