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
                    JSONObject message = new JSONObject(new String(messageEvent.getData()));
                    if (socket.isConnected()) {
                        socket.emit("gyro", message);
                        Log.d("server", "SENT MESSAGE TO SERVER");
                    }
//                    Log.d("FINALLY::", ",alpha="+(String) message.getString("alpha") + ",beta="+ (String) message.getString("beta") + ",gamma="+(String) message.getString("gamma"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });


    }


}

