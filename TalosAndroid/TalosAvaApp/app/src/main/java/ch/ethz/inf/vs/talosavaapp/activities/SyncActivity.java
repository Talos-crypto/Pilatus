package ch.ethz.inf.vs.talosavaapp.activities;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import ch.ethz.inf.vs.talosavaapp.R;
import ch.ethz.inf.vs.talosavaapp.util.Synchronizer;
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

public class SyncActivity extends AppCompatActivity {

    private Synchronizer synchronizer;

    int curSelectedFile = 1;
    int curSelectedSet = 1;

    private ProgressBar bar;
    private TextView syncOk;
    private Button onsyncDef;
    private Button onsyncOval;
    private Spinner spinner;
    private Spinner spinnerOval;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync_actibity);

        synchronizer = new Synchronizer(this, MainActivity.getLoggedInUser());

        spinner = (Spinner) findViewById(R.id.spinner);
        spinnerOval = (Spinner) findViewById(R.id.spinnerOval);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.data_files, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(0);
        spinner.setOnItemSelectedListener(new SyncDropDownListerner());

        ArrayAdapter<CharSequence> adapterOval = ArrayAdapter.createFromResource(this,
                R.array.oval_files, android.R.layout.simple_spinner_item);
        adapterOval.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerOval.setAdapter(adapterOval);
        spinnerOval.setSelection(0);
        spinnerOval.setOnItemSelectedListener(new OvalDropDownListerner());

        bar = (ProgressBar) findViewById(R.id.progressBar2);
        onsyncDef = (Button) findViewById(R.id.button2);
        onsyncOval = (Button) findViewById(R.id.ovalData);

        syncOk = (TextView) findViewById(R.id.syncok);
        syncOk.setVisibility(View.INVISIBLE);
        bar.setVisibility(View.INVISIBLE);

    }

    public void onSync(View v) {
        syncOk.setVisibility(View.INVISIBLE);
        onsyncOval.setVisibility(View.INVISIBLE);
        spinnerOval.setVisibility(View.INVISIBLE);
        (new AsyncTask<Void,Integer,Integer>() {

            private int progressMax = 1;

            @Override
            protected Integer doInBackground(Void... params) {
                User user = MainActivity.getLoggedInUser();
                try {
                    synchronizer.transferDataFromFile(user, curSelectedFile, 100);
                } catch (TalosModuleException e) {
                    e.printStackTrace();
                    return 0;
                }
                return 1;
            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                bar.setVisibility(View.VISIBLE);
            }

            @Override
            protected void onPostExecute(Integer s) {
                super.onPostExecute(s);
                bar.setVisibility(View.INVISIBLE);
                syncOk.setVisibility(View.VISIBLE);
                onsyncOval.setVisibility(View.VISIBLE);
                spinnerOval.setVisibility(View.VISIBLE);
                if(s==1) {
                    syncOk.setText("Success");
                } else {
                    syncOk.setText("Failed");
                }
            }
        }).execute();
    }


    public void onOvalSync(View v) {
        syncOk.setVisibility(View.INVISIBLE);
        onsyncDef.setVisibility(View.INVISIBLE);
        spinner.setVisibility(View.INVISIBLE);
        (new AsyncTask<Void,Integer,Integer>() {

            private int progressMax = 1;

            @Override
            protected Integer doInBackground(Void... params) {
                User user = MainActivity.getLoggedInUser();
                try {
                    synchronizer.transferOvaDataFromFile(user, curSelectedSet);
                } catch (TalosModuleException e) {
                    e.printStackTrace();
                    return 0;
                }
                return 1;
            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                bar.setVisibility(View.VISIBLE);
            }

            @Override
            protected void onPostExecute(Integer s) {
                super.onPostExecute(s);
                bar.setVisibility(View.INVISIBLE);
                syncOk.setVisibility(View.VISIBLE);
                onsyncDef.setVisibility(View.VISIBLE);
                spinner.setVisibility(View.VISIBLE);
                if(s==1) {
                    syncOk.setText("Success");
                } else {
                    syncOk.setText("Failed");
                }
            }
        }).execute();
    }

    private class SyncDropDownListerner implements AdapterView.OnItemSelectedListener {

        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
            curSelectedFile = i+1;
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {

        }
    }

    private class OvalDropDownListerner implements AdapterView.OnItemSelectedListener {

        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
            curSelectedSet = i+1;
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {

        }
    }
}
