package ch.ethz.inf.vs.talosfitbitapp.activities;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.customtabs.CustomTabsIntent;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import ch.ethz.inf.vs.talosfitbitapp.R;
import ch.ethz.inf.vs.talosfitbitapp.fitbitapi.FitbitAPI;
import ch.ethz.inf.vs.talosfitbitapp.fitbitapi.TokenInfo;
import ch.ethz.inf.vs.talosfitbitapp.util.Synchronizer;
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

public class FitbitSync extends AppCompatActivity {

    private static final String CALLBACK = "fitbittalos://logincallback";
    private static final String SCOPE = "activity%20heartrate";

    public static final String ACCESS_TOKEN_KEY = "ACC_KEY";

    private TokenInfo info = null;
    private  Synchronizer sync = null;

    private Button fromDate;
    private Button toDate;
    private boolean isFormDate = true;

    private ProgressBar bar;
    private TextView progressText;

    public int dateIndex = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fitbit_auth);
        String data = getIntent().getDataString();
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        getSupportActionBar().setTitle(getResources().getString(R.string.title_sync));

        if(data==null || !data.contains(CALLBACK)) {
            boolean auth = true;
            if(sharedPref.contains(ACCESS_TOKEN_KEY)) {
                info = TokenInfo.fromJSON(sharedPref.getString(ACCESS_TOKEN_KEY, ""));
                auth = !info.isValid();
                if(auth) {
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.remove(ACCESS_TOKEN_KEY);
                    editor.apply();
                }
            }

            if(auth) {
                Uri uri = FitbitAPI.getAccessTokenURI(getString(R.string.fitbit_client_id), SCOPE, CALLBACK);
                CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder().setToolbarColor(Color.GREEN).build();
                customTabsIntent.launchUrl(FitbitSync.this, uri);
            }

        } else {
            Uri uri = Uri.parse(data);
            try {
                info = TokenInfo.fromURI(uri);
            } catch (Exception e) {
                setResult(RESULT_CANCELED);
                finish();
            }
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(ACCESS_TOKEN_KEY, info.toString());
            editor.apply();
        }

        sync = new Synchronizer(this, info);

        Calendar c = Calendar.getInstance();
        fromDate = (Button) findViewById(R.id.fromDateSelect);
        toDate = (Button) findViewById(R.id.toDateSelect);
        bar = (ProgressBar) findViewById(R.id.progressBar);
        progressText = (TextView) findViewById(R.id.progress);
        Date to = c.getTime();
        Date from = getFromDate();
        fromDate.setText(ActivitiesUtil.titleFormat.format(from));
        toDate.setText(ActivitiesUtil.titleFormat.format(to));
        bar.setVisibility(View.INVISIBLE);
        progressText.setVisibility(View.INVISIBLE);
    }

    private Date getFromDate() {
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        if(sharedPref.contains(ActivitiesUtil.SHARED_PREF_LAST_DOWNLOAD_KEY)) {
            try {
                return ActivitiesUtil.titleFormat.parse(sharedPref.getString(ActivitiesUtil.SHARED_PREF_LAST_DOWNLOAD_KEY, ""));
            } catch (ParseException e) {
                return Calendar.getInstance().getTime();
            }
        }
        return Calendar.getInstance().getTime();
    }

    private void storeToDate(Date date) {
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = sharedPref.edit();
        edit.putString(ActivitiesUtil.SHARED_PREF_LAST_DOWNLOAD_KEY, ActivitiesUtil.titleFormat.format(date));
        edit.apply();
    }

    public void onDataSet(Date date) {
        if(isFormDate) {
            fromDate.setText(ActivitiesUtil.titleFormat.format(date));
        } else {
            toDate.setText(ActivitiesUtil.titleFormat.format(date));
        }
    }

    private static Date getDate(TextView dateView) {
        String res = dateView.getText().toString();
        try {
            return ActivitiesUtil.titleFormat.parse(res);
        } catch (ParseException e) {
            return null;
        }

    }

    public void onFromDateSelect(View v) {
        isFormDate = true;
        DialogFragment newFragment = new FitbitSyncDatePicker();
        newFragment.show(this.getFragmentManager(), "tag1");
    }

    public void onToDateSelect(View v) {
        isFormDate = false;
        DialogFragment newFragment = new FitbitSyncDatePicker();
        newFragment.show(this.getFragmentManager(), "tag2");
    }


    public synchronized void onSyncPressed(View view) {
        int numJobs = 5;
        Date from = getDate(fromDate);
        Date to = getDate(toDate);
        final AtomicInteger curCount = new AtomicInteger(0);
        AtomicInteger semCountIIn = new AtomicInteger(0);
        AtomicInteger semCountOut = new AtomicInteger(numJobs);
        (new SyncTask(from, to, curCount, semCountIIn, semCountOut, new SnycJob() {
            @Override
            public void runSync(User u, Date date) throws TalosModuleException {
                sync.transferDataStepFromDate(u, date);
            }
        })).execute();
        (new SyncTask(from, to, curCount, semCountIIn, semCountOut, new SnycJob() {
            @Override
            public void runSync(User u, Date date) throws TalosModuleException {
                sync.transferDataCaloriesFromDate(u, date);
            }
        })).execute();
        (new SyncTask(from, to, curCount, semCountIIn, semCountOut, new SnycJob() {
            @Override
            public void runSync(User u, Date date) throws TalosModuleException {
                sync.transferDataDistanceFromDate(u, date);
            }
        })).execute();
        (new SyncTask(from, to, curCount, semCountIIn, semCountOut, new SnycJob() {
            @Override
            public void runSync(User u, Date date) throws TalosModuleException {
                sync.transferDataFloorFromDate(u, date);
            }
        })).execute();
        (new SyncTask(from, to, curCount, semCountIIn, semCountOut, new SnycJob() {
            @Override
            public void runSync(User u, Date date) throws TalosModuleException {
                sync.transferDataHeartFromDate(u, date);
            }
        })).execute();
    }

    public interface SnycJob {
        public void runSync(User u, Date date) throws TalosModuleException;
    }

    public class SyncTask extends AsyncTask<Void,Integer,String> {

        private int progressMax = 1;
        private Date from;
        private Date to;
        private final AtomicInteger curCount;
        private SnycJob task;
        private AtomicInteger semCountIn;
        private AtomicInteger semCountOut;

        public SyncTask(Date from, Date to, AtomicInteger curCount, AtomicInteger semCountIn, AtomicInteger semCountOut, SnycJob task) {
            this.from = from;
            this.to = to;
            this.curCount = curCount;
            this.task = task;
            this.semCountIn = semCountIn;
            this.semCountOut = semCountOut;
        }

        @Override
        protected String doInBackground(Void... params) {
            User user = MainActivity.getLoggedInUser();
            int numDates = 0;
            Calendar start = Calendar.getInstance();
            start.setTime(from);
            Calendar end = Calendar.getInstance();
            end.setTime(to);
            end.add(Calendar.DATE, 1);

            for (; start.before(end); start.add(Calendar.DATE, 1)) {
                numDates++;
            }

            if(numDates!=0)
                progressMax = numDates*5;

            start.setTime(from);
            end.setTime(to);
            end.add(Calendar.DATE, 1);

            for (Date date = start.getTime(); start.before(end); start.add(Calendar.DATE, 1), date = start.getTime()) {
                try {
                    task.runSync(user, date);
                    int curDates = curCount.incrementAndGet();
                    reportProgress(curDates);
                } catch (TalosModuleException e) {
                    return "Failed";
                }
            }

            return "Success";
        }

    private void reportProgress(int curVal) {
        publishProgress((int) (((float)curVal/((float)progressMax))*100));
    }


    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if(semCountIn.getAndIncrement() == 0) {
            bar.setVisibility(View.VISIBLE);
            progressText.setText("Loading....");
            progressText.setVisibility(View.VISIBLE);
            bar.setProgress(0);
        }
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        synchronized (curCount) {
            bar.setProgress(values[0]);
            bar.invalidate();
        }
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        if(semCountOut.decrementAndGet() == 0) {
            storeToDate(getDate(toDate));
            bar.setVisibility(View.INVISIBLE);
            progressText.setText(s);
        }
    }
}


    public static class FitbitSyncDatePicker extends DialogFragment implements DatePickerDialog.OnDateSetListener {

        private FitbitSync attached;

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            attached = (FitbitSync) activity;
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
