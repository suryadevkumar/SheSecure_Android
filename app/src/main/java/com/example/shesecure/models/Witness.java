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
        this.name = json.optString("witnessName", "Unknown");
        this.gender = json.optString("witnessGender", "Unknown");
        this.contactNumber = json.optString("witnessContactNumber", "");
        this.address = json.optString("witnessAddress", "");
        this.photoUrl = json.optString("witnessPhoto", "");
    }

    // Getters
    public String getName() { return name; }
    public String getGender() { return gender; }
    public String getContactNumber() { return contactNumber; }
    public String getAddress() { return address; }
    public String getPhotoUrl() { return photoUrl; }
}