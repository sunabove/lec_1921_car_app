package com.carapp;

import android.annotation.SuppressLint;
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
    private Animation carAnimation = null ;

    // orientation sensor
    private Orientation orientation;
    private AttitudeIndicator attitudeIndicator;
    // -- orientation sensor

    private long motionTime = System.currentTimeMillis();

    private String motionCurr = "";

    public int getLayoutId() {
        return R.layout.activity_car ;
    }

    @SuppressLint("SourceLockedOrientationActivity")
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
                motionEnabled = ! motionEnabled ;

                if( motionEnabled ) {
                    carAni.setImageResource(R.drawable.car_top_01_move);
                    moveCar(Motion.STOP, status );
                } else {
                    carAni.clearAnimation();
                    carAni.setImageResource(R.drawable.car_top_03_stop);
                }

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
            //this.carAni.setImageResource(R.drawable.car_top_01_move);
        } else {
            stop.setText( "START" );

            //this.carAni.setImageResource(R.drawable.car_top_03_stop);
        }
    }
    // -- paintUI

    private void animateCarAdvance( int dir ) {
        if( null != this.carAnimation ) {
            this.carAni.clearAnimation();
        }

        this.carAni.setImageResource( 1 == dir ? R.drawable.car_top_02_drive : R.drawable.car_top_01_move );

        // logo animation
        int relative = Animation.RELATIVE_TO_SELF ;
        Animation animation = new TranslateAnimation(
                relative, 0.0f,
                relative, 0.0f,
                relative, dir*0.6f,
                relative, -dir*0.6f);

        animation.setDuration( 2_500 );
        animation.setRepeatCount( 0 );
        animation.setFillAfter(true);

        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                carAni.clearAnimation();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        this.carAnimation = animation ;

        this.carAni.startAnimation( animation );
    }

    private void animateCarRotate( int dir ) {
        if( null != this.carAnimation ) {
            this.carAni.clearAnimation();
        }

        this.carAni.setImageResource(R.drawable.car_top_01_move );

        int relative = Animation.RELATIVE_TO_SELF ;

        Animation animation = new RotateAnimation(
                0, dir*70,
                relative, 0.5f,
                relative,  0.5f);

        animation.setDuration( 2_500 );
        animation.setRepeatCount( 0 );
        animation.setFillAfter(true);

        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                carAni.clearAnimation();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        this.carAnimation = animation ;

        this.carAni.startAnimation( animation );
    }

    public void moveCar(final String motion, final EditText status ) {
        final Activity_02_Car activity = this ;

        Runnable runnable = new Runnable() {
            String motionPrev = activity.motionCurr ;
            @Override
            public void run() {
                Log.d( "motion", String.format("motion prev = %s, motion curr = %s", motionPrev, motion ));


                if (motionPrev.equalsIgnoreCase(motion)) {
                    // do nothing
                } else if ( Motion.FORWARD.contains(motion.toUpperCase())) {
                    animateCarAdvance( 1 );
                } else if ( Motion.BACKWARD.contains(motion.toUpperCase())) {
                    animateCarAdvance( -1 );
                } else if ( Motion.RIGHT.contains(motion.toUpperCase())) {
                    animateCarRotate( 1 );
                } else if ( Motion.LEFT.contains(motion.toUpperCase())) {
                    animateCarRotate( -1 );
                } else if( "STOP".equalsIgnoreCase( Motion.STOP)) {
                    carAni.clearAnimation();
                    carAni.setImageResource(R.drawable.car_top_03_stop);
                }

                paintUI();
            }
        };

        this.motionCurr = motion ;

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

        new Handler().postDelayed( runnable, 0);
    }

    // pitch roll 값이 변했을 경우, 차를 제어한다.
    private void pitchRollUpdated( double pitch, double roll ) {

        pitch = -prettyDegree(pitch);
        roll = -prettyDegree(roll);

        this.pitch.setText( String.format( "%5.2f", pitch));
        this.roll.setText( String.format( "%5.2f", roll));

        if( true ) {
            return ;
        }

        final long now = System.currentTimeMillis();

        if( ! motionEnabled ) {
            // do nothing!
        }else if (now - motionTime < 700 ) {
            // do nothing!
        } else {
            String motion = "" ;

            if ( 45 <= pitch) {
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

            if( 0 < motion.length() ){
                this.moveCar(motion, status);
            }

            motionTime = now ;
        }
    }
    // -- pitchRollUpdated

    public void onGyroChanged( float [] values ) {
        String tag = "gyro";

        double rx = Math.toDegrees( values[0] ) % 360 ;
        double ry = Math.toDegrees( values[1] ) % 360 ;
        double rz = Math.toDegrees( values[2] ) % 360 ;

        rx = this.prettyDegree( rx );
        ry = this.prettyDegree( ry );
        rz = this.prettyDegree( rz );

        double pitchRate  = rx ;
        double rollRate   = rz ;

        // this.pitch.setText( String.format( "%5.2f", pitchRate ));
        // this.roll.setText( String.format( "%5.2f", rollRate ));

        if( false ) {
            Log.d(tag, String.format("r/s2 x = %3.6f, y = %3.6f, z = %3.6f", rx, ry, rz));
        }

        final long now = System.currentTimeMillis();

        if( ! motionEnabled ) {
            // do nothing!
        }else if (now - motionTime < 700 ) {
            // do nothing!
        } else {
            String motion = "" ;

            if ( -80 >= pitchRate ) {
                motion = Motion.FORWARD ;
            } else if ( 80 <= pitchRate ) {
                motion = Motion.BACKWARD;
            } else if ( -100 >= rollRate) {
                motion = Motion.RIGHT ;
            } else if ( 100 <= rollRate) {
                motion = Motion.LEFT ;
            } else {
                //motion = Motion.STOP ;
            }

            if( 0 < motion.length() ){
                this.moveCar(motion, status);

                motionTime = now ;
            }
        }

    }

}
