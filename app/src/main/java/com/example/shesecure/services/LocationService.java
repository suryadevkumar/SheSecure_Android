package com.example.shesecure.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
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
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;

public class LocationService extends Service {

    // Location tracking components
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    // Location data holders
    private static Location staticCurrentLocation;
    private static final MutableLiveData<Location> liveLocation = new MutableLiveData<>();

    // Configuration
    private static final long UPDATE_INTERVAL = 10000; // 10 seconds
    private static final float UPDATE_DISTANCE = 10; // 10 meters
    private static final long BACKEND_INTERVAL = 30000; // 30 seconds
    private long lastBackendUpdate = 0;
    private OkHttpClient httpClient = new OkHttpClient();
    private String mapsApiKey;

    public static boolean isRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            mapsApiKey = new SecurePrefs(this).getGoogleMapsApiKey();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!isUser()) {
            stopSelf();
            return START_NOT_STICKY;
        }
        isRunning = true;
        startForegroundService();
        startLocationUpdates();
        return START_STICKY;
    }

    private boolean isUser() {
        SharedPreferences prefs = getSharedPreferences("SheSecurePrefs", MODE_PRIVATE);
        return "User".equals(prefs.getString("userType", null));
    }

    private void startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "location_channel",
                    "Location Tracking",
                    NotificationManager.IMPORTANCE_LOW
            );
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }

        startForeground(1, new NotificationCompat.Builder(this, "location_channel")
                .setContentTitle(getString(R.string.app_name) + " Location Tracking")
                .setContentText("Tracking your location for safety")
                .setSmallIcon(R.drawable.sos_icon)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build());
    }

    private void startLocationUpdates() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult result) {
                if (result == null) return;
                for (Location location : result.getLocations()) {
                    updateLocation(location);
                }
            }
        };

        try {
            fusedLocationClient.requestLocationUpdates(
                    new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL)
                            .setMinUpdateDistanceMeters(UPDATE_DISTANCE)
                            .build(),
                    locationCallback,
                    Looper.getMainLooper()
            );
        } catch (SecurityException e) {
            Log.e("LocationService", "Location permission missing", e);
            stopSelf();
        }
    }

    private void updateLocation(Location location) {
        staticCurrentLocation = location;
        liveLocation.postValue(location);

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastBackendUpdate >= BACKEND_INTERVAL) {
            new Thread(() -> {
                try {
                    PlaceDetails place = getPlaceDetails(location.getLatitude(), location.getLongitude());
                    sendToBackend(location, currentTime, place);
                } catch (Exception e) {
                    Log.e("LocationService", "Error processing location", e);
                    sendToBackend(location, currentTime, new PlaceDetails("Unknown", "Unknown"));
                }
            }).start();
            lastBackendUpdate = currentTime;
        }
    }

    private PlaceDetails getPlaceDetails(double lat, double lng) throws IOException, JSONException {
        Request request = new Request.Builder()
                .url("https://maps.googleapis.com/maps/api/geocode/json?latlng=" + lat + "," + lng + "&key=" + mapsApiKey)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            JSONObject json = new JSONObject(response.body().string());
            if (json.getString("status").equals("OK")) {
                JSONObject result = json.getJSONArray("results").getJSONObject(0);
                return new PlaceDetails(
                        result.getJSONArray("address_components").getJSONObject(0).optString("long_name", "Unknown"),
                        result.optString("formatted_address", "Unknown")
                );
            }
            return new PlaceDetails("Unknown", "Unknown");
        }
    }

    private void sendToBackend(Location location, long time, PlaceDetails place) {
        try {
            SharedPreferences prefs = getSharedPreferences("SheSecurePrefs", MODE_PRIVATE);

            String baseUrl = new SecurePrefs(this).getGoogleMapsApiKey();
            ApiService api = RetrofitClient.getClient(baseUrl).create(ApiService.class);

            api.saveUserLocation(
                    "Bearer " + prefs.getString("token", ""),
                    RequestBody.create(
                            MediaType.parse("application/json"),
                            new JSONObject()
                                    .put("latitude", location.getLatitude())
                                    .put("longitude", location.getLongitude())
                                    .put("displayName", place.displayName)
                                    .put("formattedAddress", place.formattedAddress)
                                    .put("startTime", time - BACKEND_INTERVAL)
                                    .put("endTime", time)
                                    .toString()
                    )
            ).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                    if (!response.isSuccessful()) {
                        Log.e("LocationService", "Failed to save location: " + response.code());
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Log.e("LocationService", "Backend error", t);
                }
            });
        } catch (Exception e) {
            Log.e("LocationService", "Error sending to backend", e);
        }
    }

    public static Location getCurrentLocation() {
        return staticCurrentLocation;
    }

    public static LiveData<Location> getLiveLocation() {
        return liveLocation;
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static boolean isServiceRunning() {
        return isRunning;
    }

    private static class PlaceDetails {
        final String displayName;
        final String formattedAddress;

        PlaceDetails(String displayName, String formattedAddress) {
            this.displayName = displayName;
            this.formattedAddress = formattedAddress;
        }
    }
}