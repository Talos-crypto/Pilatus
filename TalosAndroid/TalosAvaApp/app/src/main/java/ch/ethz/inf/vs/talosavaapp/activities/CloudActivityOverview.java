package ch.ethz.inf.vs.talosavaapp.activities;


import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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

public class CloudActivityOverview extends AppCompatActivity {

    private TextView dateTitle;
    private List<Date> availableDates;
    int curPosition = 0;

    private ListView list;
    private ArrayList<CloudListItem> itemsList = new ArrayList<>();
    private static AtomicInteger loadCount = new AtomicInteger();
    private static ArrayList<CloudListItem> cachedOld = null;
    private static final Object mutex = new Object();
    private SharedUser shareU = null;

    private ImageButton lbutton;
    private ImageButton rbutton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_overview);
        list = (ListView) findViewById(R.id.listView);

        Intent intent = getIntent();
        if(intent.hasExtra(ActivitiesUtil.SHARED_USER_KEY)) {
            this.shareU = SharedUser.decodeFromString(intent.getStringExtra(ActivitiesUtil.SHARED_USER_KEY));
        } else if(savedInstanceState!=null && savedInstanceState.getString(ActivitiesUtil.SHARED_USER_KEY) != null) {
            this.shareU = SharedUser.decodeFromString(savedInstanceState.getString(ActivitiesUtil.SHARED_USER_KEY));
        }

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                if (!itemsList.isEmpty()) {
                    CloudListItem item = itemsList.get(position);
                    if(!item.type.equals(Datatype.Sleep)) {
                        Intent intent = new Intent(getApplicationContext(), DataWeeklyActivity.class);
                        intent.putExtra(ActivitiesUtil.DETAIL_DATE_KEY, ActivitiesUtil.titleFormat.format(availableDates.get(curPosition)));
                        intent.putExtra(ActivitiesUtil.DATATYPE_KEY, item.type.name());
                        if (shareU != null) {
                            intent.putExtra(ActivitiesUtil.SHARED_USER_KEY, shareU.encodeAsString());
                        }
                        startActivity(intent);
                    }
                }

            }
        });

        lbutton = (ImageButton) findViewById(R.id.imageButton);
        rbutton = (ImageButton) findViewById(R.id.imageButton2);
        dateTitle = (TextView) findViewById(R.id.selectedDate);
        lbutton.setVisibility(View.INVISIBLE);
        rbutton.setVisibility(View.INVISIBLE);
        dateTitle.setVisibility(View.INVISIBLE);

        getDates(shareU);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if(shareU!=null)
            outState.putString(ActivitiesUtil.SHARED_USER_KEY, shareU.encodeAsString());
        super.onSaveInstanceState(outState);
    }

    private void getDates(final SharedUser sharedUser) {
        final Context con = this;
        (new AsyncTask<Void, Void, List<Date>>() {
            @Override
            protected List<Date> doInBackground(Void... params) {
                User u = MainActivity.getLoggedInUser();
                TalosAvaSimpleAPI api = new TalosAvaSimpleAPI(con, MainActivity.getLoggedInUser());
                try {
                    return api.getDates(u, sharedUser);
                } catch (TalosModuleException e) {
                    return null;
                }
            }

            @Override
            protected void onPostExecute(List<Date> items) {
                super.onPostExecute(items);
                availableDates = items;
                curPosition = availableDates.size()-1;
                dateTitle.setVisibility(View.VISIBLE);
                if(items.isEmpty()) {
                    dateTitle.setText("No Data");
                } else {
                    lbutton.setVisibility(View.VISIBLE);
                    updateToDate(availableDates.get(availableDates.size()-1), sharedUser);
                }

            }
        }).execute();
    }

    private synchronized void updateToDate(final Date updateDate, final SharedUser sharedUser) {
        final Context con = this;
        (new AsyncTask<Void, Void, ArrayList<CloudListItem>>() {
            @Override
            protected ArrayList<CloudListItem> doInBackground(Void... params) {
                User u = MainActivity.getLoggedInUser();
                final TalosAvaSimpleAPI api = new TalosAvaSimpleAPI(con, u);
                try {
                    return api.getCloudListItems(u, updateDate, sharedUser);
                } catch (TalosModuleException e) {
                    e.printStackTrace();
                }
                return null;

            }

            @Override
            protected void onPostExecute(ArrayList<CloudListItem> items) {
                super.onPostExecute(items);
                itemsList = items;
                if(0==loadCount.getAndIncrement())
                    cachedOld = items;
                CloudListAdapter adapter = new CloudListAdapter(getApplicationContext(), items);
                list.setAdapter(adapter);
                adapter.notifyDataSetChanged();
                list.invalidateViews();
            }
        }).execute();
    }
    public synchronized void onRightClick(View v) {
        synchronized (mutex) {
            int temp = curPosition + 1;
            if (temp < availableDates.size()) {
                curPosition++;
                updateToDate(availableDates.get(curPosition), this.shareU);
                dateTitle.setText(ActivitiesUtil.titleFormat.format(availableDates.get(curPosition)));
                if(curPosition==availableDates.size()-1)
                    rbutton.setVisibility(View.INVISIBLE);
                if(lbutton.getVisibility() == View.INVISIBLE)
                    lbutton.setVisibility(View.VISIBLE);
            }
        }
    }

    public synchronized void onLeftClick(View v) {
        synchronized (mutex) {
            int temp = curPosition - 1;
            if (temp >= 0) {
                curPosition--;
                updateToDate(availableDates.get(curPosition), this.shareU);
                dateTitle.setText(ActivitiesUtil.titleFormat.format(availableDates.get(curPosition)));
                if(rbutton.getVisibility() == View.INVISIBLE)
                    rbutton.setVisibility(View.VISIBLE);
                if(curPosition==0)
                    lbutton.setVisibility(View.INVISIBLE);
            }

        }
    }

    public synchronized void onToOval(View v) {
        Intent intent = new Intent(getApplicationContext(), OvalInfoActivity.class);
        intent.putExtra(ActivitiesUtil.DETAIL_DATE_KEY, ActivitiesUtil.titleFormat.format(availableDates.get(curPosition)));
        if (shareU != null) {
            intent.putExtra(ActivitiesUtil.SHARED_USER_KEY, shareU.encodeAsString());
        }
        startActivity(intent);
    }

    public static class CloudListItem implements Serializable {
        private Datatype type;
        private String content;
        public CloudListItem(Datatype type, String content) {
            this.type = type;
            this.content = content;
        }
    }

    public static class CloudListAdapter extends ArrayAdapter<CloudListItem> {

        private ArrayList<CloudListItem> items;

        public CloudListAdapter(Context context, ArrayList<CloudListItem> items) {
            super(context, R.layout.list_cloud_layout, items);
            this.items = items;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            CloudListItem item = items.get(position);
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_cloud_layout, parent, false);
            }
            TextView type = (TextView) convertView.findViewById(R.id.clouddataype);
            TextView content = (TextView) convertView.findViewById(R.id.numtoday);
            ImageView imgView = (ImageView) convertView.findViewById(R.id.cloudimage);
            RelativeLayout layout = (RelativeLayout) convertView.findViewById(R.id.relLayoutList);
            LinearLayout linearLayout = (LinearLayout) convertView.findViewById(R.id.cloudlinlay);

            layout.setBackgroundColor(getContext().getResources().getColor(R.color.lightBG));
            linearLayout.setBackgroundColor(getContext().getResources().getColor(R.color.heavierBG));


            type.setText(item.type.getDisplayRep());
            content.setText(item.content + " "+ item.type.getUnit());
            content.setTextColor(Color.BLACK);
            type.setTextColor(Color.BLACK);
            imgView.setBackgroundResource(item.type.getImgResource());

            return convertView;
        }
    }
}
