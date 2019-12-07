package com.carapp;

import android.content.Context;
import android.content.res.Resources;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.Formatter;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Locale;

public abstract class ComActivity extends AppCompatActivity implements ComInterface {

    protected RequestQueue requestQueue ;
    protected boolean motionEnabled = false ;

    public abstract int getLayoutId() ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView( this.getLayoutId() );

        this.requestQueue = Volley.newRequestQueue(this);
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
}
