package com.carapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams;

public class GoogleMapActivity extends ComActivity implements OnMapReadyCallback {

    private GoogleMap map;
    private FusedLocationProviderClient fusedLocationClient;
    private MarkerOptions myLocMarker ;
    private WebView videoView ;
    private Button stop ;
    private EditText status ;
    private boolean videoFullWidth = false ;

    public int getLayoutId() {
        return R.layout.activity_maps;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.motionEnabled = false ;

        this.videoView = this.findViewById(R.id.videoView);
        this.stop = this.findViewById(R.id.stop );
        this.status = this.findViewById(R.id.status);

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
                if( motionEnabled ) {
                    moveCar("stop", status );
                }

                motionEnabled = ! motionEnabled ;

                stop.setText( motionEnabled ? "STOP" : "START" );
            }
        });

        this.videoView.setOnTouchListener(new View.OnTouchListener(){

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction()==MotionEvent.ACTION_MOVE){
                    return false;
                }

                if (event.getAction()==MotionEvent.ACTION_UP){
                    whenVideoViewClicked();
                }

                return false;
            }
        });
    }

    public void whenVideoViewClicked() {
        Log.d( TAG, "VideoView Clicked.");

        startActivity(new android.content.Intent(this, com.carapp.VideoActivity.class));
    }

    public void whenVideoViewClickedOld() {
        Log.d( TAG, "VideoView Clicked.");

        final WebView videoView = this.videoView;

        int w = videoView.getWidth() ;
        int sw = this.getScreenWidth() ;
        int sh = this.getScreenHeight() ;

        if( ! videoFullWidth ) {
            w = ( sw - 8 ) ;

            status.setText( "동영상 화면을 최대로 확장합니다." );
        } else {
            w = sw / 2;
            status.setText( "동영상 이전 크기로 복원합니다." );
        }

        int h = (int) ( w*3.0/4.0 );

        LayoutParams params = new LayoutParams(w, h);
        params.bottomMargin = 4;
        params.leftMargin = 4;
        params.gravity = Gravity.LEFT | Gravity.BOTTOM ;

        videoView.setLayoutParams( params );

        videoFullWidth = ! videoFullWidth ;
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        Log.v( TAG, "onResume");

        this.playVideo();
    }

    @SuppressLint("MissingPermission")
    private void getLastLocation(){
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                fusedLocationClient.getLastLocation().addOnCompleteListener(
                        new OnCompleteListener<Location>() {
                            @Override
                            public void onComplete(@NonNull Task<Location> task) {
                                Location location = task.getResult();
                                if (location != null) {
                                    LatLng latlng = new LatLng(location.getLatitude(), location.getLongitude());

                                    myLocMarker = new MarkerOptions().position(latlng).title("현재 나의 위치") ;
                                    map.addMarker(myLocMarker).showInfoWindow();

                                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, map.getMaxZoomLevel() - 5));

                                    status.setText( "지도를 핸드폰 현재 위치로 이동하였습니다.");
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("sunabove", "requestCode" + requestCode + "resultCode = " + resultCode );
        // Check which request we're responding to
        if (requestCode == 0) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                // The user picked a contact.
                // The Intent's data Uri identifies which contact was selected.

                // Do something with the contact here (bigger example below)
            }
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

        // Add a marker in Sydney and move the camera
        LatLng latlng = new LatLng(37.5866, 126.97);
        map.addMarker(new MarkerOptions().position(latlng).title("청와대"));

        //mMap.moveCamera(CameraUpdateFactory.newLatLng(latlng));
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, 17));

        status.setText( "지도가 로드되었습니다.");

        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                status.setText( "현재 위치를 체크중입니다.");
                getLastLocation();
            }
        }, 5_000);

    }

    int PERMISSION_ID = 44;

    private boolean checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        return false;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                PERMISSION_ID
        );
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER
        );
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
}
