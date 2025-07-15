package com.example.shesecure.services;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.shesecure.R;
import com.example.shesecure.SheSecureApp;
import com.example.shesecure.utils.ApiUtils;
import com.example.shesecure.utils.SecurePrefs;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import org.json.JSONObject;

import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import retrofit2.Call;

public class LocationService extends Service {

    private static final String TAG = "LocationService";
    private static final String CHANNEL_ID = "location_channel";
    private static final int NOTIFICATION_ID = 1;

    // Location update intervals
    private static final long UPDATE_INTERVAL = 10000; // 10 seconds
    private static final long FASTEST_UPDATE_INTERVAL = 5000; // 5 seconds
    private static final float UPDATE_DISTANCE = 10; // 10 meters

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private OkHttpClient httpClient;
    private ApiService apiService;

    // Static variables for global access
    private static Location staticCurrentLocation;
    private static final MutableLiveData<Location> liveLocation = new MutableLiveData<>();
    private static volatile boolean isRunning = false;

    // Instance variables
    private Location lastSavedLocation;
    private long lastUpdateTime = 0;
    private boolean isFirstLocationUpdate = true;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "LocationService onCreate");

        // Initialize HTTP client
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        // Initialize API service
        apiService = ApiUtils.initializeApiService(this, ApiService.class);

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        setupLocationCallback();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "LocationService onStartCommand");

        // Check if user is authorized and has permissions
        if (!isUserAuthorized() || !hasLocationPermission()) {
            Log.w(TAG, "User not authorized or missing permissions");
            stopSelf();
            return START_NOT_STICKY;
        }

        // Start foreground service
        startForegroundService();

        // Start location updates
        startLocationUpdates();

        isRunning = true;
        return START_STICKY; // Service will be restarted if killed
    }

    private boolean isUserAuthorized() {
        SharedPreferences prefs = getSharedPreferences("SheSecurePrefs", MODE_PRIVATE);
        String userType = prefs.getString("userType", null);
        String token = prefs.getString("token", null);

        return "User".equals(userType) && token != null && !token.isEmpty();
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void startForegroundService() {
        createNotificationChannel();

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("SheSecure Location Tracking")
                .setContentText("Tracking your location for safety")
                .setSmallIcon(R.drawable.sos_icon)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setShowWhen(false)
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Location Tracking",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Continuous location tracking for safety");
            channel.setShowBadge(false);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void setupLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult result) {
                if (result == null || result.getLocations().isEmpty()) {
                    Log.w(TAG, "Location result is null or empty");
                    return;
                }

                Location location = result.getLastLocation();
                if (location != null) {
                    handleLocationUpdate(location);
                }
            }
        };
    }

    private void startLocationUpdates() {
        if (!hasLocationPermission()) {
            Log.e(TAG, "Location permission not granted");
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL)
                .setMinUpdateIntervalMillis(FASTEST_UPDATE_INTERVAL)
                .setMinUpdateDistanceMeters(UPDATE_DISTANCE)
                .setWaitForAccurateLocation(true)
                .build();

        try {
            fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
            );
            Log.d(TAG, "Location updates started");
        } catch (SecurityException e) {
            Log.e(TAG, "Location permission missing", e);
            stopSelf();
        }
    }

    private void handleLocationUpdate(Location location) {
        if (location == null) return;

        // Update static location for global access
        staticCurrentLocation = location;

        // Post to LiveData for observers
        liveLocation.postValue(location);

        Log.d(TAG, "Location updated: " + location.getLatitude() + ", " + location.getLongitude());

        // Check if we should save this location
        if (shouldSaveLocation(location)) {
            saveLocationToBackend(location);
        }
    }

    private boolean shouldSaveLocation(Location newLocation) {
        if (isFirstLocationUpdate) {
            return true;
        }

        if (lastSavedLocation == null) {
            return true;
        }

        // Check if user has moved significant distance
        float distance = lastSavedLocation.distanceTo(newLocation);
        if (distance >= UPDATE_DISTANCE) {
            return true;
        }

        // Check if enough time has passed (backup condition)
        long currentTime = System.currentTimeMillis();
        long timeDiff = currentTime - lastUpdateTime;
        if (timeDiff >= UPDATE_INTERVAL * 3) { // 60 seconds
            return true;
        }

        return false;
    }

    private void saveLocationToBackend(Location location) {
        new Thread(() -> {
            try {
                long currentTime = System.currentTimeMillis();
                long startTime = isFirstLocationUpdate ? currentTime : lastUpdateTime;

                // Get place details
                PlaceDetails placeDetails = getPlaceDetails(location.getLatitude(), location.getLongitude());

                // Get auth token
                SharedPreferences prefs = getSharedPreferences("SheSecurePrefs", MODE_PRIVATE);
                String token = prefs.getString("token", "");

                if (token.isEmpty()) {
                    Log.e(TAG, "No auth token found");
                    return;
                }

                // Prepare location data
                JSONObject locationData = new JSONObject();
                locationData.put("latitude", location.getLatitude());
                locationData.put("longitude", location.getLongitude());
                locationData.put("displayName", placeDetails.displayName);
                locationData.put("formattedAddress", placeDetails.formattedAddress);
                locationData.put("startTime", startTime);
                locationData.put("endTime", currentTime);

                RequestBody body = RequestBody.create(
                        MediaType.parse("application/json"),
                        locationData.toString()
                );

                // Make API call
                apiService.saveUserLocation("Bearer " + token, body).enqueue(
                        new retrofit2.Callback<okhttp3.ResponseBody>() {
                            @Override
                            public void onResponse(Call<okhttp3.ResponseBody> call,
                                                   retrofit2.Response<okhttp3.ResponseBody> response) {
                                if (response.isSuccessful()) {
                                    Log.d(TAG, "Location saved successfully");
                                    lastSavedLocation = location;
                                    lastUpdateTime = currentTime;
                                    isFirstLocationUpdate = false;
                                } else {
                                    Log.e(TAG, "Failed to save location: " + response.code());
                                }
                            }

                            @Override
                            public void onFailure(Call<okhttp3.ResponseBody> call, Throwable t) {
                                Log.e(TAG, "Error saving location", t);
                            }
                        });

            } catch (Exception e) {
                Log.e(TAG, "Error processing location", e);
            }
        }).start();
    }

    private PlaceDetails getPlaceDetails(double lat, double lng) {
        try {
            SecurePrefs securePrefs = ((SheSecureApp) getApplication()).getSecurePrefs();
            String mapsApiKey = securePrefs.getGoogleMapsApiKey();

            if (mapsApiKey.isEmpty()) {
                Log.w(TAG, "Maps API key not found");
                return new PlaceDetails("Unknown", "Unknown Location");
            }

            String url = "https://maps.googleapis.com/maps/api/geocode/json?latlng=" +
                    lat + "," + lng + "&key=" + mapsApiKey;

            Request request = new Request.Builder()
                    .url(url)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    JSONObject json = new JSONObject(response.body().string());

                    if ("OK".equals(json.getString("status"))) {
                        JSONObject result = json.getJSONArray("results").getJSONObject(0);

                        String displayName = "Unknown";
                        if (result.has("address_components") &&
                                result.getJSONArray("address_components").length() > 0) {
                            displayName = result.getJSONArray("address_components")
                                    .getJSONObject(0).getString("long_name");
                        }

                        String formattedAddress = result.optString("formatted_address", "Unknown Location");

                        return new PlaceDetails(displayName, formattedAddress);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting place details", e);
        }

        return new PlaceDetails("Unknown", "Unknown Location");
    }

    // Static methods for global access
    public static Location getCurrentLocation() {
        return staticCurrentLocation;
    }

    public static LiveData<Location> getLiveLocation() {
        return liveLocation;
    }

    public static boolean isServiceRunning() {
        return isRunning;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "LocationService onDestroy");

        isRunning = false;

        // Stop location updates
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

    // Helper class for place details
    private static class PlaceDetails {
        final String displayName;
        final String formattedAddress;

        PlaceDetails(String displayName, String formattedAddress) {
            this.displayName = displayName;
            this.formattedAddress = formattedAddress;
        }
    }
}