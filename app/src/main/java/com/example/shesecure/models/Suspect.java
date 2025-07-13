package com.example.shesecure.models;

import org.json.JSONException;
import org.json.JSONObject;

public class Suspect {
    private String name;
    private String gender;
    private String photoUrl;
    private String description;

    public Suspect(JSONObject json) throws JSONException {
        this.name = json.optString("name", "Unknown");
        this.gender = json.optString("gender", "Unknown");
        this.photoUrl = json.optString("photoUrl", "");
        this.description = json.optString("description", "");
    }

    // Getters
    public String getName() { return name; }
    public String getGender() { return gender; }
    public String getPhotoUrl() { return photoUrl; }
    public String getDescription() { return description; }
}