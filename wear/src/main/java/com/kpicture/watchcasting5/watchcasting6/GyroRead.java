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
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import org.json.JSONException;
import org.json.JSONObject;

public class GyroRead extends Service implements SensorEventListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private TextView mTextView;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private Sensor gyroscope;
    private Sensor orientationS;
    private SensorManager sensorManager;
    private float[] mGravity;
    private float[] mGeomagnetic;
    //private float orientation[];
    private int i;
    private float DEG = 57.2957795f;
    private final String TAG = "GYRO::";
    private GoogleApiClient mGoogleApiClient;
    JSONObject message, oldmessage;
    NodeApi.GetConnectedNodesResult nodes;
    float[] gData = new float[3];           // Gravity or accelerometer
    float[] mData = new float[3];           // Magnetometer
    float[] orientation = new float[3];
    float[] Rmat = new float[9];
    float[] R2 = new float[9];
    float[] Imat = new float[9];
    boolean haveGrav = false;
    boolean haveAccel = false;
    boolean haveMag = false;

    @Override
    public void onCreate() {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        orientationS = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, orientationS,SensorManager.SENSOR_DELAY_FASTEST);
        message = new JSONObject();
        oldmessage = new JSONObject();

        // connect to Companion app
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();

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

       new Thread(new Runnable() {
            @Override
            public void run() {

                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();

                while (true) {
                    try {
                       // if (!(oldmessage.getString("alpha").equals(message.getString("alpha")))) {

                           oldmessage = message;

                           for (Node node : nodes.getNodes()) {

                                MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), "", message.toString().getBytes()).await();

                                if (!result.getStatus().isSuccess()) {
                                    Log.e("MessageToSmartcastingApp", "Message sending error");
                                } else {
                                   // Log.e("MessageToSmartcastingApp", "Message sent successfully to: " + node.getDisplayName());
                                }
                            }
                       // }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
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
      //  Log.e("WearableSensor", "Sensor read!");

/*        float[] lastRotVal = new float[3];
        float[] rotation = new float[9];
        float[] orientation = new float[3];

        try{
            System.arraycopy(event.values, 0, lastRotVal, 0, event.values.length);
        } catch (IllegalArgumentException e) {
            //Hardcode the size to handle a bug on Samsung devices running Android 4.3
            System.arraycopy(event.values, 0, lastRotVal, 0, 3);
        }

        SensorManager.getRotationMatrixFromVector(rotation, lastRotVal);
        SensorManager.getOrientation(rotation, orientation);
*/
/*        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            mGravity = event.values;
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            mGeomagnetic = event.values;
        if (mGravity != null && mGeomagnetic != null) {
            float R[] = new float[9];
            float I[] = new float[9];
            oldmessage=message;
            boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
            if (success) {
                orientation = new float[3];
                SensorManager.getOrientation(R, orientation);
                if (orientation[0] != Float.NaN && orientation[1] != Float.NaN && orientation[2] != Float.NaN) {
                    try {
                        message.put("alpha", -57.2957795*orientation[0]);
                        message.put("gamma", -57.2957795*orientation[1]);
                        message.put("beta", (-57.2957795*orientation[2]));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        */

       try {
            if (event.sensor.getType() == Sensor.TYPE_ORIENTATION) {
                message.put("alpha", -(event.values[0]+180));
                message.put("gamma", event.values[1]);
                message.put("beta", event.values[2]+30);
                //Log.e("WearableSensor", "Sensor read!: "+ event.values[0] +", "+event.values[1]+", "+event.values[2]);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
/*
        float[] data;
        switch( event.sensor.getType() ) {
            case Sensor.TYPE_GRAVITY:
                gData[0] = event.values[0];
                gData[1] = event.values[1];
                gData[2] = event.values[2];
                haveGrav = true;
                break;
            case Sensor.TYPE_ACCELEROMETER:
                if (haveGrav) break;    // don't need it, we have better
                gData[0] = event.values[0];
                gData[1] = event.values[1];
                gData[2] = event.values[2];
                haveAccel = true;
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                mData[0] = event.values[0];
                mData[1] = event.values[1];
                mData[2] = event.values[2];
                haveMag = true;
                break;
            default:
                return;
        }

        if ((haveGrav || haveAccel) && haveMag) {
            SensorManager.getRotationMatrix(Rmat, Imat, gData, mData);
            SensorManager.remapCoordinateSystem(Rmat,
                    SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X, R2);
            // Orientation isn't as useful as a rotation matrix, but
            // we'll show it here anyway.
            SensorManager.getOrientation(R2, orientation);
            float incl = SensorManager.getInclination(Imat);
           // Log.d(TAG, "mh: " + (int)(orientation[0]*DEG));
            try {
                message.put("beta", -(int)(orientation[1]*DEG));
                message.put("gamma",  -(int)(orientation[2]*DEG));
                message.put("alpha", -(int)(orientation[0]*DEG));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            //Log.d(TAG, "pitch: " + (int)(orientation[1]*DEG));
            //Log.d(TAG, "roll: " + (int)(orientation[2]*DEG));
            //Log.d(TAG, "yaw: " + (int)(orientation[0]*DEG));
           // Log.d(TAG, "inclination: " + (int)(incl*DEG));
        }*/
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.e("WearableSensor", "Accuracy of sensor changed");
    }
}
