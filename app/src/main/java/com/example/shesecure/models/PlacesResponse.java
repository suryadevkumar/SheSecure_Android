package com.example.shesecure.models;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class PlacesResponse {
    @SerializedName("places")
    private List<Place> places;

    public List<Place> getPlaces() {
        return places != null ? places : new ArrayList<>();
    }
}