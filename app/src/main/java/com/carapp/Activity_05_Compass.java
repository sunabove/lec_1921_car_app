package com.carapp;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class Activity_05_Compass extends ComActivity {

    private Compass compass;
    private ImageView arrowView;
    private TextView sotwLabel;

    private float currentAzimuth;
    private SOTWFormatter sotwFormatter;
    protected boolean sotwNewLine = false ;

    @Override
    public int getLayoutId() {
        return R.layout.activity_compass;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.sotwFormatter = new SOTWFormatter(this);

        //arrowView = findViewById(R.id.main_image_hands);
        this.arrowView = findViewById(R.id.main_image_dial);
        this.sotwLabel = findViewById(R.id.sotw_label);

        TextView status = findViewById(R.id.status);

        if( null != status ) {
            status.setText( "" );
        }

        this.setupCompass();
    }

    @Override
    protected void onStart() {
        super.onStart();
        compass.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        compass.stop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        compass.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        compass.stop();
    }

    private void setupCompass() {
        compass = new Compass(this);
        Compass.CompassListener cl = getCompassListener();
        compass.setListener(cl);
    }

    private void adjustArrow(float azimuth) {
        //Log.d(TAG, "will set rotation from " + currentAzimuth + " to " + azimuth);

        Animation an = new RotateAnimation(-currentAzimuth, -azimuth,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f);
        currentAzimuth = azimuth;

        an.setDuration(500);
        an.setRepeatCount(0);
        an.setFillAfter(true);

        arrowView.startAnimation(an);
    }

    public void adjustSotwLabel(float azimuth) {
        sotwLabel.setText(sotwFormatter.format(azimuth, sotwNewLine));
    }

    private Compass.CompassListener getCompassListener() {
        return new Compass.CompassListener() {
            @Override
            public void onNewAzimuth(final float azimuth) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adjustArrow(azimuth);
                        adjustSotwLabel(azimuth);
                    }
                });
            }
        };
    }
}
