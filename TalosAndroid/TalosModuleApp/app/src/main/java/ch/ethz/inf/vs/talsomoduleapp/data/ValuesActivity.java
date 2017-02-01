package ch.ethz.inf.vs.talsomoduleapp.data;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;

import java.util.List;

import ch.ethz.inf.vs.talosmodule.exceptions.TalosModuleException;
import ch.ethz.inf.vs.talsomoduleapp.AppUtil;
import ch.ethz.inf.vs.talsomoduleapp.DBInterfaceHolder;
import ch.ethz.inf.vs.talsomoduleapp.MainActivity;
import ch.ethz.inf.vs.talsomoduleapp.R;
import ch.ethz.inf.vs.talsomoduleapp.dbmodel.DBStoreInterface;
import ch.ethz.inf.vs.talsomoduleapp.dbmodel.PhoneSensor;
import ch.ethz.inf.vs.talsomoduleapp.dbmodel.SensorMeasurement;

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

public class ValuesActivity extends ActionBarActivity {

    //spublic final static String DATA_TYPE_TAG = "dataType";

    private static final int NUM_VALUES = 20;

    private ExpandableListAdapter adapter;
    private ExpandableListView expandableListView;
    private TextView valuesCount;
    private TextView maxVal;
    private TextView minVal;

    private PhoneSensor sensor;
    private String dataType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_values);
        TextView title = (TextView) findViewById(R.id.titleValues);
        expandableListView = (ExpandableListView) findViewById(R.id.expandableListView);
        sensor = new PhoneSensor(getIntent().getExtras());
        dataType = getIntent().getType();
        valuesCount = (TextView) findViewById(R.id.valuesCount);
        maxVal = (TextView) findViewById(R.id.max);
        minVal = (TextView) findViewById(R.id.min);
        title.setText(dataType);

        new LoadValuesDBTask().execute();
    }

    private class LoadValuesDBTask extends AsyncTask<Void, Void,List<SensorMeasurement>> {

        @Override
        protected List<SensorMeasurement> doInBackground(Void... value) {
            DBStoreInterface store = DBInterfaceHolder.getCon();
            List<SensorMeasurement> res = null;
            try {
                res = store.loadMeasurementsWithTag(MainActivity.getLoggedInUser(), sensor, dataType, NUM_VALUES);
            } catch (TalosModuleException e) {
                e.printStackTrace();
            }
            return res;
        }

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected void onPostExecute(List<SensorMeasurement> res) {
            adapter = new ValuesAdapter(ValuesActivity.this.getApplicationContext(),res);
            expandableListView.setAdapter(adapter);
            new LoadValuesCountDBTask().execute();
        }
    }

    private class LoadValuesCountDBTask extends AsyncTask<Void, Void, ValuesMeta> {

        @Override
        protected ValuesMeta doInBackground(Void... value) {
            DBStoreInterface store = DBInterfaceHolder.getCon();
            ValuesMeta res = null;
            try {
                res = store.loadMetaOfMeasurementsWithTag(MainActivity.getLoggedInUser(), sensor, dataType);
            } catch (TalosModuleException e) {
                e.printStackTrace();
            }
            return res;
        }

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected void onPostExecute(ValuesMeta res) {
            int count = expandableListView.getAdapter().getCount();
            if(count>NUM_VALUES)
                count = NUM_VALUES;
            valuesCount.setText("Num Values: " + String.valueOf(count) + "/" + String.valueOf(res.count));
            maxVal.setText(AppUtil.setPoint(String.valueOf(res.max)));
            minVal.setText(AppUtil.setPoint(String.valueOf(res.min)));
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_values, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == android.R.id.home) {
            Intent myIntent = new Intent(this, DataSensorActivity.class);
            myIntent.putExtras(sensor.getBundle());
            startActivity(myIntent);
            return true;
        } else if(id == R.id.action_settings) {
            Intent myIntent = new Intent(ValuesActivity.this, MainActivity.class);
            startActivity(myIntent);
        }

        return super.onOptionsItemSelected(item);
    }

    public static class ValuesMeta {
        public int min;
        public int max;
        public int count;

        public ValuesMeta(int min, int max, int count) {
            this.min = min;
            this.max = max;
            this.count = count;
        }
    }
}
