package com.carapp;

import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.util.Log;
import android.view.*;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.VideoView;

import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class CarActivity extends CompassActivity implements Orientation.Listener {

    private WebView videoView ;
    private Button forward;
    private Button backward;
    private Button left;
    private Button right;
    private Button stop;

    private EditText status ;
    private EditText pitch ;
    private EditText roll ;
    private FloatingActionButton goToMap ;
    private ImageView compassDial ;
    private ImageView compassHands ;

    // orientation sensor
    private Orientation orientation;
    private AttitudeIndicator attitudeIndicator;
    // -- orientation sensor

    private String currMotion = "";

    protected static class Motion {
        public static final String FORWARD = "FORWARD" ;
        public static final String BACKWARD = "BACKWARD" ;
        public static final String LEFT = "LEFT" ;
        public static final String RIGHT = "RIGHT" ;
        public static final String STOP = "STOP" ;
    }

    public double prettyDegree( double degree ) {
        degree = degree % 360 ;

        if( 180 < degree ) {
            degree = degree - 360 ;
        }
        return degree ;
    }

    public int getLayoutId() {
        return R.layout.activity_car ;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.hideActionBar();

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        this.sotwNewLine = true ;

        this.orientation = new Orientation(this);
        this.attitudeIndicator = this.findViewById(R.id.attitude_indicator);

        this.status = this.findViewById(R.id.status);
        this.pitch = this.findViewById(R.id.pitch);
        this.roll = this.findViewById(R.id.roll);
        this.compassDial = this.findViewById(R.id.main_image_dial);
        this.compassHands = this.findViewById(R.id.main_image_hands);

        this.goToMap = this.findViewById(R.id.goToMap );

        // hide keyboard always
        this.status.setInputType(InputType.TYPE_NULL);
        this.pitch.setInputType(InputType.TYPE_NULL);
        this.roll.setInputType(InputType.TYPE_NULL);

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
                moveCar( Motion.FORWARD, status );
            }
        });

        backward.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                moveCar( Motion.BACKWARD, status );
            }
        });

        left.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                moveCar( Motion.LEFT, status );
            }
        });

        right.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                moveCar( Motion.RIGHT, status );
            }
        });

        stop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                motionPrev = "";

                if( motionEnabled ) {
                    moveCar(Motion.STOP, status );
                }

                motionEnabled = ! motionEnabled ;

                paintUI();
            }
        });

        // 지도 버튼을 클릭하면 지도 화면으로 이동한다.
        goToMap.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new android.content.Intent(CarActivity.this, com.carapp.GoogleMapActivity.class));
            }
        });

        this.compassDial.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new android.content.Intent(CarActivity.this, com.carapp.CompassActivity.class));
            }
        });

        this.compassHands.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new android.content.Intent(CarActivity.this, com.carapp.CompassActivity.class));
            }
        });

        // 비디오 화면을 클릭하면 전체 영상 보기 화면으로 이동한다.
        this.videoView.setOnTouchListener(new View.OnTouchListener(){

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction()==MotionEvent.ACTION_MOVE){
                    return false;
                }

                if (event.getAction()==MotionEvent.ACTION_UP){
                    Log.d( TAG, "VideoView Clicked.");
                    startActivity(new android.content.Intent(CarActivity.this, com.carapp.VideoActivity.class));
                }

                return false;
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

        int gray = Color.parseColor("#d3d3d3") ;

        int black = Color.parseColor("#000000") ;

        int yellow = Color.parseColor("#ffff00") ;

        int green = Color.parseColor("#00FF00") ;

        String currMotion = this.currMotion ;

        forward.setBackgroundColor( currMotion.equalsIgnoreCase( Motion.FORWARD ) ? green : gray );
        backward.setBackgroundColor( currMotion.equalsIgnoreCase( Motion.BACKWARD ) ? green : gray );
        left.setBackgroundColor( currMotion.equalsIgnoreCase( Motion.LEFT ) ? green : gray );
        right.setBackgroundColor( currMotion.equalsIgnoreCase( Motion.RIGHT ) ? green : gray );
        stop.setBackgroundColor( currMotion.equalsIgnoreCase( Motion.STOP ) ? green : gray );

        forward.setTextColor( currMotion.equalsIgnoreCase( Motion.FORWARD ) ? yellow : black );
        backward.setTextColor( currMotion.equalsIgnoreCase( Motion.BACKWARD ) ? yellow : black );
        left.setTextColor( currMotion.equalsIgnoreCase( Motion.LEFT ) ? yellow : black );
        right.setTextColor( currMotion.equalsIgnoreCase( Motion.RIGHT ) ? yellow : black );
        stop.setTextColor( currMotion.equalsIgnoreCase( Motion.STOP ) ? yellow : black );

        if( ! motionEnabled ) {
            stop.setBackgroundColor( gray );
            stop.setTextColor( black );
        }
    }
    // -- paintUI

    private long motionTime = System.currentTimeMillis();
    private String motionPrev = "" ;

    // pitch roll 값이 변했을 경우, 차를 제어한다.
    public void pitchRollUpdated( double pitch, double roll ) {
        pitch = -prettyDegree(pitch);
        roll = -prettyDegree(roll);

        String text = String.format("pitch: %05.2f  roll %05.2f", pitch, roll);

        this.pitch.setText( String.format( "%5.2f", pitch));
        this.roll.setText( String.format( "%5.2f", roll));

        final long now = System.currentTimeMillis();

        if( ! motionEnabled ) {
            // do nothing!
        }else if (now - motionTime < 700 ) {
            // do nothing!
        } else {
            String motion = "" ;
            if (15 <= roll) {
                motion = Motion.RIGHT ;
            } else if ( -15 >= roll) {
                motion = Motion.LEFT ;
            } else if ( 45 <= pitch) {
                motion = Motion.FORWARD ;
            } else if ( 32 >= pitch) {
                motion = Motion.BACKWARD ;
            } else {
                motion = Motion.STOP ;
            }

            /*
            if( false && motion.equalsIgnoreCase( motionPrev ) ) {
                // do nothing!
            }
            */

            if( true ){
                this.moveCar(motion, status);
                this.motionPrev = motion;
            }

            motionTime = now ;
        }
    }
    // -- pitchRollUpdated

    public void moveCar(final String motion, final EditText status ) {
        this.currMotion = motion ;

        String url = String.format("http://10.3.141.1/car.json?motion=%s", motion.toLowerCase() );

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if( null != status ) {
                            status.setText(response.toString());
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if( null != status ) {
                    status.setText("That didn't work!");
                }
            }
        });

        requestQueue.add(stringRequest);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                paintUI();
            }
        }, 0);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        Log.v( TAG, "onResume");

        this.hideActionBar();

        this.orientation.startListening(this);

        this.playVideo();
    }

    @Override
    protected void onPause() {
        super.onPause();

        orientation.stopListening();
    }

    @Override
    public void onOrientationChanged(float pitch, float roll) {
        attitudeIndicator.setAttitude(pitch, roll);
    }

}
