package com.kpicture.watchcasting5.watchcasting6;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;


import org.json.JSONObject;


public class AccelerometerListener implements SensorEventListener {

    private Sensor accelerometer;
    private SensorManager sensorManager;

    public AccelerometerListener(SensorManager sensorManager) {
        this.sensorManager = sensorManager;
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (Math.abs(event.values[0]) > 7.5f ||
                Math.abs(event.values[1]) > 7.5f ||
                Math.abs(event.values[2]) > 7.5f) {
//            Log.d("phoneAccel", "OVER_THRESHOLD: " + event.values[0] + " " + event.values[1] + " " + event.values[2]);
            Communication.delimeterDetected = true;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.e("WearableSensor", "Accuracy of sensor changed");
    }
}
