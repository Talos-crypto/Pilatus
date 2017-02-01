package ch.ethz.inf.vs.talsomoduleapp.dbmodel;

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

public class SensorMeasurement {

    private int ID = 0;

    private String dataType;

    private PhoneSensor belongsTo = null;
    private String belongsToStr = null;

    private String timeStamp;

    private int data;

    public SensorMeasurement(int ID, String dataType, PhoneSensor belongsTo, String timeStamp, int data) {
        this.ID = ID;
        this.dataType = dataType;
        this.belongsTo = belongsTo;
        this.timeStamp = timeStamp;
        this.data = data;
    }

    public SensorMeasurement(int ID, String dataType, String belongsTo, String timeStamp, int data) {
        this.ID = ID;
        this.dataType = dataType;
        this.belongsToStr = belongsTo;
        this.timeStamp = timeStamp;
        this.data = data;
    }


    public SensorMeasurement(String dataType, PhoneSensor belongsTo, String timeStamp, int data) {
        this.dataType = dataType;
        this.belongsTo = belongsTo;
        this.timeStamp = timeStamp;
        this.data = data;
    }

    public PhoneSensor getBelongsTo() {
        return belongsTo;
    }

    public void setBelongsTo(PhoneSensor belongsTo) {
        this.belongsTo = belongsTo;
    }

    public int getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    public int getData() {
        return data;
    }

    public void setData(int data) {
        this.data = data;
    }

    public String getBelongsToStr() {
        if(belongsTo!=null)
            return belongsTo.getNameID();
        return belongsToStr;
    }

    public void setBelongsToStr(String belongsToStr) {
        this.belongsToStr = belongsToStr;
    }
}
