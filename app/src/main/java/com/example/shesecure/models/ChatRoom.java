package com.example.shesecure.models;

import com.google.gson.annotations.SerializedName;

public class ChatRoom {
    @SerializedName("_id")
    private String _id;
    @SerializedName("chatRequest")
    private ChatRequest chatRequest;
    @SerializedName("user")
    private User user;
    @SerializedName("counsellor")
    private User counsellor;
    @SerializedName("isEnded")
    private boolean isEnded;
    @SerializedName("endedAt")
    private String endedAt;
    @SerializedName("createdAt")
    private String createdAt;
    @SerializedName("unreadCount")
    private int unreadCount;
    private boolean isOnline;

    // Getters and setters
    public String getId() { return _id; }
    public void setId(String _id) { this._id = _id; }
    public ChatRequest getChatRequest() { return chatRequest; }
    public void setChatRequest(ChatRequest chatRequest) { this.chatRequest = chatRequest; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public User getCounsellor() {return counsellor;}
    public void setCounsellor(User counsellor) {this.counsellor = counsellor;}
    public boolean isEnded() { return isEnded; }
    public void setEnded(boolean ended) { isEnded = ended; }
    public String getEndedAt() { return endedAt; }
    public void setEndedAt(String endedAt) { this.endedAt = endedAt; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public int getUnreadCount() { return unreadCount; }
    public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }
    public boolean isOnline() { return isOnline; }
    public void setOnline(boolean online) { isOnline = online; }
}