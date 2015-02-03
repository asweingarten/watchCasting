package com.kpicture.watchcasting5.watchcasting6;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
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

import java.util.ArrayList;
import java.util.List;

import javax.sql.CommonDataSource;

public class GyroRead extends Service implements SensorEventListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private ConcurrentBuffer messageBufferSystem = new ConcurrentBuffer();
    private Node phone;
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
    List<JSONObject> messageBuffer = new ArrayList<JSONObject>();
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
            phone = nodes.getNodes().get(0);
//                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();

            while (true) {
                try {
//                        oldmessage = message;
                    List<JSONObject> messages = messageBufferSystem.getMessages();
                    for (JSONObject m : messages) {
                        MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(mGoogleApiClient, phone.getId(), "", m.toString().getBytes()).await();

//                            if (!result.getStatus().isSuccess()) {
//                                Log.e("MessageToSmartcastingApp", "Message sending error");
//                            } else {
//
//                                // Log.e("MessageToSmartcastingApp", "Message sent successfully to: " + node.getDisplayName());
//                            }
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

       JSONObject sensorMessage = new JSONObject();
       try {
           switch( event.sensor.getType() ) {
               case Sensor.TYPE_GRAVITY:
                   sensorMessage.put("gravityX", event.values[0]);
                   sensorMessage.put("gravityY",event.values[1]);
                   sensorMessage.put("gravityZ",event.values[2]);
                   break;
               case Sensor.TYPE_ACCELEROMETER:
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
            messageBufferSystem.addMessage(sensorMessage);
//           messageBuffer.add(sensorMessage);
//           message = sensorMessage;
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.e("WearableSensor", "Accuracy of sensor changed");
    }


    private class ConcurrentBuffer {
        public List<JSONObject> messageBufferOne = new ArrayList<JSONObject>();
        public List<JSONObject> messageBufferTwo = new ArrayList<JSONObject>();
        private List<JSONObject> writeMessageBuffer = messageBufferOne;
        private boolean onMessageBufferOne = true;

        synchronized public void addMessage(JSONObject message) {
            writeMessageBuffer.add(message);
        }

        synchronized public List<JSONObject> getMessages() {
            return swapBuffers();
        }

        synchronized private List<JSONObject> swapBuffers() {
            if (onMessageBufferOne) {
                messageBufferTwo.clear();
                writeMessageBuffer = messageBufferTwo;
                return messageBufferOne;
            } else {
                messageBufferOne.clear();
                writeMessageBuffer = messageBufferOne;
                return messageBufferTwo;
            }
        }
    }
}
