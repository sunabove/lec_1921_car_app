package com.carapp;

import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;

public class CarActivity extends ComActivity {

    WebView videoView ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car);

        this.videoView = this.findViewById(R.id.videoView);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        Log.v( "sunabove", "onResume");

        videoView.getSettings().setLoadWithOverviewMode(true);
        videoView.getSettings().setUseWideViewPort(true);

        videoView.loadUrl("http://10.3.141.1/video_feed");
    }

}
