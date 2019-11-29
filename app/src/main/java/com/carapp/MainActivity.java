package com.carapp;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends ComActivity {

    boolean isActive = false ;
    boolean serverActive = false ;
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

        this.isActive = true ;
        serverActive = false ;

        checkServer();
    }

    private void checkServer() {

        status.setText( "Checking the server connectivity.\n Wait seconds, please!" );

        new Thread(new Runnable() {
            @Override
            public void run() {
                while( ! serverActive && isActive ) {
                    try {
                        URL url = new URL("http://10.3.141.1/info.html");
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        //conn.setConnectTimeout( 10_000 ); // timing out in a minute

                        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        String str;
                        while ((str = in.readLine()) != null) {
                            //
                        }
                        serverActive = true;

                        Thread.currentThread().sleep( 2_000 );
                    } catch (Exception e) {
                        //e.printStackTrace();
                    }
                }
            }
        }).start();

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d("sunabove", "run: postDelayerd");
                if( serverActive ) {
                    isActive = false ;
                    startActivity(new android.content.Intent( MainActivity.this, com.carapp.CarActivity.class));
                } else {
                    handler.postDelayed( this, 3_000 );
                }
            }
        }, 3000);
    }
}
