package com.example.shesecure.models;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class ReportLocation implements Serializable {
    private String id;
    private double latitude;
    private double longitude;
    private String displayName;
    private String formattedAddress;
    private String startTime;
    private String endTime;
    private String createdAt;
    private String updatedAt;

    public ReportLocation(JSONObject json) throws JSONException {
        this.id = json.optString("_id", "");
        this.latitude = json.getDouble("latitude");
        this.longitude = json.getDouble("longitude");
        this.displayName = json.optString("displayName", "Location not specified");
        this.formattedAddress = json.optString("formattedAddress", "Address not available");
        this.startTime = json.optString("startTime", "");
        this.endTime = json.optString("endTime", "");
        this.createdAt = json.optString("createdAt", "");
        this.updatedAt = json.optString("updatedAt", "");
    }

    // Getters
    public String getId() { return id; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getDisplayName() { return displayName; }
    public String getFormattedAddress() { return formattedAddress; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
}