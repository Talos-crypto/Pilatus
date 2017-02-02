package ch.ethz.inf.vs.talosfitbitapp.activities;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
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

import ch.ethz.inf.vs.talosfitbitapp.R;
import ch.ethz.inf.vs.talosfitbitapp.fitbitapi.FitbitAPI;
import ch.ethz.inf.vs.talosfitbitapp.fitbitapi.TokenInfo;
import ch.ethz.inf.vs.talosfitbitapp.talos.TalosAPIFactory;
import ch.ethz.inf.vs.talosfitbitapp.talos.TalosModuleFitbitAPI;
import ch.ethz.inf.vs.talosfitbitapp.util.LogInException;
import ch.ethz.inf.vs.talosfitbitapp.util.TalosGoogleAuth;
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

public class MainActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {

    private static GoogleApiClient mGoogleApiClient = null;
    private static final int RC_SIGN_IN = 9001;


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
                Log.d("Err", "Failed Login");
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
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mGoogleApiClient = TalosGoogleAuth.createGoogleApiClient(this,this, getString(R.string.server_client_id));
        logoutButton = (Button) findViewById(R.id.logbutton);
        updateUI(logInState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==100) {
            if(resultCode==RESULT_OK) {
                TokenInfo info = (TokenInfo) data.getSerializableExtra(FitbitSync.ACCESS_TOKEN_KEY);
                FitbitAPI api = new FitbitAPI(info);
                api.testExample();
            }
        }

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
        }

    }

    private void register(final User user) {
        final TalosModuleFitbitAPI api = TalosAPIFactory.createAPI(this);
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    api.registerUser(user);
                } catch (TalosModuleException e) {
                    e.printStackTrace();
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
            findViewById(R.id.syndata).setVisibility(View.VISIBLE);
            findViewById(R.id.mycloud).setVisibility(View.VISIBLE);
            findViewById(R.id.fitbitimg).setVisibility(View.VISIBLE);
            findViewById(R.id.cloudimg).setVisibility(View.VISIBLE);
            findViewById(R.id.userimg).setVisibility(View.VISIBLE);
            logInState = true;
        } else {
            logoutButton.setText("LogIn");
            findViewById(R.id.syndata).setVisibility(View.INVISIBLE);
            findViewById(R.id.mycloud).setVisibility(View.INVISIBLE);
            findViewById(R.id.fitbitimg).setVisibility(View.INVISIBLE);
            findViewById(R.id.cloudimg).setVisibility(View.INVISIBLE);
            findViewById(R.id.userimg).setVisibility(View.INVISIBLE);
            logInState = false;
        }
    }

    public void onSyncData(View v) {
        Intent intent = new Intent(this, FitbitSync.class);
        startActivity(intent);
    }

    public void onMyCloud(View v) {
        Intent intent = new Intent(this, CloudSelectActivity.class);
        startActivity(intent);
    }

    public void onLogButton(View v) {
        if(logInState)
            signOut();
        else
            signIn();
    }

    private void signIn() {
        TalosGoogleAuth.startGoogleLogin(this, mGoogleApiClient, RC_SIGN_IN);
    }


}
