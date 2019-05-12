package com.example.satellitetracker;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.textclassifier.TextLinks;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_screen);

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
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
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
        String url = "https://api.myjson.com/bins/92hzy";
        //String url = "https://api.myjson.com/bins/92h234234234234242234";
        JSONObject getParams = new JSONObject();
        button.setEnabled(false);

        try {
            getParams.put("satName", String.valueOf(satName.getText()));
            getParams.put("latitude", latitude);
            getParams.put("longitude", longitude);
            getParams.put("mobileTracker", "true");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest objectRequest = new JsonObjectRequest(Request.Method.GET, url,
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
                //showRequestAlert();
                new ErrorFragment().show(getSupportFragmentManager(), "tag");
                Log.i(TAG, "onErrorResponse: " + "failed");
            }
        });
        requestQueue.add(objectRequest);
    }

    private void openTracker() {
        intent.putExtra("satellite_name", String.valueOf(satName.getText()));
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
}