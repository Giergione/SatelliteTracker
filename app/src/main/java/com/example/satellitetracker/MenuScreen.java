package com.example.satellitetracker;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class MenuScreen extends AppCompatActivity {

    private Button button;
    private EditText satName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_screen);

        satName = (EditText) findViewById(R.id.nameInput);
        button = (Button) findViewById(R.id.menuButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openTracker();
            }
        });
    }

    private void openTracker() {
        Intent intent = new Intent(this, SatelliteTrackerApplication.class)
                .putExtra("satellite_name", String.valueOf(satName.getText()));
        startActivity(intent);
    }
}