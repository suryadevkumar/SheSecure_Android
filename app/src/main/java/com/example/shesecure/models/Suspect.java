package com.example.shesecure.models;

import org.json.JSONException;
import org.json.JSONObject;

public class Suspect {
    private String name;
    private String gender;
    private String photoUrl;

    public Suspect(JSONObject json) throws JSONException {
        this.name = json.optString("suspectName", "Unknown");
        this.gender = json.optString("suspectGender", "Unknown");
        this.photoUrl = json.optString("suspectPhoto", "");
    }

    // Getters
    public String getName() { return name; }
    public String getGender() { return gender; }
    public String getPhotoUrl() { return photoUrl; }
}