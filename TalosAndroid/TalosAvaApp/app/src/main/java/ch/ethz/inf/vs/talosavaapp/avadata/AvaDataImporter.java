package ch.ethz.inf.vs.talosavaapp.avadata;

import android.content.Context;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.Iterator;

import ch.ethz.inf.vs.talosavaapp.R;

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

public class AvaDataImporter implements Iterator<AvaDataEntry>, Closeable {

    BufferedReader reader;

    String curLine = null;

    public AvaDataImporter(Context c, int num) {
        int rawId = c.getResources().getIdentifier("data"+num,
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

    private static int computeAVGBpm(String line) {
        String filtered = line.replace("[", "");
        filtered = filtered.replace("]", "");
        filtered = filtered.replace(" ", "");
        String[] numsStr = filtered.split(",");
        int[] nums = new int[numsStr.length];
        for(int i=0; i<nums.length; i++) {
            nums[i] = Integer.valueOf(numsStr[i]);
        }

        BigDecimal a = BigDecimal.ZERO;
        for(int i=0; i<nums.length; i++) {
            a = a.add(BigDecimal.valueOf(nums[i]));
        }
        a = a.divide(BigDecimal.valueOf(nums.length), BigDecimal.ROUND_HALF_UP);
        a = BigDecimal.valueOf(10000).divide(a, BigDecimal.ROUND_HALF_UP);
        a = a.multiply(BigDecimal.valueOf(6));
        return a.intValue();

    }

    private static AvaDataEntry createDataEnryFromLine(String line) {
        String[] arraySplit = line.split("\\[|\\]");
        String[] left = arraySplit[0].replace("\"", "").split(",");
        String mid = arraySplit[1];
        String[] right = arraySplit[2].replace("\"", "").split(",");
        String[] splits = new String[left.length+right.length];
        System.arraycopy(left, 0, splits, 0, left.length);
        System.arraycopy(right, 1, splits, left.length+1, right.length-1);
        splits[left.length] = mid;
        if(!(splits.length==13 || splits.length==14))
            throw new RuntimeException("Wrong Line: " + line);
        AvaDataEntry entry = new AvaDataEntry();
        entry.avg_bpm = computeAVGBpm(splits[8]);
        int index = 9;
        if(splits.length==13) {

        } else {
            index++;
        }
        int sleepState = Integer.valueOf(splits[index++]);
        entry.sleep_state_deep = sleepState==2 ? 1 : 0;
        entry.sleep_state_low = sleepState==1 ? 1 : 0;
        entry.temp_amb = Integer.valueOf(splits[index++]);
        entry.temp_skin = Integer.valueOf(splits[index++]);
        entry.time_stamp = Integer.valueOf(splits[index]);
        return entry;
    }

    @Override
    public boolean hasNext() {
        return curLine!=null;
    }

    @Override
    public AvaDataEntry next() {
        if(curLine == null)
            return null;
        AvaDataEntry res = createDataEnryFromLine(curLine);
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

    @Override
    public void close() throws IOException {
        reader.close();
    }
}
