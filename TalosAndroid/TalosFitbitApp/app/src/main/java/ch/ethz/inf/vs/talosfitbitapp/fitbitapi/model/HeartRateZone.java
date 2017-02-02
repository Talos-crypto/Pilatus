
package ch.ethz.inf.vs.talosfitbitapp.fitbitapi.model;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class HeartRateZone {

    @SerializedName("caloriesOut")
    @Expose
    private Double caloriesOut;
    @SerializedName("max")
    @Expose
    private Integer max;
    @SerializedName("min")
    @Expose
    private Integer min;
    @SerializedName("minutes")
    @Expose
    private Integer minutes;
    @SerializedName("name")
    @Expose
    private String name;

    /**
     * 
     * @return
     *     The caloriesOut
     */
    public Double getCaloriesOut() {
        return caloriesOut;
    }

    /**
     * 
     * @param caloriesOut
     *     The caloriesOut
     */
    public void setCaloriesOut(Double caloriesOut) {
        this.caloriesOut = caloriesOut;
    }

    /**
     * 
     * @return
     *     The max
     */
    public Integer getMax() {
        return max;
    }

    /**
     * 
     * @param max
     *     The max
     */
    public void setMax(Integer max) {
        this.max = max;
    }

    /**
     * 
     * @return
     *     The min
     */
    public Integer getMin() {
        return min;
    }

    /**
     * 
     * @param min
     *     The min
     */
    public void setMin(Integer min) {
        this.min = min;
    }

    /**
     * 
     * @return
     *     The minutes
     */
    public Integer getMinutes() {
        return minutes;
    }

    /**
     * 
     * @param minutes
     *     The minutes
     */
    public void setMinutes(Integer minutes) {
        this.minutes = minutes;
    }

    /**
     * 
     * @return
     *     The name
     */
    public String getName() {
        return name;
    }

    /**
     * 
     * @param name
     *     The name
     */
    public void setName(String name) {
        this.name = name;
    }

}
