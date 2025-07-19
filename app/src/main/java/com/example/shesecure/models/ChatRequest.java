package com.example.shesecure.models;

import com.google.gson.annotations.SerializedName;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.JsonAdapter;
import java.lang.reflect.Type;

public class ChatRequest {
    @SerializedName("_id")
    private String _id;
    private String problemType;
    private String brief;
    private String status;
    private String createdAt;

    @SerializedName("acceptedBy")
    @JsonAdapter(UserDeserializer.class)
    private User acceptedBy;

    private User user;

    // Fixed Custom deserializer for acceptedBy field
    public static class UserDeserializer implements JsonDeserializer<User> {
        @Override
        public User deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json.isJsonNull()) {
                return null;
            }

            if (json.isJsonPrimitive()) {
                // If it's a string (just ID), create a User with only ID
                User user = new User();
                user.setId(json.getAsString());
                return user;
            } else if (json.isJsonObject()) {
                // Manual deserialization to avoid infinite recursion
                User user = new User();

                if (json.getAsJsonObject().has("_id")) {
                    user.setId(json.getAsJsonObject().get("_id").getAsString());
                }
                if (json.getAsJsonObject().has("firstName")) {
                    user.setFirstName(json.getAsJsonObject().get("firstName").getAsString());
                }
                if (json.getAsJsonObject().has("lastName")) {
                    user.setLastName(json.getAsJsonObject().get("lastName").getAsString());
                }
                if (json.getAsJsonObject().has("email")) {
                    user.setEmail(json.getAsJsonObject().get("email").getAsString());
                }
                if (json.getAsJsonObject().has("userType")) {
                    user.setUserType(json.getAsJsonObject().get("userType").getAsString());
                }

                return user;
            }
            return null;
        }
    }

    // Getters and setters
    public String getId() { return _id; }
    public void setId(String _id) { this._id = _id; }
    public String getProblemType() { return problemType; }
    public void setProblemType(String problemType) { this.problemType = problemType; }
    public String getBrief() { return brief; }
    public void setBrief(String brief) { this.brief = brief; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public User getAcceptedBy() {
        return acceptedBy;
    }

    public void setAcceptedBy(User acceptedBy) {
        this.acceptedBy = acceptedBy;
    }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}