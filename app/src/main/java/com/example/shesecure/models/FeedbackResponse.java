package com.example.shesecure.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class FeedbackResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private List<Feedback> data;

    // Constructors
    public FeedbackResponse() {}

    public FeedbackResponse(boolean success, String message, List<Feedback> data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<Feedback> getData() {
        return data;
    }

    public void setData(List<Feedback> data) {
        this.data = data;
    }
}