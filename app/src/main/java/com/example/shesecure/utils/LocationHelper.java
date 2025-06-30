package com.example.shesecure.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.example.shesecure.services.LocationService;
import org.json.JSONException;
import org.json.JSONObject;

public class LocationHelper {
    public static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    public static boolean checkLocationPermissions(Context context) {
        boolean fineLocation = ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarseLocation = ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            boolean backgroundLocation = ContextCompat.checkSelfPermission(context,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
            return fineLocation && coarseLocation && backgroundLocation;
        }
        return fineLocation && coarseLocation;
    }

    public static void requestLocationPermissions(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                    activity,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    },
                    LOCATION_PERMISSION_REQUEST_CODE
            );
        } else {
            ActivityCompat.requestPermissions(
                    activity,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_PERMISSION_REQUEST_CODE
            );
        }
    }

    public static boolean shouldTrackLocation(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("SheSecurePrefs", Context.MODE_PRIVATE);

        String token = prefs.getString("token", null);
        String userJson = prefs.getString("user", null);

        if (token == null || userJson == null) {
            return false;
        }

        try {
            JSONObject user = new JSONObject(userJson);
            String userType = user.getString("userType");
            return "User".equals(userType);
        } catch (JSONException e) {
            return false;
        }
    }

    public static void startLocationService(Context context) {
        if (!checkLocationPermissions(context) || !shouldTrackLocation(context)) {
            return;
        }

        Intent serviceIntent = new Intent(context, LocationService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }

    public static void stopLocationService(Context context) {
        Intent serviceIntent = new Intent(context, LocationService.class);
        context.stopService(serviceIntent);
    }
}