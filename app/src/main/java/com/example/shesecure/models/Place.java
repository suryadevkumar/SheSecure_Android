package com.example.shesecure.models;

import com.google.gson.annotations.SerializedName;

public class Place {
    @SerializedName("formattedAddress")
    private String formattedAddress;

    @SerializedName("location")
    private Location location;

    @SerializedName("displayName")
    private DisplayName displayName;

    public String getFormattedAddress() {
        return formattedAddress;
    }

    public Location getLocation() {
        return location;
    }

    public String getDisplayName() {
        return displayName != null ? displayName.text : "";
    }

    public static class Location {
        @SerializedName("latitude")
        private double latitude;
        @SerializedName("longitude")
        private double longitude;

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }
    }

    public static class DisplayName {
        @SerializedName("text")
        private String text;
        @SerializedName("languageCode")
        private String languageCode;
    }
}