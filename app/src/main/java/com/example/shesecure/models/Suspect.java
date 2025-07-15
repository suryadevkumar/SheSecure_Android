package com.example.shesecure.models;

import android.net.Uri;
import org.json.JSONException;
import org.json.JSONObject;

public class Suspect {
    private String name;
    private String gender;
    private String photoUrl;
    private Uri photoUri;

    // Constructor for JSON data
    public Suspect(JSONObject json) throws JSONException {
        this.name = json.optString("suspectName", "Unknown");
        this.gender = json.optString("suspectGender", "Unknown");
        this.photoUrl = json.optString("suspectPhoto", "");
    }

    // Constructor for local form data
    public Suspect(String name, String gender, Uri photoUri) {
        this.name = name;
        this.gender = gender;
        this.photoUri = photoUri;
    }

    // Getters
    public String getName() { return name; }
    public String getGender() { return gender; }
    public String getPhotoUrl() { return photoUrl; }
    public Uri getPhotoUri() { return photoUri; }

    // Setters (for dynamic updates)
    public void setName(String name) { this.name = name; }
    public void setGender(String gender) { this.gender = gender; }  // Needed for Spinner selection
    public void setPhotoUri(Uri photoUri) { this.photoUri = photoUri; }

    // Convert to JSON (for API submission)
    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("suspectName", name);
        json.put("suspectGender", gender);
        if (photoUrl != null) json.put("suspectPhoto", photoUrl);
        return json;
    }
}