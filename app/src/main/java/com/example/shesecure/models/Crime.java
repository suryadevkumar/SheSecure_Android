// Crime.java
package com.example.shesecure.models;

import java.util.List;

public class Crime {
    private String id;
    private String typeOfCrime;
    private String description;
    private String createdAt;
    private List<String> crimePhotos;

    // Constructors
    public Crime() {}

    public Crime(String id, String typeOfCrime, String description, String createdAt, List<String> crimePhotos) {
        this.id = id;
        this.typeOfCrime = typeOfCrime;
        this.description = description;
        this.createdAt = createdAt;
        this.crimePhotos = crimePhotos;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTypeOfCrime() {
        return typeOfCrime;
    }

    public void setTypeOfCrime(String typeOfCrime) {
        this.typeOfCrime = typeOfCrime;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public List<String> getCrimePhotos() {
        return crimePhotos;
    }

    public void setCrimePhotos(List<String> crimePhotos) {
        this.crimePhotos = crimePhotos;
    }
}