package com.example.satellitetracker;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.textclassifier.TextLinks;
import android.widget.Button;
import android.widget.EditText;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

public class MenuScreen extends AppCompatActivity {

    private Button button;
    private EditText satName;
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_screen);

        requestQueue = Volley.newRequestQueue(this);

        satName = (EditText) findViewById(R.id.nameInput);
        button = (Button) findViewById(R.id.menuButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                jsonParse();
                openTracker();
            }
        });
    }

    private void jsonParse() {
        String url = "test.url";
        JsonObjectRequest objectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONArray jsonArray = response.getJSONArray("rollAngles");
                } catch (Exception e) {
                    
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
    }

    private void openTracker() {
        Intent intent = new Intent(this, SatelliteTrackerApplication.class)
                .putExtra("satellite_name", String.valueOf(satName.getText()));
        RequestQueue mQueue = Volley.newRequestQueue(this);

        startActivity(intent);
    }
}