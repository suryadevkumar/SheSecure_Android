package com.example.shesecure.socket;

import android.content.Context;
import org.json.JSONObject;
import io.socket.client.Socket;

public class SocketManager {
    private static SocketManager instance;
    private CentralizedSocketManager centralManager;

    private SocketManager(Context context) {
        centralManager = CentralizedSocketManager.getInstance();
    }

    public static synchronized SocketManager getInstance(Context context) {
        if (instance == null) {
            instance = new SocketManager(context);
        }
        return instance;
    }

    public void startSharing(String shareId, double latitude, double longitude) {
        Socket socket = centralManager.getLocationSocket();
        if (socket != null && socket.connected()) {
            try {
                JSONObject data = new JSONObject();
                data.put("shareId", shareId);
                data.put("latitude", latitude);
                data.put("longitude", longitude);
                data.put("timestamp", System.currentTimeMillis());
                socket.emit("location:update", data);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void stopSharing() {
        Socket socket = centralManager.getLocationSocket();
        if (socket != null) {
            socket.emit("location:end_session");
        }
    }

    public boolean isConnected() {
        Socket socket = centralManager.getLocationSocket();
        return socket != null && socket.connected();
    }
}