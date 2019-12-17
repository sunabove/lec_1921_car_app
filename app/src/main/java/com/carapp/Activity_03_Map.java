package com.carapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.view.animation.TranslateAnimation;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Dash;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams;
import androidx.lifecycle.Lifecycle;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;

public class Activity_03_Map extends ComActivity implements OnMapReadyCallback , Orientation.Listener  {

    private GoogleMap map;
    private FusedLocationProviderClient fusedLocationClient;

    private Marker myPhoneMarker;
    private Marker currCarMarker;
    private int currMarkerUpdCnt = 0 ;
    private Polyline gpsPath = null ;
    private GpsLog gpsLog = new GpsLog();
    private LatLng lastGpsLatLng ;

    private WebView videoView ;
    private FloatingActionButton stop ;
    private FloatingActionButton autopilot ;
    private EditText status ;
    private EditText log ;

    private EditText pitch ;
    private EditText roll ;

    private ImageView carAni ;
    private Animation carAnimation = null ;

    // orientation sensor
    private Orientation orientation;

    private String currMotion = "";
    private long motionTime = System.currentTimeMillis();

    private int moveCnt = 0 ;
    private String motionCurr = "" ;

    private boolean videoFullWidth = false ;

    // auto pilot
    private Marker pathStart ;
    private Marker pathEnd ;
    private Polyline autoPilotPath ;
    private boolean isAutopilot = false;
    // -- auto pilot

    public int getLayoutId() {
        return R.layout.activity_maps;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.hideActionBar();

        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        this.videoView = this.findViewById(R.id.videoView);
        this.stop = this.findViewById(R.id.stop );
        this.status = this.findViewById(R.id.status);
        this.log = this.findViewById(R.id.log);

        this.autopilot = this.findViewById(R.id.autopilot);

        this.pitch = this.findViewById(R.id.pitch);
        this.roll = this.findViewById(R.id.roll);

        this.carAni = this.findViewById(R.id.carAni);

        this.motionEnabled = false ;

        this.orientation = new Orientation(this);

        this.stop.setEnabled( false );
        this.autopilot.setEnabled( false );

        this.status.setText( "" );

        // hide keyboard always
        this.status.setInputType(InputType.TYPE_NULL);

        //setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        this.stop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                whenStopClicked( v );
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
                    whenVideoViewClicked();
                }

