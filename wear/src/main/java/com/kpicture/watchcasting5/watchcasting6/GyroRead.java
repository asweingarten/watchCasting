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

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import org.json.JSONException;
import org.json.JSONObject;

public class GyroRead extends Service implements SensorEventListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private Node phone;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private Sensor gyroscope;
    private Sensor orientationS;
    private SensorManager sensorManager;
    private float DEG = 57.2957795f;
    private final String TAG = "GYRO::";
    private GoogleApiClient mGoogleApiClient;
    boolean haveGrav = false;

    @Override
    public void onCreate() {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        orientationS = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, orientationS,SensorManager.SENSOR_DELAY_FASTEST);

        // connect to Companion app
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();

        Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
            @Override
            public void onResult(NodeApi.GetConnectedNodesResult getConnectedNodesResult) {
                phone = getConnectedNodesResult.getNodes().get(0);

            }
        });

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
        //throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onConnected(Bundle bundle) {
       Log.e("MessageToSmartcastingApp", "Succesfully connected to SmartcastingApp1");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e("MessageToSmartcastingApp", "Connection suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e("MessageToSmartcastingApp", "Connection Failed");
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

       final JSONObject sensorMessage = new JSONObject();
       try {
           switch( event.sensor.getType() ) {
               case Sensor.TYPE_GRAVITY:
                   sensorMessage.put("gravityX", event.values[0]);
                   sensorMessage.put("gravityY",event.values[1]);
                   sensorMessage.put("gravityZ",event.values[2]);
                   break;
               case Sensor.TYPE_ACCELEROMETER:
                   if (haveGrav) break;
                   sensorMessage.put("accX",event.values[0]);
                   sensorMessage.put("accY",event.values[1]);
                   sensorMessage.put("accZ",event.values[2]);
                   break;
               case Sensor.TYPE_LINEAR_ACCELERATION:
                   if (haveGrav) break;    // don't need it, we have better
                   sensorMessage.put("accX",event.values[0]);
                   sensorMessage.put("accY",event.values[1]);
                   sensorMessage.put("accZ",event.values[2]);
                   break;
               case Sensor.TYPE_MAGNETIC_FIELD:
                   sensorMessage.put("magnetX",event.values[0]);
                   sensorMessage.put("magnetY",event.values[1]);
                   sensorMessage.put("magnetZ",event.values[2]);
                   break;
               case Sensor.TYPE_GYROSCOPE:
                   sensorMessage.put("gyroscopeX",event.values[0]);
                   sensorMessage.put("gyroscopeY",event.values[1]);
                   sensorMessage.put("gyroscopeZ",event.values[2]);
                   break;
               case Sensor.TYPE_ORIENTATION:
                   sensorMessage.put("alpha", -(event.values[0]+180));
                   sensorMessage.put("gamma", event.values[1]);
                   sensorMessage.put("beta", event.values[2]+30);
               default:
                break;
           }
           sensorMessage.put("timestamp", event.timestamp);
           if (phone != null) {
               Wearable.MessageApi.sendMessage(mGoogleApiClient, phone.getId(), "", sensorMessage.toString().getBytes()).setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                   @Override
                   public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                   }
               });
           }

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.e("WearableSensor", "Accuracy of sensor changed");
    }
}
