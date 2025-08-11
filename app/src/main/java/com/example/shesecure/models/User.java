package com.example.shesecure.models;

import com.google.gson.annotations.SerializedName;
import org.json.JSONException;
import org.json.JSONObject;

public class User {
    public AdditionalDetails additionalDetails;
    @SerializedName("_id")
    private String _id;
    private String firstName;
    private String lastName;
    private String email;
    private String mobileNumber;
    private String userType;
    private String profileImage;

    // Empty constructor for Gson
    public User() {}

    // Constructor for login/signup
    public User(String firstName, String lastName, String email, String mobileNumber, String userType) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.mobileNumber = mobileNumber;
        this.userType = userType;
    }

    // Constructor for JSON parsing
    public User(JSONObject json) throws JSONException {
        this._id = json.optString("_id", "");
        this.firstName = json.optString("firstName", "");
        this.lastName = json.optString("lastName", "");
        this.email = json.optString("email", "");
        this.mobileNumber = json.optString("mobileNumber", "");
        this.userType = json.optString("userType", "User");
        this.profileImage = json.optString("profileImage", "");
    }

    // Getters
    public String getId() { return _id; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getMobileNumber() { return mobileNumber; }
    public String getUserType() { return userType; }
    public String getProfileImage() { return profileImage; }

    // Setters
    public void setId(String _id) { this._id = _id; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setEmail(String email) { this.email = email; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }
    public void setUserType(String userType) { this.userType = userType; }
    public void setProfileImage(String profileImage) { this.profileImage = profileImage; }

    public void setAdditionalDetails(AdditionalDetails additionalDetails) {
        this.additionalDetails = additionalDetails;
    }

    public AdditionalDetails getAdditionalDetails() {
        return additionalDetails;
    }

    // Helper method
    public String getFullName() {
        return firstName + " " + lastName;
    }

    public String getInitials() {
        return ""+(firstName != null && !firstName.isEmpty() ? firstName.charAt(0) : "") + (lastName != null && !lastName.isEmpty() ? lastName.charAt(0) : "");
    }
}