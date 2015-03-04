package com.kpicture.watchcasting5.watchcasting6;

import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.ByteBuffer;

public class ListenToWearableService extends WearableListenerService {

    private IOSocket socket;
    @Override
    public void onCreate() {
          socket = Communication.socket;

        socket.connect();


     GoogleApiClient mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        Log.e("DATA API LOG:", "onConnected: " + connectionHint);
                        // Now you can use the Data Layer API
                    }
                    @Override
                    public void onConnectionSuspended(int cause) {
                        Log.e("DATA API LOG:", "onConnectionSuspended: " + cause);
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        Log.e("DATA API LOG:", "onConnectionFailed: " + result);
                    }
                })
                        // Request access only to the Wearable API
                .addApi(Wearable.API)
                .build();

        mGoogleApiClient.connect();

        Log.e("PhoneConnection","Phone connected? :"+mGoogleApiClient.isConnected());

        Wearable.MessageApi.addListener(mGoogleApiClient, new MessageApi.MessageListener() {
            @Override
            public void onMessageReceived(MessageEvent messageEvent) {
                try {
//                    JSONObject message = new JSONObject(new String(messageEvent.getData()));
                    byte[] inboundMessage = messageEvent.getData();
                    float sensorType = 0;
                    float sensorVal1 = -999;
                    float sensorVal2 = -999;
                    float sensorVal3 = -999;
                    float timestamp  = -999;
                    for(int i = 0; i < inboundMessage.length; i+=20) {
                        sensorType = floatFromBytes(inboundMessage, i);
                        sensorVal1 = floatFromBytes(inboundMessage, i+4);
                        sensorVal2 = floatFromBytes(inboundMessage, i+8);
                        sensorVal3 = floatFromBytes(inboundMessage, i+12);
                        timestamp  = floatFromBytes(inboundMessage, i+16);

                        JSONObject outboundMessage = new JSONObject();
                        outboundMessage.put("sensor", sensorNameFromId(Math.round(sensorType)));
                        outboundMessage.put("x", sensorVal1);
                        outboundMessage.put("y", sensorVal2);
                        outboundMessage.put("z", sensorVal3);
                        outboundMessage.put("timestamp", timestamp);
                        if (socket.isConnected())
                            socket.emit("gyro", outboundMessage);
                    }


                    // @TODO: put in delimeter

                    if (socket.isConnected()) {
//                        socket.emit("gyro", message);
                        Log.d("server", "SENT MESSAGE TO SERVER");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

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
        Log.d("sensorID", ""+sensorId);
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




}

