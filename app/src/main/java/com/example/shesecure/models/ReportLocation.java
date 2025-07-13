package com.example.shesecure.models;

import org.json.JSONException;
import org.json.JSONObject;

public class ReportLocation {
    private double latitude;
    private double longitude;
    private String formattedAddress;
    private String displayName;

    public ReportLocation(JSONObject json) throws JSONException {
        this.latitude = json.getDouble("latitude");
        this.longitude = json.getDouble("longitude");
        this.formattedAddress = json.optString("formattedAddress", "Address not available");
        this.displayName = json.optString("displayName", "Location not specified");
    }

    // Getters
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getFormattedAddress() { return formattedAddress; }
    public String getDisplayName() { return displayName; }
}