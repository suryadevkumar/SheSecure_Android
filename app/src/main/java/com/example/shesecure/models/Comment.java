// Comment.java
package com.example.shesecure.models;

import java.util.List;

public class Comment {
    private String id;
    private String text;
    private String createdAt;
    private String supportStatus;
    private String userId;
    private String userName;
    private String userImage;

    // Constructors
    public Comment() {}

    public Comment(String id, String text, String createdAt, String supportStatus,
                   String userId, String userName, String userImage) {
        this.id = id;
        this.text = text;
        this.createdAt = createdAt;
        this.supportStatus = supportStatus;
        this.userId = userId;
        this.userName = userName;
        this.userImage = userImage;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getSupportStatus() {
        return supportStatus;
    }

    public void setSupportStatus(String supportStatus) {
        this.supportStatus = supportStatus;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserImage() {
        return userImage;
    }

    public void setUserImage(String userImage) {
        this.userImage = userImage;
    }
}
