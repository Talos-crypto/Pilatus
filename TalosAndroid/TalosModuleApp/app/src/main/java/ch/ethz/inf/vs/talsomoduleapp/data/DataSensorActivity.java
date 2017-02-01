package ch.ethz.inf.vs.talsomoduleapp.data;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;

import ch.ethz.inf.vs.talosmodule.exceptions.TalosModuleException;
import ch.ethz.inf.vs.talsomoduleapp.DBInterfaceHolder;
import ch.ethz.inf.vs.talsomoduleapp.MainActivity;
import ch.ethz.inf.vs.talsomoduleapp.R;
import ch.ethz.inf.vs.talsomoduleapp.dbmodel.DBStoreInterface;
import ch.ethz.inf.vs.talsomoduleapp.dbmodel.PhoneSensor;

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

public class DataSensorActivity extends ActionBarActivity {

    private Context context;

    private TextView sensorName;
    private TextView vendorName;
    private TextView idName;
    private TextView description;
    private TextView belongsTo;
    private ProgressBar progressBar;

    private ListView listView;
    private ListAdapter adapter;

    private List<Pair<String,String>> typesAverages;

    private PhoneSensor sensor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_sensor);
        sensorName = (TextView) findViewById(R.id.nameData);
        vendorName = (TextView) findViewById(R.id.vendorData);
        idName = (TextView) findViewById(R.id.idData);
        description = (TextView) findViewById(R.id.descriptionData);
        belongsTo = (TextView) findViewById(R.id.belongData);
        listView = (ListView) findViewById(R.id.listView2);
        progressBar = (ProgressBar) findViewById(R.id.progressBar3);
        progressBar.setVisibility(View.INVISIBLE);
        if(savedInstanceState==null)
            sensor = new PhoneSensor(getIntent().getExtras());
        else
            sensor = new PhoneSensor(savedInstanceState);
        context = this.getApplicationContext();

        // Set Sensor values
        sensorName.setText(sensor.getName());
        vendorName.setText(sensor.getVendor());
        idName.setText(sensor.getNameID());
        description.setText(sensor.getDescription());
        belongsTo.setText(sensor.getBelongsTo());

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                Intent myIntent = new Intent(DataSensorActivity.this, ValuesActivity.class);
                myIntent.putExtras(sensor.getBundle());
                myIntent.setType(typesAverages.get(position).first);
                startActivity(myIntent);

            }
        });

        new LoadSensorsDBTask().execute();
    }

    private class LoadSensorsDBTask extends AsyncTask<Void, Void,List<Pair<String,String>>> {

        @Override
        protected List<Pair<String,String>> doInBackground(Void... value) {
            DBStoreInterface store = DBInterfaceHolder.getCon();
            List<Pair<String,String>> res = null;
            try {
                res = store.loadValueTypesAndAverage(MainActivity.getLoggedInUser(), sensor);
            } catch (TalosModuleException e) {
                e.printStackTrace();
            }
            return res;
        }

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setProgress(0);
        }

        @Override
        protected void onPostExecute(List<Pair<String,String>> res) {
            progressBar.setVisibility(View.INVISIBLE);
            adapter = new TypeValDataAdapter(context,res.toArray(new Pair[res.size()]));
            listView.setAdapter(adapter);
            typesAverages = res;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putAll(sensor.getBundle());
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_data_sensor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent myIntent = new Intent(DataSensorActivity.this, MainActivity.class);
            startActivity(myIntent);
        }

        return super.onOptionsItemSelected(item);
    }
}
