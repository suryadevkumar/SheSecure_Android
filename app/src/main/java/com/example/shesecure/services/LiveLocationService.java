package com.example.shesecure.services;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import com.example.shesecure.socket.CentralizedSocketManager;
import com.example.shesecure.utils.AuthManager;
import io.socket.client.Socket;
import org.json.JSONObject;
import java.util.UUID;

public class LiveLocationService extends Service {
    private static final String TAG = "LiveLocationService";
    private CentralizedSocketManager socketManager;
    private String shareId;
    private boolean isSharing = false;

    @Override
    public void onCreate() {
        super.onCreate();
        // Centralized instance lein
        socketManager = CentralizedSocketManager.getInstance();

        // Agar kisi wajah se socket init nahi hua (app kill hone ke baad service restart hui)
        if (socketManager.getLocationSocket() == null) {
            socketManager.init(getApplicationContext());
        }

        // Check for existing session
        shareId = AuthManager.getLiveLocationShareId(this);
        if (shareId != null) {
            isSharing = true;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case "START_SHARING":
                    if (!isSharing) {
                        startSharing();
                    }
                    break;
                case "STOP_SHARING":
                    stopSharing();
                    stopSelf();
                    break;
                case "UPDATE_LOCATION":
                    if (isSharing && intent.hasExtra("latitude") && intent.hasExtra("longitude")) {
                        double lat = intent.getDoubleExtra("latitude", 0);
                        double lng = intent.getDoubleExtra("longitude", 0);
                        updateLocation(lat, lng);
                    }
                    break;
            }
        }
        return START_STICKY;
    }

    private void startSharing() {
        shareId = UUID.randomUUID().toString();
        AuthManager.saveLiveLocationShareId(this, shareId);
        isSharing = true;

        Socket locationSocket = socketManager.getLocationSocket();

        // Location emit logic using Namespace socket
        if (locationSocket != null) {
            if (!locationSocket.connected()) {
                locationSocket.connect();
            }

            // Get initial location
            Location currentLocation = LocationService.getCurrentLocation();
            if (currentLocation != null) {
                emitLocationData(currentLocation.getLatitude(), currentLocation.getLongitude());
            }
        }

        Log.d(TAG, "Live location sharing started: " + shareId);
    }

    private void updateLocation(double latitude, double longitude) {
        if (!isSharing || shareId == null) return;
        emitLocationData(latitude, longitude);
    }

    private void emitLocationData(double lat, double lng) {
        Socket locationSocket = socketManager.getLocationSocket();

        if (locationSocket != null && locationSocket.connected()) {
            try {
                JSONObject data = new JSONObject();
                data.put("shareId", shareId);
                data.put("latitude", lat);
                data.put("longitude", lng);
                data.put("timestamp", System.currentTimeMillis());

                // Namespace emit
                locationSocket.emit("location:update", data);
                Log.d(TAG, "Location sent: " + lat + ", " + lng);
            } catch (Exception e) {
                Log.e(TAG, "JSON Error: " + e.getMessage());
            }
        } else {
            Log.w(TAG, "Location socket not connected, retrying connection...");
            if (locationSocket != null) locationSocket.connect();
        }
    }

    private void stopSharing() {
        if (!isSharing) return;

        Socket locationSocket = socketManager.getLocationSocket();
        if (locationSocket != null && locationSocket.connected()) {
            locationSocket.emit("location:stop", shareId);
        }

        AuthManager.clearLiveLocationShareId(this);
        isSharing = false;
        shareId = null;

        Intent broadcastIntent = new Intent("LOCATION_SHARING_STOPPED");
        sendBroadcast(broadcastIntent);

        Log.d(TAG, "Live location sharing stopped");
    }

    @Override
    public void onDestroy() {
        // Service kill hone par socket disconnect nahi karenge
        // kyunki chat ya SOS chal raha ho sakta hai.
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}