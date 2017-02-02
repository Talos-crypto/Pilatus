package ch.ethz.inf.vs.talosavaapp.activities;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

import ch.ethz.inf.vs.talosavaapp.R;
import ch.ethz.inf.vs.talosavaapp.talos.TalosAvaSimpleAPI;
import ch.ethz.inf.vs.talosavaapp.util.UserAdapter;
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

public class FriendsActivity extends AppCompatActivity {

    private ListView listView;
    private ArrayList<SharedUser> itemsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);

        listView = (ListView) findViewById(R.id.viewFriends);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                if (!itemsList.isEmpty()) {
                    SharedUser item = itemsList.get(position);
                    Intent intent = new Intent(getApplicationContext(), CloudActivityOverview.class);
                    intent.putExtra(ActivitiesUtil.SHARED_USER_KEY, item.encodeAsString());
                    startActivity(intent);
                }

            }
        });

        loadSharedFriends();
    }

    private void loadSharedFriends() {
        final Context con = this;
        (new AsyncTask<Void, Void, ArrayList<SharedUser>>() {
            @Override
            protected ArrayList<SharedUser> doInBackground(Void... params) {
                User u = MainActivity.getLoggedInUser();
                TalosAvaSimpleAPI api = new TalosAvaSimpleAPI(con, u);
                List<SharedUser> res = api.getApi().getAccesses(u);
                ArrayList<SharedUser> arrayList = new ArrayList<>();
                for(SharedUser user : res) {
                    arrayList.add(user);
                }
                return arrayList;
            }

            @Override
            protected void onPostExecute(ArrayList<SharedUser> items) {
                super.onPostExecute(items);
                itemsList = items;
                UserAdapter adapter = new UserAdapter(getApplicationContext(), items);
                listView.setAdapter(adapter);
                adapter.notifyDataSetChanged();
                listView.invalidateViews();
            }
        }).execute();
    }


    public void onEditFriends(View v) {
        Intent intent = new Intent(this, MySharesActivity.class);
        startActivity(intent);
    }
}
