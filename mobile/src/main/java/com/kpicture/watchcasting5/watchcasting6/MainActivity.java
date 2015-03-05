package com.kpicture.watchcasting5.watchcasting6;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONObject;


public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent listenToWearable = new Intent(this, ListenToWearableService.class);

        Log.i("MainActivityPhone", "Indent created");

        // potentially add data to the intent
        //i.putExtra("KEY1", "Value to be used by the service");
        this.startService(listenToWearable);
        Log.i("MainActivityPhone", "Intent started");
        Button button = (Button)findViewById(R.id.recordBtn);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView info = (TextView)findViewById(R.id.info);
                info.setText("Button Pressed");
                try {
                    JSONObject jo = new JSONObject();
                    if (Communication.socket.isConnected()) {
                        Communication.socket.emit("record", jo);
                    }
                } catch (Exception e) {
                    Log.e("record", "Error emitting record message");
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
