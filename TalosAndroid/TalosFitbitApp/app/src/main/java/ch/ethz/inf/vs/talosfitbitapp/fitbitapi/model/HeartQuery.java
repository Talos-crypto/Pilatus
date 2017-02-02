
package ch.ethz.inf.vs.talosfitbitapp.fitbitapi.model;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class HeartQuery {

    public static transient final int DIST_RAD = 2;

    private String dateTime;

    @SerializedName("activities-heart")
    @Expose
    private List<ActivitiesHeart> activitiesHeart = new ArrayList<ActivitiesHeart>();
    @SerializedName("activities-heart-intraday")
    @Expose
    private ActivitiesHeartIntraday activitiesHeartIntraday;

    public HeartQuery(String dateTime, ActivitiesHeartIntraday activitiesHeartIntraday) {
        this.activitiesHeartIntraday = activitiesHeartIntraday;
        this.dateTime = dateTime;
    }

    public String getDateTime() {
        return dateTime;
    }

    /**
     * 
     * @return
     *     The activitiesHeart
     */
    public List<ActivitiesHeart> getActivitiesHeart() {
        return activitiesHeart;
    }

    /**
     * 
     * @param activitiesHeart
     *     The activities-heart
     */
    public void setActivitiesHeart(List<ActivitiesHeart> activitiesHeart) {
        this.activitiesHeart = activitiesHeart;
    }

    /**
     * 
     * @return
     *     The activitiesHeartIntraday
     */
    public ActivitiesHeartIntraday getActivitiesHeartIntraday() {
        return activitiesHeartIntraday;
    }

    /**
     * 
     * @param activitiesHeartIntraday
     *     The activities-heart-intraday
     */
    public void setActivitiesHeartIntraday(ActivitiesHeartIntraday activitiesHeartIntraday) {
        this.activitiesHeartIntraday = activitiesHeartIntraday;
    }

    public static HeartQuery fromJSON(String json) {
        //hack strange stuff coming back from fitbit evil stuff
        JSONObject jo = null;
        JSONArray other = null;
        String date;
        try {
            jo = new JSONObject(json);
            other = jo.getJSONArray("activities-heart");
            jo = jo.getJSONObject("activities-heart-intraday");
            date = other.getJSONObject(0).getString("dateTime");
        } catch (JSONException e) {
            return null;
        }

        Gson gson = new Gson();
        ActivitiesHeartIntraday day = gson.fromJson(jo.toString(), ActivitiesHeartIntraday.class);
        return new HeartQuery(date, day);

    }

}
