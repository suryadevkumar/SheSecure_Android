package com.example.shesecure.models;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

public class Message implements Serializable {
    @SerializedName("_id")
    private String _id;
    @SerializedName("chatRoom")
    private String chatRoomId;
    @SerializedName("sender")
    private User sender;
    @SerializedName("content")
    private String content;
    @SerializedName("isSystem")
    private boolean isSystem;
    @SerializedName("readBy")
    private List<String> readBy;
    @SerializedName("createdAt")
    private String createdAt;

    // Getters and setters
    public String getId() { return _id; }
    public void setId(String _id) { this._id = _id; }
    public String getChatRoomId() { return chatRoomId; }
    public void setChatRoomId(String chatRoomId) { this.chatRoomId = chatRoomId; }
    public User getSender() { return sender; }
    public void setSender(User sender) { this.sender = sender; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public boolean isSystem() { return isSystem; }
    public void setSystem(boolean system) { isSystem = system; }
    public List<String> getReadBy() { return readBy; }
    public void setReadBy(List<String> readBy) { this.readBy = readBy; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}