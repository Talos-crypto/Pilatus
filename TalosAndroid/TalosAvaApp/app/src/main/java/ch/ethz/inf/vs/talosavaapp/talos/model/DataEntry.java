package ch.ethz.inf.vs.talosavaapp.talos.model;

import java.sql.Date;
import java.sql.Time;

import ch.ethz.inf.vs.talosavaapp.talos.Datatype;
import ch.ethz.inf.vs.talosmodule.exceptions.TalosModuleException;
import ch.ethz.inf.vs.talosmodule.main.TalosResult;

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

public class DataEntry implements DataEntryAgrDate, DataEntryAgrTime, DataOval {

    private int id;

    private Date date;

    private Time time;

    private String type;

    private int value;

    private DataEntry(int id, Date date, Time time, String type, int value) {
        this.id = id;
        this.date = date;
        this.time = time;
        this.type = type;
        this.value = value;
    }

    public DataEntry(Date date, int value) {
        this.date = date;
        this.value = value;
    }

    public DataEntry(Time time, int value) {
        this.time = time;
        this.value = value;
    }

    public static DataEntryAgrDate createFromTalosResult(TalosResult res, Datatype type) throws TalosModuleException {
        String date = res.getString("date");
        int sum = res.getInt("SUM(data)");
        int max= res.getInt("COUNT(data)");
        sum = Datatype.performAVG(type, sum, max);
        return new DataEntry(Date.valueOf(date), sum);
    }

    public static DataEntryAgrTime createFromTalosResultTime(TalosResult res, Datatype type) throws TalosModuleException {
        String time = res.getString("agrTime");
        int sum = res.getInt("SUM(data)");
        int max= res.getInt("COUNT(data)");
        sum = Datatype.performAVG(type, sum, max);
        return new DataEntry(Time.valueOf(time), sum);
    }

    public static DataOval createFromTalosResultTimeOval(TalosResult res) throws TalosModuleException {
        String time = res.getString("time");
        String date = res.getString("date");
        String type = res.getString("datatype");
        int data= res.getInt("data");
        return new DataEntry(-1, Date.valueOf(date), Time.valueOf(time), type, data);
    }

    public int getId() {
        return id;
    }

    public Date getDate() {
        return date;
    }

    public Time getTime() {
        return time;
    }

    public String getType() {
        return type;
    }

    public int getValue() {
        return value;
    }
}
