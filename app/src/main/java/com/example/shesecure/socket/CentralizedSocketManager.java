package com.example.shesecure.socket;

import android.content.Context;
import android.util.Log;
import com.example.shesecure.utils.SecurePrefs;
import io.socket.client.IO;
import io.socket.client.Socket;
import java.net.URISyntaxException;

public class CentralizedSocketManager {
    private static final String TAG = "SocketManager";
    private static CentralizedSocketManager instance;

    private Socket chatSocket;
    private Socket locationSocket;
    private Socket sosSocket;

    private CentralizedSocketManager() {}

    public static synchronized CentralizedSocketManager getInstance() {
        if (instance == null) {
            instance = new CentralizedSocketManager();
        }
        return instance;
    }

    // Login success hote hi ise call karein
    public void init(Context context) {
        try {
            SecurePrefs securePrefs = SecurePrefs.getInstance(context);
            String baseUrl = securePrefs.getApiBaseUrl();

            IO.Options options = IO.Options.builder()
                    .setForceNew(false)
                    .setReconnection(true)
                    .setTransports(new String[]{"websocket"})
                    .setReconnectionDelay(2000)
                    .build();

            // Ek hi base connection par 3 alag namespaces
            chatSocket = IO.socket(baseUrl + "/chat", options);
            locationSocket = IO.socket(baseUrl + "/location", options);
            sosSocket = IO.socket(baseUrl + "/sos", options);

            chatSocket.on(Socket.EVENT_CONNECT, args -> Log.d(TAG, "CONNECTED HAI" + baseUrl));
            chatSocket.on(Socket.EVENT_CONNECT_ERROR, args -> Log.e(TAG, "ERROR HAII" + baseUrl + args[0]));

            connectAll();
            Log.d(TAG, "All Sockets Initialized (Chat, Location, SOS)");

        } catch (Exception e) {
            Log.e(TAG, "Initialization Error: " + e.getMessage());
        }
    }

    private void connectAll() {
        if (chatSocket != null) chatSocket.connect();
        if (locationSocket != null) locationSocket.connect();
        if (sosSocket != null) sosSocket.connect();
    }

    public Socket getChatSocket() { return chatSocket; }
    public Socket getLocationSocket() { return locationSocket; }
    public Socket getSosSocket() { return sosSocket; }

    public boolean isChatConnected() {
        return chatSocket != null && chatSocket.connected();
    }

    public void disconnectAll() {
        if (chatSocket != null) chatSocket.disconnect();
        if (locationSocket != null) locationSocket.disconnect();
        if (sosSocket != null) sosSocket.disconnect();
    }
}