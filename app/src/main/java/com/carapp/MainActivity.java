package com.carapp;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends ComActivity {

    private boolean activityAlive = false ;
    private boolean serverActive = false ;
    private TextView status ;
    private TextView error ;
    private TextView wifi ;
    private TextView ipaddr ;

    private String errorMessage = "" ;

    private int mode = 0 ;

    public int getLayoutId() {
        return R.layout.activity_main ;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.status = this.findViewById(R.id.status);
        this.error = this.findViewById(R.id.error);

        this.wifi = this.findViewById(R.id.wifi);
        this.ipaddr = this.findViewById(R.id.ipaddr);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        Log.v( "sunabove", "onResume");

        this.activityAlive = true ;
        this.serverActive = false ;

        this.wifi.setText( this.getWifiSsid() );
        this.ipaddr.setText( this.getIpAddr() );

        this.errorMessage = "";
        this.error.setText( errorMessage );

        this.checkServer();
    }

    private void checkServer() {

        status.setTextColor(Color.parseColor("#009688"));
        status.setText( "서버 연결중입니다.\n잠시만 기다려 주세요!" );

        this.wifi.setText( this.getWifiSsid() );
        this.ipaddr.setText( this.getIpAddr() );

        this.serverActive = false ;

        new Thread(new Runnable() {
            @Override
            public void run() {
                while( ! serverActive && activityAlive) {
                    try {
                        mode = 1;
                        sleep( 3000 );

                        URL url = new URL("http://10.3.141.1/info.html");

                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                        conn.setConnectTimeout(5_000); //set timeout to 5 seconds
                        conn.connect();

                        BufferedReader in ;
                        if (200 <= conn.getResponseCode() && conn.getResponseCode() <= 299) {
                            in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        } else {
                            in = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                        }

                        while ( in.readLine() != null) {
                            // do nothing.
                        }

                        serverActive = true;
                        errorMessage = "";

                        sleep( 3_000 );

                        mode = 2;

                    } catch (Exception e) {
                        mode = 3 ;

                        errorMessage = e.getMessage();

                        e.printStackTrace();
                    }

                    sleep( 5_000 );
                }
            }
        }).start();

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d("sunabove", "run: postDelayerd");
                error.setText( errorMessage );

                wifi.setText( getWifiSsid() );
                ipaddr.setText( getIpAddr() );

                if( 1 == mode ) {
                    status.setTextColor(Color.parseColor("#009688"));
                    status.setText( "서버 연결중입니다.\n잠시만 기다려 주세요!" );
                } else if ( 2 == mode ) {
                    status.setTextColor(Color.parseColor("#009688"));
                    status.setText( "서버 연결에 성공하였습니다.\n차량 제어 화면으로 전환합니다.\n잠시만 기다려 주세요." );

                    activityAlive = false ;

                    new Handler().postDelayed(new Runnable() {
                        public void run() {
                            activityAlive = false ;
                            boolean test = false ;
                            if( ! test ) {
                                startActivity(new android.content.Intent(MainActivity.this, com.carapp.CarActivity.class));
                            } else {
                                startActivity(new android.content.Intent(MainActivity.this, com.carapp.GoogleMapActivity.class));
                            }
                        }
                    }, 3000);
                } else if ( 3 == mode ) {
                    status.setTextColor(Color.parseColor("#FF0000"));
                    String ipAddr = getIpAddr();

                    if( ipAddr.startsWith( "10.3.")) {
                        status.setText("차량 서버 실행 여부를 체크하세요.\n\n잠시후 다시 연결을 시도합니다.");
                    } else {
                        status.setText("라즈베리파이 공유기를 연결하세요.\n\n잠시후 다시 연결을 시도합니다.");
                    }
                }

                if(activityAlive) {
                     handler.postDelayed( this, 500 );
                }
            }
        }, 2_000);
    }
}
