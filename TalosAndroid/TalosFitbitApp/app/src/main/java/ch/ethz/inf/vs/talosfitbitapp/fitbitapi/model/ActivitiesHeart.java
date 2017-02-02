
package ch.ethz.inf.vs.talosfitbitapp.fitbitapi.model;

import java.util.ArrayList;
import java.util.List;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ActivitiesHeart {

    @SerializedName("customHeartRateZones")
    @Expose
    private List<Object> customHeartRateZones = new ArrayList<Object>();
    @SerializedName("dateTime")
    @Expose
    private String dateTime;
    @SerializedName("heartRateZones")
    @Expose
    private List<HeartRateZone> heartRateZones = new ArrayList<HeartRateZone>();
    @SerializedName("value")
    @Expose
    private String value;

    /**
     * 
     * @return
     *     The customHeartRateZones
     */
    public List<Object> getCustomHeartRateZones() {
        return customHeartRateZones;
    }

    /**
     * 
     * @param customHeartRateZones
     *     The customHeartRateZones
     */
    public void setCustomHeartRateZones(List<Object> customHeartRateZones) {
        this.customHeartRateZones = customHeartRateZones;
    }

    /**
     * 
     * @return
     *     The dateTime
     */
    public String getDateTime() {
        return dateTime;
    }

    /**
     * 
     * @param dateTime
     *     The dateTime
     */
    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    /**
     * 
     * @return
     *     The heartRateZones
     */
    public List<HeartRateZone> getHeartRateZones() {
        return heartRateZones;
    }

    /**
     * 
     * @param heartRateZones
     *     The heartRateZones
     */
    public void setHeartRateZones(List<HeartRateZone> heartRateZones) {
        this.heartRateZones = heartRateZones;
    }

    /**
     * 
     * @return
     *     The value
     */
    public String getValue() {
        return value;
    }

    /**
     * 
     * @param value
     *     The value
     */
    public void setValue(String value) {
        this.value = value;
    }

}
