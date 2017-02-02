package ch.ethz.inf.vs.talosavaapp.avadata;

import android.content.Context;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

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

public class AvaOvalDataImporter implements Iterator<AvaOvalDataEntry>, Closeable {

    private DateFormat df = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");

    private int id;

    BufferedReader reader;

    String curLine = null;

    public AvaOvalDataImporter(Context c, int id) {
        this.id = id;
        int rawId = c.getResources().getIdentifier("idcycle"+id,
                "raw", c.getPackageName());
        InputStream ins = c.getResources().openRawResource(rawId);
        reader = new BufferedReader(new InputStreamReader(ins));
        try {
            reader.readLine();
            curLine = reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private AvaOvalDataEntry createDataEnryFromLine(String line) {
        if(line == null || line.isEmpty() || line.contains("Date"))
            return null;
        String[] parts = line.split(",");
        try {
            if(parts[2].isEmpty() || parts[3].isEmpty())
                return null;
            Date date = df.parse(parts[1]);
            int hr = (new BigDecimal(parts[2])).multiply(BigDecimal.valueOf(100.0)).intValue();
            int temp = (new BigDecimal(parts[3])).multiply(BigDecimal.valueOf(100.0)).intValue();
            return new AvaOvalDataEntry(date, hr, temp, this.id);

        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    @Override
    public boolean hasNext() {
        return curLine!=null;
    }

    @Override
    public AvaOvalDataEntry next() {
        if(curLine == null)
            return null;
        AvaOvalDataEntry res = createDataEnryFromLine(curLine);
        try {
            curLine = reader.readLine();
        } catch (IOException e) {
            curLine = null;
        }
        return res;
    }

    @Override
    public void remove() {

    }
}
