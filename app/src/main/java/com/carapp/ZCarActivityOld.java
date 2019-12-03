package com.carapp;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;

import androidx.core.text.HtmlCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

public class ZCarActivityOld extends ComActivity {

    WebView videoView ;
    Button forward;
    Button backward;
    Button left;
    Button right;
    Button stop;

    EditText status ;

    RequestQueue requestQueue ;
    boolean motionEnabled = false ;

    // motion sensor
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetic;

    public double toDegree( double radian ) {
        double degree = Math.toDegrees( radian );

        degree = degree % 360 ;

        if( 180 < degree ) {
            degree = degree - 360 ;
        }
        return degree ;
    }

    private SensorEventListener orientationListener = new SensorEventListener() {

        // Gravity rotational data
        private float gravity[];
        // Magnetic rotational data
        private float magnetic[]; //for magnetic rotational data
        private float accels[] = new float[3];
        private float mags[] = new float[3];
        private float[] values = new float[3];

        // azimuth, pitch and roll
        private float azimuth;
        private float pitch;
        private float roll;

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            switch (event.sensor.getType()) {
                case Sensor.TYPE_MAGNETIC_FIELD:
                    mags = event.values.clone();
                    break;
                case Sensor.TYPE_ACCELEROMETER:
                    accels = event.values.clone();
                    break;
            }

            if (mags != null && accels != null) {
                gravity = new float[9];
                magnetic = new float[9];
                SensorManager.getRotationMatrix(gravity, magnetic, accels, mags);
                float[] outGravity = new float[9];
                SensorManager.remapCoordinateSystem(gravity, SensorManager.AXIS_X,SensorManager.AXIS_Z, outGravity);
                SensorManager.getOrientation(outGravity, values);

                azimuth = values[0] * 57.2957795f;
                pitch =values[1] * 57.2957795f;
                roll = values[2] * 57.2957795f;
                mags = null;
                accels = null;

                double yaw_deg = toDegree( azimuth );
                double pitch_deg = toDegree( pitch );
                double roll_deg = toDegree( roll );

                String text = String.format("pitch: %05.2f roll: %05.2f yaw: %05.2f", pitch_deg, roll_deg, yaw_deg );

                status.setText( text );
            }
        }
    };;
    // -- motion sensor


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car);

        this.requestQueue = Volley.newRequestQueue(this);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetic = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        this.status = this.findViewById(R.id.status);

        videoView = this.findViewById(R.id.videoView);
        forward = this.findViewById(R.id.forward);
        backward = this.findViewById(R.id.backward);
        left = this.findViewById(R.id.left);
        right = this.findViewById(R.id.right);
        stop = this.findViewById(R.id.stop);

        forward.setText(HtmlCompat.fromHtml("&uarr;", HtmlCompat.FROM_HTML_MODE_COMPACT));
        backward.setText(HtmlCompat.fromHtml("&darr;", HtmlCompat.FROM_HTML_MODE_COMPACT));
        left.setText(HtmlCompat.fromHtml("&larr;", HtmlCompat.FROM_HTML_MODE_COMPACT));
        right.setText(HtmlCompat.fromHtml("&rarr;", HtmlCompat.FROM_HTML_MODE_COMPACT));
        // stop.setText(HtmlCompat.fromHtml("&bull;", HtmlCompat.FROM_HTML_MODE_COMPACT));

        this.paintUI();

        forward.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                getCarMotion( "forward" );
            }
        });

        backward.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                getCarMotion( "backward" );
            }
        });

        left.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                getCarMotion( "left" );
            }
        });

        right.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                getCarMotion( "right" );
            }
        });

        stop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                getCarMotion( "stop" );
            }
        });

        // stop / start listener
        stop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                motionEnabled = ! motionEnabled ;

                paintUI();
            }
        });
    }

    public void paintUI() {
        if( motionEnabled )  {
            stop.setText( "STOP" );
        } else {
            stop.setText( "START" );
        }

        forward.setEnabled( motionEnabled );
        backward.setEnabled( motionEnabled );
        left.setEnabled( motionEnabled );
        right.setEnabled( motionEnabled );
    }

    public void getCarMotion( String motion) {

        if( "stop".equalsIgnoreCase( motion ) ) {
            requestQueue.cancelAll(TAG);
        }

        // Instantiate the RequestQueue.
        String url = String.format("http://10.3.141.1/car.json?motion=%s", motion);

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        status.setText( response.toString() );
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                status.setText("That didn't work!");
            }
        });

        // Add the request to the RequestQueue.
        requestQueue.add(stringRequest);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        Log.v( "sunabove", "onResume");

        videoView.getSettings().setLoadWithOverviewMode(true);
        videoView.getSettings().setUseWideViewPort(true);

        videoView.loadUrl( "http://10.3.141.1/video_feed" );

        sensorManager.registerListener(orientationListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(orientationListener, magnetic, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener( orientationListener );
    }

}
