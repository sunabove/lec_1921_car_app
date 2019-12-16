package com.carapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class Activity_01_Splash extends ComActivity {

    private boolean activityAlive = false ;
    private boolean serverActive = false ;
    private TextView status ;
    private TextView error ;
    private TextView wifi ;
    private TextView ipaddr;
    private ImageView logo;
    private SeekBar seekBar ;
    private ImageView wifiLogo;
    private CheckBox goToMap ;
    private Spinner wifiSpinner ;

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

        this.wifiLogo = this.findViewById(R.id.wifiLogo);
        this.goToMap = this.findViewById(R.id.goToMap);
        this.wifiSpinner = this.findViewById(R.id.wifiSpinner);

        this.wifiSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                whenWifiItemSelected( parent, view, position, id );
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

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

        if( true ) {
            Boolean goToMapChecked = sharedPref.getBoolean("goToMapChecked", false );
            this.goToMap.setChecked( goToMapChecked );
        }

        this.goToMap.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            String tag = "goToMap";
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.d( tag, "goToMap checked = " + isChecked );

                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putBoolean( "goToMapChecked", isChecked  );
                editor.commit();
            }

        });

        /*
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);

        registerReceiver( new WifiReceiver(), intentFilter);
        */

        // wifi scan
        final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent intent) {
                boolean success = intent.getBooleanExtra( WifiManager.EXTRA_RESULTS_UPDATED, false);
                if (success) {
                    scanSuccess( wifiManager );
                } else {
                    // scan failure handling
                    scanFailure( wifiManager );
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        context.registerReceiver(wifiScanReceiver, intentFilter);

        wifiManager.startScan();
        // -- wifi scan
    }
    // -- on create

    private void whenWifiItemSelected(AdapterView<?> parent, View view, int position, long id) {

    }

    private void scanSuccess( WifiManager wifiManager ) {
        String tag = "wifi" ;
        List<ScanResult> results = wifiManager.getScanResults();

        final Spinner spinner = this.wifiSpinner ;

        List<String> list = new ArrayList<>();


        int idx = 0 ;
        for( ScanResult scan : results ) {
            String ssid = scan.SSID ;

            Log.d( tag, String.format("[%03d] scan ssid = %s", idx, scan.SSID ) ) ;

            if( null != ssid && 0 < ssid.trim().length() ) {
                list.add(scan.SSID);
            }

            idx ++ ;
        }

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, list);
        //dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinner.setAdapter(dataAdapter);

    }

    private void scanFailure( WifiManager wifiManager ) {
        // handle failure: new scan did NOT succeed
        // consider using old scan results: these are the OLD results!
        List<ScanResult> results = wifiManager.getScanResults();
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

        this.animateLogoRotate( 1 );

        this.checkServer();
    }

    private void animateLogoRotate(final int dir) {
        if( null != this.logo ) {
            this.logo.clearAnimation();
        }

        this.logo.setImageResource(R.drawable.splash_icon );

        int relative = Animation.RELATIVE_TO_SELF ;

        int fromDegree = -50 ;
        int toDegree = 10 ;

        if( 0 > dir ) {
            fromDegree = 10 ;
            toDegree = -50;
        }

        Animation animation = new RotateAnimation(fromDegree, toDegree,
                relative, 0.5f, relative, 0.7f);

        animation.setDuration( 2_500 );
        animation.setRepeatCount( 0 );
        animation.setFillAfter(true);

        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if( 2 == mode ) {
                    animateLogoTranslate(1_500);
                } else {
                    animateLogoRotate(-dir);
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        this.logo.startAnimation( animation );
    }

    private void animateLogoTranslate(final long duration ) {
        if( null != this.logo ) {
            this.logo.clearAnimation();
        }

        // logo animation
        int relative = Animation.RELATIVE_TO_SELF ;
        TranslateAnimation animation = new TranslateAnimation(
                relative, -0.3f,
                relative, 0.3f,
                relative, 0.0f,
                relative, 0.0f);

        animation.setDuration(duration);
        animation.setRepeatCount( -1 );
        //animation.setRepeatMode(Animation.RESTART);

        animation.setAnimationListener(new Animation.AnimationListener() {
            int animationCnt = 0 ;
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                Log.d( "animation", "onAnimationRepeat "  + animationCnt );

                if( 2 == mode ) {
                    if( animationCnt > 1 ) {
                        logo.clearAnimation();

                        moveToNextActivity();
                    } else {
                        animationCnt += 1;
                    }
                }
            }
        });

        this.logo.startAnimation(animation);
    }
    // -- animateLogoTranslate

    private void moveToNextActivity() {
        new Handler().postDelayed(new Runnable() {
            public void run() {
                activityAlive = false;

                boolean test = false;
                if( false ) {
                    // do nothing
                } else if( test ) {
                    startActivity(new android.content.Intent(Activity_01_Splash.this, Activity_04_Video.class));
                } else if (test) {
                    startActivity(new android.content.Intent(Activity_01_Splash.this, Activity_03_Map.class));
                } else {
                    if( goToMap.isSelected() || goToMap.isChecked()) {
                        startActivity(new android.content.Intent(Activity_01_Splash.this, Activity_03_Map.class));
                    } else {
                        startActivity(new android.content.Intent(Activity_01_Splash.this, Activity_02_Car.class));
                    }
                }
            }
        }, 0);
    }

    public boolean isRaspberryWifiConnected() {
        String ipAddr = getIpAddr();
        return ipAddr.startsWith( "10.3.");
    }

    private void checkServer() {

        final Activity_01_Splash activity = this ;

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

                    if (isRaspberryWifiConnected()) {
                        wifiLogo.setImageResource( R.drawable.wifi_good );
                    } else {
                        wifiLogo.setImageResource( R.drawable.wifi_bad );
                    }

                    if (1 == mode) {
                        status.setTextColor(Color.parseColor("#009688"));
                        status.setText("서버 연결중입니다.\n잠시만 기다려 주세요!");
                    } else if (2 == mode) {
                        status.setTextColor(Color.parseColor("#009688"));
                        status.setText("서버 연결에 성공하였습니다.\n차량 제어 화면으로 이동합니다.\n잠시만 기다려 주세요.");

                        activityAlive = false;

                    } else if (3 == mode) {
                        status.setTextColor(Color.parseColor("#FF0000"));

                        if (isRaspberryWifiConnected()) {
                            status.setText("차량 서버 실행 여부를 체크하세요.\n\n잠시후 다시 연결을 시도합니다.");
                        } else {
                            status.setText("라즈베리파이 공유기를 연결하세요.");

                            if( false ) {
                                status.setText("라즈베리파이 공유기를 연결하세요.\n\nWi-Fi 선택 화면으로 이동합니다.");

                                activityAlive = false;

                                openWifiSelector();
                            }
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
