package com.carapp;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends ComActivity {

    TextView status ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.status = this.findViewById(R.id.status);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        Log.v( "sunabove", "onResume");

        status.setText( "Checking the server connectivity.\n Wait seconds, please!" );

        this.postDelayed(new Runnable() {
            @Override
            public void run() {
                checkServer();
            }
        }, 2000);

    }

    private void checkServer() {
        try {
            URL url = new URL("http://www.android.com/");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout( 10_000 ); // timing out in a minute

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String str;
            while ((str = in.readLine()) != null) {
                //
            }

            startActivity(new android.content.Intent( this, com.carapp.CarActivity.class));
        } catch( Exception  e ){
            e.printStackTrace();

        }

    }
}
