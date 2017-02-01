package ch.ethz.inf.vs.talsomoduleapp.sensors;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

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

public class SensorActivity extends ActionBarActivity implements SensorEventListener {

	private Sensor sensor;
	private PhoneSensor dbsensor;
	private ArrayAdapter<String> adapter;
	private SensorManager mSensorManager;
	private ProgressBar progressBar;
	private TextView stateStore;

	private Object mutex = new Object();
	private ArrayList<String> stringValues;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sensor);
		mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
		final List<Sensor> allSensors= mSensorManager.getSensorList(Sensor.TYPE_ALL);
		Bundle bundle = getIntent().getExtras();
		sensor = getSenorWithName(bundle.getString("sensorName"), allSensors);
		adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1);
		mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);

		dbsensor = new PhoneSensor(sensor.getName(),sensor.getName(),"Nexus 5", sensor.getVendor(), SensorDescription.getSensorDescription(sensor));
		
		TextView text_sensorname = (TextView) findViewById(R.id.text_sensorname);
		TextView text_power_val = (TextView) findViewById(R.id.text_power_val);
		TextView text_vendor_val = (TextView) findViewById(R.id.text_vendor_val);
		TextView text_maxrange_val = (TextView) findViewById(R.id.text_maxrange_val);
		TextView text_resolution_val = (TextView) findViewById(R.id.text_resolution_val);
		TextView text_version_val = (TextView) findViewById(R.id.text_version_val);
		stateStore = (TextView) findViewById(R.id.stateStore);
		stateStore.setVisibility(View.INVISIBLE);
		progressBar = (ProgressBar) findViewById(R.id.progressBar4);
		progressBar.setVisibility(View.INVISIBLE);
		ListView sensorValsListView = (ListView) findViewById(R.id.list_sensorvals);
		sensorValsListView.setOnItemClickListener(clickListener);
		
		//GUI
		text_sensorname.setText(sensor.getName());
		text_power_val.setText(String.valueOf(sensor.getPower()));
		text_vendor_val.setText(sensor.getVendor());
		text_maxrange_val.setText(String.valueOf(sensor.getMaximumRange()));
		text_resolution_val.setText(String.valueOf(sensor.getResolution()));
		text_version_val.setText(String.valueOf(sensor.getVersion()));
		sensorValsListView.setAdapter(adapter);
		
	}

	private AdapterView.OnItemClickListener clickListener = new AdapterView.OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			String value;
			synchronized (mutex) {
				value = stringValues.get(position);
			}
			SensorMeasurement cur = new SensorMeasurement("Data"+id,dbsensor, AppUtil.getTimeStamp(),AppUtil.convertToInteger(value));
			new StoreTempHumDBTask().execute(cur);
		}

	};

	private class StoreTempHumDBTask extends AsyncTask<SensorMeasurement, Void,String> {

		@Override
		protected String doInBackground(SensorMeasurement... value) {
			SensorMeasurement measure = (SensorMeasurement) value[0];
			DBStoreInterface store = DBInterfaceHolder.getCon();
			try {
				store.storeMeasurement(MainActivity.getLoggedInUser(), measure);
			} catch(TalosModuleException e) {
				return "Failed";
			}
			return "Success";
		}

		@Override
		protected void onPreExecute() {
			progressBar.setProgress(0);
			stateStore.setVisibility(View.INVISIBLE);
			progressBar.setVisibility(View.VISIBLE);
		}

		@Override
		protected void onPostExecute(String res) {
			stateStore.setVisibility(View.VISIBLE);
			stateStore.setText(res);
			if(res.equals("Success"))
				stateStore.setTextColor(Color.GREEN);
			else
				stateStore.setTextColor(Color.RED);
			progressBar.setVisibility(View.INVISIBLE);
		}
	}
	
	private Sensor getSenorWithName(String name, List<Sensor> allSensors) {
		for(Sensor s: allSensors) 
    		if(s.getName().equals(name))
    			return s;
		return null;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.sensor, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			Intent myIntent = new Intent(SensorActivity.this, MainActivity.class);
			startActivity(myIntent);
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		mSensorManager.unregisterListener(this);
	}
	

	@SuppressLint("NewApi") @Override
	public void onSensorChanged(SensorEvent event) {
		synchronized (mutex) {
			float[] values = event.values;
			stringValues = new ArrayList<String>();
			for (int i = 0; i < values.length; i++)
				stringValues.add(String.valueOf(values[i]));
			this.adapter.clear();
			this.adapter.addAll(stringValues);
		}
	}
	
	public void goHome(View	v)	{
		Intent	myIntent	=	new Intent(this,SensorMain.class);
		this.startActivity(myIntent);
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}
	
	
}
