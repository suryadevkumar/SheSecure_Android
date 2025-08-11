package com.example.shesecure.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shesecure.R;
import com.example.shesecure.adapters.ChatRequestAdapter;
import com.example.shesecure.adapters.ChatRoomAdapter;
import com.example.shesecure.models.ChatRequest;
import com.example.shesecure.models.ChatRoom;
import com.example.shesecure.models.Message;
import com.example.shesecure.models.User;
import com.example.shesecure.services.ApiService;
import com.example.shesecure.utils.ApiUtils;
import com.example.shesecure.utils.SecurePrefs;
import com.google.android.material.tabs.TabLayout;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatListActivity extends BaseActivity {
    private RecyclerView chatRoomsRecyclerView, chatRequestsRecyclerView;
    private ChatRoomAdapter chatRoomAdapter;
    private ChatRequestAdapter chatRequestAdapter;
    private List<ChatRoom> chatRooms = new ArrayList<>();
    private List<ChatRequest> chatRequests = new ArrayList<>();
    private TabLayout tabLayout;
    private TextView pendingRequestsBadge, unreadMessagesBadge;
    private Button newChatRequestButton;
    private LinearLayout newRequestForm;
    private EditText briefEditText;
    private Spinner problemTypeSpinner;
    private Button submitRequestButton;
    private String userId, userType, token;
    private ApiService apiService;
    private Socket socket;

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        // Get user data
        userId = authManager.getUserId();
        userType = authManager.getUserType();
        token = authManager.getToken();

        // Initialize views
        tabLayout = findViewById(R.id.tabLayout);
        pendingRequestsBadge = findViewById(R.id.pendingRequestsBadge);
        unreadMessagesBadge = findViewById(R.id.unreadMessagesBadge);
        newChatRequestButton = findViewById(R.id.newChatRequestButton);
        newRequestForm = findViewById(R.id.newRequestForm);
        problemTypeSpinner = findViewById(R.id.problemTypeEditText);
        briefEditText = findViewById(R.id.briefEditText);
        submitRequestButton = findViewById(R.id.submitRequestButton);

        // Initialize RecyclerViews
        chatRoomsRecyclerView = findViewById(R.id.chatRoomsRecyclerView);
        chatRoomsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRoomAdapter = new ChatRoomAdapter(chatRooms, this::onChatRoomClicked);
        chatRoomsRecyclerView.setAdapter(chatRoomAdapter);

        chatRequestsRecyclerView = findViewById(R.id.chatRequestsRecyclerView);
        chatRequestsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRequestAdapter = new ChatRequestAdapter(chatRequests, this::onChatRequestAccepted, userType);
        chatRequestsRecyclerView.setAdapter(chatRequestAdapter);

        // Initialize API service
        apiService = ApiUtils.initializeApiService(this, ApiService.class);

        // Setup tabs
        setupTabs();
        setupProblemTypeSpinner();

        // Setup new chat request button (only for users)
        if ("User".equals(userType)) {
            newChatRequestButton.setVisibility(View.VISIBLE);
            newChatRequestButton.setOnClickListener(v -> toggleRequestForm());
            submitRequestButton.setOnClickListener(v -> createChatRequest());
        } else {
            newChatRequestButton.setVisibility(View.GONE);
        }

        // Load data
        loadChatRooms();
        loadChatRequests();
        loadUnreadCounts();

        // Initialize socket
        initializeSocket();
    }

    private void setupProblemTypeSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.problem_types,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        problemTypeSpinner.setAdapter(adapter);
    }

    private void setupTabs() {
        tabLayout.selectTab(tabLayout.getTabAt(0));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    chatRoomsRecyclerView.setVisibility(View.VISIBLE);
                    chatRequestsRecyclerView.setVisibility(View.GONE);
                    loadChatRooms();
                    loadUnreadCounts();
                } else {
                    chatRoomsRecyclerView.setVisibility(View.GONE);
                    chatRequestsRecyclerView.setVisibility(View.VISIBLE);
                    loadChatRequests();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void toggleRequestForm() {
        if (newRequestForm.getVisibility() == View.VISIBLE) {
            newRequestForm.setVisibility(View.GONE);
            newChatRequestButton.setText("New Chat Request");
        } else {
            newRequestForm.setVisibility(View.VISIBLE);
            newChatRequestButton.setText("Cancel");
        }
    }

    private void createChatRequest() {
        String problemType = problemTypeSpinner.getSelectedItem().toString().trim();
        String brief = briefEditText.getText().toString().trim();

        if (problemType.isEmpty() || brief.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        submitRequestButton.setEnabled(false);
        submitRequestButton.setText("Submitting...");

        try {
            JSONObject requestData = new JSONObject();
            requestData.put("userId", userId);
            requestData.put("problemType", problemType);
            requestData.put("brief", brief);

            if (socket == null || !socket.connected()) {
                Toast.makeText(this, "Connecting to server...", Toast.LENGTH_SHORT).show();
                submitRequestButton.setEnabled(true);
                submitRequestButton.setText("Submit Request");
                initializeSocket();
                return;
            }

            socket.emit("create_chat_request", requestData, (Ack) args -> {
                runOnUiThread(() -> {
                    // Always reset button state first
                    submitRequestButton.setEnabled(true);
                    submitRequestButton.setText("Submit Request");

                    try {
                        if (args.length > 0) {
                            if (args[0] instanceof JSONObject) {
                                JSONObject response = (JSONObject) args[0];
                                if (response.has("error")) {
                                    Toast.makeText(ChatListActivity.this,
                                            "Error: " + response.getString("error"),
                                            Toast.LENGTH_LONG).show();
                                }
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(ChatListActivity.this,
                                "Error parsing response", Toast.LENGTH_SHORT).show();
                    }
                });
            });

        } catch (JSONException e) {
            e.printStackTrace();
            submitRequestButton.setEnabled(true);
            submitRequestButton.setText("Submit Request");
            Toast.makeText(this, "Error creating request", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadUnreadCounts() {
        Call<ResponseBody> call = apiService.getUnreadCounts(userId);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    try {
                        String json = response.body().string();
                        JSONObject countsObj = new JSONObject(json);

                        int totalUnread = 0;

                        // Update unread counts for each room
                        for (ChatRoom room : chatRooms) {
                            if (countsObj.has(room.getId())) {
                                int count = countsObj.getInt(room.getId());
                                room.setUnreadCount(count);
                                totalUnread += count;
                            } else {
                                room.setUnreadCount(0);
                            }
                        }

                        chatRoomAdapter.notifyDataSetChanged();

                        updateUnreadBadge(totalUnread);

                    } catch (Exception e) {
                        Log.e("UnreadCounts", "Error parsing response", e);
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("UnreadCounts", "Network error", t);
            }
        });
    }

    private void loadChatRooms() {
        if (token == null) {
            Toast.makeText(this, "Authentication required", Toast.LENGTH_SHORT).show();
            return;
        }

        Call<ResponseBody> call = apiService.getChatRooms("Bearer " + token, userId);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

                if (response.isSuccessful()) {
                    try {
                        String json = response.body().string();
                        JSONArray roomsArray = new JSONArray(json);
                        chatRooms.clear();

                        for (int i = 0; i < roomsArray.length(); i++) {
                            JSONObject roomObj = roomsArray.getJSONObject(i);

                            ChatRoom room = new Gson().fromJson(roomObj.toString(), ChatRoom.class);
                            chatRooms.add(room);
                        }

                        chatRoomAdapter.notifyDataSetChanged();

                    } catch (Exception e) {
                        Log.e("ChatList", "Error parsing response", e);
                        Toast.makeText(ChatListActivity.this, "Error parsing chat rooms", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "No error body";
                        Log.e("ChatList", "Error response: " + errorBody);
                        Toast.makeText(ChatListActivity.this, "Failed to load chat rooms: " + response.code(), Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("ChatList", "Network error", t);
                Toast.makeText(ChatListActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadChatRequests() {
        if (token == null) {
            Toast.makeText(this, "Authentication required", Toast.LENGTH_SHORT).show();
            return;
        }

        Call<ResponseBody> call = apiService.getChatRequests(userId);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    try {
                        String json = response.body().string();
                        Log.d("ChatRequests", "Response JSON: " + json);

                        JSONArray requestsArray = new JSONArray(json);
                        chatRequests.clear();

                        int pendingCount = 0;
                        for (int i = 0; i < requestsArray.length(); i++) {
                            JSONObject requestObj = requestsArray.getJSONObject(i);

                            ChatRequest request = new Gson().fromJson(requestObj.toString(), ChatRequest.class);

                            // Add request to list
                            chatRequests.add(request);

                            // Count pending requests
                            if ("Pending".equals(request.getStatus())) {
                                pendingCount++;
                            }
                        }

                        chatRequestAdapter.notifyDataSetChanged();
                        updatePendingBadge(pendingCount);

                    } catch (Exception e) {
                        Log.e("ChatRequests", "Error parsing response", e);
                        Toast.makeText(ChatListActivity.this, "Error loading requests", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "No error body";
                        Log.e("ChatRequests", "Error response: " + errorBody);
                        Toast.makeText(ChatListActivity.this, "Failed to load requests", Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("ChatRequests", "Network error", t);
                Toast.makeText(ChatListActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUnreadBadge(int count) {
        if (count > 0) {
            unreadMessagesBadge.setVisibility(View.VISIBLE);
            unreadMessagesBadge.setText(String.valueOf(count));
        } else {
            unreadMessagesBadge.setVisibility(View.GONE);
        }
    }

    private void updatePendingBadge(int count) {
        if (count > 0) {
            pendingRequestsBadge.setVisibility(View.VISIBLE);
            pendingRequestsBadge.setText(String.valueOf(count));
        } else {
            pendingRequestsBadge.setVisibility(View.GONE);
        }
    }

    private void onChatRoomClicked(ChatRoom chatRoom) throws JSONException {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("chatRoomId", chatRoom.getId());
        resetUnreadCount(chatRoom.getId());
        startActivity(intent);
    }

    private void resetUnreadCount(String chatRoomId) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("chatRoomId", chatRoomId);
        jsonObject.put("userId", userId);

        RequestBody requestBody = RequestBody.create(
                jsonObject.toString(),
                MediaType.parse("application/json")
        );

        Call<ResponseBody> call = apiService.markMessagesRead(requestBody);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    loadUnreadCounts();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("ResetUnread", "Failed to reset unread count", t);
            }
        });
    }

    private void onChatRequestAccepted(ChatRequest chatRequest) {
        if (socket != null && socket.connected()) {
            try {
                JSONObject acceptData = new JSONObject();
                acceptData.put("counsellorId", userId);
                acceptData.put("requestId", chatRequest.getId());

                socket.emit("accept_chat_request", acceptData, (Ack) args -> {
                    runOnUiThread(() -> {
                        if (args.length > 0) {
                            if (args[0] instanceof JSONObject) {
                                JSONObject response = (JSONObject) args[0];
                                if (response.has("error")) {
                                    try {
                                        Toast.makeText(ChatListActivity.this,
                                                "Error: " + response.getString("error"),
                                                Toast.LENGTH_LONG).show();
                                    } catch (JSONException e) {
                                        throw new RuntimeException(e);
                                    }
                                } else {
                                    // Success - no need to do anything here as socket event will handle it
                                    Toast.makeText(ChatListActivity.this,
                                            "Request accepted successfully",
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    });
                });
            } catch (JSONException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error accepting request", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Not connected to server", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateOnlineStatus(List<String> onlineUserIds) {
        for (ChatRoom room : chatRooms) {
            String partnerId = "User".equals(userType) ?
                    room.getCounsellor().getId() : room.getUser().getId();

            room.setOnline(onlineUserIds.contains(partnerId));
        }
        chatRoomAdapter.notifyDataSetChanged();
    }

    private void updateUserOnlineStatus(String userId, boolean isOnline) {
        for (ChatRoom room : chatRooms) {
            String partnerId = "User".equals(userType) ?
                    room.getCounsellor().getId() : room.getUser().getId();

            if (partnerId.equals(userId)) {
                room.setOnline(isOnline);
            }
        }
        chatRoomAdapter.notifyDataSetChanged();
    }

    private void initializeSocket() {
        try {
            SecurePrefs securePrefs = SecurePrefs.getInstance(this);
            String socketUrl = securePrefs.getApiBaseUrl()+"/chat";
            socket = IO.socket(socketUrl);

            socket.on(Socket.EVENT_CONNECT, args -> runOnUiThread(() -> {
                Log.d("Socket", "Connected");
                socket.emit("user_connected", userId);
            }));

            socket.on(Socket.EVENT_DISCONNECT, args -> runOnUiThread(() -> {
                Log.d("Socket", "Disconnected");
            }));

            socket.on("online_users", args -> runOnUiThread(() -> {
                if (args.length > 0) {
                    try {
                        JSONArray onlineUsersArray = (JSONArray) args[0];
                        List<String> onlineUserIds = new ArrayList<>();

                        for (int i = 0; i < onlineUsersArray.length(); i++) {
                            onlineUserIds.add(onlineUsersArray.getString(i));
                        }

                        // Update online status for all chat rooms
                        updateOnlineStatus(onlineUserIds);

                    } catch (JSONException e) {
                        Log.e("Socket", "Error processing online_users", e);
                    }
                }
            }));

            socket.on("user_status_change", args -> runOnUiThread(() -> {
                if (args.length > 0) {
                    try {
                        JSONObject data = (JSONObject) args[0];
                        String userId = data.getString("userId");
                        boolean isOnline = "online".equals(data.getString("status"));

                        // Update specific user's online status
                        updateUserOnlineStatus(userId, isOnline);

                    } catch (JSONException e) {
                        Log.e("Socket", "Error processing user_status_change", e);
                    }
                }
            }));

            socket.on("new_chat_request", args -> runOnUiThread(() -> {
                if ("Counsellor".equals(userType)) {
                    try {
                        if (args.length > 0 && args[0] instanceof JSONObject) {
                            JSONObject requestObj = (JSONObject) args[0];

                            ChatRequest request = new Gson().fromJson(requestObj.toString(), ChatRequest.class);

                            // Add only if not already in list
                            boolean exists = false;
                            for (ChatRequest req : chatRequests) {
                                if (req.getId().equals(request.getId())) {
                                    exists = true;
                                    break;
                                }
                            }

                            if (!exists) {
                                chatRequests.add(0, request);
                                chatRequestAdapter.notifyItemInserted(0);
                                updatePendingCount();
                            }
                        }
                    } catch (Exception e) {
                        Log.e("Socket", "Error handling new_chat_request", e);
                    }
                }
            }));

            socket.on("chat_request_created", args -> runOnUiThread(() -> {
                if ("User".equals(userType)) {
                    try {
                        if (args.length > 0 && args[0] instanceof JSONObject) {
                            JSONObject requestObj = (JSONObject) args[0];

                            ChatRequest request = new Gson().fromJson(requestObj.toString(), ChatRequest.class);

                            if (request.getUser().getId().equals(userId)) {
                                // Reset form and button state
                                problemTypeSpinner.setSelection(0);
                                briefEditText.setText("");
                                newRequestForm.setVisibility(View.GONE);
                                newChatRequestButton.setText("New Chat Request");

                                // Make sure button is enabled
                                submitRequestButton.setEnabled(true);
                                submitRequestButton.setText("Submit Request");

                                // Update requests list
                                chatRequests.clear();
                                chatRequests.add(request);
                                chatRequestAdapter.notifyDataSetChanged();
                                tabLayout.getTabAt(1).select();
                                updatePendingCount();
                            }
                        }
                    } catch (Exception e) {
                        Log.e("Socket", "Error handling chat_request_created", e);
                        // Ensure button is reset even on error
                        submitRequestButton.setEnabled(true);
                        submitRequestButton.setText("Submit Request");
                    }
                }
            }));

            // Socket event handler for chat_request_accepted
            socket.on("chat_request_accepted", args -> runOnUiThread(() -> {
                if (args.length > 0) {
                    try {
                        JSONObject data = (JSONObject) args[0];
                        JSONObject chatRequestObj = data.getJSONObject("chatRequest");
                        String requestId = chatRequestObj.getString("_id");

                        // Remove the accepted request
                        for (int i = 0; i < chatRequests.size(); i++) {
                            if (chatRequests.get(i).getId().equals(requestId)) {
                                chatRequests.remove(i);
                                chatRequestAdapter.notifyItemRemoved(i);
                                break;
                            }
                        }
                        updatePendingCount();
                        loadChatRooms();
                        loadUnreadCounts();
                        tabLayout.getTabAt(0).select();

                    } catch (Exception e) {
                        Log.e("Socket", "Error processing chat_request_accepted", e);
                    }
                }
            }));

            tabLayout.getTabAt(0).select();

            socket.on("chat_room_created", args -> runOnUiThread(() -> {
                if (args.length > 0) {
                    JSONObject roomObj = (JSONObject) args[0];

                    ChatRoom room = new Gson().fromJson(roomObj.toString(), ChatRoom.class);
                    chatRooms.add(0, room);
                    chatRoomAdapter.notifyItemInserted(0);
                }
            }));

            socket.on("new_message", args -> runOnUiThread(() -> {
                if (args.length > 0) {
                    try {
                        JSONObject messageObj = (JSONObject) args[0];
                        String chatRoomId = messageObj.getString("chatRoom");

                        // Update unread count for the specific room
                        for (ChatRoom room : chatRooms) {
                            if (room.getId().equals(chatRoomId)) {
                                int currentUnread = room.getUnreadCount();
                                room.setUnreadCount(currentUnread + 1);
                                break;
                            }
                        }

                        // Update total unread count
                        int totalUnread = 0;
                        for (ChatRoom room : chatRooms) {
                            totalUnread += room.getUnreadCount();
                        }
                        updateUnreadBadge(totalUnread);
                        loadUnreadCounts();

                        // Refresh the adapter
                        chatRoomAdapter.notifyDataSetChanged();

                    } catch (JSONException e) {
                        Log.e("Socket", "Error processing new_message", e);
                    }
                }
            }));

            socket.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void updatePendingCount() {
        int pendingCount = 0;
        for (ChatRequest req : chatRequests) {
            if ("Pending".equals(req.getStatus())) {
                pendingCount++;
            }
        }
        updatePendingBadge(pendingCount);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadChatRooms();
        loadChatRequests();
        loadUnreadCounts();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (socket != null) {
            socket.disconnect();
        }
    }
}