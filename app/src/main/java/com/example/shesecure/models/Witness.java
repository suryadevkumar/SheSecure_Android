package com.example.shesecure.models;

import org.json.JSONException;
import org.json.JSONObject;

public class Witness {
    private String name;
    private String gender;
    private String contactNumber;
    private String address;
    private String photoUrl;

    public Witness(JSONObject json) throws JSONException {
        this.name = json.optString("name", "Unknown");
        this.gender = json.optString("gender", "Unknown");
        this.contactNumber = json.optString("contactNumber", "");
        this.address = json.optString("address", "");
        this.photoUrl = json.optString("photoUrl", "");
    }

    // Getters
    public String getName() { return name; }
    public String getGender() { return gender; }
    public String getContactNumber() { return contactNumber; }
    public String getAddress() { return address; }
    public String getPhotoUrl() { return photoUrl; }
}