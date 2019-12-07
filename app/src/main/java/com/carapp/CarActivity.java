package com.carapp;

import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
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
                moveCar( "forward", status );
            }
        });

        backward.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                moveCar( "backward", status );
            }
        });

        left.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                moveCar( "left", status );
            }
        });

        right.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                moveCar( "right", status );
            }
        });

        stop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                motionPrev = "";

                if( motionEnabled ) {
                    moveCar("stop", status );
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
    }

    private long then = System.currentTimeMillis();
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
        }else if (now - then < 700 ) {
            // do nothing!
        } else {
            String motion = "" ;
            if (15 <= roll) {
                motion = "right" ;
            } else if ( -15 >= roll) {
                motion = "left" ;
            } else if (30 <= pitch) {
                motion = "forward" ;
            } else if (5 >= pitch) {
                motion = "backward" ;
            } else {
                motion = "stop" ;
            }

            if( motion.equalsIgnoreCase( motionPrev ) ) {
                // do nothing!
            } else {
                this.moveCar(motion, status);
                this.motionPrev = motion;
            }

            then = now ;
        }
    }

    public void moveCar(final String motion, final EditText status ) {
        String url = String.format("http://10.3.141.1/car.json?motion=%s", motion);

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
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        Log.v( TAG, "onResume");

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