                return false;
            }
        });

        // 자율 주행 토글
        this.autopilot.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                whenAutopilotClicked( v );
            }
        });
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        Log.v( TAG, "onResume");

        this.hideActionBar();

        this.playVideo();

        this.getCarLocationByHttp( 500 );

        this.orientation.startListening(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        orientation.stopListening();

        this.stopPlayVideo();
    }

    private void whenStopClicked(View v) {
        motionEnabled = ! motionEnabled ;

        if( motionEnabled ) { // drive
            if( isAutopilot ) {
                carAni.setImageResource(R.drawable.car_top_02_drive );

                gpsLog = new GpsLog();
                gpsPath.remove();

                if( null == pathEnd ) {
                    motionEnabled = false ;

                    stop.setImageResource( R.drawable.start_btn_icon);

                    status.setText( "화면을 터치하여 목적지를 설정하세요." );

                    Toast.makeText( getApplicationContext(),"화면을 터치하여 목적지를 설정하세요.",Toast.LENGTH_SHORT).show();
                } else {
                    this.animateCarAutoPilot();

                    stop.setImageResource( R.drawable.stop_btn_icon);

                    Toast.makeText( getApplicationContext(),"자율 주행을 시작합니다.",Toast.LENGTH_SHORT).show();

                    status.setText( "자율 주행을 시작합니다." );

                    moveCar( Motion.AUTOPILOT, status, 0, 0, pathEnd.getPosition() );
                }
            } else {
                carAni.setImageResource(R.drawable.car_top_01_move);

                stop.setImageResource( R.drawable.stop_btn_icon);

                status.setText( "수동 주행" );

                Toast.makeText( getApplicationContext(),"차량 수동 주행을 시작합니다.",Toast.LENGTH_SHORT).show();
            }
        } else { // stop_btn_icon
            carAni.clearAnimation();
            carAni.setImageResource(R.drawable.car_top_03_stop);

            stop.setImageResource( R.drawable.start_btn_icon);

            Toast.makeText( getApplicationContext(),"차량 주행을 종료합니다..",Toast.LENGTH_SHORT).show();

            moveCar( Motion.STOP, status, 0, 0 );
        }
    }

    private void whenAutopilotClicked( View v ){
        isAutopilot = ! isAutopilot ;

        if( isAutopilot ) { // autopilot enabled
            autopilot.setImageResource(R.drawable.autopilot_enabled);

            Toast.makeText( getApplicationContext(),"자율 주행 모드입니다.",Toast.LENGTH_SHORT).show();

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    status.setText( "화면을 터치하여 목적지를 설정하세요." );
                    Toast.makeText( getApplicationContext(),"화면을 터치하여 목적지를 설정하세요.",Toast.LENGTH_SHORT).show();
                }
            }, 1_200 );

            gpsLog = new GpsLog();

            if( null != gpsPath ) {
                gpsPath.remove();
            }

            // 출발 지점 추가
            LatLng latLng = null ;

            if( null != lastGpsLatLng ) {
                latLng = lastGpsLatLng ;
            } else if( null != myPhoneMarker ) {
                latLng = myPhoneMarker.getPosition();
            } else {
                latLng = map.getCameraPosition().target ;
            }

            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(latLng);
            markerOptions.title( "출발지" );

            pathStart = map.addMarker(markerOptions);
            pathStart.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.path_start));

            if( null != myPhoneMarker ) {
                myPhoneMarker.hideInfoWindow();
            }

            pathStart.showInfoWindow();
            // 출발 지점 추가
        } else { // autopilot disabled
            autopilot.setImageResource(R.drawable.autopilot_disabled);

            Toast.makeText( getApplicationContext(),"수동 주행 모드입니다.",Toast.LENGTH_SHORT).show();

            if( null != pathStart ) {
                pathStart.remove();
                pathStart = null ;
            }

            if( null != pathEnd ) {
                pathEnd.remove();
                pathEnd = null ;
            }

            if( null != autoPilotPath ) {
                autoPilotPath.remove();
                autoPilotPath = null ;
            }
        }
    }

    private void whenMapClick(LatLng latLng) {
        final String tag = "google map";

        Log.d( tag, "onMapClick");

        if( ! isAutopilot ) {
            Log.d( tag, "Autopilot is not enabled." );
        } else if( isAutopilot ) {

            if (null != pathEnd) {
                pathEnd.remove();
            }

            if (null != autoPilotPath) {
                autoPilotPath.remove();
            }

            // clear gps log
            this.gpsLog = new GpsLog();

            if (null != gpsPath) {
                gpsPath.remove();
            }

            // 도착 지점 추가
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(latLng);
            markerOptions.title("목적지");

            pathEnd = map.addMarker(markerOptions);
            pathEnd.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.path_end));

            if (null != pathEnd) {
                pathEnd.hideInfoWindow();
            }

            pathEnd.showInfoWindow();
            // -- 도착 지점 추가

            // 출발지 -> 도착지 경로 표시

            if (null != pathStart) {
                List<PatternItem> pattern = Arrays.<PatternItem>asList(new Dash(20), new Gap(10));
                //List<PatternItem> pattern = Arrays.<PatternItem>asList(new Dot(), new Gap(20), new Dash(30), new Gap(20));

                PolylineOptions polyOptions = new PolylineOptions().width(20).color(Color.GREEN).geodesic(true);
                polyOptions.add(pathStart.getPosition());
                polyOptions.add(pathEnd.getPosition());
                polyOptions.pattern(pattern);

                autoPilotPath = map.addPolyline(polyOptions);
            }

            Toast.makeText(getApplicationContext(), "목적지가 설정되었습니다.", Toast.LENGTH_SHORT).show();

            // -- 출발지 -> 도착지 경로 표시
        }

    }

    public void whenVideoViewClicked() {
        Log.d( TAG, "VideoView Clicked.");

        boolean useActivity = false ;

        if( useActivity ) {
            startActivity(new android.content.Intent(Activity_03_Map.this, Activity_04_Video.class));
            return ;
        }

        final WebView videoView = this.videoView;

        int sw = this.getScreenWidth() ;
        int sh = this.getScreenHeight() ;

        int m = 4;
        int w = videoView.getWidth() ;

        if( ! videoFullWidth ) {
            w = ( sw - 2*m ) ;

            status.setText( "동영상 화면을 최대로 확장합니다." );
        } else {
            w = sw / 2;
            status.setText( "동영상 이전 크기로 복원합니다." );
        }

        int h = (int) ( w*3.0/4.0 );

        LayoutParams params = new LayoutParams(w, h);
        params.bottomMargin = m;
        params.leftMargin = m;
        params.gravity = Gravity.LEFT | Gravity.BOTTOM ;

        videoView.setLayoutParams( params );

        videoFullWidth = ! videoFullWidth ;
    }

    private void getCarLocationBySocket() {
        Intent intent = new Intent(this, SocketService.class);
        intent.setAction( SocketService.ACTION_CURR_LOC );
        startService(intent);
    }

    // 차량의 최근 위치를 반환한다.
    private void getCarLocationByHttp( final long delay ) {
        final Handler handler = new Handler() ;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                getCarLocationByHttpImpl( delay );
            }
        }, delay );
    }

    private int carLocCnt = 0 ;
    private void getCarLocationByHttpImpl( final long delay ) {
        String url = "http://10.3.141.1/send_me_curr_pos.json";
        final String tag = "car location" ;

        carLocCnt += 1;
        Log.d( tag, String.format("[%04d] Getting car location by http ...", carLocCnt ) );

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        setCarLocationByJsonString( response );

                        if( Activity_03_Map.this.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                            getCarLocationByHttp( delay );
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();

                        Log.d( tag, "Error has occured" );

                        getCarLocationByHttp( 3*delay );
                    }
                });

        this.requestQueue.add( jsonObjectRequest );
    }

    private void setCarLocationByJsonString( Object obj ) {
        try {
            JSONObject response = null ;

            if( obj instanceof JSONObject ) {
                response = (JSONObject) obj ;
            }

            Object test = null;

            try {
                test = response.get("latitude");
            } catch ( Exception e ) {
                test = null;
            }

            if( null == test || 1 > test.toString().trim().length() ) {
                Log.d( "sunabove", "There is no gps data.");
                return ;
            }

            boolean isAutopilot = this.isAutopilot;

            double latitude = Double.parseDouble(response.get("latitude").toString().trim());
            double longitude = Double.parseDouble(response.get("longitude").toString().trim());
            double heading = Double.parseDouble(response.get("heading").toString().trim());
            double altitude = Double.parseDouble(response.get("altitude").toString().trim());
            String timestamp = response.get( "timestamp" ).toString().trim();

            String text = "" ;
            text += String.format(  "Lat     :  %3.6f °", prettyAngle60( latitude ) );
            text += String.format("   Lon   : %3.6f °", prettyAngle60( longitude ) ) ;
            text += String.format("\nHead : %3.6f °", prettyAngle60( heading ) ) ;
            text += String.format("   Alt    :  %3.6f m", altitude ) ;

            log.setText( text );

            if( null != currCarMarker ) {
                currCarMarker.remove();
            }

            if( null !=gpsPath ) {
                gpsPath.remove();

            }

            final String tag = "car location" ;

            if( true ) {
                LatLng latLng = new LatLng(latitude, longitude);
                GpsLog gpsLog = Activity_03_Map.this.gpsLog ;

                if( null == lastGpsLatLng ) {
                    lastGpsLatLng = latLng;

                    gpsLog.add( latLng );
                } else if( null != lastGpsLatLng ){
                    float dists [] = getDistance( lastGpsLatLng, latLng );
                    float dist = dists[ 0 ];

                    Log.d( tag , String.format("dist = %f", dist ) );

                    if( 0.1f > dist ) {
                        Log.d( tag , String.format("dist is small = %f", dist ) );
                        if( 0 < gpsLog.size() ) {
                            gpsLog.remove(gpsLog.size() - 1);
                        }
                        gpsLog.add( latLng );
                    } else {
                        gpsLog.add( latLng );
                    }

                    lastGpsLatLng = latLng;
                }

                if( 1_000 < gpsLog.size() ) {
                    while( 1_000 < gpsLog.size() ) {
                        gpsLog.remove( 0 );
                    }
                }

                int color = isAutopilot ? Color.RED : Color.BLUE ;
                int width = isAutopilot ? 12 : 10 ;

                PolylineOptions polyOptions = new PolylineOptions().width( width ).color( color ).geodesic(true);

                if( isAutopilot ) {
                    List<PatternItem> pattern = Arrays.<PatternItem>asList(new Dash(20), new Gap(10));
                    polyOptions.pattern( pattern );
                }

                for( LatLng log : gpsLog ) {
                    polyOptions.add( log );
                }

                gpsPath = map.addPolyline( polyOptions );
            }

            if( true ){
                currMarkerUpdCnt += 1 ;

                LatLng latLng = new LatLng(latitude, longitude);
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(latLng);
                markerOptions.title(String.format("현재 차량 위치 [%04d]", currMarkerUpdCnt ));

                currCarMarker = map.addMarker(markerOptions);

                currCarMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.car_map_icon_02));

                if( 1 > gpsLog.size() ) {
                    currCarMarker.setRotation( (float) heading );
                } else {
                    double gpsHeading = gpsLog.getHeading( heading );

                    currCarMarker.setRotation( (float) gpsHeading );

                    Log.d( "heading" , "heading = " + gpsHeading );
                }

                //currCarMarker.showInfoWindow();

                Projection projection = map.getProjection();
                Point scrPos = projection.toScreenLocation(currCarMarker.getPosition());

                double x = scrPos.x;
                double y = scrPos.y;

                int sw = getScreenWidth();
                int sh = getScreenHeight();

                double xr = Math.abs( sw/2.0 - x )/sw ;
                double yr = Math.abs( sh/2.0 - y )/sh ;

                if( false ) {
                    Log.d("screen range", "xr = " + xr);
                    Log.d("screen range", "yr = " + yr);
                }

                if( 0.35 < xr || 0.4 < yr ) {
                    map.animateCamera(CameraUpdateFactory.newLatLng(latLng));
                }

            }

        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    // 핸드폰의 최근 위치를 반환한다.
    @SuppressLint("MissingPermission")
    private void getPhoneLastLocation(){
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                fusedLocationClient.getLastLocation().addOnCompleteListener(
                        new OnCompleteListener<Location>() {
                            @Override
                            public void onComplete(@NonNull Task<Location> task) {
                                Location location = task.getResult();
                                if (location != null) {
                                    if( null != myPhoneMarker ) {
                                        myPhoneMarker.remove();
                                    }

                                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

                                    MarkerOptions options = new MarkerOptions();
                                    options.position(latLng).title("현재 나의 위치") ;

                                    myPhoneMarker = map.addMarker(options);
                                    myPhoneMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.smart_phone_icon_01));

                                    myPhoneMarker.showInfoWindow();

                                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, map.getMaxZoomLevel() - 2 ));

                                    status.setText( "지도를 핸드폰 현재 위치로 이동하였습니다.");

                                    stop.setEnabled( true );
                                    autopilot.setEnabled( true );

                                    SharedPreferences.Editor editor = sharedPref.edit();
                                    editor.putFloat( "lastPhoneLat", (float) latLng.latitude );
                                    editor.putFloat( "lastPhoneLng", (float) latLng.longitude);
                                    editor.commit();
                                }
                            }
                        }
                );
            } else {
                Toast.makeText(this, "Turn on location", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        } else {
            requestPermissions();
        }
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;

        final Activity_03_Map activity = this;

        // Add a marker in Sydney and move the camera
        if( false ) {
            map.addMarker(new MarkerOptions().position(new LatLng(37.5866, 126.97)).title("청와대"));
        }

        Float lat = sharedPref.getFloat("lastPhoneLat", 37.5866f );
        Float lng = sharedPref.getFloat("lastPhoneLat", 126.97f );

        LatLng latlng = new LatLng(lat, lng);

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, 17));

        status.setText( "지도가 로드되었습니다.");

        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                status.setText( "현재 위치를 체크중입니다.");
                getPhoneLastLocation();
            }
        }, 5_000);

        googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                activity.whenMapClick( latLng );
            }
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_ID) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Granted. Start getting the location information
            }
        }
    }

    public void paintUI() {
        // do nothing.
    }

    public void moveCar(final String motion, final EditText status, double pitchDeg, double rollDeg ) {
        this.moveCar( motion, status, pitchDeg, rollDeg, null);
    }

    public void moveCar(final String motion, final EditText status, double pitchDeg, double rollDeg, LatLng latLng ) {
        pitchDeg = pitchDeg % 360;
        rollDeg = rollDeg % 360;

        final Activity_03_Map activity = this ;

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

        String url = String.format("http://10.3.141.1/car_drive.json?motion=%s&pitchDeg=%f&rollDeg=%f", motion, pitchDeg, rollDeg);

        if( null != latLng ) {
            url += "&lat=" + latLng.latitude ;
            url += "&lng=" + latLng.longitude ;
        }

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if( null != status ) {
                            moveCnt += 1 ;
                            status.setText(String.format("%s [%04d]", motion.toUpperCase(), moveCnt) );
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

        if( Motion.STOP.equalsIgnoreCase( motion ) ) {
            requestQueue.cancelAll(new RequestQueue.RequestFilter() {
                @Override
                public boolean apply(Request<?> request) {
                    return true;
                }
            });
        }

        requestQueue.add(stringRequest);

        new Handler().postDelayed( runnable, 0);
    }

    // pitch roll 값이 변했을 경우, 차를 제어한다.
    public void onOrientationChanged( float pitch, float roll ) {
        this.pitchRollUpdated( pitch, roll );
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
        } else if (now - motionTime < 700 ) {
            // do nothing!
        } else if( isAutopilot ) {
            // do nothing
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

            if( Motion.STOP.equalsIgnoreCase( this.motionCurr ) && Motion.STOP.equalsIgnoreCase( motion ) ) {
                // do nothing!
            } else if( true ){
                this.moveCar(motion, status, pitch, roll );

                this.motionCurr = motion;

                motionTime = now ;
            }

        }
    }
    // -- pitchRollUpdated

    public void onGyroChanged( float [] values ) {

    }

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
        animation.setRepeatCount( -1 );
        animation.setRepeatMode(Animation.RESTART);

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
        animation.setRepeatCount( -1 );
        animation.setFillAfter(true);

        this.carAnimation = animation ;

        this.carAni.startAnimation( animation );
    }

    private int aniAutoPilotDir = 1 ;
    private void animateCarAutoPilot() {
        if( null != this.carAnimation ) {
            this.carAni.clearAnimation();
        }

        this.carAni.setImageResource(R.drawable.car_top_02_drive );

        int relative = Animation.RELATIVE_TO_SELF ;

        final int dir = aniAutoPilotDir ;

        Animation animation = new RotateAnimation(
                -dir*20, dir*20,
                relative, 0.5f,
                relative,  0.5f);

        animation.setDuration( 2_000 );
        animation.setRepeatCount( 0 );
        animation.setFillAfter(true);

        final Activity_03_Map activity = this ;

        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if( activity.isAutopilot && activity.motionEnabled ) {
                            aniAutoPilotDir = -aniAutoPilotDir;
                            animateCarAutoPilot();
                        }
                    }
                }, 700 );
            }

            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        this.carAnimation = animation ;

        this.carAni.startAnimation( animation );
    }
}
