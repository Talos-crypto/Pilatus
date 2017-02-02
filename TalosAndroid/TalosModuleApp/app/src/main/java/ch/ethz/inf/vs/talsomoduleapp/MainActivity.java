package ch.ethz.inf.vs.talsomoduleapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import java.util.concurrent.TimeUnit;

import ch.ethz.inf.vs.talosmodule.exceptions.TalosModuleException;
import ch.ethz.inf.vs.talosmodule.main.User;
import ch.ethz.inf.vs.talsomoduleapp.ble.BleActivity;
import ch.ethz.inf.vs.talsomoduleapp.data.DataActivity;
import ch.ethz.inf.vs.talsomoduleapp.dbmodel.DBStoreInterface;
import ch.ethz.inf.vs.talsomoduleapp.sensors.SensorMain;

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

public class MainActivity extends ActionBarActivity implements GoogleApiClient.OnConnectionFailedListener {

    private static final int RC_SIGN_IN = 9001;

    private static GoogleApiClient mGoogleApiClient = null;

    private Button logoutButton = null;
    private static boolean logInState = false;

    public synchronized static User getLoggedInUser() throws LogInException {
        OptionalPendingResult<GoogleSignInResult> opr = Auth.GoogleSignInApi.silentSignIn(mGoogleApiClient);
        if (opr.isDone()) {
            // If the user's cached credentials are valid, the OptionalPendingResult will be "done"
            // and the GoogleSignInResult will be available instantly.
            Log.d("temp", "Got cached sign-in");
            GoogleSignInResult result = opr.get();
            if(result.isSuccess()) {
                GoogleSignInAccount acc = result.getSignInAccount();
                return TalosGoogleAuth.getUserFromGoogleAcc(acc);
            } else {
                throw new LogInException("Failed Login");
            }

        } else {
            if(Looper.myLooper() == Looper.getMainLooper())
                throw new LogInException("Not allowed to be called from UI.Thread");

            GoogleSignInResult res = opr.await(2000, TimeUnit.MILLISECONDS);
            if(res.isSuccess()) {
                GoogleSignInAccount acc = res.getSignInAccount();
                return TalosGoogleAuth.getUserFromGoogleAcc(acc);
            } else {
                Log.d("Err", res.getStatus().toString());
                throw new LogInException("Failed Login");
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initDBConnection();
        mGoogleApiClient = TalosGoogleAuth.createGoogleApiClient(this,this, getString(R.string.Google_Client_ID));
        logoutButton = (Button) findViewById(R.id.logout);
        updateUI(logInState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_main, menu);

        return true;
    }

    @Override
    public void onStart() {
        super.onStart();
        updateUI(logInState);
        /*
        OptionalPendingResult<GoogleSignInResult> opr = Auth.GoogleSignInApi.silentSignIn(mGoogleApiClient);
        if (opr.isDone()) {
            // If the user's cached credentials are valid, the OptionalPendingResult will be "done"
            // and the GoogleSignInResult will be available instantly.
            Log.d("temp", "Got cached sign-in");
            GoogleSignInResult result = opr.get();
            handleSignInResult(result);
        } else {
            Log.d("temp", "Silent sign in");
            opr.setResultCallback(new ResultCallback<GoogleSignInResult>() {
                @Override
                public void onResult(GoogleSignInResult googleSignInResult) {
                    handleSignInResult(googleSignInResult);
                }
            });
        }*/
    }



    private void initDBConnection() {
        Context con = this.getApplicationContext();
        /*try {
            DBInterfaceHolder.init(con, R.raw.sensys6);
        } catch (TalosModuleException e) {
            e.printStackTrace();
        }*/
    }

    public void onBleSensors(View v) {
        Intent intent = new Intent(this, BleActivity.class);
        startActivity(intent);
    }

    public void onSensors(View v) {
        Intent intent = new Intent(this, SensorMain.class);
        startActivity(intent);
    }

    public void onMyData(View v) {
        Intent intent = new Intent(this, DataActivity.class);
        startActivity(intent);
    }

    public void onLogout(View v) {
        if(logInState)
            signOut();
        else
            signIn();
    }

    private void signIn() {
        TalosGoogleAuth.startGoogleLogin(this, mGoogleApiClient, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            handleSignInResult(TalosGoogleAuth.getResultFromLoginIntent(data));
        }
    }

    private void handleSignInResult(final GoogleSignInResult result) {
        Log.d("GoogleLogin", "handleSignInResult:" + result.isSuccess());
        if (result.isSuccess()) {
            GoogleSignInAccount acc = result.getSignInAccount();
            User user = TalosGoogleAuth.getUserFromGoogleAcc(acc);
            Log.d("GoogleLogin", "Mail: " + acc.getEmail());
            Log.d("GoogleLogin", "Name: " + acc.getDisplayName());
            Log.d("GoogleLogin", "Token: " + acc.getIdToken());

            register(user);
            updateUI(true);
        } else {
            updateUI(false);
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    /*
                    AlertDialog.Builder builder1 = new AlertDialog.Builder(getApplicationContext());
                    builder1.setMessage("Error Login in with Google");
                    builder1.setCancelable(true);
                    builder1.setPositiveButton("Retry",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                    signIn();
                                }
                            });
                    builder1.setNegativeButton("Terminate",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                    Intent intent = new Intent(Intent.ACTION_MAIN);
                                    intent.addCategory(Intent.CATEGORY_HOME);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                }
                            });
                    AlertDialog alert11 = builder1.create();
                    alert11.show();*/
                }
            });
        }

    }

    private void register(final User user) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                DBStoreInterface dbStoreInterface = DBInterfaceHolder.getCon();
                try {
                    dbStoreInterface.registerUser(user);
                } catch (TalosModuleException e) {
                    return null;
                }
                return null;
            }
        }.execute();

    }

    private void signOut() {
        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        updateUI(false);
                    }
                });
    }


    private void updateUI(boolean signedIn) {
        if (signedIn) {
            logoutButton.setText("Logout");
            findViewById(R.id.blesensors).setVisibility(View.VISIBLE);
            findViewById(R.id.mycloud).setVisibility(View.VISIBLE);
            findViewById(R.id.sensors).setVisibility(View.VISIBLE);
            logInState = true;
        } else {
            logoutButton.setText("LogIn");
            findViewById(R.id.blesensors).setVisibility(View.GONE);
            findViewById(R.id.mycloud).setVisibility(View.GONE);
            findViewById(R.id.sensors).setVisibility(View.GONE);
            logInState = false;
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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }
}
