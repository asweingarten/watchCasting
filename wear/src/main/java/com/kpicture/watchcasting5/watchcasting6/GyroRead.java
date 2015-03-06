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
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.nio.ByteBuffer;

public class GyroRead extends Service implements SensorEventListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private final int BUFFER_SIZE = 10000;
    private float[][] babs = new float[BUFFER_SIZE][5];
    private int start_row = 0;
    private int end_row = 0;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private Sensor gyroscope;
    private Sensor orientation;
    private SensorManager sensorManager;
    private GoogleApiClient mGoogleApiClient;
    private boolean sendData = false;
    private boolean listenToPhone = true;

    @Override
    public void onCreate() {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
//        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        orientation = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, orientation,SensorManager.SENSOR_DELAY_FASTEST);
        for (int i = 0; i < BUFFER_SIZE; i++){
            babs[i] = new float[5];
            babs[i][0] = 0;
            babs[i][1] = 0;
            babs[i][2] = 0;
            babs[i][3] = 0;
            babs[i][4] = 0;
        }

        // connect to Companion app
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();

        Wearable.MessageApi.addListener(mGoogleApiClient, new MessageApi.MessageListener() {
            @Override
            public void onMessageReceived(MessageEvent messageEvent) {
                Log.d("MESSAGE RECEIVED", "FROM PHONE");
                if (messageEvent.getData().length == 1 && listenToPhone) {
                    Log.d("MESSAGE RECEIVED", "WE LISTENED");
                    listenToPhone = false;
                    sendData = !sendData;
                }
            }
        });

    }

    // @TODO: determine which node is phone and which is watch

    @Override
    public void onConnected(Bundle bundle) {
       Log.e("MsgToSmartcastingApp", "Succesfully connected to SmartcastingApp1");

       new Thread(new Runnable() {
            @Override
            public void run() {

                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();

                while (true) {

                    try {

                        if (!sendData) continue;
                        Thread.sleep(2000);
                        listenToPhone = true;
                        sendData = false;

                        int current_end_row;

                        if (end_row < start_row) {
                            current_end_row = BUFFER_SIZE+end_row;
                        } else {
                            current_end_row = end_row;
                        }

                        int SIZE_OF_BYTE_BUFFER = (current_end_row-start_row+1)*20;
                        ByteBuffer bb;
                        byte[] bytebuffer = new byte[SIZE_OF_BYTE_BUFFER];
                        Log.e("SIZE", new Integer(SIZE_OF_BYTE_BUFFER).toString());

                        for (int i = 0; i < current_end_row-start_row; i++) {
                            for (int j = 0; j < 5; j++) {
                                int bits = Float.floatToIntBits(babs[(start_row+i)% BUFFER_SIZE][j]);
                                bb = ByteBuffer.allocate(4);
                                bb.putInt(bits);

                                bytebuffer[i*4*5+j*4+0] = bb.get(0);
                                bytebuffer[i*4*5+j*4+1] = bb.get(1);
                                bytebuffer[i*4*5+j*4+2] = bb.get(2);
                                bytebuffer[i*4*5+j*4+3] = bb.get(3);

                            }
                        }
                         for (Node node : nodes.getNodes()) {

                            MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), "",bytebuffer).await();

                            if (!result.getStatus().isSuccess()) {
                                Log.e("MsgToSmartcastingApp", "Message sending error");
                            } else {
                                Log.e("MsgToSmartcastingApp", "Message sent successfully to: " + node.getDisplayName());
                            }

                        }

                        // set new start (for next readout);
                        start_row = current_end_row % BUFFER_SIZE;

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            }
        }).start();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (!sendData) return;

        switch( event.sensor.getType() ) {
            case Sensor.TYPE_GRAVITY:
                babs[end_row][0] = 1;
                babs[end_row][1] = event.values[0];
                babs[end_row][2] = event.values[1];
                babs[end_row][3] = event.values[2];
                babs[end_row][4] = (float) event.timestamp;
                break;
            case Sensor.TYPE_LINEAR_ACCELERATION:
                babs[end_row][0] = 2;
                babs[end_row][1] = event.values[0];
                babs[end_row][2] = event.values[1];
                babs[end_row][3] = event.values[2];
                babs[end_row][4] = (float) event.timestamp;
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                babs[end_row][0] = 3;
                babs[end_row][1] = event.values[0];
                babs[end_row][2] = event.values[1];
                babs[end_row][3] = event.values[2];
                babs[end_row][4] = (float) event.timestamp;
                break;
            case Sensor.TYPE_ORIENTATION:
                babs[end_row][0] = 4;
                babs[end_row][1] = event.values[0];
                babs[end_row][2] = event.values[1];
                babs[end_row][3] = event.values[2];
                babs[end_row][4] = (float) event.timestamp;
                break;
            case Sensor.TYPE_GYROSCOPE:
                babs[end_row][0] = 5;
                babs[end_row][1] = event.values[0];
                babs[end_row][2] = event.values[1];
                babs[end_row][3] = event.values[2];
                babs[end_row][4] = (float) event.timestamp;
                break;
            default:
                return;
        }
        end_row = (end_row + 1) % BUFFER_SIZE;

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.e("WearableSensor", "Accuracy of sensor changed");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e("MsgToSmartcastingApp", "Connection suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e("MsgToSmartcastingApp", "Connection Failed");
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
}
