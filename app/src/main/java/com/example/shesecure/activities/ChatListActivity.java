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
import com.example.shesecure.socket.CentralizedSocketManager;
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
    private CentralizedSocketManager socketManager;

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        // Get user data
        userId = authManager.getUserId();
        userType = authManager.getUserType();
        token = authManager.getToken();

        // Initialize socket manager
        socketManager = CentralizedSocketManager.getInstance();

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
        chatRoomAdapter = new ChatRoomAdapter(this, chatRooms, this::onChatRoomClicked);
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
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.problem_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        problemTypeSpinner.setAdapter(adapter);
    }

    private void setupTabs() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    chatRoomsRecyclerView.setVisibility(View.VISIBLE);
                    chatRequestsRecyclerView.setVisibility(View.GONE);
                } else {
                    chatRoomsRecyclerView.setVisibility(View.GONE);
                    chatRequestsRecyclerView.setVisibility(View.VISIBLE);
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void toggleRequestForm() {
        newRequestForm.setVisibility(newRequestForm.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        newChatRequestButton.setText(newRequestForm.getVisibility() == View.VISIBLE ? "Cancel" : "New Chat Request");
    }

    private void createChatRequest() {
        String problem = problemTypeSpinner.getSelectedItem().toString();
        String brief = briefEditText.getText().toString();
        Socket chatSocket = socketManager.getChatSocket();

        if (chatSocket != null && chatSocket.connected()) {
            try {
                JSONObject data = new JSONObject();
                data.put("userId", userId);
                data.put("problemType", problem);
                data.put("brief", brief);

                chatSocket.emit("create_chat_request", data);
                submitRequestButton.setEnabled(false);

            } catch (Exception e) {
                e.printStackTrace();
                submitRequestButton.setEnabled(true);
            }
        } else {
            Toast.makeText(this, "Socket not connected", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadUnreadCounts() {
        apiService.getUnreadCounts(userId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    try {
                        JSONObject counts = new JSONObject(response.body().string());
                        int total = 0;
                        for (ChatRoom room : chatRooms) {
                            int c = counts.optInt(room.getId(), 0);
                            room.setUnreadCount(c);
                            total += c;
                        }
                        chatRoomAdapter.notifyDataSetChanged();
                        updateUnreadBadge(total);
                    } catch (Exception e) { e.printStackTrace(); }
                }
            }
            @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
        });
    }

    private void loadChatRooms() {
        apiService.getChatRooms("Bearer " + token, userId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String json = response.body().string();
                        JSONArray array = new JSONArray(json);
                        chatRooms.clear();
                        for (int i = 0; i < array.length(); i++) {
                            chatRooms.add(new Gson().fromJson(array.getJSONObject(i).toString(), ChatRoom.class));
                        }
                        chatRoomAdapter.notifyDataSetChanged();
                        loadUnreadCounts();
                    } catch (Exception e) { e.printStackTrace(); }
                }
            }
            @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
        });
    }

    private void loadChatRequests() {
        apiService.getChatRequests(userId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String json = response.body().string();
                        JSONArray array = new JSONArray(json);
                        chatRequests.clear();
                        for (int i = 0; i < array.length(); i++) {
                            chatRequests.add(new Gson().fromJson(array.getJSONObject(i).toString(), ChatRequest.class));
                        }
                        chatRequestAdapter.notifyDataSetChanged();
                        updatePendingCount();
                    } catch (Exception e) { e.printStackTrace(); }
                }
            }
            @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
        });
    }

    private void updateUnreadBadge(int count) {
        unreadMessagesBadge.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
        unreadMessagesBadge.setText(String.valueOf(count));
    }

    private void updatePendingBadge(int count) {
        pendingRequestsBadge.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
        pendingRequestsBadge.setText(String.valueOf(count));
    }

    private void onChatRoomClicked(ChatRoom chatRoom) {
        chatRoom.setUnreadCount(0);
        chatRoomAdapter.notifyDataSetChanged();
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("chatRoomId", chatRoom.getId());
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
        Socket chatSocket = socketManager.getChatSocket();
        if (chatSocket != null && chatSocket.connected()) {
            try {
                JSONObject data = new JSONObject();
                data.put("counsellorId", userId);
                data.put("requestId", chatRequest.getId());
                chatSocket.emit("accept_chat_request", data);
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private void updateOnlineStatus(List<String> onlineUserIds) {
        for (ChatRoom room : chatRooms) {
            String partnerId = "User".equals(userType) ? room.getCounsellor().getId() : room.getUser().getId();
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
        Socket chatSocket = socketManager.getChatSocket();

        // Ensure initialized
        if (chatSocket == null) {
            socketManager.init(this);
            chatSocket = socketManager.getChatSocket();
        }

        // Clean Listeners
        chatSocket.off("online_users");
        chatSocket.off("user_status_change");
        chatSocket.off("new_chat_request");
        chatSocket.off("chat_request_created");
        chatSocket.off("chat_request_accepted");
        chatSocket.off("new_message");

        // Listen for online users
        chatSocket.on("online_users", args -> runOnUiThread(() -> {
            try {
                JSONArray array = (JSONArray) args[0];
                List<String> onlineIds = new ArrayList<>();
                for (int i = 0; i < array.length(); i++) onlineIds.add(array.getString(i));
                updateOnlineStatus(onlineIds);
            } catch (Exception e) { e.printStackTrace(); }
        }));

        // New Chat Request (For Counsellor)
        chatSocket.on("new_chat_request", args -> runOnUiThread(() -> {
            if ("Counsellor".equals(userType)) {
                JSONObject obj = (JSONObject) args[0];
                ChatRequest req = new Gson().fromJson(obj.toString(), ChatRequest.class);
                chatRequests.add(0, req);
                chatRequestAdapter.notifyItemInserted(0);
                updatePendingCount();
            }
        }));

        chatSocket.on("chat_request_created", args -> runOnUiThread(() -> {

            submitRequestButton.setEnabled(true);
            briefEditText.setText("");
            toggleRequestForm();

            try {
                JSONObject obj = (JSONObject) args[0];

                ChatRequest req = new Gson()
                        .fromJson(obj.toString(), ChatRequest.class);

                chatRequests.add(0, req);
                chatRequestAdapter.notifyItemInserted(0);
                updatePendingCount();

            } catch (Exception e) {
                e.printStackTrace();
            }

            Toast.makeText(this, "Request Sent!", Toast.LENGTH_SHORT).show();
        }));

        // Request Accepted
        chatSocket.on("chat_request_accepted", args -> runOnUiThread(() -> {
            loadChatRequests();
            loadChatRooms();
            updatePendingCount();
            tabLayout.selectTab(tabLayout.getTabAt(0));
        }));

        // New Message (Update Badge)
        chatSocket.on("new_message", args -> runOnUiThread(() -> {
            loadUnreadCounts();
        }));

        if (!chatSocket.connected()) chatSocket.connect();
    }

    private void updatePendingCount() {
        int count = 0;
        for (ChatRequest r : chatRequests) if ("Pending".equals(r.getStatus())) count++;
        updatePendingBadge(count);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadChatRooms();
        loadChatRequests();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Socket chatSocket = socketManager.getChatSocket();
        if (chatSocket != null) {
            chatSocket.off("online_users");
            chatSocket.off("new_chat_request");
            chatSocket.off("chat_request_accepted");
            chatSocket.off("new_message");
        }
    }
}