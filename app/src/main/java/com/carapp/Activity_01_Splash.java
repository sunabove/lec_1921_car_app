package com.carapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class Activity_01_Splash extends ComActivity {

    private boolean activityAlive = false ;
    private boolean serverActive = false ;
    private TextView status ;
    private TextView error ;
    private TextView wifi ;
    private TextView ipaddr ;
    private ImageView logo;
    private SeekBar seekBar ;

    private String errorMessage = "" ;

    private int mode = 0 ;

    private int wifeChangeCnt = 0 ;

    public int getLayoutId() {
        return R.layout.activity_splash;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        this.status = this.findViewById(R.id.status);
        this.error = this.findViewById(R.id.error);

        this.wifi = this.findViewById(R.id.wifi);
        this.ipaddr = this.findViewById(R.id.ipaddr);

        this.logo = this.findViewById(R.id.logo);
        this.seekBar = this.findViewById(R.id.seekBar);

        class WifiReceiver extends BroadcastReceiver {

            @Override
            public void onReceive(Context context, Intent intent) {
                NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if(info != null && info.isConnected()) {
                    wifeChangeCnt += 1;
                    if( 1 < wifeChangeCnt && isRaspberryWifiConnected() ) {
                        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                        String ssid = wifiInfo.getSSID();

                        Log.d(TAG, "wifi changed to " + ssid);
                    }
                }
            }
        }

        /*
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);

        registerReceiver( new WifiReceiver(), intentFilter);
        */
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

        this.seekBar.setEnabled( false );
        this.seekBar.setProgress( 0 );

        this.checkServer();
    }

    public boolean isRaspberryWifiConnected() {
        String ipAddr = getIpAddr();
        return ipAddr.startsWith( "10.3.");
    }

    private void checkServer() {

        status.setTextColor(Color.parseColor("#009688"));
        status.setText( "서버 연결중입니다.\n잠시만 기다려 주세요!" );

        this.wifi.setText( this.getWifiSsid() );
        this.ipaddr.setText( this.getIpAddr() );

        this.serverActive = false ;

        if( true ) {
            final Handler handler = new Handler();

            handler.postDelayed( new Runnable() {
                SeekBar seekBar = Activity_01_Splash.this.seekBar ;
                int dir = 1 ;

                public void run() {
                    seekBar.setEnabled( true );
                    int max = seekBar.getMax();
                    int progress = seekBar.getProgress();
                    if( 1 == mode || 2 == mode || 3 == mode ) {
                        if( progress >= max ) {
                            dir = - 1 ;
                        } else if( 1 > progress ) {
                            dir = 1 ;
                        }

                        seekBar.setProgress( progress + dir*10 );
                        //seekBar.invalidate();
                    }

                    //Log.d( "seekBar", "progress = " + seekBar.getProgress() );

                    if( activityAlive ) {
                        handler.postDelayed( this, 500 );
                    }
                }
            }, 500 );
        }

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

                        //e.printStackTrace();
                    }

                    sleep( 5_000 );
                }
            }
        }).start();

        if( true ) {
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    //Log.d(TAG, "run: postDelayerd");

                    error.setText(errorMessage);

                    wifi.setText(getWifiSsid());
                    ipaddr.setText(getIpAddr());

                    if (1 == mode) {
                        status.setTextColor(Color.parseColor("#009688"));
                        status.setText("서버 연결중입니다.\n잠시만 기다려 주세요!");
                    } else if (2 == mode) {
                        status.setTextColor(Color.parseColor("#009688"));
                        status.setText("서버 연결에 성공하였습니다.\n차량 제어 화면으로 이동합니다.\n잠시만 기다려 주세요.");

                        activityAlive = false;

                        new Handler().postDelayed(new Runnable() {
                            public void run() {
                                activityAlive = false;
                                boolean test = true;
                                if( test ) {
                                    startActivity(new android.content.Intent(Activity_01_Splash.this, Activity_04_Video.class));
                                } else if (test) {
                                    startActivity(new android.content.Intent(Activity_01_Splash.this, Activity_03_Map.class));
                                } else {
                                    startActivity(new android.content.Intent(Activity_01_Splash.this, Activity_02_Car.class));
                                }
                            }
                        }, 3000);
                    } else if (3 == mode) {
                        status.setTextColor(Color.parseColor("#FF0000"));

                        if (isRaspberryWifiConnected()) {
                            status.setText("차량 서버 실행 여부를 체크하세요.\n\n잠시후 다시 연결을 시도합니다.");
                        } else {
                            status.setText("라즈베리파이 공유기를 연결하세요.\n\nWi-Fi 선택 화면으로 이동합니다.");

                            activityAlive = false;

                            Toast.makeText(status.getContext(), "Wi-Fi 선택 화면으로 이동합니다.", Toast.LENGTH_SHORT).show();

                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                                }
                            }, 2_000);
                        }
                    }

                    if (activityAlive) {
                        handler.postDelayed(this, 1_500);
                    }
                }
            }, 2_000);
        }
    }


}
