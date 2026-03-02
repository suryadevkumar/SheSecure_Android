package com.example.shesecure.activities;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
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
import com.example.shesecure.socket.CentralizedSocketManager;
import com.example.shesecure.utils.ApiUtils;
import com.example.shesecure.utils.AuthManager;
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
    private String chatRoomId, userId, userType, token;
    protected AuthManager authManager;
    private ApiService apiService;
    private CentralizedSocketManager socketManager;
    private ChatRoom currentRoom;
    private boolean isTyping = false;
    private Handler typingHandler = new Handler();
    private LinearLayout typingIndicatorContainer, endChatConfirmationLayout, messageInputLayout, chatEndedLayout;
    private ImageView typingIndicatorDot1, typingIndicatorDot2, typingIndicatorDot3;
    private ValueAnimator typingAnimator;
    private View onlineIndicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        authManager = new AuthManager(this);

        token = authManager.getToken();

        // Get chat room ID from intent
        chatRoomId = getIntent().getStringExtra("chatRoomId");
        if (chatRoomId == null) {
            finish();
            return;
        }

        // Initialize socket manager
        socketManager = CentralizedSocketManager.getInstance();

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
        userId = authManager.getUserId();
        userType = authManager.getUserType();

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
                                if (room.getEndRequestStatus()) {
                                    endChatConfirmationLayout.setVisibility(View.VISIBLE);
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

                Message systemMessage = new Message();
                systemMessage.setSystem(true);
                systemMessage.setContent("Counselor has requested to end this chat. Please accept or decline.");
                systemMessage.setCreatedAt(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(new Date()));
                systemMessage.setReadBy(new ArrayList<>());
                messages.add(systemMessage);
                messageAdapter.notifyItemInserted(messages.size() - 1);
                scrollToBottom();
            } else {
                endChatConfirmationLayout.setVisibility(View.GONE);
            }
        });
    }

    private void confirmEndChat(boolean accept) {
        Socket chatSocket = socketManager.getChatSocket();
        if (chatSocket != null && chatSocket.connected()){
            try {
                JSONObject data = new JSONObject();
                data.put("chatRoomId", chatRoomId);
                data.put("userId", userId);
                data.put("accepted", accept);

                chatSocket.emit("end_chat_response", data);
                showEndChatConfirmation(false);

                if (accept) {
                    currentRoom.setEnded();
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
        Socket chatSocket = socketManager.getChatSocket();
        if (chatSocket != null && chatSocket.connected() && currentRoom != null) {
            String partnerId = "User".equals(userType) ?
                    currentRoom.getCounsellor().getId() :
                    currentRoom.getUser().getId();

            try {
                JSONObject data = new JSONObject();
                data.put("userId", partnerId);
                chatSocket.emit("check_user_status", data);
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

            typingIndicatorDot1.setAlpha(progress < 0.33f ? progress * 3 : (1 - (progress - 0.33f) * 1.5f));
            typingIndicatorDot2.setAlpha(progress < 0.66f ?
                    (progress > 0.33f ? (progress - 0.33f) * 3 : 0f) :
                    (1 - (progress - 0.66f) * 1.5f));
            typingIndicatorDot3.setAlpha(progress > 0.66f ? (progress - 0.66f) * 3 : 0f);
        });
    }

    @SuppressLint("SetTextI18n")
    private void updateChatHeader() {
        if (currentRoom == null) return;

        if ("User".equals(userType)) {
            chatTitleTextView.setText(currentRoom.getCounsellor().getFullName());
            userInitialsView.setText(currentRoom.getCounsellor().getInitials().toUpperCase());
        } else {
            chatTitleTextView.setText(currentRoom.getUser().getFullName());
            userInitialsView.setText(currentRoom.getUser().getInitials().toUpperCase());
        }

        chatSubtitleTextView.setText(currentRoom.getProblemType() + ": " +
                currentRoom.getBrief());
    }

    private void sendMessage() {
        String content = messageEditText.getText().toString().trim();
        if (content.isEmpty()) return;
        Socket chatSocket = socketManager.getChatSocket();
        if (chatSocket != null && chatSocket.connected()) {
            try {
                JSONObject data = new JSONObject();
                data.put("chatRoomId", chatRoomId);
                data.put("senderId", userId);
                data.put("content", messageEditText.getText().toString());
                chatSocket.emit("send_message", data);

                Message temp = new Message();
                temp.setContent(content);
                temp.setChatRoomId(chatRoomId);

                User sender = new User();
                sender.setId(userId);

                temp.setSender(sender);
                temp.setCreatedAt(new Date().toString());

                messages.add(temp);
                messageAdapter.notifyItemInserted(messages.size() - 1);
                scrollToBottom();

                messageEditText.setText("");
            } catch (Exception e) { }
        }
    }

    private void emitTyping(boolean typing) {
        Socket chatSocket = socketManager.getChatSocket();
        if (chatSocket != null && chatSocket.connected()) {
            try {
                JSONObject data = new JSONObject();
                data.put("chatRoomId", chatRoomId);
                data.put("userId", userId);
                chatSocket.emit(typing ? "user_typing" : "user_stopped_typing", data);
            } catch (Exception e) { }
        }
    }

    private void requestEndChat() {
        Socket chatSocket = socketManager.getChatSocket();
        if (chatSocket != null && chatSocket.connected() && "Counsellor".equals(userType)) {
            try {
                JSONObject data = new JSONObject();
                data.put("chatRoomId", chatRoomId);
                data.put("counsellorId", userId);

                chatSocket.emit("request_end_chat", data);
                Toast.makeText(this, "End chat request sent", Toast.LENGTH_SHORT).show();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void markMessagesAsRead() {
        Socket chatSocket = socketManager.getChatSocket();
        if (chatSocket != null && chatSocket.connected()) {
            try {
                JSONObject readData = new JSONObject();
                readData.put("chatRoomId", chatRoomId);
                readData.put("userId", userId);

                chatSocket.emit("mark_messages_read", readData);
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
        // Socket should already be initialized by ChatListActivity
        // Just ensure it's connected
        Socket chatSocket = socketManager.getChatSocket();

        if (chatSocket == null) {
            socketManager.init(this);
            chatSocket = socketManager.getChatSocket();
        }

        if (chatSocket.connected()) {
            chatSocket.emit("user_connected", userId);
            markMessagesAsRead();
            checkPartnerOnlineStatus();
        }

        // 2. Clear previous listeners (to avoid duplicate UI updates)
        chatSocket.off("new_message");
        chatSocket.off("user_typing");
        chatSocket.off("user_stopped_typing");
        chatSocket.off("user_status_change");
        chatSocket.off("chat_ended");
        chatSocket.off("end_chat_request");
        chatSocket.off("end_chat_request_canceled");

        Socket finalChatSocket = chatSocket;

        chatSocket.on(Socket.EVENT_CONNECT, args -> {
            Log.d("SOCKET", "CONNECTED");

            finalChatSocket.emit("user_connected", userId);

            runOnUiThread(() -> {
                markMessagesAsRead();
                checkPartnerOnlineStatus();
            });
        });

        // Set up event listeners specific to this chat
        chatSocket.on("user_status_change", args -> runOnUiThread(() -> {
            try {
                JSONObject obj = (JSONObject) args[0];
                String status = obj.getString("status");
                updateOnlineStatus(status.equals("online"));
            } catch (Exception e) { }
        }));

        chatSocket.on("new_message", args -> runOnUiThread(() -> {
            try {
                JSONObject data = (JSONObject) args[0];
                Message msg = new Gson().fromJson(data.toString(), Message.class);
                if (msg.getChatRoomId().equals(chatRoomId)) {
                    messages.add(msg);
                    messageAdapter.notifyItemInserted(messages.size() - 1);
                    scrollToBottom();
                    markMessagesAsRead();
                }
            } catch (Exception e) { Log.e("Socket", "Error msg"); }
        }));

        chatSocket.on("messages_read", args -> runOnUiThread(() -> {

            for (Message msg : messages) {

                if (msg.getReadBy() == null) {
                    msg.setReadBy(new ArrayList<>());
                }

                if (!msg.getReadBy().contains(userId)) {
                    msg.getReadBy().add(userId);
                }
            }

            messageAdapter.notifyDataSetChanged();
        }));

        chatSocket.on("user_typing", args -> runOnUiThread(() -> {
            try {
                JSONObject obj = (JSONObject) args[0];

                if (obj.getString("chatRoomId").equals(chatRoomId)
                        && !obj.getString("userId").equals(userId)) {

                    typingIndicatorContainer.setVisibility(View.VISIBLE);

                    if (!typingAnimator.isRunning())
                        typingAnimator.start();
                }

            } catch (Exception e) { }
        }));

        chatSocket.on("user_stopped_typing", args -> runOnUiThread(() -> {
            typingIndicatorContainer.setVisibility(View.GONE);
            typingAnimator.cancel();
        }));

        chatSocket.on("end_chat_request", args ->{
            if (!isFinishing() && !isDestroyed()) {

                runOnUiThread(() -> {
                    if ("User".equals(userType)) {
                        showEndChatConfirmation(true);
                    }
                });
            }

        });

        chatSocket.on("end_chat_request_canceled", args -> runOnUiThread(() -> {
            if ("User".equals(userType)) showEndChatConfirmation(false);
        }));

        chatSocket.on("clear_end_request_lock", args -> runOnUiThread(() -> {
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

        chatSocket.on("chat_ended", args -> runOnUiThread(() -> {
            currentRoom.setEnded();
            updateChatStatus();
            Toast.makeText(this, "Chat has ended", Toast.LENGTH_SHORT).show();
        }));

        if (!chatSocket.connected()) chatSocket.connect();
    }

}