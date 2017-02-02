
package ch.ethz.inf.vs.talosfitbitapp.fitbitapi.model;

import java.util.ArrayList;
import java.util.List;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ActivitiesHeartIntraday {

    @SerializedName("dataset")
    @Expose
    private List<DoubleDataSet> dataset = new ArrayList<DoubleDataSet>();
    @SerializedName("datasetInterval")
    @Expose
    private Integer datasetInterval;
    @SerializedName("datasetType")
    @Expose
    private String datasetType;

    /**
     * 
     * @return
     *     The dataset
     */
    public List<DoubleDataSet> getDataset() {
        return dataset;
    }

    /**
     * 
     * @param dataset
     *     The dataset
     */
    public void setDataset(List<DoubleDataSet> dataset) {
        this.dataset = dataset;
    }

    /**
     * 
     * @return
     *     The datasetInterval
     */
    public Integer getDatasetInterval() {
        return datasetInterval;
    }

    /**
     * 
     * @param datasetInterval
     *     The datasetInterval
     */
    public void setDatasetInterval(Integer datasetInterval) {
        this.datasetInterval = datasetInterval;
    }

    /**
     * 
     * @return
     *     The datasetType
     */
    public String getDatasetType() {
        return datasetType;
    }

    /**
     * 
     * @param datasetType
     *     The datasetType
     */
    public void setDatasetType(String datasetType) {
        this.datasetType = datasetType;
    }

}
