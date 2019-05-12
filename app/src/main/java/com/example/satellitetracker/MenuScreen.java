package com.example.satellitetracker;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.textclassifier.TextLinks;
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
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MenuScreen extends AppCompatActivity {

    private Button button;
    private EditText satName;
    private RequestQueue requestQueue;
    Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_screen);

        intent = new Intent(this, SatelliteTrackerApplication.class);

        requestQueue = Volley.newRequestQueue(this);

        satName = findViewById(R.id.nameInput);
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
        JsonObjectRequest objectRequest = new JsonObjectRequest(Request.Method.GET, url,
                null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONArray jsonArray = response.getJSONArray("rollAngles");
                    ArrayList<String> azimuths = new ArrayList<>();
                    ArrayList<String> elevations = new ArrayList<>();

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject point = jsonArray.getJSONObject(i);
                        azimuths.add(point.getString("azimuthAngle"));
                        elevations.add(point.getString("altitudeAngle"));
                    }

                    intent.putExtra("date_time", jsonArray.getJSONObject(0).getString("currentDateTime"));
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
            }
        });
        requestQueue.add(objectRequest);
    }

    private void openTracker() {
        intent.putExtra("satellite_name", String.valueOf(satName.getText()));
        startActivity(intent);
    }
}