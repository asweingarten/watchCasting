package com.kpicture.watchcasting5.watchcasting6;

import android.util.Log;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Communication {
    public static Boolean delimeterDetected = false;
    public static IOSocket socket = new IOSocket("http://10.0.1.19:3000", new MessageCallback() {

        @Override
        public void onMessage(String message) {
            // Handle simple messages
        }

        @Override
        public void onConnect() {
            // Socket connection opened
            System.out.println("Connection to server established");
            Log.i("onConnect", "Connected to server established");

        }

        @Override
        public void onDisconnect() {
            // Socket connection closed
        }

        @Override
        public void on(String event, JSONObject... data) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onMessage(JSONObject json) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onConnectFailure() {
            Log.e("socketio", "connection failed");
            // TODO Auto-generated method stub

        }
    });
}
