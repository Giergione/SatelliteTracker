package com.example.satellitetracker;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;


public class SatelliteTrackerApplication extends AppCompatActivity
        implements SurfaceHolder.Callback, LocationListener, SensorEventListener { //implements SensorEventListener

    private RelativeLayout overlay;
    ImageView yellowDot;

    private Camera mCamera;
    private SurfaceHolder mSurfaceHolder;
    private boolean isCameraviewOn = false;

    TextView LongNr;
    TextView LatNr;

    private static SensorManager sensorManager;

    private LocationManager locationManager;
    private String locationProvider;

    private SensorManager mSensorManager;

    private Sensor gyrosensor;
    private Sensor sensorRotation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        overlay = findViewById(R.id.overlay);
        yellowDot=findViewById(R.id.marker);


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        sensorManager=(SensorManager)getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        gyrosensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        setupListeners();
        setupLayout();
    }

    private void setupListeners() {

    }

    private void setupLayout() {
        getWindow().setFormat(PixelFormat.UNKNOWN);
        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.cameraPreview);
        mSurfaceHolder = surfaceView.getHolder();
        mSurfaceHolder.addCallback(this);
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        LongNr = (TextView) findViewById(R.id.LongNr);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        if (isCameraviewOn) {
            mCamera.stopPreview();
            isCameraviewOn = false;
        }

        if (mCamera != null) {
            try {
                mCamera.setPreviewDisplay(mSurfaceHolder);
                mCamera.startPreview();
                isCameraviewOn = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mCamera = Camera.open();
        mCamera.setDisplayOrientation(90);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
        isCameraviewOn = false;
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (sensor!=null) {
            sensorManager.registerListener(this,sensor,SensorManager.SENSOR_DELAY_FASTEST);
        } else {
            Toast.makeText(SatelliteTrackerApplication.this, "Sensor not supported", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

}