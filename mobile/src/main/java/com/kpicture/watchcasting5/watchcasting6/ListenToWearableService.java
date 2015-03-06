package com.kpicture.watchcasting5.watchcasting6;

import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

public class ListenToWearableService extends WearableListenerService implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private IOSocket socket;
    private AccelerometerListener accelListener;
    private float lastTimestamp = 0;
    private GoogleApiClient mGoogleApiClient;

    @Override
    public void onConnected(Bundle bundle) {
        Log.e("MsgToSmartcastingApp", "Succesfully connected to watch");
        new Thread(new Runnable() {
            @Override
            public void run() {

                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();

                while (true) {
                    if (Communication.delimeterDetected) {
                        Communication.delimeterDetected = false;
                        Log.d("to watch message", "num nodes: " + nodes.getNodes().size());

                        // @TODO: debounce delimeter, ensure message gets to watch
                        try {

                            for (Node node : nodes.getNodes()) {

                                MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), "",new byte[1]).await();
                                Log.d("to watch message", ""+result.getStatus());
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                }
            }
        }).start();
    }

    @Override
    public void onCreate() {
        socket = Communication.socket;
        socket.connect();
        accelListener = new AccelerometerListener((SensorManager)getSystemService(SENSOR_SERVICE));

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
                Log.d("nodes", ""+getConnectedNodesResult.getNodes().size());
            }
        });

        Log.e("PhoneConnection","Phone connected? :"+mGoogleApiClient.isConnected());

        Wearable.MessageApi.addListener(mGoogleApiClient, new MessageApi.MessageListener() {

            byte[] inboundMessage;
            JSONObject outboundMessage = new JSONObject();
            @Override
            public void onMessageReceived(MessageEvent messageEvent) {
                if (messageEvent.getData().length == 1) return;

                inboundMessage = messageEvent.getData();
                outboundMessage = extractMessage(inboundMessage, outboundMessage);

            }
        });

    }

    private JSONObject extractMessage(byte[] inboundMessage, JSONObject outboundMessage) {
        float sensorType;
        float sensorVal1;
        float sensorVal2;
        float sensorVal3;
        float timestamp;

        sensorType = 0;
        sensorVal1 = -999;
        sensorVal2 = -999;
        sensorVal3 = -999;
        timestamp  = -999;
        for(int i = 0; i < inboundMessage.length; i+=20) {
            sensorType = floatFromBytes(inboundMessage, i);
            sensorVal1 = floatFromBytes(inboundMessage, i + 4);
            sensorVal2 = floatFromBytes(inboundMessage, i + 8);
            sensorVal3 = floatFromBytes(inboundMessage, i + 12);
            timestamp = floatFromBytes(inboundMessage, i + 16);
            try {
                outboundMessage.put("sensor", sensorNameFromId(Math.round(sensorType)));
                outboundMessage.put("x", sensorVal1);
                outboundMessage.put("y", sensorVal2);
                outboundMessage.put("z", sensorVal3);
                outboundMessage.put("timestamp", timestamp);

                if (socket.isConnected()) {
                    try {
                        socket.emit("gyro", outboundMessage);
                    } catch (Exception e) {
                        Log.e("emitting message", "could not broadcast");
                    }
                }
            } catch (Exception e) {

            }
        }

        return outboundMessage;
    }

    private float floatFromBytes(byte[] sourceArray, int start) {
        int intBits;
        byte[] tempArray = new byte[4];
        ByteBuffer bb;

        for (int i = 0; i < 4; i++) {
            tempArray[i] = sourceArray[start+i];
        }

        bb = ByteBuffer.wrap(tempArray);

        intBits = bb.getInt();
        return Float.intBitsToFloat(intBits);
    }

    private String sensorNameFromId(int sensorId) {
        switch(sensorId) {
            case 1:
                return "GRAVITY";
            case 2:
                return "ACCELEROMETER";
            case 3:
                return "MAGNETOMETER";
            case 4:
                return "GYRO";
            default:
                return "BAD ID";
        }
    }

    private void emitDelimeter(float timestamp) {
        if (timestamp - lastTimestamp < 1000000000) {
            Communication.delimeterDetected = false;
            return;
        }
        lastTimestamp = timestamp;
        Log.d("DELIMITER", "DELIMETER EMMITTED");
        JSONObject delimeter = new JSONObject();
        try {
            delimeter.put("DELIMETER", timestamp);
            socket.emit("gyro", delimeter);
        }
        catch (Exception e) {
//
        }
        Communication.delimeterDetected = false;

    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e("MsgToSmartcastingApp", "Connection suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e("MsgToSmartcastingApp", "Connection Failed");
    }

}

