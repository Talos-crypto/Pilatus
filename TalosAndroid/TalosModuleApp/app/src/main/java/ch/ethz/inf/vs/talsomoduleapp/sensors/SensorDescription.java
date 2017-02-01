package ch.ethz.inf.vs.talsomoduleapp.sensors;

import android.hardware.Sensor;
import android.util.SparseArray;

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

public class SensorDescription {

    private static SparseArray<String> desc = null;

    static {
        desc = new SparseArray<String>();
        desc.put(Sensor.TYPE_ACCELEROMETER, "Measures the acceleration force in m/s2 that is applied to a device on all three physical axes (x, y, and z), including the force of gravity.");
        desc.put(Sensor.TYPE_AMBIENT_TEMPERATURE, "Measures the ambient room temperature in degrees Celsius (°C). See note below.");
        desc.put(Sensor.TYPE_GRAVITY, "Measures the force of gravity in m/s2 that is applied to a device on all three physical axes (x, y, z).");
        desc.put(Sensor.TYPE_GYROSCOPE,"Measures a devices rate of rotation in rad/s around each of the three physical axes (x, y, and z).");
        desc.put(Sensor.TYPE_LIGHT,"Measures the ambient light level (illumination) in lx.");
        desc.put(Sensor.TYPE_LINEAR_ACCELERATION,"Measures the acceleration force in m/s2 that is applied to a device on all three physical axes (x, y, and z), excluding the force of gravity.");
        desc.put(Sensor.TYPE_MAGNETIC_FIELD,"Measures the ambient geomagnetic field for all three physical axes (x, y, z) in μT.");
        desc.put(Sensor.TYPE_PRESSURE,"Measures the ambient air pressure in hPa or mbar.");
        desc.put(Sensor.TYPE_PROXIMITY,"Measures the proximity of an object in cm relative to the view screen of a device. This sensor is typically used to determine whether a handset is being held up to a persons ear.");
        desc.put(Sensor.TYPE_RELATIVE_HUMIDITY,"Measures the relative ambient humidity in percent (%).");
        desc.put(Sensor.TYPE_ROTATION_VECTOR,"Measures the orientation of a device by providing the three elements of the devices rotation vector.");
    }

    public static String getSensorDescription(Sensor s) {
        String res = desc.get(s.getType());
        if(res==null)
            return "No Description";
        else
            return res;
    }

}
