package com.example.shesecure.socket;

import android.content.Context;
import android.util.Log;

import com.example.shesecure.utils.SecurePrefs;

import io.socket.client.IO;
import io.socket.client.Socket;
import org.json.JSONException;
import org.json.JSONObject;
import java.net.URISyntaxException;

public class SocketManager {
    private static final String TAG = "SocketManager";
    private static SocketManager instance;
    private Socket socket;
    private String shareId;
    private boolean isConnected = false;

    private SocketManager(Context context) {
        try {
            SecurePrefs securePrefs = SecurePrefs.getInstance(context);
            String socketUrl = securePrefs.getApiBaseUrl()+"/location";
            IO.Options options = IO.Options.builder()
                    .setForceNew(true)
                    .setReconnection(true)
                    .setReconnectionAttempts(Integer.MAX_VALUE)
                    .setReconnectionDelay(1000)
                    .build();

            socket = IO.socket(socketUrl, options);

            setupEventListeners();
        } catch (URISyntaxException e) {
            Log.e(TAG, "Socket initialization error", e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static synchronized SocketManager getInstance(Context context) {
        if (instance == null) {
            instance = new SocketManager(context);
        }
        return instance;
    }

    private void setupEventListeners() {
        socket.on(Socket.EVENT_CONNECT, args -> {
            Log.d(TAG, "Socket connected");
            isConnected = true;
        });

        socket.on(Socket.EVENT_DISCONNECT, args -> {
            Log.d(TAG, "Socket disconnected");
            isConnected = false;
        });

        socket.on(Socket.EVENT_CONNECT_ERROR, args -> {
            Log.e(TAG, "Socket connection error");
            if (args.length > 0 && args[0] instanceof Exception) {
                Log.e(TAG, "Error details", (Exception) args[0]);
            }
        });

        socket.on("location:session_ended", args -> {
            Log.d(TAG, "Location session ended by server");
            shareId = null;
        });
    }

    public void connect() {
        if (!isConnected && !socket.connected()) {
            socket.connect();
        }
    }

    public void disconnect() {
        if (isConnected || socket.connected()) {
            socket.disconnect();
        }
    }

    public boolean isConnected() {
        return socket == null || !socket.connected();
    }

    public void startSharing(String shareId, double latitude, double longitude) {
        this.shareId = shareId;

        try {
            // Ensure we're connected
            if (!isConnected) {
                connect();
            }

            // Join the room
            JSONObject joinData = new JSONObject();
            joinData.put("shareId", shareId);
            socket.emit("location:join", joinData);

            // Send initial location
            JSONObject locationData = new JSONObject();
            locationData.put("shareId", shareId);
            locationData.put("latitude", latitude);
            locationData.put("longitude", longitude);
            locationData.put("timestamp", System.currentTimeMillis());
            socket.emit("location:update", locationData);

        } catch (Exception e) {
            Log.e(TAG, "Error starting sharing", e);
        }
    }

    public void updateLocation(double latitude, double longitude) {
        if (shareId == null || !isConnected) return;

        try {
            JSONObject data = new JSONObject();
            data.put("shareId", shareId);
            data.put("latitude", latitude);
            data.put("longitude", longitude);
            data.put("timestamp", System.currentTimeMillis());

            socket.emit("location:update", data);
            Log.d(TAG, "Location update sent: " + latitude + ", " + longitude);
        } catch (JSONException e) {
            Log.e(TAG, "Error creating location update JSON", e);
        }
    }

    public void stopSharing() {
        if (shareId != null && isConnected) {
            try {
                if (isConnected) {
                    socket.emit("location:end_session", shareId);
                }

                socket.off(shareId);

                if (!isSharing()) {
                    socket.disconnect();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error stopping sharing", e);
            } finally {
                shareId = null;
            }
        }
    }

    public boolean isSharing() {
        return shareId != null;
    }
}