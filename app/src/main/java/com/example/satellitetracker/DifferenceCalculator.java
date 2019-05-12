package com.example.satellitetracker;

import android.util.Log;
import android.view.Display;
import android.widget.ImageView;

import java.util.Observable;

public class DifferenceCalculator {

    private static final String TAG = "DifferenceCalculator";

    private float targetAzimuth;
    private float targetElevation;
    private int deviceWidth = SatelliteTrackerApplication.deviceWidth;
    private int deviceHeight = SatelliteTrackerApplication.deviceHeight;
    private float horizontalFov = (float) SatelliteTrackerApplication.horizontalFoV;
    private float verticalFov = (float) SatelliteTrackerApplication.verticalFoV;
    private int[] centerCoordinates = {(SatelliteTrackerApplication.deviceWidth / 2),
            (SatelliteTrackerApplication.deviceHeight / 2)};


    public DifferenceCalculator(float azimuth, float elevation) {
        //this.centerCoordinates[0] = deviceWidth / 2;
        //this.centerCoordinates[1] = deviceHeight / 2;
        synchronized (this) {
            if (azimuth < 0) {
                this.targetAzimuth = 360 + azimuth;
            }
            this.targetAzimuth = azimuth;
            this.targetElevation = elevation;
        }
        SatelliteTrackerApplication.latestUI = false;

    }


    public int[] getDifferenceMatrix(float currentAzimuth, float currentElevation, float currentRotation) {
        int[] values = new int[2];
        values[0] = Math.round(getVerticalPlacement(currentElevation));
        values[1] = Math.round(getHorizontalPlacement(currentAzimuth));

        values = rotationTransformation(values, currentRotation);


        return values;
    }

    private float getHorizontalPlacement(float currentAzimuth) {

        float difference = ((currentAzimuth - targetAzimuth + 180) %360 ) - 180;
        float u = (difference/180);
        float a = (180f/horizontalFov)*deviceHeight;
        float shiftInPixels = u * a;
        if (currentAzimuth < 0) {
            //return centerCoordinates[1] + Math.round(shiftInPixels);
            return Math.round(shiftInPixels);
        } else {
            //return centerCoordinates[1] - Math.round(shiftInPixels);
            return - Math.round(shiftInPixels);
        }
    }

    private float getVerticalPlacement(float currentElevation) {
        float difference = targetElevation - currentElevation;
        float u = (difference/180);
        float a = (180f/verticalFov)*deviceWidth;
        float shiftInPixels = u * a;

        return Math.round(shiftInPixels);

    }

    private int[] rotationTransformation(int[] values, float rotation) {
        int[] transformed = new int[3];
        float rotationDifference;
        if (rotation < -90f) {
            rotationDifference = 450f + rotation;
        } else {
             rotationDifference = rotation +90f;
        }
        double difference = Math.toRadians(rotationDifference);
        double x = Math.cos(difference) * values[0] + Math.sin(difference) * values[1];
        double y = -Math.sin(difference) * values[0] + Math.cos(difference) * values[1];

        transformed[0] = centerCoordinates[0] + (int) x;
        transformed[1] = centerCoordinates[1] + (int) y;

        //arrow rotation calculations
        float xLength = (float) x;
        float yLength = (float) y;
        double angle = Math.toDegrees(Math.atan2(xLength,-yLength));
        transformed[2] = (int) Math.round(angle);


        return transformed;
    }

    public synchronized float getTargetAzimuth() {
        return targetAzimuth;
    }

    public synchronized float getTargetElevation() {
        return targetElevation;
    }
}
