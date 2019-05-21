package com.example.satellitetracker;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.CountDownTimer;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Calendar;
import java.io.IOException;
import java.util.Date;


public class SatelliteTrackerApplication extends AppCompatActivity
        implements SurfaceHolder.Callback, LocationListener, SensorEventListener {

    private static final long TIMER_DURATION = 30000;
    private static final int ALLOWED_ERROR_MARGIN = 500;
    private static final String TAG = "TrackerApplication";

    //Time stamp
    Date time = Calendar.getInstance().getTime();

    //Back button
    private Button backButton;

    //Name Input
    private TextView satNamePlace;
    private String satName;

    //API data
    public static ArrayList<String> azimuths;
    public static ArrayList<String> elevations;
    public static String dateTime;
    private CountDownTimer countDownTimer;

    //Layout
    private RelativeLayout overlay;
    private ImageView yellowDot;
    private ImageView compassArrow;
    private ImageView greenIndicator;
    private TextView azimuthValue;
    private TextView elevationValue;
    private TextView rotationValue;
    private TextView LongNr;
    private TextView LatNr;
    private TextView currentAzimuthView;
    private TextView currentElevationView;
    private TextView timeTillNextSet;
    private TextView initialCountdown;
    private TextView initialCountdownText;
    private TextView targetAzimuthText;
    private TextView targetElevationText;
    private TextView countDownText;


    //Camera utility
    private Camera mCamera;
    private SurfaceHolder mSurfaceHolder;
    private boolean isCameraviewOn = false;

    //Location utility
    private LocationManager locationManager;
    private String locationProvider;

    //Sensor utility
    private static SensorManager sensorManager;
    private Sensor rotationSensor;

    //Orientation sensor matrixes
    float[] rotationMatrix = new float[16];
    float[] outRotationMatrix = new float[16];
    float[] orientationValues = new float[3];
    float azimuth;
    float elevation;
    float rotation;

    public static int deviceWidth;
    public static int deviceHeight;
    public static double verticalFoV;
    public static double horizontalFoV;

    //Difference calculator for adjusting the marker
    //update with every change in target elevation and azimuth
    public static DifferenceCalculator differenceCalculator;
    public static boolean latestUI = false;
    public static boolean lastPoint = false;
    public static boolean riseReached = false;
    public static boolean activeSession = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        overlay = findViewById(R.id.overlay);
        yellowDot = findViewById(R.id.marker);
        compassArrow = findViewById(R.id.arrow);

        Intent intent = getIntent();
        satName = intent.getExtras().getString("satellite_name");
        azimuths = intent.getStringArrayListExtra("azimuths");
        elevations = intent.getStringArrayListExtra("elevations");
        dateTime = intent.getStringExtra("date_time");

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Missing permissions");
            return;
        }


        backButton = findViewById(R.id.closeButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goBackToMenu();
            }
        });

        setupLayout();

        sensorManager =(SensorManager)getSystemService(Context.SENSOR_SERVICE);
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);


        Criteria criteria = new Criteria();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationProvider = locationManager.getBestProvider(criteria, true);
        Log.d(TAG, "Provider chosen: " + locationProvider);


        Location lastLocation = locationManager.getLastKnownLocation(locationProvider);
        if (lastLocation != null) {
            onLocationChanged(lastLocation);
        } else {
            LatNr.setText("NaN");
            LongNr.setText("NaN");
        }


        Display display = getWindowManager().getDefaultDisplay();
        deviceWidth = display.getWidth();
        deviceHeight = display.getHeight();

        differenceCalculator = new DifferenceCalculator(0,0);

        Calendar startTime = Calendar.getInstance();

        //Dummy rise time for testing
        //startTime.set(Calendar.HOUR_OF_DAY, 12);
        //startTime.set(Calendar.MINUTE, 0);
        //startTime.set(Calendar.SECOND, 0);

        startTime.set(Calendar.HOUR_OF_DAY, Integer.valueOf(dateTime.substring(dateTime.length()-8, dateTime.length()-6)));
        startTime.set(Calendar.MINUTE, Integer.valueOf(dateTime.substring(dateTime.length()-5,dateTime.length()-3)));
        startTime.set(Calendar.SECOND, Integer.valueOf(dateTime.substring(dateTime.length()-2)));

        Calendar currentTime = Calendar.getInstance();

        long timeUntillFirstPoint;
        if (time.compareTo(startTime.getTime()) < 0) {
            timeUntillFirstPoint = (startTime.getTime().getTime() - currentTime.getTime().getTime());
        } else {
            Log.i(TAG, "onCreate: time.getday - " + currentTime.get(Calendar.DAY_OF_YEAR));
            startTime.set(Calendar.DAY_OF_YEAR, currentTime.get(Calendar.DAY_OF_YEAR)+1);
            timeUntillFirstPoint = (startTime.getTime().getTime() - currentTime.getTime().getTime());
        }
        Log.i(TAG, "onCreate: timeuntillfirstpoint: " +timeUntillFirstPoint);

        setTimedEvent(startTime.getTimeInMillis());


        countDownTimer = new CountDownTimer(timeUntillFirstPoint, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int seconds = (int) millisUntilFinished / 1000;
                initialCountdown.setText(String.valueOf(seconds));
            }

            @Override
            public void onFinish() {
                initialCountdown.setText("...");
            }
        }.start();
    }

    private void setTimedEvent(long timeInMillis) {
        Intent intent = new Intent(this, PositionUpdateAlarm.class);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        alarmManager.set(AlarmManager.RTC, timeInMillis,  pendingIntent);
    }



    private void setupLayout() {
        getWindow().setFormat(PixelFormat.UNKNOWN);
        SurfaceView surfaceView = findViewById(R.id.cameraPreview);
        mSurfaceHolder = surfaceView.getHolder();
        mSurfaceHolder.addCallback(this);
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        initialCountdown = findViewById(R.id.initializingCountdown);
        initialCountdownText = findViewById(R.id.initializeText1);
        LongNr = findViewById(R.id.LongNr);
        LatNr = findViewById(R.id.LatNr);
        azimuthValue = findViewById(R.id.testAz);
        elevationValue = findViewById(R.id.testEl);
        rotationValue = findViewById(R.id.RotationDegree);

        satNamePlace = findViewById(R.id.SatName);
        satNamePlace.setText(satName);

        currentAzimuthView = findViewById(R.id.currentAzimuth);
        currentElevationView = findViewById(R.id.currentElevation);
        timeTillNextSet = findViewById(R.id.timeTillNextSet);
        targetAzimuthText = findViewById(R.id.targetAzimuthText);
        targetElevationText = findViewById(R.id.targetElevationText);
        countDownText = findViewById(R.id.countdownText);
        greenIndicator = findViewById(R.id.greenindicator);

    }

    @Override
    public void onLocationChanged(Location location) {
        //Log.d(TAG, "onLocationChanged");
        LongNr.setText(Double.toString(location.getLongitude()));
        LatNr.setText(Double.toString(location.getLatitude()));
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
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
        horizontalFoV = mCamera.getParameters().getHorizontalViewAngle();
        verticalFoV = mCamera.getParameters().getVerticalViewAngle();

        Log.i(TAG, "onCreate: horizontalFov - " + horizontalFoV);
        Log.i(TAG, "onCreate: verticalFov - " + verticalFoV);
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

        if (locationManager != null) {
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            locationManager.requestLocationUpdates(locationProvider, 1000, 1, this);
        }

        if (rotationSensor != null) {
            sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }



    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }


    @Override
    public void onSensorChanged(SensorEvent event) {


        switch (event.sensor.getType()) {
            case Sensor.TYPE_ROTATION_VECTOR:
                rotationMatrix = new float[16];
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
        }

        switch (getWindowManager().getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_0:
                SensorManager.remapCoordinateSystem(rotationMatrix,
                        SensorManager.AXIS_X, SensorManager.AXIS_Z,
                        outRotationMatrix);
                break;
            case Surface.ROTATION_90:
                SensorManager.remapCoordinateSystem(rotationMatrix,
                        SensorManager.AXIS_Y,
                        SensorManager.AXIS_MINUS_X,
                        outRotationMatrix);
                break;
            case Surface.ROTATION_180:
                SensorManager.remapCoordinateSystem(rotationMatrix,
                        SensorManager.AXIS_MINUS_X,
                        SensorManager.AXIS_MINUS_Z,
                        outRotationMatrix);
                break;
            case Surface.ROTATION_270:
                SensorManager.remapCoordinateSystem(rotationMatrix,
                        SensorManager.AXIS_MINUS_Y,
                        SensorManager.AXIS_X, outRotationMatrix);
                break;
        }
        SensorManager.getOrientation(outRotationMatrix, orientationValues);

        azimuth = (float) (Math.toDegrees(orientationValues[0]));
        elevation = (float) Math.toDegrees(-orientationValues[1]);
        rotation = (float) Math.toDegrees(orientationValues[2]);

        azimuthValue.setText(Float.toString(azimuth));
        elevationValue.setText(Float.toString(elevation));
        rotationValue.setText(Float.toString(rotation));

        //if next elevation and azimuth necessary re-call constructor

        int[] markerPlacementMatrix = differenceCalculator.getDifferenceMatrix(azimuth,elevation,rotation);

        yellowDot.setY(markerPlacementMatrix[1]);
        yellowDot.setX(markerPlacementMatrix[0]);
        compassArrow.setRotation(markerPlacementMatrix[2]);

        if (Math.abs(markerPlacementMatrix[3]) < ALLOWED_ERROR_MARGIN) {
            int x = ALLOWED_ERROR_MARGIN - Math.abs(markerPlacementMatrix[3]);
            float alpha = (float) x / ALLOWED_ERROR_MARGIN;
            greenIndicator.setAlpha(alpha);
        } else {
            greenIndicator.setAlpha(0f);
        }

        if (!latestUI) {
            updateUI();
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d(TAG, "onAccuracyChanged "  + sensor.getName());
    }


    public void updateUI() {
        if (riseReached) {
            countDownTimer = new CountDownTimer(TIMER_DURATION, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    int seconds = (int) millisUntilFinished / 1000;
                    timeTillNextSet.setText(String.valueOf(seconds));

                }

                @Override
                public void onFinish() {
                    Log.i(TAG, "onFinish: reached finish on last point");
                    if (lastPoint) {
                        endTheSession();
                    }
                }
            };
            compassArrow.setVisibility(View.VISIBLE);
            yellowDot.setVisibility(View.VISIBLE);
            greenIndicator.setVisibility(View.VISIBLE);
            riseReached = false;
            activeSession = true;

            initialCountdownText.setVisibility(View.INVISIBLE);
            initialCountdown.setVisibility(View.INVISIBLE);
            countDownText.setVisibility(View.VISIBLE);
            targetAzimuthText.setVisibility(View.VISIBLE);
            targetElevationText.setVisibility(View.VISIBLE);
        }
        if (activeSession) {
            float elevation = differenceCalculator.getTargetElevation();
            float azimuth = differenceCalculator.getTargetAzimuth();
            currentElevationView.setText(String.valueOf(elevation));
            currentAzimuthView.setText(String.valueOf(azimuth));
        }
        countDownTimer.cancel();
        countDownTimer.start();
        latestUI = true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(this, MenuScreen.class);
        finish();
        startActivity(intent);
    }

    private void goBackToMenu() {
        Intent intent = new Intent(this, MenuScreen.class);
        finish();
        startActivity(intent);
    }

    private void endTheSession() {
        Toast.makeText(getBaseContext(), "End of the session", Toast.LENGTH_LONG).show();
        Intent intent = new Intent(this, MenuScreen.class);
        finish();
        startActivity(intent);
    }


}