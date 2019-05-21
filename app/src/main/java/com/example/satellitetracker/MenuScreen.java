package com.example.satellitetracker;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MenuScreen extends AppCompatActivity implements LocationListener,
        ErrorFragment.CancelListener {

    private static final String TAG = "MenuScreen";

    private Button button;
    private AutoCompleteTextView satName;
    private RequestQueue requestQueue;
    Intent intent;

    LocationManager locationManager;
    private double longitude;
    private double latitude;

    String[] satellites = SatelliteNames.satellites;

    int PERMISSION_ALL = 1;
    String[] PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.BODY_SENSORS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.INTERNET,
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_screen);

        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }

        intent = new Intent(this, SatelliteTrackerApplication.class);

        requestQueue = Volley.newRequestQueue(this);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.select_dialog_singlechoice, satellites);
        satName = findViewById(R.id.nameInput);
        satName.setThreshold(1);
        satName.setAdapter(adapter);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Location location = locationManager.getLastKnownLocation(locationManager.NETWORK_PROVIDER);
        onLocationChanged(location);

        button = findViewById(R.id.menuButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                jsonParse();
            }
        });
    }

    private void jsonParse() {
        String uri = String.format("http://1.2.3.4:8080/endpoint?satName=%1$s&latitude=%2$s&longitude=%3$s&mobileTracker=%4$s",
                String.valueOf(satName.getText()), String.valueOf(latitude), String.valueOf(longitude), "true");
        uri = uri.replaceAll(" ", "%20").toLowerCase();

        String testUri = "https://api.myjson.com/bins/92hzy";
        button.setEnabled(false);


        JsonObjectRequest objectRequest = new JsonObjectRequest(Request.Method.GET, testUri,
                null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONArray jsonArray = response.getJSONArray("rollAngles");
                    String startTime = response.getString("riseTime");
                    ArrayList<String> azimuths = new ArrayList<>();
                    ArrayList<String> elevations = new ArrayList<>();

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject point = jsonArray.getJSONObject(i);
                        azimuths.add(point.getString("azimuthAngle"));
                        elevations.add(point.getString("altitudeAngle"));
                    }

                    intent.putExtra("date_time", startTime);
                    intent.putStringArrayListExtra("azimuths", azimuths);
                    intent.putStringArrayListExtra("elevations", elevations);
                    openTracker();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
                new ErrorFragment().show(getSupportFragmentManager(), "tag");
                Log.i(TAG, "onErrorResponse: " + "failed");
            }
        });
        /*
        {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> getParams = new HashMap<>();
                getParams.put("satName", String.valueOf(satName.getText()));
                getParams.put("latitude", String.valueOf(latitude));
                getParams.put("longitude", String.valueOf(longitude));
                getParams.put("mobileTracker", "true");
                return getParams;
            }
        };
        */


        Log.i(TAG, "jsonParse: " + uri);
        requestQueue.add(objectRequest);
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }



    private void openTracker() {
        intent.putExtra("satellite_name", String.valueOf(satName.getText()));
        finish();
        startActivity(intent);
    }

    @Override
    public void onLocationChanged(Location location) {
        latitude = location.getLatitude();
        longitude = location.getLongitude();
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
    public void onCancel() {
        button.setEnabled(true);
    }

    @Override
    public void onBackPressed() {
        System.exit(0);
    }
}