/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.ethz.inf.vs.talsomoduleapp.ble;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;
import java.util.UUID;

import ch.ethz.inf.vs.talosmodule.exceptions.TalosModuleException;
import ch.ethz.inf.vs.talsomoduleapp.AppUtil;
import ch.ethz.inf.vs.talsomoduleapp.DBInterfaceHolder;
import ch.ethz.inf.vs.talsomoduleapp.MainActivity;
import ch.ethz.inf.vs.talsomoduleapp.R;
import ch.ethz.inf.vs.talsomoduleapp.dbmodel.DBStoreInterface;
import ch.ethz.inf.vs.talsomoduleapp.dbmodel.PhoneSensor;
import ch.ethz.inf.vs.talsomoduleapp.dbmodel.SensorMeasurement;

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

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class DeviceControlActivity extends ActionBarActivity {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private TextView mConnectionState;
    private TextView mDataField;
    private TextView mDataField2;
    private TextView mData;
    private TextView mData2;

    private TextView result;
    private ProgressBar progressBar;
    
    private BluetoothGattCharacteristic CharTempHum;
    
    private String mDeviceName;
    private String mDeviceAddress;
    private BluetoothLeService mBluetoothLeService;
    
    private boolean mConnected = false;
    private boolean processQuery = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    private PhoneSensor sens = null;
    private TempHumValue curData = null;
    private Object mutex = new Object();

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";
    
    private static final UUID RH_T_SERVICE = UUID.fromString("0000AA20-0000-1000-8000-00805f9b34fb");
    private static final UUID H_T_CHARACTERISTIC = UUID.fromString("0000AA21-0000-1000-8000-00805f9b34fb");

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
            	String temp = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
                String hum = intent.getStringExtra(BluetoothLeService.EXTRA_DATA2);
                displayData(temp, hum);
                mBluetoothLeService.readCharacteristic(CharTempHum);//read characteristic to get new values
            }
        }
    };


    // If a given GATT characteristic is selected, check for supported features.  This sample
    // demonstrates 'Read' and 'Notify' features.  See
    // http://d.android.com/reference/android/bluetooth/BluetoothGatt.html for the complete
    // list of supported characteristic features.
    

    private void clearUI() {
        mDataField.setText(R.string.no_data);
        mDataField2.setText(R.string.no_data);
        mData.setText(R.string.data1);
        mData2.setText(R.string.data2);
        
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gatt_services_characteristics);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        // Sets up UI references.
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);

        mConnectionState = (TextView) findViewById(R.id.connection_state);
        mDataField = (TextView) findViewById(R.id.data_value);
        mDataField2 = (TextView) findViewById(R.id.data_value2);
        mData = (TextView) findViewById(R.id.data);
        mData2 = (TextView) findViewById(R.id.data_2);
        result =(TextView) findViewById(R.id.textView);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setVisibility(View.INVISIBLE);

        //getActionBar().setTitle(mDeviceName);
        //getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        sens = new PhoneSensor(mDeviceAddress,mDeviceName,"Extern Board","SENSORIN","Measures Temperature and Humidity");
    }

    public synchronized void onStoreToCloud(View view) {
        if(mConnected && curData!=null) {
            TempHumValue atTheMoment;
            synchronized (mutex) {
                atTheMoment = curData;
            }
            new StoreTempHumDBTask().execute(atTheMoment);
        }
    }

    private class StoreTempHumDBTask extends AsyncTask<TempHumValue, Void,String> {

        @Override
        protected String doInBackground(TempHumValue... value) {
            TempHumValue tempHum = (TempHumValue) value[0];
            SensorMeasurement  temp = new SensorMeasurement("Temperature",
                    sens,
                    AppUtil.getTimeStamp(),
                    AppUtil.convertToInteger(tempHum.getTemp()));
            SensorMeasurement  hum = new SensorMeasurement("Humidity",
                    sens,
                    AppUtil.getTimeStamp(),
                    AppUtil.convertToInteger(tempHum.getHum()));

            DBStoreInterface store = DBInterfaceHolder.getCon();
            try {
                store.storeMeasurement(MainActivity.getLoggedInUser(), temp);
                store.storeMeasurement(MainActivity.getLoggedInUser(), hum);
            } catch(TalosModuleException e) {
                return "Failed";
            }
            return "Success";
        }

        @Override
        protected void onPreExecute() {
            progressBar.setProgress(0);
            result.setVisibility(View.INVISIBLE);
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(String res) {
            result.setVisibility(View.VISIBLE);
            result.setText(res);
            if(res.equals("Success"))
                result.setTextColor(Color.GREEN);
            else
                result.setTextColor(Color.RED);
            progressBar.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mDataField.setText(R.string.no_data);
        mDataField2.setText(R.string.no_data);
        mData.setText(R.string.data1);
        mData2.setText(R.string.data2);
        unregisterReceiver(mGattUpdateReceiver);
        mBluetoothLeService.disconnect();//disconnect from device
        updateConnectionState(R.string.disconnected);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resourceId);
            }
        });
    }

    private void displayData(String data1, String data2) {
        if (data1 != null && data2!=null) {
            synchronized (mutex) {
                curData = new TempHumValue(data1,data2);
                mDataField.setText(data1+ " Degree");
                mData.setText(R.string.temperature);
                mData2.setText(R.string.humidity);
                mDataField2.setText(data2+ " %");
            }
        }
    }
    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
        	if(gattService.getUuid().equals(RH_T_SERVICE)){//only add the RH&T Service to the list
        		
	            List<BluetoothGattCharacteristic> gattCharacteristics =
	                    gattService.getCharacteristics();
	
	            // Loops through available Characteristics.
	            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
	            	if(gattCharacteristic.getUuid().equals(H_T_CHARACTERISTIC)){//only add the H&T Characteristic
	            		
	            		BluetoothGattCharacteristic rht = new BluetoothGattCharacteristic(
	            				H_T_CHARACTERISTIC,
	            				BluetoothGattCharacteristic.PROPERTY_READ
	            				| BluetoothGattCharacteristic.PROPERTY_NOTIFY,
	            				BluetoothGattCharacteristic.PERMISSION_READ);
	            		CharTempHum = rht;
	            		gattService.addCharacteristic(rht);
		                
	            	}
	            }
	            
        	}
        }
        
        mBluetoothLeService.readCharacteristic(CharTempHum);//read characteristic to get values(notifications don't work)
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private static class TempHumValue {

        public String temp;
        public String hum;

        public TempHumValue(String temp, String hum) {
            this.temp = temp;
            this.hum = hum;
        }

        public String getTemp() {
            return temp;
        }

        public String getHum() {
            return hum;
        }
    }
}
