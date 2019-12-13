package com.carapp;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.util.Log;
import android.view.*;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.view.animation.TranslateAnimation;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.core.text.HtmlCompat;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class Activity_02_Car extends Activity_05_Compass implements Orientation.Listener {

    private WebView videoView ;

    private Button stop;

    private EditText status ;
    private EditText pitch ;
    private EditText roll ;
    private FloatingActionButton goToMap ;
    private ImageView compassDial ;
    private ImageView compassHands ;
    private ImageView carAni ;

    // orientation sensor
    private Orientation orientation;
    private AttitudeIndicator attitudeIndicator;
    // -- orientation sensor

    private String currMotion = "";
    private long motionTime = System.currentTimeMillis();
    private String motionPrev = "" ;

    public int getLayoutId() {
        return R.layout.activity_car ;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.hideActionBar();

        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        this.sotwNewLine = false ;

        this.orientation = new Orientation(this);
        this.attitudeIndicator = this.findViewById(R.id.attitude_indicator);

        this.status = this.findViewById(R.id.status);
        this.pitch = this.findViewById(R.id.pitch);
        this.roll = this.findViewById(R.id.roll);
        this.compassDial = this.findViewById(R.id.main_image_dial);
        this.compassHands = this.findViewById(R.id.main_image_hands);

        this.carAni = this.findViewById( R.id.carAni );

        this.goToMap = this.findViewById(R.id.goToMap );

        // hide keyboard always
        this.status.setInputType(InputType.TYPE_NULL);
        this.pitch.setInputType(InputType.TYPE_NULL);
        this.roll.setInputType(InputType.TYPE_NULL);

        videoView = this.findViewById(R.id.videoView);
        stop = this.findViewById(R.id.stop);

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
                startActivity(new android.content.Intent(Activity_02_Car.this, Activity_03_Map.class));
            }
        });

        this.compassDial.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new android.content.Intent(Activity_02_Car.this, Activity_05_Compass.class));
            }
        });

        this.compassHands.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new android.content.Intent(Activity_02_Car.this, Activity_05_Compass.class));
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
                    if( true ) {
                        startActivity(new android.content.Intent(Activity_02_Car.this, Activity_04_Video.class));
                    }
                }

                return false;
            }
        });

        this.videoView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return false;
            }
        });
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        Log.v( TAG, "onResume");

        this.hideActionBar();

        this.paintUI();

        this.orientation.startListening(this);

        this.playVideo();
    }

    @Override
    protected void onPause() {
        super.onPause();

        orientation.stopListening();

        this.stopPlayVideo();
    }

    @Override
    public void onOrientationChanged(float pitch, float roll) {
        attitudeIndicator.setAttitude(pitch, roll);

        this.pitchRollUpdated( pitch, roll );
    }

    public void paintUI() {
        if( motionEnabled )  {
            stop.setText( "STOP" );
            this.carAni.setImageResource(R.drawable.car_top_01_move);
        } else {
            stop.setText( "START" );
            this.carAni.setImageResource(R.drawable.car_top_03_stop);
        }

        String currMotion = this.currMotion ;

        //this.animateCarAdvance( -1 );

        this.animateCarRotate( 1 );
    }
    // -- paintUI

    private Animation carAnimation = null ;

    private void animateCarAdvance( int dir ) {
        if( null != this.carAnimation ) {
            this.carAni.clearAnimation();
        }

        // logo animation
        int relative = Animation.RELATIVE_TO_SELF ;
        Animation animation = new TranslateAnimation(
                relative, 0.0f,
                relative, 0.0f,
                relative, dir*0.5f,
                relative, -dir*0.5f);

        animation.setDuration( 2_500 );
        animation.setRepeatCount( -1 );
        animation.setRepeatMode(Animation.RESTART);

        this.carAnimation = animation ;

        this.carAni.startAnimation( animation );
    }

    private void animateCarRotate( int dir ) {
        if( null != this.carAnimation ) {
            this.carAni.clearAnimation();
        }

        float currentAzimuth = 0.0f;
        float azimuth = 0.0f;

        int relative = Animation.RELATIVE_TO_SELF ;

        Animation animation = new RotateAnimation(
                0, dir*45,
                relative, 0.5f,
                relative,  0.5f);

        currentAzimuth = azimuth;

        animation.setDuration( 2_500 );
        animation.setRepeatCount( -1 );
        animation.setFillAfter(true);

        this.carAnimation = animation ;

        this.carAni.startAnimation( animation );
    }

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

    // pitch roll 값이 변했을 경우, 차를 제어한다.
    private void pitchRollUpdated( double pitch, double roll ) {
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

}
