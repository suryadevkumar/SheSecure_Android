package com.example.shesecure.utils;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Build;

import androidx.lifecycle.LiveData;
import com.example.shesecure.services.LocationService;

public class LocationHelper {

    public static void startIfUser(Context context) {
        if (isUser(context)) {
            Intent intent = new Intent(context, LocationService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }
        }
    }

    private static boolean isUser(Context context) {
        return "User".equals(context.getSharedPreferences("SheSecurePrefs", Context.MODE_PRIVATE)
                .getString("userType", null));
    }

    public static LiveData<Location> getLiveLocation() {
        return LocationService.getLiveLocation();
    }

    public static Location getCurrentLocation() {
        return LocationService.getCurrentLocation();
    }
}