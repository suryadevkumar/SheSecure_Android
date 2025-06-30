// UserInteraction.java
package com.example.shesecure.models;

public class UserInteraction {
    private String supportStatus;

    // Constructors
    public UserInteraction() {}

    public UserInteraction(String supportStatus) {
        this.supportStatus = supportStatus;
    }

    // Getters and Setters
    public String getSupportStatus() {
        return supportStatus;
    }

    public void setSupportStatus(String supportStatus) {
        this.supportStatus = supportStatus;
    }
}