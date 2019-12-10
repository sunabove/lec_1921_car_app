package com.carapp;

import android.app.ActionBar;
import android.content.Context;
import android.content.res.Resources;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONObject;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Locale;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public abstract class ComActivity extends AppCompatActivity implements ComInterface {

    private FloatingActionButton goBack ;

    protected RequestQueue requestQueue ;
    protected boolean motionEnabled = false ;

    public abstract int getLayoutId() ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView( this.getLayoutId() );

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
}
