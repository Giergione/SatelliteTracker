package com.example.satellitetracker;

import android.view.Display;
import android.widget.ImageView;

public class DifferenceCalculator {

    private float targetAzimuth;
    private float targetElevation;
    private int deviceWidth;
    private int deviceHeight;
    private int[] centerCoordinates = new int[2];


    public DifferenceCalculator(float azimuth, float elevation, int deviceHeight, int deviceWidth) {
        this.deviceHeight = deviceHeight;
        this.deviceWidth = deviceWidth;
        this.centerCoordinates[0] = deviceWidth / 2;
        this.centerCoordinates[1] = deviceHeight / 2;
        if (azimuth < 0) {
            this.targetAzimuth = 360 + azimuth;
        }
        this.targetAzimuth = azimuth;
        this.targetElevation = elevation;
    }

    public int[] getDifferenceMatrix(float currentAzimuth, float currentElevation, float currentRotation) {
        int[] values = new int[2];
        values[0] = Math.round(getVerticalPlacement(currentElevation));
        values[1] = Math.round(getHorizontalPlacement(currentAzimuth));

        values = rotationTransformation(values, currentRotation);


        return values;
    }

    private float getHorizontalPlacement(float currentAzimuth) {

        float difference = Math.abs(currentAzimuth - targetAzimuth) % 180;
        float u = (difference/180);
        float a = (180f/60f)*deviceHeight;
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
        float difference = Math.abs(currentElevation - targetElevation) % 90;
        float u = (difference/180);
        float a = (180f/35f)*deviceWidth;
        float shiftInPixels = u * a;

        if (currentElevation < 0) {
            //return centerCoordinates[0] + Math.round(shiftInPixels);
            return Math.round(shiftInPixels);
        } else {
            //return centerCoordinates[0] - Math.round(shiftInPixels);
            return - Math.round(shiftInPixels);
        }
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
}
