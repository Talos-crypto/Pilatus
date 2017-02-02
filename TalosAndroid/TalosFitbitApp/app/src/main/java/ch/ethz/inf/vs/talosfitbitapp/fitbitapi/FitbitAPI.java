package ch.ethz.inf.vs.talosfitbitapp.fitbitapi;

import android.net.Uri;

import com.google.common.io.CharStreams;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;

import ch.ethz.inf.vs.talosfitbitapp.fitbitapi.model.CaloriesQuery;
import ch.ethz.inf.vs.talosfitbitapp.fitbitapi.model.DistQuery;
import ch.ethz.inf.vs.talosfitbitapp.fitbitapi.model.FloorQuery;
import ch.ethz.inf.vs.talosfitbitapp.fitbitapi.model.HeartQuery;
import ch.ethz.inf.vs.talosfitbitapp.fitbitapi.model.StepsQuery;

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

public class FitbitAPI {

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat ("yyyy-MM-dd");
    private static final String FITBIT_API_URL = "https://api.fitbit.com";
    private static final String STEPS_RESOURCE = "/activities/steps";
    private static final String FLOOR_RESOURCE = "/activities/floors";
    private static final String HEART_RESOURCE = "/activities/heart";
    private static final String DISTANCE_RESOURCE = "/activities/distance";
    private static final String CALORIES_RESOURCE = "/activities/calories";
    private static final String USER_IDENT = "/1/user/-";
    private static final String DATE_IDENT = "/date/";

    private TokenInfo info;

    public FitbitAPI(TokenInfo info) {
        this.info = info;
    }

    public String testExample() {
        return askFitbit(FITBIT_API_URL+"/1/user/-/activities/steps/date/2016-01-12/1d.json");
    }

    public static Uri getAccessTokenURI(String clientID, String scope, String callback) {
        StringBuilder sb = new StringBuilder();
        try {
            sb.append("https://www.fitbit.com/oauth2/authorize?response_type=token&client_id=")
                    .append(clientID).append("&redirect_uri=")
                    .append(URLEncoder.encode(callback, "UTF-8"))
                    .append("&scope=")
                    .append(scope)
                    .append("&expires_in=86400&prompt=login");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return Uri.parse(sb.toString());
    }

    public StepsQuery getStepsFromDate(Date date)throws FitbitAPIException  {
        StringBuilder sb = new StringBuilder();
        sb.append(FITBIT_API_URL)
                .append(USER_IDENT)
                .append(STEPS_RESOURCE)
                .append(DATE_IDENT)
                .append(dateFormat.format(date))
                .append("/1d/15min.json");
        String res = askFitbit(sb.toString());
        return StepsQuery.fromJSON(res);
    }

    public FloorQuery getFloorsFromDate(Date date)throws FitbitAPIException  {
        StringBuilder sb = new StringBuilder();
        sb.append(FITBIT_API_URL)
                .append(USER_IDENT)
                .append(FLOOR_RESOURCE)
                .append(DATE_IDENT)
                .append(dateFormat.format(date))
                .append("/1d/15min.json");
        String res = askFitbit(sb.toString());
        return FloorQuery.fromJSON(res);
    }

    public HeartQuery getHearthRateFromDate(Date date)throws FitbitAPIException  {
        StringBuilder sb = new StringBuilder();
        sb.append(FITBIT_API_URL)
                .append(USER_IDENT)
                .append(HEART_RESOURCE)
                .append(DATE_IDENT)
                .append(dateFormat.format(date))
                .append("/1d/1min.json");
        String res = askFitbit(sb.toString());
        return HeartQuery.fromJSON(res);
    }

    public DistQuery getDistanceFromDate(Date date)throws FitbitAPIException  {
        StringBuilder sb = new StringBuilder();
        sb.append(FITBIT_API_URL)
                .append(USER_IDENT)
                .append(DISTANCE_RESOURCE)
                .append(DATE_IDENT)
                .append(dateFormat.format(date))
                .append("/1d/15min.json");
        String res = askFitbit(sb.toString());
        return DistQuery.fromJSON(res);
    }

    public CaloriesQuery getCaloriesFromDate(Date date)throws FitbitAPIException  {
        StringBuilder sb = new StringBuilder();
        sb.append(FITBIT_API_URL)
                .append(USER_IDENT)
                .append(CALORIES_RESOURCE)
                .append(DATE_IDENT)
                .append(dateFormat.format(date))
                .append("/1d/15min.json");
        String res = askFitbit(sb.toString());
        return CaloriesQuery.fromJSON(res);
    }

    private String askFitbit(String url1) throws FitbitAPIException {
        BufferedInputStream in = null;
        HttpURLConnection httpClient = null;
        try {

            URL url = new URL(url1);
            httpClient  = (HttpURLConnection) url.openConnection();
            httpClient.setRequestProperty("Authorization", info.getToken_type() + " " + info.getAccess_token());

            httpClient.connect();
            in = new BufferedInputStream(httpClient.getInputStream());

            String res =  CharStreams.toString(new InputStreamReader(in, "UTF-8"));

            if (httpClient.getResponseCode() == HttpURLConnection.HTTP_OK) {
                return res;
            } else {
                throw new FitbitAPIException(res);
            }


        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(httpClient!=null)
                httpClient.disconnect();
            if(in!=null)
                try {in.close();} catch (IOException e) {e.printStackTrace();}
        }

        return null;
    }
}
