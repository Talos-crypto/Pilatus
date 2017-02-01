package ch.ethz.inf.vs.talsomoduleapp.ble;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.UUID;

import ch.ethz.inf.vs.talsomoduleapp.MainActivity;
import ch.ethz.inf.vs.talsomoduleapp.R;

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

public class BleActivity extends ActionBarActivity {

	  private static final UUID RH_T_SERVICE = UUID.fromString("0000AA20-0000-1000-8000-00805f9b34fb");
	  private static final UUID H_T_CHARACTERISTIC = UUID.fromString("0000AA21-0000-1000-8000-00805f9b34fb");
	
	  private LeDeviceListAdapter mLeDeviceListAdapter;
	    private BluetoothAdapter mBluetoothAdapter;
	    private boolean mScanning;
	    private Handler mHandler;
		private ListView mListView;
	private ProgressBar mProgress;

	    private static final int REQUEST_ENABLE_BT = 1;
	    // Stops scanning after 10 seconds.
	    private static final long SCAN_PERIOD = 10000;

	    @Override
	    public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
			setContentView(R.layout.activity_ble);
	        //getActionBar().setTitle(R.string.title_devices);
	        mHandler = new Handler();

	        // Use this check to determine whether BLE is supported on the device.  Then you can
	        // selectively disable BLE-related features.
	        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
	            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
	            finish();
	        }

	        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
	        // BluetoothAdapter through BluetoothManager.
	        final BluetoothManager bluetoothManager =
	                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
	        mBluetoothAdapter = bluetoothManager.getAdapter();

	        // Checks if Bluetooth is supported on the device.
	        if (mBluetoothAdapter == null) {
	            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
	            finish();
	            return;
	        }

			mListView = (ListView) findViewById(R.id.listBLE);
			mProgress = (ProgressBar) findViewById(R.id.progressBar5);
			mProgress.setVisibility(View.INVISIBLE);

			mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
					final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
					if (device == null) return;
					final Intent intent = new Intent(BleActivity.this, DeviceControlActivity.class);
					intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.getName());
					intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
					if (mScanning) {
						mBluetoothAdapter.stopLeScan(mLeScanCallback);
						mScanning = false;
					}
					startActivity(intent);
				}
			});
	    }

		public void onHome(View Holder) {
			Intent intent = new Intent(this, MainActivity.class);
			startActivity(intent);
		}

	    @Override
	    public boolean onCreateOptionsMenu(Menu menu) {
	        getMenuInflater().inflate(R.menu.main, menu);
	        if (!mScanning) {
	            menu.findItem(R.id.menu_stop).setVisible(false);
	            menu.findItem(R.id.menu_scan).setVisible(true);
	            menu.findItem(R.id.menu_refresh).setActionView(null);
				mProgress.setVisibility(View.INVISIBLE);
	        } else {
	            menu.findItem(R.id.menu_stop).setVisible(true);
	            menu.findItem(R.id.menu_scan).setVisible(false);
				mProgress.setVisibility(View.VISIBLE);
				mProgress.setProgress(0);
	        }
	        return true;
	    }

	    @Override
	    public boolean onOptionsItemSelected(MenuItem item) {

	        switch (item.getItemId()) {
	            case R.id.menu_scan:
	                mLeDeviceListAdapter.clear();
					mProgress.setVisibility(View.VISIBLE);
					mProgress.setProgress(0);
	                scanLeDevice(true);
	                break;
	            case R.id.menu_stop:
	                scanLeDevice(false);
					mProgress.setVisibility(View.INVISIBLE);
	                break;
				case android.R.id.home:
					onBackPressed();
					return true;
	        }
	        return true;
	    }

	    @Override
	    protected void onResume() {
	        super.onResume();

	        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
	        // fire an intent to display a dialog asking the user to grant permission to enable it.
	        if (!mBluetoothAdapter.isEnabled()) {
	            if (!mBluetoothAdapter.isEnabled()) {
	                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
	                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
	            }
	        }

	        // Initializes list view adapter.
	        mLeDeviceListAdapter = new LeDeviceListAdapter();
			mListView.setAdapter(mLeDeviceListAdapter);
	        scanLeDevice(true);
	    }

	    @Override
	    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	        // User chose not to enable Bluetooth.
	        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
	            finish();
	            return;
	        }
	        super.onActivityResult(requestCode, resultCode, data);
	    }

	    @Override
	    protected void onPause() {
	        super.onPause();
	        scanLeDevice(false);
	        mLeDeviceListAdapter.clear();
	    }

	    private void scanLeDevice(final boolean enable) {
	        if (enable) {
	            // Stops scanning after a pre-defined scan period.
	            mHandler.postDelayed(new Runnable() {
	                @Override
	                public void run() {
	                    mScanning = false;
	                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
	                    invalidateOptionsMenu();
	                }
	            }, SCAN_PERIOD);

	            mScanning = true;
	            mBluetoothAdapter.startLeScan(mLeScanCallback);//new UUID[]{RH_T_SERVICE},
	        } else {
	            mScanning = false;
	            mBluetoothAdapter.stopLeScan(mLeScanCallback);
	        }
	        invalidateOptionsMenu();
	    }

	    // Adapter for holding devices found through scanning.
	    private class LeDeviceListAdapter extends BaseAdapter {
	        private ArrayList<BluetoothDevice> mLeDevices;
	        private LayoutInflater mInflator;

	        public LeDeviceListAdapter() {
	            super();
	            mLeDevices = new ArrayList<BluetoothDevice>();
	            mInflator = BleActivity.this.getLayoutInflater();
	        }

	        public void addDevice(BluetoothDevice device) {
	            if(!mLeDevices.contains(device) && (device.getName() != null) && device.getName().equals("SHTC1 smart gadget")) { //only display SHTC1 smart gadget devices
	                mLeDevices.add(device);
	            }
	        }

	        public BluetoothDevice getDevice(int position) {
	            return mLeDevices.get(position);
	        }

	        public void clear() {
	            mLeDevices.clear();
	        }

	        @Override
	        public int getCount() {
	            return mLeDevices.size();
	        }

	        @Override
	        public Object getItem(int i) {
	            return mLeDevices.get(i);
	        }

	        @Override
	        public long getItemId(int i) {
	            return i;
	        }

	        @Override
	        public View getView(int i, View view, ViewGroup viewGroup) {
	            ViewHolder viewHolder;
	            // General ListView optimization code.
	            if (view == null) {
	                view = mInflator.inflate(R.layout.listitem_device, null);
	                viewHolder = new ViewHolder();
	                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
	                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
	                view.setTag(viewHolder);
	            } else {
	                viewHolder = (ViewHolder) view.getTag();
	            }

	            BluetoothDevice device = mLeDevices.get(i);
	            final String deviceName = device.getName();
	            if (deviceName != null && deviceName.length() > 0)
	                viewHolder.deviceName.setText(deviceName);
	            else
	                viewHolder.deviceName.setText(R.string.unknown_device);
	            viewHolder.deviceAddress.setText(device.getAddress());

	            return view;
	        }
	    }

	    // Device scan callback.
	    private BluetoothAdapter.LeScanCallback mLeScanCallback =
	            new BluetoothAdapter.LeScanCallback() {

	        @Override
	        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
	            runOnUiThread(new Runnable() {
	                @Override
	                public void run() {
	                    mLeDeviceListAdapter.addDevice(device);
	                    mLeDeviceListAdapter.notifyDataSetChanged();
	                }
	            });
	        }
	    };

	    static class ViewHolder {
	        TextView deviceName;
	        TextView deviceAddress;
	    }


    
}
