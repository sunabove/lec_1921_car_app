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

        //String vidAddress = "https://archive.org/download/ksnn_compilation_master_the_internet/ksnn_compilation_master_the_internet_512kb.mp4";
        String url = "http://10.3.141.1/stream.mjpg";

        videoView.loadUrl("http://10.3.141.1/stream.mjpg");
    }

}
