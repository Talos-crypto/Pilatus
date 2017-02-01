package ch.ethz.inf.vs.talsomoduleapp.data;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;

import java.util.ArrayList;
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

public class DataActivity extends ActionBarActivity {

    private Context con;
    private ListAdapter mAdapt;
    private ListView mListView;
    private ProgressBar mProgress;
    private List<PhoneSensor> sensors;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data);
        mProgress = (ProgressBar) findViewById(R.id.progressBar2);
        mListView = (ListView) findViewById(R.id.listView);
        con = this.getApplicationContext();
        loadSenorsFromCloud();

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                Intent myIntent = new Intent(DataActivity.this, DataSensorActivity.class);
                myIntent.putExtras(sensors.get(position).getBundle());
                startActivity(myIntent);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_data_sensor, menu);
        return true;
    }

    private void loadSenorsFromCloud() {
        new LoadSensorsDBTask().execute();
    }


    private class LoadSensorsDBTask extends AsyncTask<Void, Void,List<PhoneSensor>> {

        @Override
        protected List<PhoneSensor> doInBackground(Void... value) {
            DBStoreInterface store = DBInterfaceHolder.getCon();
            List<PhoneSensor> sens = null;
            try {
                sens = store.loadSensors(MainActivity.getLoggedInUser(), 10);
            } catch (TalosModuleException e) {
                Log.d("StoreSens", "Error occured: " + e.getMessage());
                return new ArrayList<>();
            }
            return sens;
        }

        @Override
        protected void onPreExecute() {
            mProgress.setProgress(0);
            mProgress.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(List<PhoneSensor> res) {
            sensors = res;
            mAdapt = new SensorDataAdapter(con, res.toArray(new PhoneSensor[res.size()]));
            mProgress.setVisibility(View.INVISIBLE);
            mListView.setAdapter(mAdapt);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent myIntent = new Intent(DataActivity.this, MainActivity.class);
            startActivity(myIntent);
        }

        return super.onOptionsItemSelected(item);
    }


}
