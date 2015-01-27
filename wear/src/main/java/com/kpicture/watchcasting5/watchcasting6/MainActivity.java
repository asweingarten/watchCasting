package com.kpicture.watchcasting5.watchcasting6;

import android.app.Activity;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.widget.TextView;
import android.content.Intent;
import android.content.Context;

public class MainActivity extends Activity {

    private TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.text);
            }
        });

        // use this to start and trigger a service
        Intent i = new Intent(this, GyroRead.class);
        Log.i("MainActivity", "Indent created");

        // potentially add data to the intent
        //i.putExtra("KEY1", "Value to be used by the service");
        this.startService(i);
        Log.i("MainActivity", "Intent started");

    }
}
