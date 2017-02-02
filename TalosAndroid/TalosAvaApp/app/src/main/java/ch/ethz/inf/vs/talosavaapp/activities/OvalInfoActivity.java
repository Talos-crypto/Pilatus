package ch.ethz.inf.vs.talosavaapp.activities;

import ch.ethz.inf.vs.talosavaapp.R;
import ch.ethz.inf.vs.talosavaapp.avadata.DefaultOvalCompute;
import ch.ethz.inf.vs.talosavaapp.avadata.OvalComputation;
import ch.ethz.inf.vs.talosavaapp.talos.Datatype;
import ch.ethz.inf.vs.talosavaapp.talos.TalosAvaSimpleAPI;
import ch.ethz.inf.vs.talosavaapp.talos.model.DataOval;
import ch.ethz.inf.vs.talosmodule.exceptions.TalosModuleException;
import ch.ethz.inf.vs.talosmodule.main.SharedUser;
import ch.ethz.inf.vs.talosmodule.main.User;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.Date;
import java.util.ArrayList;
import java.util.List;

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

public class OvalInfoActivity extends AppCompatActivity {

    private LineChart ovalChart;
    private SharedUser shareU = null;

    private ImageButton lbutton;
    private ImageButton rbutton;

    private TextView title;
    private TextView ovalPredVal;

