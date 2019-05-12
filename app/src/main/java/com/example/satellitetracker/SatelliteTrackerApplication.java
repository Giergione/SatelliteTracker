package com.example.satellitetracker;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.nfc.Tag;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.BoringLayout;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Timer;
import java.util.TimerTask;


public class SatelliteTrackerApplication extends AppCompatActivity
        implements SurfaceHolder.Callback, LocationListener, SensorEventListener {

    private static final long TIMER_DURATION = 30000;

    //Time stamp
    Date time = Calendar.getInstance().getTime();
    private TextView currentTime;

    //Name Input
    private TextView satNamePlace;
    private String satName;

    //API data
    private String dateTime;
    public static ArrayList<String> azimuths;
    public static ArrayList<String> elevations;
    private TextView dateTimeView;
    private TextView currentAzimuthView;
    private TextView currentElevationView;
    private TextView timeTillNextSet;
    private CountDownTimer countDownTimer;

    //Layout
    private RelativeLayout overlay;
    private static final String TAG = "TrackerApplication";
    private ImageView yellowDot;
    private ImageView compassArrow;


    //Camera utility
    private Camera mCamera;
    private SurfaceHolder mSurfaceHolder;
    private boolean isCameraviewOn = false;

    //Location utility
    private TextView LongNr;
    private TextView LatNr;
    private LocationManager locationManager;
    private String locationProvider;

    //Sensor utility
    private TextView azimuthValue;
    private TextView elevationValue;
    private TextView rotationValue;
    private static SensorManager sensorManager;
    private Sensor gyroSensor;
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
    public  static boolean latestUI = false;

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
        dateTime = intent.getExtras().getString("date_time");
        azimuths = intent.getStringArrayListExtra("azimuths");
        elevations = intent.getStringArrayListExtra("elevations");

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //Log.e(TAG, "Missing permissions");
            return;
        }

        //new RESTRequest().requestAzimuths(satName);
        setupLayout();

        sensorManager =(SensorManager)getSystemService(Context.SENSOR_SERVICE);
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        Criteria criteria = new Criteria();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationProvider = locationManager.getBestProvider(criteria, true);
        //Log.d(TAG, "Provider chosen: " + locationProvider);


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

        //startTime.set(Calendar.HOUR_OF_DAY, 11);
        //startTime.set(Calendar.MINUTE, 45);
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
                timeTillNextSet.setText(String.valueOf(seconds));
            }

            @Override
            public void onFinish() {
                countDownTimer = new CountDownTimer(TIMER_DURATION, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        int seconds = (int) millisUntilFinished / 1000;
                        timeTillNextSet.setText(String.valueOf(seconds));
                    }

                    @Override
                    public void onFinish() {

                    }
                };
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
        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.cameraPreview);
        mSurfaceHolder = surfaceView.getHolder();
        mSurfaceHolder.addCallback(this);
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        LongNr = (TextView) findViewById(R.id.LongNr);
        LatNr = (TextView) findViewById(R.id.LatNr);
        azimuthValue = (TextView) findViewById(R.id.testAz);
        elevationValue = (TextView) findViewById(R.id.testEl);
        rotationValue = (TextView) findViewById(R.id.RotationDegree);

        currentTime = (TextView) findViewById(R.id.timeStamp);
        currentTime.setText(time.toString());

        satNamePlace = (TextView) findViewById(R.id.SatName); //testing purposes only
        satNamePlace.setText(satName); //testing purposes only

        dateTimeView = (TextView) findViewById(R.id.firstDateTime);
        currentAzimuthView = (TextView) findViewById(R.id.currentAzimuth);
        currentElevationView = (TextView) findViewById(R.id.currentElevation);
        timeTillNextSet = (TextView) findViewById(R.id.timeTillNextSet);
        dateTimeView.setText(dateTime);

        //(TextView) ((TextView) findViewById(R.id.SatName)).setText(Double.toString(thetaH));
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
        //Log.d(TAG, "onResume");

        if (locationManager != null) {
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            locationManager.requestLocationUpdates(locationProvider, 1000, 1, this);
        }

        if (gyroSensor != null){
            sensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (rotationSensor != null) {
            sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        //Log.d(TAG, "onStatusChanged " + provider);
    }

    @Override
    public void onProviderEnabled(String provider) {
        //Log.d(TAG, "onProviderEnabled " + provider);
    }

    @Override
    public void onProviderDisabled(String provider) {
        //Log.d(TAG, "onProviderDisabled " + provider);
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

        //if next elevation+azimuth necessary re-call constructor

        int[] markerPlacementMatrix = differenceCalculator.getDifferenceMatrix(azimuth,elevation,rotation);

        yellowDot.setY(markerPlacementMatrix[1]);
        yellowDot.setX(markerPlacementMatrix[0]);
        compassArrow.setRotation(markerPlacementMatrix[2]);

        if (!latestUI) {
            updateObservedCoords();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d(TAG, "onAccuracyChanged "  + sensor.getName());
    }


    public void updateObservedCoords() {
        countDownTimer.cancel();
        countDownTimer.start();
        float elevation = differenceCalculator.getTargetElevation();
        float azimuth = differenceCalculator.getTargetAzimuth();
        currentElevationView.setText(String.valueOf(elevation));
        currentAzimuthView.setText(String.valueOf(azimuth));
        latestUI = true;
    }

}