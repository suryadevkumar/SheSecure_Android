package com.example.shesecure.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.shesecure.R;
import com.example.shesecure.utils.SecurePrefs;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Date;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LocationService extends Service {
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Location lastLocation;
    private Date lastLocationTime;
    private static final long LOCATION_UPDATE_INTERVAL = 10000; // 10 seconds
    private static final float LOCATION_UPDATE_DISTANCE = 10; // 10 meters
    private boolean shouldTrackLocation = false;

    @Override
    public void onCreate() {
        super.onCreate();

        // Check if we should track location
        SharedPreferences prefs = getSharedPreferences("SheSecurePrefs", MODE_PRIVATE);
        String token = prefs.getString("token", null);
        String userJson = prefs.getString("user", null);

        try {
            if (token != null && userJson != null) {
                JSONObject user = new JSONObject(userJson);
                String userType = user.getString("userType");
                shouldTrackLocation = "User".equals(userType);
            }
        } catch (JSONException e) {
            Log.e("LocationService", "Error parsing user JSON", e);
        }

        if (!shouldTrackLocation) {
            stopSelf();
            return;
        }

        // Create notification for foreground service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "location_channel",
                    "Location Tracking",
                    NotificationManager.IMPORTANCE_LOW
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);

            Notification notification = new NotificationCompat.Builder(this, "location_channel")
                    .setContentTitle("SheSecure Location Tracking")
                    .setContentText("Tracking your location for safety features")
                    .setSmallIcon(R.drawable.call)
                    .build();

            startForeground(1, notification);
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        setupLocationCallback();
        startLocationUpdates();
    }

    private void setupLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null || !shouldTrackLocation) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    handleNewLocation(location);
                }
            }
        };
    }

    private void startLocationUpdates() {
        if (!shouldTrackLocation) return;

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_UPDATE_INTERVAL)
                .setMinUpdateDistanceMeters(LOCATION_UPDATE_DISTANCE)
                .build();

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        } catch (SecurityException e) {
            Log.e("LocationService", "Lost location permission. Could not request updates.", e);
            stopSelf();
        }
    }

    private void handleNewLocation(Location location) {
        if (!shouldTrackLocation) return;

        Date now = new Date();

        if (lastLocation == null || location.distanceTo(lastLocation) > LOCATION_UPDATE_DISTANCE) {
            if (lastLocation != null) {
                sendLocationToBackend(
                        lastLocation.getLatitude(),
                        lastLocation.getLongitude(),
                        lastLocationTime,
                        now
                );
            }

            lastLocation = location;
            lastLocationTime = now;

            sendLocationToBackend(
                    location.getLatitude(),
                    location.getLongitude(),
                    now,
                    now
            );

            updateNearbyPlaces(location.getLatitude(), location.getLongitude());
        }
    }

    private void sendLocationToBackend(double latitude, double longitude, Date startTime, Date endTime) {
        SharedPreferences prefs = getSharedPreferences("SheSecurePrefs", MODE_PRIVATE);
        String token = prefs.getString("token", null);
        String userJson = prefs.getString("user", null);

        if (token == null || userJson == null) {
            return;
        }

        try {
            JSONObject user = new JSONObject(userJson);
            String userId = user.getString("_id");

            // Create JSON request body
            JSONObject requestBody = new JSONObject();
            requestBody.put("latitude", latitude);
            requestBody.put("longitude", longitude);
            requestBody.put("displayName", "Current Location");
            requestBody.put("formattedAddress", "Unknown Address");
            requestBody.put("startTime", startTime.getTime());
            requestBody.put("endTime", endTime.getTime());
            requestBody.put("userId", userId);

            // Make API call
            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json"),
                    requestBody.toString()
            );

            SecurePrefs securePrefs =  new SecurePrefs(getApplicationContext());
            securePrefs.saveKeys();

            String baseUrl = securePrefs.getAPI();

            ApiService apiService = RetrofitClient.getClient(baseUrl).create(ApiService.class);
            Call<ResponseBody> call = apiService.saveUserLocation(
                    "Bearer " + token,
                    body
            );

            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (!response.isSuccessful()) {
                        Log.e("LocationService", "Failed to save location: " + response.code());
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Log.e("LocationService", "Error saving location", t);
                }
            });

        } catch (Exception e) {
            Log.e("LocationService", "Error creating location JSON", e);
        }
    }

    private void updateNearbyPlaces(double latitude, double longitude) {
        // Implement nearby places search if needed
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }

        if (lastLocation != null && lastLocationTime != null) {
            sendLocationToBackend(
                    lastLocation.getLatitude(),
                    lastLocation.getLongitude(),
                    lastLocationTime,
                    new Date()
            );
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}