package com.example.shesecure.models;

import java.util.List;

public class EmergencyContactResponse {
    private List<EmergencyContact> contacts;

    public List<EmergencyContact> getContacts() {
        return contacts;
    }

    public void setContacts(List<EmergencyContact> contacts) {
        this.contacts = contacts;
    }
}