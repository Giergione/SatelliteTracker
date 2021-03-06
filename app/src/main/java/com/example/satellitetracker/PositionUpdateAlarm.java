package com.example.satellitetracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import static com.example.satellitetracker.SatelliteTrackerApplication.azimuths;
import static com.example.satellitetracker.SatelliteTrackerApplication.elevations;

public class PositionUpdateAlarm extends BroadcastReceiver {

    private static final String TAG = "PositionUpdateAlarm";
    private static final int waitInMillis = 30000;


    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "onReceive: my alarm event");
        SatelliteTrackerApplication.riseReached = true;
        updaterRunnable updaterRunnable = new updaterRunnable();
        new Thread(updaterRunnable).start();
    }

    class updaterRunnable implements Runnable {



        @Override
        public void run() {
            for (int i = 0; i < azimuths.size(); i++) {
                final String currentAzimuth = azimuths.get(i);
                final String currentElevation = elevations.get(i);
                SatelliteTrackerApplication.differenceCalculator =
                        new DifferenceCalculator(Float.valueOf(currentAzimuth),
                                Float.valueOf(currentElevation));
                Log.i(TAG, "azimuth: " + azimuths.get(i) + " - elevation: " + elevations.get(i));


                try {
                    Thread.sleep(waitInMillis);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            SatelliteTrackerApplication.lastPoint = true;
            Log.i(TAG, "reached the last point");
        }
    }
}
