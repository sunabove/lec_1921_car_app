package com.carapp;

import android.Manifest;
import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import io.socket.client.Socket;

public abstract class ComActivity extends AppCompatActivity implements ComInterface {

    protected static final int gray = Color.parseColor("#d3d3d3") ;
    protected static final int yellow = Color.parseColor("#ffff00") ;
    protected static final int green = Color.parseColor("#00FF00") ;
    protected static final int black = Color.parseColor("#FFFFFF") ;
    protected static final int red = Color.parseColor("#FF0000") ;

    protected static class Motion {
        public static final String FORWARD = "FORWARD" ;
        public static final String BACKWARD = "BACKWARD" ;

        public static final String LEFT = "LEFT" ;
        public static final String RIGHT = "RIGHT" ;

        public static final String STOP = "STOP" ;

        public static final String AUTOPILOT = "AUTOPILOT" ;
    }

    protected Context context ;

    protected SharedPreferences sharedPref = null;

    protected RequestQueue requestQueue ;
    protected boolean motionEnabled = false ;

    protected Socket socket = null;

    protected FloatingActionButton goBack ;

    public abstract int getLayoutId() ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView( this.getLayoutId() );

        this.context = this.getApplicationContext();

        if( null == sharedPref ) {
            sharedPref = getSharedPreferences("mySettings", MODE_PRIVATE);
        }

        this.requestQueue = Volley.newRequestQueue(this);

        this.goBack = this.findViewById(R.id.goBack);

        if( null != goBack ) {
            this.goBack.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    TextView status = findViewById(R.id.status);

                    if( null != status ) {
                        status.setText( "이전 화면으로 돌아갑니다." );
                    }

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            // 이전 화면으로 돌아감.
                            finish();
                        }
                    }, 300);
                }
            });
        }
    }

    public double prettyDegree( double degree ) {
        degree = degree % 360 ;

        if( 180 < degree ) {
            degree = degree - 360 ;
        }
        return degree ;
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    public <T extends View> T findViewById(@IdRes int id) {
        return (T) super.findViewById(id);
    }

    public void postDelayed(Runnable r, int delayMillis) {
        new Handler().postDelayed( r, delayMillis);
    }

    public void sleep( long millis ) {
        try {
            Thread.currentThread().sleep(millis);
        } catch ( Exception e ) {
            //
        }
    }

    public int getScreenWidth() {
        return Resources.getSystem().getDisplayMetrics().widthPixels;
    }

    public int getScreenHeight() {
        return Resources.getSystem().getDisplayMetrics().heightPixels;
    }

    // 와이 파이 이름을 반환한다.
    public String getWifiSsid() {
        String ssid = "" ;

        try {
            WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

            WifiInfo info = wifiManager.getConnectionInfo();
            ssid = info.getSSID();
        } catch ( Exception e ) {
            e.printStackTrace();
        }

        if( ssid.startsWith( "\"") ) {
            ssid = ssid.substring( 1 );
        }

        if( ssid.endsWith( "\"")) {
            ssid = ssid.substring( 0, ssid.length() - 1 );
        }

        return ssid;
    }

    // 아이피 주소를 반환한다.
    public String getIpAddr() {
        String ipAddr = "" ;

        try {
            WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

            WifiInfo info = wifiManager.getConnectionInfo();

            int ipInt = info.getIpAddress();
            ipAddr = InetAddress.getByAddress(
                    ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(ipInt).array())
                    .getHostAddress();
        } catch ( Exception e ) {
            e.printStackTrace();
        }

        return ipAddr;
    }

    // 동영상을 플레이 한다.
    protected void playVideo() {
        super.onResume();

        Log.v( TAG, "playVideo");

        WebView videoView = this.findViewById(R.id.videoView);
        if( null != videoView ) {
            videoView.getSettings().setLoadWithOverviewMode(true);
            videoView.getSettings().setUseWideViewPort(true);

            videoView.loadUrl("http://10.3.141.1/video_feed");
        }
    }

    // 동영상을 중지 한다.
    protected void stopPlayVideo() {
        super.onResume();

        Log.v( TAG, "playVideo");

        WebView videoView = this.findViewById(R.id.videoView);
        if( null != videoView ) {
            videoView.loadUrl("about:blank");
        }
    }

    public void hideActionBar() {
        // If the Android version is lower than Jellybean, use this call to hide
        // the status bar.
        if (Build.VERSION.SDK_INT < 16) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            View decorView = getWindow().getDecorView();
            // Hide the status bar.
            int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
            // Remember that you should never show the action bar if the
            // status bar is hidden, so hide that too if necessary.
            ActionBar actionBar = getActionBar();
            if( null != actionBar ) {
                actionBar.hide();
            }
        }
    }

    protected static final int PERMISSION_ID = 44;

    protected boolean checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        return false;
    }

    protected void requestPermissions() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                PERMISSION_ID
        );
    }

    protected boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER
        );
    }

    // 두 위경도 사이의 거리를 m로 구한다.
    public float[] getDistance(LatLng from, LatLng to ) {
        float[] results = new float[2];
        Location.distanceBetween( from.latitude, from.longitude, to.latitude, to.longitude, results );

        return results ;
    }

    public double prettyAngle60(double angleDegDecimal ) {
        double angle = angleDegDecimal %360 ;

        int ang = (int) angle ;
        double deg = angle - ang ;
        deg = 60*deg;

        angle = angle + deg ;

        return angle ;
    }

    // 와이파이 선택창을 오픈한다.
    public void openWifiSelector() {
        Toast.makeText( context, "Wi-Fi 선택 화면으로 이동합니다.", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
            }
        }, 2_000);
    }

}
