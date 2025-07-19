package com.example.shesecure.activities;

import android.animation.ValueAnimator;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shesecure.R;
import com.example.shesecure.adapters.MessageAdapter;
import com.example.shesecure.models.ChatRoom;
import com.example.shesecure.models.Message;
import com.example.shesecure.models.User;
import com.example.shesecure.services.ApiService;
import com.example.shesecure.utils.ApiUtils;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.socket.client.IO;
import io.socket.client.Socket;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends AppCompatActivity {
    private RecyclerView messagesRecyclerView;
    private MessageAdapter messageAdapter;
    private List<Message> messages = new ArrayList<>();
    private EditText messageEditText;
    private Button sendButton, endChatAcceptButton, endChatDeclineButton;
    private TextView chatTitleTextView, chatSubtitleTextView, userInitialsView, chatStatusTextView;
    private String chatRoomId, userId, userType;
    private SharedPreferences prefs;
    private ApiService apiService;
    private Socket socket;
    private ChatRoom currentRoom;
    private boolean isTyping = false;
    private Handler typingHandler = new Handler();
    private LinearLayout typingIndicatorContainer, endChatConfirmationLayout, messageInputLayout, chatEndedLayout;
    private ImageView typingIndicatorDot1, typingIndicatorDot2, typingIndicatorDot3;;
    private ValueAnimator typingAnimator;
    private View onlineIndicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Get chat room ID from intent
        chatRoomId = getIntent().getStringExtra("chatRoomId");
        if (chatRoomId == null) {
            finish();
            return;
        }

        messageInputLayout = findViewById(R.id.messageInputLayout);
        chatStatusTextView = findViewById(R.id.chatStatusTextView);
        chatEndedLayout = findViewById(R.id.chatEndedLayout);

        // Initialize typing indicator views
        typingIndicatorContainer = findViewById(R.id.typingIndicatorContainer);
        typingIndicatorDot1 = findViewById(R.id.typingIndicatorDot1);
        typingIndicatorDot2 = findViewById(R.id.typingIndicatorDot2);
        typingIndicatorDot3 = findViewById(R.id.typingIndicatorDot3);

        // Initialize views
        messagesRecyclerView = findViewById(R.id.messagesRecyclerView);
        messageEditText = findViewById(R.id.messageEditText);
        sendButton = findViewById(R.id.sendButton);
        chatTitleTextView = findViewById(R.id.chatTitleTextView);
        chatSubtitleTextView = findViewById(R.id.chatSubtitleTextView);
        userInitialsView = findViewById(R.id.userInitialsView);
        onlineIndicator = findViewById(R.id.onlineIndicator);

        // Setup RecyclerView
        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        messageAdapter = new MessageAdapter(messages, this::isCurrentUser);
        messagesRecyclerView.setAdapter(messageAdapter);

        // Initialize end chat confirmation views
        endChatConfirmationLayout = findViewById(R.id.endChatConfirmationLayout);
        endChatAcceptButton = findViewById(R.id.endChatAcceptButton);
        endChatDeclineButton = findViewById(R.id.endChatDeclineButton);

        endChatAcceptButton.setOnClickListener(v -> confirmEndChat(true));
        endChatDeclineButton.setOnClickListener(v -> confirmEndChat(false));

        // Get user data
        prefs = getSharedPreferences("SheSecurePrefs", MODE_PRIVATE);
        userId = prefs.getString("userId", null);
        userType = prefs.getString("userType", null);

        // Initialize API service
        apiService = ApiUtils.initializeApiService(this, ApiService.class);

        // Load chat room details and messages
        loadChatRoom();
        loadMessages();
        setupTypingAnimation();

        // Setup send button
        sendButton.setOnClickListener(v -> sendMessage());

        // Setup typing listener
        messageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!isTyping && s.length() > 0) {
                    isTyping = true;
                    emitTyping(true);
                } else if (isTyping && s.length() == 0) {
                    isTyping = false;
                    emitTyping(false);
                }

                // Reset typing timeout
                typingHandler.removeCallbacks(typingTimeout);
                typingHandler.postDelayed(typingTimeout, 2000);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Initialize socket
        initializeSocket();
    }

    private void loadChatRoom() {
        String token = prefs.getString("token", null);
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

                        // Find the current room from the array
                        for (int i = 0; i < roomsArray.length(); i++) {
                            JSONObject roomObj = roomsArray.getJSONObject(i);
                            ChatRoom room = new Gson().fromJson(roomObj.toString(), ChatRoom.class);
                            if (room.getId().equals(chatRoomId)) {
                                currentRoom = room;
                                updateChatHeader();
                                updateChatStatus();
                                if (room.isOnline()) {
                                    updateOnlineStatus(room.isOnline());
                                }
                                break;
                            }
                        }

                        if (currentRoom == null) {
                            Toast.makeText(ChatActivity.this, "Chat room not found", Toast.LENGTH_SHORT).show();
                        }
                        updateChatHeader();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(ChatActivity.this, "Failed to load chat details", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(ChatActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadMessages() {
        String token = prefs.getString("token", null);
        if (token == null) {
            Toast.makeText(this, "Authentication required", Toast.LENGTH_SHORT).show();
            return;
        }

        Call<ResponseBody> call = apiService.getMessages("Bearer " + token, chatRoomId);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    try {
                        String json = response.body().string();
                        JSONArray messagesArray = new JSONArray(json);
                        messages.clear();

                        for (int i = 0; i < messagesArray.length(); i++) {
                            JSONObject messageObj = messagesArray.getJSONObject(i);
                            Message message = new Gson().fromJson(messageObj.toString(), Message.class);
                            messages.add(message);
                        }

                        messageAdapter.notifyDataSetChanged();
                        scrollToBottom();

                        // Mark messages as read
                        markMessagesAsRead();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(ChatActivity.this, "Failed to load messages", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(ChatActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showEndChatConfirmation(boolean show) {
        runOnUiThread(() -> {
            if (show) {
                endChatConfirmationLayout.setVisibility(View.VISIBLE);

                // Add a system message to notify user
                Message systemMessage = new Message();
                systemMessage.setSystem(true);
                systemMessage.setContent("Counselor has requested to end this chat. Please accept or decline.");
                systemMessage.setCreatedAt(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(new Date()));

                messages.add(systemMessage);
                messageAdapter.notifyItemInserted(messages.size() - 1);
                scrollToBottom();
            } else {
                endChatConfirmationLayout.setVisibility(View.GONE);
            }
        });
    }

    private void confirmEndChat(boolean accept) {
        if (socket != null && socket.connected()) {
            try {
                JSONObject data = new JSONObject();
                data.put("chatRoomId", chatRoomId);
                data.put("userId", userId);
                data.put("accepted", accept);

                socket.emit("end_chat_response", data);
                showEndChatConfirmation(false);

                if (accept) {
                    currentRoom.setEnded(true);
                    Toast.makeText(this, "Chat ended", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Chat continue", Toast.LENGTH_SHORT).show();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void updateChatStatus() {
        runOnUiThread(() -> {
            if (currentRoom != null) {
                if (currentRoom.isEnded()) {
                    chatStatusTextView.setText("Ended");
                    chatStatusTextView.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.red)));
                    messageInputLayout.setVisibility(View.GONE);
                    chatEndedLayout.setVisibility(View.VISIBLE);

                } else {
                    chatStatusTextView.setText("Active");
                    chatStatusTextView.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.green)));
                    messageInputLayout.setVisibility(View.VISIBLE);
                    chatEndedLayout.setVisibility(View.GONE);
                }
            }
        });
    }

    private void updateOnlineStatus(boolean isOnline) {
        runOnUiThread(() -> {
            if (isOnline) {
                onlineIndicator.setVisibility(View.VISIBLE);
            } else {
                onlineIndicator.setVisibility(View.GONE);
            }
        });
    }

    private void checkPartnerOnlineStatus() {
        if (socket != null && socket.connected() && currentRoom != null) {
            String partnerId = "User".equals(userType) ?
                    currentRoom.getCounsellor().getId() :
                    currentRoom.getUser().getId();

            try {
                JSONObject data = new JSONObject();
                data.put("userId", partnerId);
                socket.emit("check_user_status", data);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void setupTypingAnimation() {
        typingAnimator = ValueAnimator.ofFloat(0f, 1f);
        typingAnimator.setDuration(1000);
        typingAnimator.setRepeatCount(ValueAnimator.INFINITE);
        typingAnimator.setInterpolator(new LinearInterpolator());

        typingAnimator.addUpdateListener(animation -> {
            float progress = (float) animation.getAnimatedValue();

            // Animate each dot with a slight delay
            typingIndicatorDot1.setAlpha(progress < 0.33f ? progress * 3 : (1 - (progress - 0.33f) * 1.5f));
            typingIndicatorDot2.setAlpha(progress < 0.66f ?
                    (progress > 0.33f ? (progress - 0.33f) * 3 : 0f) :
                    (1 - (progress - 0.66f) * 1.5f));
            typingIndicatorDot3.setAlpha(progress > 0.66f ? (progress - 0.66f) * 3 : 0f);
        });
    }

    private void updateChatHeader() {
        if (currentRoom == null) return;

        if ("User".equals(userType)) {
            chatTitleTextView.setText(currentRoom.getCounsellor().getFullName());
            userInitialsView.setText(currentRoom.getCounsellor().getInitials().toUpperCase());
        } else {
            chatTitleTextView.setText(currentRoom.getUser().getFullName());
            userInitialsView.setText(currentRoom.getUser().getInitials().toUpperCase());
        }

        chatSubtitleTextView.setText(currentRoom.getChatRequest().getProblemType() + ": " +
                currentRoom.getChatRequest().getBrief());
    }

    private void sendMessage() {
        if (currentRoom != null && currentRoom.isEnded()) {
            Toast.makeText(this, "Chat has ended. Cannot send messages.", Toast.LENGTH_SHORT).show();
            return;
        }

        String messageText = messageEditText.getText().toString().trim();
        if (messageText.isEmpty() || currentRoom == null || currentRoom.isEnded()) return;

        // Create a local message object first
        Message localMessage = new Message();
        localMessage.setContent(messageText);
        localMessage.setChatRoomId(chatRoomId);
        localMessage.setCreatedAt(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(new Date()));

        // Set sender info (you might need to create a User object with current user details)
        User sender = new User();
        sender.setId(userId);
        localMessage.setSender(sender);

        // Add to local list and update UI
        messages.add(localMessage);
        messageAdapter.notifyItemInserted(messages.size() - 1);
        scrollToBottom();

        // Then send via socket
        if (socket != null && socket.connected()) {
            try {
                JSONObject messageData = new JSONObject();
                messageData.put("chatRoomId", chatRoomId);
                messageData.put("senderId", userId);
                messageData.put("content", messageText);

                socket.emit("send_message", messageData);
                messageEditText.setText("");
            } catch (JSONException e) {
                e.printStackTrace();
                // Remove the local message if sending fails
                messages.remove(localMessage);
                messageAdapter.notifyDataSetChanged();
            }
        }
    }

    private void requestEndChat() {
        if (socket != null && socket.connected() && "Counsellor".equals(userType)) {
            try {
                JSONObject data = new JSONObject();
                data.put("chatRoomId", chatRoomId);
                data.put("counsellorId", userId);

                socket.emit("request_end_chat", data);
                Toast.makeText(this, "End chat request sent", Toast.LENGTH_SHORT).show();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void markMessagesAsRead() {
        if (socket != null && socket.connected()) {
            try {
                JSONObject readData = new JSONObject();
                readData.put("chatRoomId", chatRoomId);
                readData.put("userId", userId);

                socket.emit("mark_messages_read", readData);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void emitTyping(boolean typing) {
        if (socket != null && socket.connected()) {
            try {
                JSONObject typingData = new JSONObject();
                typingData.put("chatRoomId", chatRoomId);
                typingData.put("userId", userId);

                if (typing) {
                    socket.emit("user_typing", typingData);
                } else {
                    socket.emit("user_stopped_typing", typingData);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private Runnable typingTimeout = () -> {
        if (isTyping) {
            isTyping = false;
            emitTyping(false);
        }
    };

    private void scrollToBottom() {
        if (messages.size() > 0) {
            messagesRecyclerView.scrollToPosition(messages.size() - 1);
        }
    }

    private boolean isCurrentUser(Message message) {
        return message.getSender() != null && message.getSender().getId().equals(userId);
    }

    private void initializeSocket() {
        try {
            socket = IO.socket("http://10.0.2.2:3000/chat");

            socket.on(Socket.EVENT_CONNECT, args -> runOnUiThread(() -> {
                Log.d("Socket", "Connected");
                socket.emit("user_connected", userId);
            }));

            socket.on(Socket.EVENT_DISCONNECT, args -> runOnUiThread(() -> {
                Log.d("Socket", "Disconnected");
            }));

            socket.on(Socket.EVENT_CONNECT_ERROR, args -> runOnUiThread(() -> {
                Log.e("Socket", "Connection error");
                Toast.makeText(ChatActivity.this, "Connection error", Toast.LENGTH_SHORT).show();
            }));

            socket.on("user_status", args -> runOnUiThread(() -> {
                if (args.length > 0) {
                    try {
                        JSONObject statusObj = (JSONObject) args[0];
                        String statusUserId = statusObj.getString("userId");
                        String status = statusObj.getString("status");

                        String partnerId = "User".equals(userType) ?
                                currentRoom.getCounsellor().getId() :
                                currentRoom.getUser().getId();

                        if (statusUserId.equals(partnerId)) {
                            updateOnlineStatus("online".equals(status));
                        }
                    } catch (JSONException e) {
                        Log.e("Socket", "Error processing user_status", e);
                    }
                }
            }));

            socket.on("online_users", args -> runOnUiThread(() -> {
                if (args.length > 0 && currentRoom != null) {
                    try {
                        JSONArray onlineUsers = (JSONArray) args[0];
                        String partnerId = "User".equals(userType) ?
                                currentRoom.getCounsellor().getId() :
                                currentRoom.getUser().getId();

                        boolean isOnline = false;
                        for (int i = 0; i < onlineUsers.length(); i++) {
                            if (onlineUsers.getString(i).equals(partnerId)) {
                                isOnline = true;
                                break;
                            }
                        }
                        updateOnlineStatus(isOnline);
                    } catch (JSONException e) {
                        Log.e("Socket", "Error processing online_users", e);
                    }
                }
            }));

            socket.on("new_message", args -> runOnUiThread(() -> {
                try {
                    if (args.length > 0) {
                        JSONObject messageObj = (JSONObject) args[0];
                        Log.d("Socket", "New message received: " + messageObj.toString());

                        Message message = new Gson().fromJson(messageObj.toString(), Message.class);

                        // Enhanced validation
                        if (message == null || message.getId() == null) {
                            Log.e("Socket", "Invalid message received");
                            return;
                        }

                        if (message.getChatRoomId() != null && message.getChatRoomId().equals(chatRoomId)) {
                            // Check if message exists by ID and content
                            boolean exists = false;
                            for (int i = 0; i < messages.size(); i++) {
                                Message existing = messages.get(i);
                                if (existing.getId() != null && existing.getId().equals(message.getId())) {
                                    // Update existing message if needed
                                    if (!existing.getContent().equals(message.getContent())) {
                                        messages.set(i, message);
                                        messageAdapter.notifyItemChanged(i);
                                    }
                                    exists = true;
                                    break;
                                }
                            }

                            if (!exists) {
                                messages.add(message);
                                messageAdapter.notifyItemInserted(messages.size() - 1);
                                scrollToBottom();

                                // Mark as read if not sent by current user
                                if (!isCurrentUser(message)) {
                                    markMessagesAsRead();
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e("Socket", "Error processing new message", e);
                }
            }));

            socket.on("user_typing", args -> runOnUiThread(() -> {
                if (args.length > 0) {
                    try {
                        JSONObject typingObj = (JSONObject) args[0];
                        String typingRoomId = typingObj.getString("chatRoomId");
                        String typingUserId = typingObj.getString("userId");

                        if (typingRoomId.equals(chatRoomId) && !typingUserId.equals(userId)) {
                            typingIndicatorContainer.setVisibility(View.VISIBLE);
                            if (!typingAnimator.isRunning()) {
                                typingAnimator.start();
                            }
                        }
                    } catch (JSONException e) {
                        Log.e("Socket", "Error handling typing event", e);
                    }
                }
            }));

            socket.on("user_stopped_typing", args -> runOnUiThread(() -> {
                if (args.length > 0) {
                    try {
                        JSONObject typingObj = (JSONObject) args[0];
                        String typingRoomId = typingObj.getString("chatRoomId");

                        if (typingRoomId.equals(chatRoomId)) {
                            typingIndicatorContainer.setVisibility(View.GONE);
                            if (typingAnimator.isRunning()) {
                                typingAnimator.cancel();
                            }
                        }
                    } catch (JSONException e) {
                        Log.e("Socket", "Error handling stop typing event", e);
                    }
                }
            }));

            socket.on("end_chat_request", args -> runOnUiThread(() -> {
                if (args.length > 0) {
                    try {
                        JSONObject requestObj = (JSONObject) args[0];
                        String requestRoomId = requestObj.getString("chatRoomId");

                        if (requestRoomId.equals(chatRoomId)) {
                            // Only show for users (counselors initiate the request)
                            if ("User".equals(userType)) {
                                showEndChatConfirmation(true);
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }));

            socket.on("clear_end_request_lock", args -> runOnUiThread(() -> {
                if (args.length > 0) {
                    try {
                        JSONObject requestObj = (JSONObject) args[0];
                        String requestRoomId = requestObj.getString("chatRoomId");

                        if (requestRoomId.equals(chatRoomId)) {
                            showEndChatConfirmation(false);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }));

            socket.on("chat_ended", args -> runOnUiThread(() -> {
                if (args.length > 0) {
                    try {
                        JSONObject endObj = (JSONObject) args[0];
                        String endedRoomId = endObj.getString("chatRoomId");

                        if (endedRoomId.equals(chatRoomId)) {
                            currentRoom.setEnded(true);
                            updateChatStatus();
                            updateChatHeader();
                            Toast.makeText(ChatActivity.this, "Chat has ended", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }));

            socket.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (socket != null) {
            socket.disconnect();
        }
        typingHandler.removeCallbacks(typingTimeout);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (socket != null && !socket.connected()) {
            socket.connect();
        }
        checkPartnerOnlineStatus();
    }
}