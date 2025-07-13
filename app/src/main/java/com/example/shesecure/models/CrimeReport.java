package com.example.shesecure.models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CrimeReport implements Serializable {
    private String id;
    private String typeOfCrime;
    private String description;
    private String status;
    private String dateOfCrime;
    private String createdAt;
    private String firUrl;
    private List<String> photoUrls = new ArrayList<>();
    private List<String> videoUrls = new ArrayList<>();
    private List<Suspect> suspects = new ArrayList<>();
    private List<Witness> witnesses = new ArrayList<>();
    private ReportLocation location;
    private User assignedAdmin;

    public CrimeReport(JSONObject json) throws JSONException {
        this.id = json.getString("_id");
        this.typeOfCrime = json.getString("typeOfCrime");
        this.description = json.getString("description");
        this.status = json.getString("status");
        this.dateOfCrime = json.getString("dateOfCrime");
        this.createdAt = json.getString("createdAt");
        this.firUrl = json.optString("FIR");

        // Parse arrays
        JSONArray photosArray = json.optJSONArray("crimePhotos");
        if (photosArray != null) {
            for (int i = 0; i < photosArray.length(); i++) {
                photoUrls.add(photosArray.getString(i));
            }
        }

        JSONArray videosArray = json.optJSONArray("crimeVideos");
        if (videosArray != null) {
            for (int i = 0; i < videosArray.length(); i++) {
                videoUrls.add(videosArray.getString(i));
            }
        }

        // Parse location
        if (json.has("location")) {
            this.location = new ReportLocation(json.getJSONObject("location"));
        }

        // Parse assigned admin
        if (json.has("assignedAdmin")) {
            this.assignedAdmin = new User(json.getJSONObject("assignedAdmin"));
        }

        // Parse suspects and witnesses
        JSONArray suspectsArray = json.optJSONArray("suspects");
        if (suspectsArray != null) {
            for (int i = 0; i < suspectsArray.length(); i++) {
                suspects.add(new Suspect(suspectsArray.getJSONObject(i)));
            }
        }

        JSONArray witnessesArray = json.optJSONArray("witnesses");
        if (witnessesArray != null) {
            for (int i = 0; i < witnessesArray.length(); i++) {
                witnesses.add(new Witness(witnessesArray.getJSONObject(i)));
            }
        }
    }

    public String getFormattedDate() {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            Date date = inputFormat.parse(createdAt);
            return outputFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return createdAt;
        }
    }

    // Getters
    public String getId() { return id; }
    public String getTypeOfCrime() { return typeOfCrime; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public String getDateOfCrime() { return dateOfCrime; }
    public String getCreatedAt() { return createdAt; }
    public String getFirUrl() { return firUrl; }
    public List<String> getPhotoUrls() { return photoUrls; }
    public List<String> getVideoUrls() { return videoUrls; }
    public List<Suspect> getSuspects() { return suspects; }
    public List<Witness> getWitnesses() { return witnesses; }
    public ReportLocation getLocation() { return location; }
    public User getAssignedAdmin() { return assignedAdmin; }
}