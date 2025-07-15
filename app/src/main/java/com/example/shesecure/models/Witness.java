package com.example.shesecure.models;

import android.net.Uri;
import org.json.JSONException;
import org.json.JSONObject;

public class Witness {
    private String name;
    private String gender;
    private String contactNumber;
    private String address;
    private String photoUrl;
    private Uri photoUri;

    // Constructor for JSON data (API response)
    public Witness(JSONObject json) throws JSONException {
        this(
                json.optString("witnessName", "Unknown"),
                json.optString("witnessGender", "Unknown"),
                json.optString("witnessContactNumber", ""),
                json.optString("witnessAddress", ""),
                json.optString("witnessPhoto", ""),
                null // photoUri not available from JSON
        );
    }

    // Constructor for local form data
    public Witness(String name, String gender, String contactNumber,
                   String address, Uri photoUri) {
        this(name, gender, contactNumber, address, "", photoUri);
    }

    // Master constructor (private)
    private Witness(String name, String gender, String contactNumber,
                    String address, String photoUrl, Uri photoUri) {
        this.name = name;
        this.gender = gender;
        this.contactNumber = contactNumber;
        this.address = address;
        this.photoUrl = photoUrl;
        this.photoUri = photoUri;
    }

    // Getters
    public String getName() { return name; }
    public String getGender() { return gender; }
    public String getContactNumber() { return contactNumber; }
    public String getAddress() { return address; }
    public String getPhotoUrl() { return photoUrl; }
    public Uri getPhotoUri() { return photoUri; }

    // Setters
    public void setName(String name) { this.name = name; }
    public void setGender(String gender) { this.gender = gender; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }
    public void setAddress(String address) { this.address = address; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
    public void setPhotoUri(Uri photoUri) { this.photoUri = photoUri; }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("witnessName", name);
        json.put("witnessGender", gender);
        json.put("witnessContactNumber", contactNumber);
        json.put("witnessAddress", address);
        if (photoUrl != null && !photoUrl.isEmpty()) {
            json.put("witnessPhoto", photoUrl);
        }
        return json;
    }
}