package com.example.satellitetracker;

import android.preference.PreferenceActivity;
import android.util.Log;

import org.json.*;
import com.loopj.android.http.*;

import java.util.ArrayList;

import cz.msebera.android.httpclient.entity.mime.Header;

public class DataRequest {

    private static final String BASE_URL = "https://api.twitter.com/1/";
    private static AsyncHttpClient client = new AsyncHttpClient();


    public static void get(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        client.get(getAbsoluteUrl(url), params, responseHandler);
    }

    public static void post(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        client.post(getAbsoluteUrl(url), params, responseHandler);
    }

    private static String getAbsoluteUrl(String relativeUrl) {
        return BASE_URL + relativeUrl;
    }

    public ArrayList<String> requestTLAConversion(String satName, double longitude, double latitude)
            throws JSONException {

        RequestParams params = new RequestParams();
        params.put("satelliteName", satName);
        params.put("logitude", longitude);
        params.put("latitude", latitude);
        params.put("mobileTracker", true);

        ArrayList<String> response = new ArrayList<>();
        return response;
    }


}
