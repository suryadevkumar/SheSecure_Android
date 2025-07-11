package com.example.shesecure.models;

import com.google.gson.annotations.SerializedName;

public class EmergencyContact {
    @SerializedName("_id")
    private String id;
    private String name;
    @SerializedName("contactNumber")
    private String phoneNumber;

    public EmergencyContact() {}

    public EmergencyContact(String name, String phoneNumber) {
        this.name = name;
        this.phoneNumber = phoneNumber;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}