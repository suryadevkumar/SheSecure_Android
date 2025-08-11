package com.example.shesecure.utils;

import android.location.Location;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.shesecure.models.CrimeReport;
import com.example.shesecure.models.Place;
import com.example.shesecure.models.PlacesResponse;
import com.example.shesecure.services.ApiService;
import com.example.shesecure.services.ApiService.CrimeNearbyResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LocationDataManager {
    private static volatile LocationDataManager instance;

    // LiveData objects
    private final MutableLiveData<List<CrimeReport>> nearestCrimes = new MutableLiveData<>();
    private final MutableLiveData<List<Place>> nearestPoliceStations = new MutableLiveData<>();
    private final MutableLiveData<List<Place>> nearestHospitals = new MutableLiveData<>();

    // Current data caches
    private List<CrimeReport> currentCrimes = new ArrayList<>();
    private List<Place> currentPoliceStations = new ArrayList<>();
    private List<Place> currentHospitals = new ArrayList<>();

    // Dependencies
    private ApiService apiService;
    private String authToken;
    private String googleApiKey;
    private Location lastKnownLocation;

    private LocationDataManager() {
        // Initialize with empty lists
        nearestCrimes.setValue(new ArrayList<>());
        nearestPoliceStations.setValue(new ArrayList<>());
        nearestHospitals.setValue(new ArrayList<>());
    }

    public static LocationDataManager getInstance() {
        if (instance == null) {
            synchronized (LocationDataManager.class) {
                if (instance == null) {
                    instance = new LocationDataManager();
                }
            }
        }
        return instance;
    }

    public void initialize(ApiService apiService, String authToken, String googleApiKey) {
        this.apiService = apiService;
        this.authToken = authToken;
        this.googleApiKey = googleApiKey;
        Log.d("LocationDataManager", "Initialized with API services");
    }

    // Main location update method
    public void updateUserLocation(Location newLocation) {
        if (newLocation == null || apiService == null || authToken == null || googleApiKey == null) {
            Log.w("LocationDataManager", "Cannot update location - missing dependencies");
            return;
        }

        lastKnownLocation = newLocation;
        Log.d("LocationDataManager", "Updating location to: " + newLocation.getLatitude() +
                ", " + newLocation.getLongitude());

        updateCrimeData(newLocation);
        updateNearbyPlaces(newLocation, "police", places -> {
            currentPoliceStations = places;
            nearestPoliceStations.postValue(new ArrayList<>(currentPoliceStations));
            Log.d("LocationDataManager", "Updated " + places.size() + " police stations");
        });

        updateNearbyPlaces(newLocation, "hospital", places -> {
            currentHospitals = places;
            nearestHospitals.postValue(new ArrayList<>(currentHospitals));
            Log.d("LocationDataManager", "Updated " + places.size() + " hospitals");
        });
    }

    private void updateCrimeData(Location location) {
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("latitude", location.getLatitude());
            requestBody.put("longitude", location.getLongitude());

            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json"),
                    requestBody.toString()
            );

            apiService.getCrimesNearMe(authToken, body).enqueue(new Callback<CrimeNearbyResponse>() {
                @Override
                public void onResponse(Call<CrimeNearbyResponse> call, Response<CrimeNearbyResponse> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().success) {
                        currentCrimes = response.body().crimes;
                        nearestCrimes.postValue(new ArrayList<>(currentCrimes));
                        Log.d("LocationDataManager", "Updated " + currentCrimes.size() + " crimes");
                    } else {
                        Log.w("LocationDataManager", "Failed to update crimes: " +
                                (response.body() != null ? response.body().message : "null response"));
                        nearestCrimes.postValue(new ArrayList<>(currentCrimes));
                    }
                }

                @Override
                public void onFailure(Call<CrimeNearbyResponse> call, Throwable t) {
                    Log.e("LocationDataManager", "Crime API call failed", t);
                    nearestCrimes.postValue(new ArrayList<>(currentCrimes));
                }
            });
        } catch (Exception e) {
            Log.e("LocationDataManager", "Error in updateCrimeData", e);
            nearestCrimes.postValue(new ArrayList<>(currentCrimes));
        }
    }

    private void updateNearbyPlaces(Location location, String type, PlacesCallback callback) {
        try {
            JSONObject locationRestriction = new JSONObject();
            JSONObject center = new JSONObject();
            center.put("latitude", location.getLatitude());
            center.put("longitude", location.getLongitude());

            JSONObject circle = new JSONObject();
            circle.put("center", center);
            circle.put("radius", 5000); // 5km radius

            locationRestriction.put("circle", circle);

            JSONObject requestBody = new JSONObject();
            requestBody.put("includedTypes", new JSONArray().put(type));
            requestBody.put("locationRestriction", locationRestriction);

            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json"),
                    requestBody.toString()
            );

            // Update field mask to match the API response structure
            String fieldMask = "places.displayName,places.formattedAddress,places.location";

            apiService.searchNearbyPlaces(
                    googleApiKey,
                    fieldMask,
                    body
            ).enqueue(new Callback<PlacesResponse>() {
                @Override
                public void onResponse(Call<PlacesResponse> call, Response<PlacesResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        callback.onSuccess(response.body().getPlaces());
                    } else {
                        Log.w("LocationDataManager", "Failed to load " + type + " places: " +
                                (response.errorBody() != null ? response.errorBody().toString() : ""));
                        callback.onSuccess(new ArrayList<>());
                    }
                }

                @Override
                public void onFailure(Call<PlacesResponse> call, Throwable t) {
                    Log.e("LocationDataManager", type + " API call failed", t);
                    callback.onSuccess(new ArrayList<>());
                }
            });
        } catch (Exception e) {
            Log.e("LocationDataManager", "Error in updateNearbyPlaces for " + type, e);
            callback.onSuccess(new ArrayList<>());
        }
    }

    // Force update all data without API calls
    public void forceUpdateAllLocations() {
        Log.d("LocationDataManager", "Forcing update of all location data");

        nearestCrimes.postValue(new ArrayList<>(currentCrimes));
        nearestPoliceStations.postValue(new ArrayList<>(currentPoliceStations));
        nearestHospitals.postValue(new ArrayList<>(currentHospitals));

        Log.d("LocationDataManager",
                "Force update complete - Crimes: " + currentCrimes.size() +
                        ", Police: " + currentPoliceStations.size() +
                        ", Hospitals: " + currentHospitals.size());
    }

    // Refresh all data with API calls
    public void refreshAllData() {
        if (lastKnownLocation != null) {
            Log.d("LocationDataManager", "Refreshing all data from APIs");
            updateUserLocation(lastKnownLocation);
        } else {
            Log.w("LocationDataManager", "No location available - falling back to force update");
            forceUpdateAllLocations();
        }
    }

    // LiveData getters
    public LiveData<List<CrimeReport>> getNearestCrimes() {
        return nearestCrimes;
    }

    public LiveData<List<Place>> getNearestPoliceStations() {
        return nearestPoliceStations;
    }

    public LiveData<List<Place>> getNearestHospitals() {
        return nearestHospitals;
    }

    // Current data getters
    public List<CrimeReport> getCurrentCrimes() {
        return new ArrayList<>(currentCrimes);
    }

    public List<Place> getCurrentPoliceStations() {
        return new ArrayList<>(currentPoliceStations);
    }

    public List<Place> getCurrentHospitals() {
        return new ArrayList<>(currentHospitals);
    }

    public Location getLastKnownLocation() {
        return lastKnownLocation;
    }

    public boolean hasValidData() {
        return !currentCrimes.isEmpty() || !currentPoliceStations.isEmpty() || !currentHospitals.isEmpty();
    }

    private interface PlacesCallback {
        void onSuccess(List<Place> places);
    }
}