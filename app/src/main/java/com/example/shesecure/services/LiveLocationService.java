package com.example.shesecure.services;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import com.example.shesecure.socket.SocketManager;
import com.example.shesecure.utils.AuthManager;

import java.util.UUID;

public class LiveLocationService extends Service {
    private static final String TAG = "LiveLocationService";
    private SocketManager socketManager;
    private String shareId;
    private boolean isSharing = false;

    @Override
    public void onCreate() {
        super.onCreate();

        // Then initialize other components
        socketManager = SocketManager.getInstance(this);
        socketManager.connect();

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
                    if (!isSharing) {  // Add this check
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

        // Connect socket if not already connected
        if (socketManager.isConnected()) {
            socketManager.connect();
        }

        // Get current location and start sharing
        Location currentLocation = LocationService.getCurrentLocation();
        if (currentLocation != null) {
            socketManager.startSharing(shareId,
                    currentLocation.getLatitude(),
                    currentLocation.getLongitude());
        } else {
            Log.w(TAG, "Current location is null when starting sharing");
        }

        Log.d(TAG, "Live location sharing started with ID: " + shareId);
    }

    private void updateLocation(double latitude, double longitude) {
        if (!isSharing || shareId == null) return;

        // Ensure socket is connected
        if (socketManager.isConnected()) {
            socketManager.connect();
        }

        socketManager.updateLocation(latitude, longitude);
    }

    private void stopSharing() {
        if (!isSharing) return;

        socketManager.stopSharing();
        AuthManager.clearLiveLocationShareId(this);
        isSharing = false;
        shareId = null;

        Intent broadcastIntent = new Intent("LOCATION_SHARING_STOPPED");
        sendBroadcast(broadcastIntent);

        Log.d(TAG, "Live location sharing stopped");
    }

    @Override
    public void onDestroy() {
        socketManager.disconnect();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}