    private List<Integer> sets = new ArrayList<>();
    private int curPosition = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_oval_info);

        lbutton = (ImageButton) findViewById(R.id.lovalbutton);
        rbutton = (ImageButton) findViewById(R.id.rovalbutton);
        title = (TextView) findViewById(R.id.ovalTitle);
        ovalPredVal = (TextView) findViewById(R.id.predictedDate);
        ovalPredVal.setText("");

        lbutton.setVisibility(View.INVISIBLE);
        rbutton.setVisibility(View.INVISIBLE);
        title.setVisibility(View.INVISIBLE);

        ovalChart = (LineChart) findViewById(R.id.linechartOval);
        ovalChart.getAxisRight().setEnabled(false);
        ovalChart.setClickable(false);
        XAxis xAxis = ovalChart.getXAxis();
        xAxis.setEnabled(true);
        //xAxis.setValueFormatter(new XValFormatter());
        xAxis.setPosition(XAxis.XAxisPosition.TOP);
        xAxis.setTextSize(10f);
        xAxis.setTextColor(Color.BLACK);
        xAxis.setDrawAxisLine(true);
        xAxis.setDrawLabels(true);
        xAxis.setDrawGridLines(false);
        xAxis.setAvoidFirstLastClipping(true);
        xAxis.setLabelsToSkip(4);
        ovalChart.setDescription("");
        ovalChart.getLegend().setEnabled(false);
        ovalChart.setGridBackgroundColor(getResources().getColor(R.color.lightBG));
        ovalChart.setVisibility(View.INVISIBLE);

        Intent intent = getIntent();
        if(intent.hasExtra(ActivitiesUtil.SHARED_USER_KEY)) {
            this.shareU = SharedUser.decodeFromString(intent.getStringExtra(ActivitiesUtil.SHARED_USER_KEY));
        }


        loadSets(this.shareU);


    }

    private void loadSets(final SharedUser sharedUser) {
        final Context con = this;
        (new AsyncTask<Void, Void, List<Integer>>() {
            @Override
            protected List<Integer> doInBackground(Void... params) {
                User u = MainActivity.getLoggedInUser();
                TalosAvaSimpleAPI api = new TalosAvaSimpleAPI(con, MainActivity.getLoggedInUser());
                try {
                    return api.getAvailableSets(u, sharedUser);
                } catch (TalosModuleException e) {
                    return null;
                }
            }

            @Override
            protected void onPostExecute(List<Integer> items) {
                super.onPostExecute(items);
                sets = items;
                curPosition = 0;
                title.setVisibility(View.VISIBLE);
                if(items.isEmpty()) {
                    title.setText("No Data");
                } else {
                    title.setText("Set " + items.get(0));
                    loadData(sharedUser, items.get(0));
                }

                if(items.size()>1)
                    rbutton.setVisibility(View.VISIBLE);

            }
        }).execute();
    }

    private static List<Double> extractTemperatureData(List<DataOval> data) {
        List<Double> result = new ArrayList<>();
        for(DataOval item : data) {
            Datatype type = Datatype.valueOf(item.getType());
            if(type.equals(Datatype.OvalTemp)) {
                result.add(Double.valueOf(type.formatValue(item.getValue())));
            }
        }
        return result;
    }

    private static List<Date> extractDateTemperatureData(List<DataOval> data) {
        List<Date> result = new ArrayList<>();
        for(DataOval item : data) {
            Datatype type = Datatype.valueOf(item.getType());
            if(type.equals(Datatype.OvalTemp)) {
                result.add(item.getDate());
            }
        }
        return result;
    }

    private synchronized void loadData(final SharedUser sharedUser, final int set) {
        final Context con = this;
        (new AsyncTask<Void, Void, ArrayList<DataOval>>() {
            @Override
            protected ArrayList<DataOval> doInBackground(Void... params) {
                User u = MainActivity.getLoggedInUser();
                final TalosAvaSimpleAPI api = new TalosAvaSimpleAPI(con, u);
                try {
                    return api.getDataForSet(u, set, sharedUser);
                } catch (TalosModuleException e) {
                    e.printStackTrace();
                }
                return null;

            }

            @Override
            protected void onPostExecute(ArrayList<DataOval> items) {
                super.onPostExecute(items);
                List<Double> temps = extractTemperatureData(items);
                List<Date> dates = extractDateTemperatureData(items);
                computeOval(temps, dates);
                plotValues(temps, dates);
            }
        }).execute();
    }

    private void computeOval(List<Double> temps, List<Date> dates) {
        OvalComputation compute = new DefaultOvalCompute();
        Date date = dates.get(compute.computeOval(temps));
        ovalPredVal.setText(ActivitiesUtil.titleFormat.format(date));
    }

    private void plotValues(List<Double> temps, List<Date> dates) {
        ovalChart.setVisibility(View.VISIBLE);
        ArrayList<String> xVals = new ArrayList<String>();
        ArrayList<Entry> yVals = new ArrayList<Entry>();
        for(int index=0; index<temps.size(); index ++) {
            Date time = dates.get(index);
            xVals.add(ActivitiesUtil.ovalPlotFormat.format(time));
            yVals.add(new Entry(temps.get(index).floatValue(), index));
        }

        LineDataSet set1 = new LineDataSet(yVals, "DataSet2");
        setLineLayout(set1);

        ArrayList<LineDataSet> dataSets = new ArrayList<>();
        dataSets.add(set1);
        LineData data = new LineData(xVals, dataSets);
        ovalChart.setData(data);
        ovalChart.animateX(3000);
        ovalChart.getAxisLeft().setStartAtZero(false);
        ovalChart.getAxisLeft().setAxisMaxValue(38f);
        ovalChart.getAxisLeft().setAxisMinValue(34f);
        ovalChart.getAxisRight().setStartAtZero(false);
        ovalChart.getAxisRight().setAxisMaxValue(38f);
        ovalChart.getAxisRight().setAxisMinValue(34f);
        ovalChart.invalidate();

    }

    private void setLineLayout(LineDataSet set1) {
        set1.setDrawValues(false);
        set1.setColor(getResources().getColor(R.color.heavierBG));
        set1.setCircleColor(getResources().getColor(R.color.heavierBG));
        set1.setCircleColorHole(getResources().getColor(R.color.lightBG));
        set1.setLineWidth(3f);
        set1.setCircleSize(4f);

    }

    private final Object mutex = new Object();

    public void onLeftClick(View v) {
        synchronized (mutex) {
            int temp = curPosition - 1;
            if (temp >= 0) {
                curPosition--;
                loadData(this.shareU, sets.get(curPosition));
                title.setText("Set " + sets.get(curPosition));
                if(curPosition==0)
                    lbutton.setVisibility(View.INVISIBLE);
                if(curPosition==sets.size()-2) {
                    rbutton.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    public void onRightClick(View v) {
        synchronized (mutex) {
            int temp = curPosition + 1;
            if (temp < sets.size()) {
                curPosition++;
                loadData(this.shareU, sets.get(curPosition));
                title.setText("Set " + sets.get(curPosition));
                if(curPosition==sets.size()-1)
                    rbutton.setVisibility(View.INVISIBLE);
                if(curPosition==1) {
                    lbutton.setVisibility(View.VISIBLE);
                }
            }
        }
    }
}
