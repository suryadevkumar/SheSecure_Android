package com.example.shesecure.activities;

import android.Manifest;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.maps.model.PolylineOptions;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import com.example.shesecure.R;
import com.example.shesecure.SheSecureApp;
import com.example.shesecure.adapters.PlaceAdapter;
import com.example.shesecure.models.CrimeReport;
import com.example.shesecure.models.Place;
import com.example.shesecure.services.ApiService;
import com.example.shesecure.utils.ApiUtils;
import com.example.shesecure.utils.LocationDataManager;
import com.example.shesecure.utils.SecurePrefs;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MapService extends BaseActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Marker userMarker;
    private FusedLocationProviderClient fusedLocationClient;
    private String authToken;
    private Map<String, Marker> crimeMarkers = new HashMap<>();
    private Map<String, Marker> policeMarkers = new HashMap<>();
    private Map<String, Marker> hospitalMarkers = new HashMap<>();
    private List<CrimeReport> crimes = new ArrayList<>();
    private static final int MARKER_ANIMATION_DURATION = 1000;
    private static final int CAMERA_ANIMATION_DURATION = 1500;
    private static final float DEFAULT_ZOOM = 15f;
    private Circle staticSafetyCircle;
    private Circle animatedRippleCircle;
    private Circle safetyRadiusCircle;
    private ValueAnimator radiusAnimator;
    private CrimeReport nearestCrime;
    private CardView nearestCrimeBanner;
    private TextView nearestCrimeType, nearestCrimeDescription, safetyPercentageText, nearestCrimeDistance;
    private int rippleColor = Color.GREEN;
    private CheckBox crimeCheckbox, policeCheckbox, hospitalCheckbox;
    private PlaceAdapter policeAdapter, hospitalAdapter;
    private Polyline currentPath;
    private LinearLayout placesPanel;
    private boolean isPlacesPanelVisible = false;
    private LatLng currentLocationLatLng;
    private final MutableLiveData<Location> locationObserver = new MutableLiveData<>();

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_service);

        // Initialize UI components
        ImageView menuButton = findViewById(R.id.menu_button);
        crimeCheckbox = findViewById(R.id.crime_checkbox);
        policeCheckbox = findViewById(R.id.police_checkbox);
        hospitalCheckbox = findViewById(R.id.hospital_checkbox);
        placesPanel = findViewById(R.id.places_panel);

        // Set up menu button
        menuButton.setOnClickListener(v -> togglePlacesPanel());

        // Initialize services and adapters
        initializeServices();
        setupAdapters();
        setupCheckboxListeners();

        nearestCrimeBanner = findViewById(R.id.nearest_crime_banner);
        nearestCrimeType = findViewById(R.id.nearest_crime_type);
        nearestCrimeDescription = findViewById(R.id.nearest_crime_description);
        safetyPercentageText = findViewById(R.id.safety_percentage);
        nearestCrimeDistance = findViewById(R.id.nearest_crime_distance);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        setupMap();
    }

    private void togglePlacesPanel() {
        isPlacesPanelVisible = !isPlacesPanelVisible;
        placesPanel.setVisibility(isPlacesPanelVisible ? View.VISIBLE : View.GONE);
    }

    private void initializeServices() {
        ApiService apiService = ApiUtils.initializeApiService(this, ApiService.class);
        SecurePrefs securePrefs = ((SheSecureApp) this.getApplicationContext()).getSecurePrefs();
        String googleApiKey = securePrefs.getGoogleMapsApiKey();

        authToken = "Bearer " + authManager.getToken();

        LocationDataManager.getInstance().initialize(apiService, authToken, googleApiKey);
        observeCrimeLocations();
        observePlacesData();
    }

    private void setupAdapters() {
        policeAdapter = new PlaceAdapter(new ArrayList<>(), place -> {
            if (place.getLocation() != null) {
                showPathToPlace(place.getLocation(), "Police Station");
                togglePlacesPanel();
            }
        });

        hospitalAdapter = new PlaceAdapter(new ArrayList<>(), place -> {
            if (place.getLocation() != null) {
                showPathToPlace(place.getLocation(), "Hospital");
                togglePlacesPanel();
            }
        });

        RecyclerView policeList = findViewById(R.id.police_list);
        RecyclerView hospitalList = findViewById(R.id.hospital_list);

        policeList.setLayoutManager(new LinearLayoutManager(this));
        hospitalList.setLayoutManager(new LinearLayoutManager(this));

        policeList.setAdapter(policeAdapter);
        hospitalList.setAdapter(hospitalAdapter);
    }

    private void setupCheckboxListeners() {
        crimeCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateMapMarkersVisibility();
        });

        policeCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateMapMarkersVisibility();
        });

        hospitalCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateMapMarkersVisibility();
        });
    }

    private void updateMapMarkersVisibility() {
        // Update crime markers visibility
        for (Marker marker : crimeMarkers.values()) {
            if (marker != null) {
                marker.setVisible(crimeCheckbox.isChecked());
            }
        }

        // Update police markers visibility
        for (Marker marker : policeMarkers.values()) {
            if (marker != null) {
                marker.setVisible(policeCheckbox.isChecked());
            }
        }

        // Update hospital markers visibility
        for (Marker marker : hospitalMarkers.values()) {
            if (marker != null) {
                marker.setVisible(hospitalCheckbox.isChecked());
            }
        }
    }

    private void observeCrimeLocations() {
        LocationDataManager.getInstance().getNearestCrimes().removeObservers(this);
        LocationDataManager.getInstance().getNearestCrimes().observe(this, newCrimes -> {
            if (newCrimes != null && !newCrimes.isEmpty()) {
                crimes = newCrimes;
                if (mMap != null) {
                    updateMapWithCrimes(newCrimes);
                }
            }
        });

        locationObserver.removeObservers(this);
        locationObserver.observe(this, location -> {
            if (location != null) {
                LocationDataManager.getInstance().updateUserLocation(location);
            }
        });
    }

    private void observePlacesData() {
        LocationDataManager.getInstance().getNearestPoliceStations().observe(this, policeStations -> {
            if (policeStations != null) {
                policeAdapter.updatePlaces(policeStations);
                updatePoliceMarkers(policeStations);
            }
        });

        LocationDataManager.getInstance().getNearestHospitals().observe(this, hospitals -> {
            if (hospitals != null) {
                hospitalAdapter.updatePlaces(hospitals);
                updateHospitalMarkers(hospitals);
            }
        });
    }

    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                mMap.setMyLocationEnabled(true);
            }

            if (currentLocation != null) {
                updateMapWithLocation(currentLocation);
            } else {
                fusedLocationClient.getLastLocation()
                        .addOnSuccessListener(this, location -> {
                            if (location != null) {
                                currentLocation = location;
                                updateMapWithLocation(location);
                            }
                        });
            }
        } catch (SecurityException e) {
            Log.e("MAP_ERROR", "SecurityException: " + e.getMessage());
        }

        List<CrimeReport> existingCrimes = LocationDataManager.getInstance().getCurrentCrimes();
        if (existingCrimes != null && !existingCrimes.isEmpty()) {
            updateMapWithCrimes(existingCrimes);
        }
    }

    @Override
    protected void onLocationUpdated(@NonNull Location location) {
        currentLocationLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        updateMapWithLocation(location);
    }

    private void updateMapWithLocation(Location location) {
        if (mMap != null) {
            LatLng newPosition = new LatLng(location.getLatitude(), location.getLongitude());
            currentLocationLatLng = newPosition;

            if (userMarker == null) {
                userMarker = mMap.addMarker(new MarkerOptions()
                        .position(newPosition)
                        .title("Your Location"));
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(newPosition, DEFAULT_ZOOM),
                        CAMERA_ANIMATION_DURATION, null);

                addSafetyRadiusCircle(newPosition);
            } else {
                animateMarker(userMarker, newPosition);
                updateSafetyRadiusCircle(newPosition);
            }
            updateNearestCrimeDisplay();
        }
    }

    private void updateMapWithCrimes(List<CrimeReport> crimes) {
        if (mMap == null || crimes == null) return;

        for (Marker marker : crimeMarkers.values()) {
            if (marker != null) {
                marker.remove();
            }
        }
        crimeMarkers.clear();

        CrimeReport nearestCrime = null;
        float minDistance = Float.MAX_VALUE;

        for (CrimeReport crime : crimes) {
            if (crime != null && crime.getLocation() != null && crime.getId() != null) {
                LatLng crimeLatLng = new LatLng(
                        crime.getLocation().getLatitude(),
                        crime.getLocation().getLongitude()
                );

                MarkerOptions markerOptions = new MarkerOptions()
                        .position(crimeLatLng)
                        .title(crime.getTypeOfCrime())
                        .snippet(crime.getDescription());

                try {
                    Drawable vectorDrawable = ContextCompat.getDrawable(this, R.drawable.danger);
                    if (vectorDrawable != null) {
                        Bitmap bitmap = Bitmap.createBitmap(
                                vectorDrawable.getIntrinsicWidth(),
                                vectorDrawable.getIntrinsicHeight(),
                                Bitmap.Config.ARGB_8888);
                        Canvas canvas = new Canvas(bitmap);
                        vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                        vectorDrawable.draw(canvas);
                        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(bitmap));
                    }
                } catch (Exception e) {
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                }

                Marker marker = mMap.addMarker(markerOptions);
                marker.setVisible(crimeCheckbox.isChecked());
                crimeMarkers.put(crime.getId(), marker);

                try {
                    float distance = Float.parseFloat(crime.getDistance());
                    if (distance < minDistance) {
                        minDistance = distance;
                        nearestCrime = crime;
                    }
                } catch (NumberFormatException e) {
                    Log.e("CRIME_DISTANCE", "Invalid distance format", e);
                }
            }
        }

        this.nearestCrime = nearestCrime;
        updateNearestCrimeDisplay();
    }

    private void updatePoliceMarkers(List<Place> policeStations) {
        if (mMap == null) return;

        for (Marker marker : policeMarkers.values()) {
            if (marker != null) {
                marker.remove();
            }
        }
        policeMarkers.clear();
        int iconWidth = 80;
        int iconHeight = 80;

        for (Place police : policeStations) {
            if (police != null && police.getLocation() != null) {
                LatLng policeLatLng = new LatLng(
                        police.getLocation().getLatitude(),
                        police.getLocation().getLongitude()
                );

                MarkerOptions markerOptions = new MarkerOptions()
                        .position(policeLatLng)
                        .title(police.getDisplayName())
                        .snippet("Police Station");

                try {
                    Drawable vectorDrawable = ContextCompat.getDrawable(this, R.drawable.policeicon);
                    if (vectorDrawable != null) {
                        Bitmap bitmap = Bitmap.createBitmap(iconWidth, iconHeight, Bitmap.Config.ARGB_8888);
                        Canvas canvas = new Canvas(bitmap);
                        vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                        vectorDrawable.draw(canvas);
                        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(bitmap));
                    } else {
                        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
                    }
                } catch (Exception e) {
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
                }

                Marker marker = mMap.addMarker(markerOptions);
                marker.setVisible(policeCheckbox.isChecked());
                policeMarkers.put(police.getDisplayName(), marker);
            }
        }
    }

    private void updateHospitalMarkers(List<Place> hospitals) {
        if (mMap == null) return;

        for (Marker marker : hospitalMarkers.values()) {
            if (marker != null) {
                marker.remove();
            }
        }
        hospitalMarkers.clear();

        int iconWidth = 76;
        int iconHeight = 80;

        for (Place hospital : hospitals) {
            if (hospital != null && hospital.getLocation() != null) {
                LatLng hospitalLatLng = new LatLng(
                        hospital.getLocation().getLatitude(),
                        hospital.getLocation().getLongitude()
                );

                MarkerOptions markerOptions = new MarkerOptions()
                        .position(hospitalLatLng)
                        .title(hospital.getDisplayName())
                        .snippet("Hospital");

                try {
                    Drawable vectorDrawable = ContextCompat.getDrawable(this, R.drawable.hospitalicon);
                    if (vectorDrawable != null) {
                        Bitmap bitmap = Bitmap.createBitmap(iconWidth, iconHeight, Bitmap.Config.ARGB_8888);
                        Canvas canvas = new Canvas(bitmap);
                        vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                        vectorDrawable.draw(canvas);
                        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(bitmap));
                    } else {
                        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
                    }
                } catch (Exception e) {
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                }

                Marker marker = mMap.addMarker(markerOptions);
                marker.setVisible(hospitalCheckbox.isChecked());
                hospitalMarkers.put(hospital.getDisplayName(), marker); // Using display name as ID
            }
        }
    }

    // Add this method to your MapService class
    private void showPathToPlace(Place.Location placeLocation, String placeType) {
        if (mMap == null || currentLocationLatLng == null || placeLocation == null) return;

        // Clear previous path if any
        if (currentPath != null) {
            currentPath.remove();
        }

        // Create LatLng from Place.Location
        LatLng destinationLatLng = new LatLng(placeLocation.getLatitude(), placeLocation.getLongitude());

        // Hide all markers except user and destination
        for (Marker marker : crimeMarkers.values()) {
            if (marker != null) marker.setVisible(false);
        }
        for (Marker marker : policeMarkers.values()) {
            if (marker != null) marker.setVisible(false);
        }
        for (Marker marker : hospitalMarkers.values()) {
            if (marker != null) marker.setVisible(false);
        }

        // Show user marker
        if (userMarker != null) {
            userMarker.setVisible(true);
        }

        // Find destination marker and make it visible
        Marker destinationMarker = findMarkerByLatLng(destinationLatLng, placeType);
        if (destinationMarker == null) {
            destinationMarker = mMap.addMarker(new MarkerOptions()
                    .position(destinationLatLng)
                    .title(placeType));
        }
        destinationMarker.setVisible(true);

        // Get directions from Google Directions API
        getDirections(currentLocationLatLng, destinationLatLng);
    }

    // Add this new method to get directions from Google Directions API
    private void getDirections(LatLng origin, LatLng destination) {
        SecurePrefs securePrefs = ((SheSecureApp) this.getApplicationContext()).getSecurePrefs();
        String googleApiKey = securePrefs.getGoogleMapsApiKey();

        String url = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=" + origin.latitude + "," + origin.longitude +
                "&destination=" + destination.latitude + "," + destination.longitude +
                "&mode=driving" +
                "&key=" + googleApiKey;

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("DIRECTIONS_API", "Failed to get directions: " + e.getMessage());
                // Fallback to straight line if API fails
                runOnUiThread(() -> drawStraightLine(origin, destination));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    try {
                        List<LatLng> routePoints = parseDirectionsResponse(responseBody);
                        if (!routePoints.isEmpty()) {
                            runOnUiThread(() -> drawRoute(routePoints));
                        } else {
                            // Fallback to straight line if no route found
                            runOnUiThread(() -> drawStraightLine(origin, destination));
                        }
                    } catch (JSONException e) {
                        Log.e("DIRECTIONS_API", "Failed to parse directions response: " + e.getMessage());
                        runOnUiThread(() -> drawStraightLine(origin, destination));
                    }
                } else {
                    Log.e("DIRECTIONS_API", "Directions API response not successful");
                    runOnUiThread(() -> drawStraightLine(origin, destination));
                }
            }
        });
    }

    // Add this method to parse the directions API response
    private List<LatLng> parseDirectionsResponse(String responseBody) throws JSONException {
        List<LatLng> routePoints = new ArrayList<>();

        JSONObject jsonResponse = new JSONObject(responseBody);
        JSONArray routes = jsonResponse.getJSONArray("routes");

        if (routes.length() > 0) {
            JSONObject route = routes.getJSONObject(0);
            JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
            String encodedPolyline = overviewPolyline.getString("points");

            // Decode the polyline
            routePoints = decodePolyline(encodedPolyline);
        }

        return routePoints;
    }

    // Add this method to decode Google's encoded polyline format
    private List<LatLng> decodePolyline(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)), (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }

    //method to draw the actual route
    private void drawRoute(List<LatLng> routePoints) {
        if (mMap == null || routePoints.isEmpty()) return;

        PolylineOptions polylineOptions = new PolylineOptions()
                .addAll(routePoints)
                .width(8)
                .color(Color.BLUE)
                .geodesic(true);

        currentPath = mMap.addPolyline(polylineOptions);

        // Zoom to show the route
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        for (LatLng point : routePoints) {
            boundsBuilder.include(point);
        }

        LatLngBounds bounds = boundsBuilder.build();
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
    }

    // fallback method for straight line (keep your existing logic)
    private void drawStraightLine(LatLng origin, LatLng destination) {
        currentPath = mMap.addPolyline(new PolylineOptions()
                .add(origin, destination)
                .width(5)
                .color(Color.RED) // Different color to indicate it's a fallback
                .geodesic(true));

        // Zoom to show both points
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(
                new LatLngBounds.Builder()
                        .include(origin)
                        .include(destination)
                        .build(), 100));
    }

    private Marker findMarkerByLatLng(LatLng latLng, String placeType) {
        Map<String, Marker> markersToSearch = placeType.equals("Police Station") ? policeMarkers : hospitalMarkers;

        for (Marker marker : markersToSearch.values()) {
            if (marker != null && marker.getPosition().equals(latLng)) {
                return marker;
            }
        }
        return null;
    }

    private void addSafetyRadiusCircle(LatLng position) {
        // Remove existing circles
        if (safetyRadiusCircle != null) {
            safetyRadiusCircle.remove();
        }
        if (staticSafetyCircle != null) {
            staticSafetyCircle.remove();
        }
        if (animatedRippleCircle != null) {
            animatedRippleCircle.remove();
        }

        // Create static background circle (like React's main Circle)
        int backgroundColor;
        if (rippleColor == Color.RED) {
            backgroundColor = Color.argb(51, 255, 0, 0); // 0.2 opacity like React
        } else {
            backgroundColor = Color.argb(51, 0, 255, 0); // 0.2 opacity like React
        }

        staticSafetyCircle = mMap.addCircle(new CircleOptions()
                .center(position)
                .radius(2000) // Fixed radius like React's main circle
                .strokeWidth(2)
                .strokeColor(Color.argb(204, // 0.8 opacity like React
                        Color.red(rippleColor),
                        Color.green(rippleColor),
                        Color.blue(rippleColor)))
                .fillColor(backgroundColor));

        // Create animated ripple circle (starts small and grows)
        animatedRippleCircle = mMap.addCircle(new CircleOptions()
                .center(position)
                .radius(500) // Start with smaller radius
                .strokeWidth(2)
                .strokeColor(Color.argb(204, // 0.8 opacity like React
                        Color.red(rippleColor),
                        Color.green(rippleColor),
                        Color.blue(rippleColor)))
                .fillColor(Color.argb(51, // Same fill as static circle
                        Color.red(rippleColor),
                        Color.green(rippleColor),
                        Color.blue(rippleColor))));

        // Keep reference to static circle for backward compatibility
        safetyRadiusCircle = staticSafetyCircle;

        // Start the simple ripple animation like React
        startSimpleRippleAnimation();
    }

    private void startSimpleRippleAnimation() {
        if (radiusAnimator != null) {
            radiusAnimator.cancel();
        }

        // Create animator that goes from 500 to 2000 (like React's 50 to 200 * 10)
        radiusAnimator = ValueAnimator.ofFloat(500, 2000);
        radiusAnimator.setDuration(3000); // 100ms * 30 iterations ≈ 3000ms
        radiusAnimator.setInterpolator(new LinearInterpolator());
        radiusAnimator.setRepeatCount(ValueAnimator.INFINITE);
        radiusAnimator.setRepeatMode(ValueAnimator.RESTART);

        radiusAnimator.addUpdateListener(animation -> {
            if (animatedRippleCircle != null) {
                float radius = (float) animation.getAnimatedValue();
                animatedRippleCircle.setRadius(radius);
            }
        });

        radiusAnimator.start();
    }

    private void updateSafetyRadiusCircle(LatLng newPosition) {
        // Update static circle position and colors
        if (staticSafetyCircle != null) {
            staticSafetyCircle.setCenter(newPosition);

            int backgroundColor;
            if (rippleColor == Color.RED) {
                backgroundColor = Color.argb(51, 255, 0, 0); // 0.2 opacity
            } else {
                backgroundColor = Color.argb(51, 0, 255, 0); // 0.2 opacity
            }

            staticSafetyCircle.setFillColor(backgroundColor);
            staticSafetyCircle.setStrokeColor(Color.argb(204, // 0.8 opacity
                    Color.red(rippleColor),
                    Color.green(rippleColor),
                    Color.blue(rippleColor)));
        }

        // Update animated ripple circle position and colors
        if (animatedRippleCircle != null) {
            animatedRippleCircle.setCenter(newPosition);

            animatedRippleCircle.setStrokeColor(Color.argb(204, // 0.8 opacity
                    Color.red(rippleColor),
                    Color.green(rippleColor),
                    Color.blue(rippleColor)));

            animatedRippleCircle.setFillColor(Color.argb(51, // Same fill as static
                    Color.red(rippleColor),
                    Color.green(rippleColor),
                    Color.blue(rippleColor)));
        }
    }

    private void animateMarker(final Marker marker, final LatLng finalPosition) {
        final LatLng startPosition = marker.getPosition();
        final LinearInterpolator interpolator = new LinearInterpolator();
        final long startTime = System.currentTimeMillis();

        final Handler handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = System.currentTimeMillis() - startTime;
                float t = interpolator.getInterpolation(
                        (float) elapsed / MARKER_ANIMATION_DURATION);

                if (t < 1.0f) {
                    double lat = startPosition.latitude +
                            (finalPosition.latitude - startPosition.latitude) * t;
                    double lng = startPosition.longitude +
                            (finalPosition.longitude - startPosition.longitude) * t;
                    marker.setPosition(new LatLng(lat, lng));
                    handler.postDelayed(this, 16);
                } else {
                    marker.setPosition(finalPosition);
                }
            }
        });
    }

    private void updateNearestCrimeDisplay() {
        if (nearestCrime == null || currentLocation == null) {
            rippleColor = Color.GREEN;
            safetyPercentageText.setText("100% Safe");
            nearestCrimeBanner.setCardBackgroundColor(ContextCompat.getColor(this, R.color.green));
            return;
        }

        nearestCrimeBanner.setVisibility(View.VISIBLE);
        nearestCrimeType.setText(nearestCrime.getTypeOfCrime());
        nearestCrimeDescription.setText(nearestCrime.getDescription());

        try {
            float distanceInKm = Float.parseFloat(nearestCrime.getDistance());
            float distanceInMeters = distanceInKm * 1000;

            String distanceText;
            if (distanceInKm < 1) {
                distanceText = String.format(Locale.getDefault(), "%d M", (int)distanceInMeters);
            } else {
                distanceText = String.format(Locale.getDefault(), "%.2f KM", distanceInKm);
            }
            nearestCrimeDistance.setText(distanceText);

            float safetyPercentage;
            if (distanceInMeters <= 500) {
                safetyPercentage = 50 * (distanceInMeters / 500);
            } else {
                safetyPercentage = 50 + (50 * ((distanceInMeters - 500) / 1500));
            }
            safetyPercentage = Math.max(0, Math.min(100, safetyPercentage));
            safetyPercentageText.setText(String.format(Locale.getDefault(), "%.0f%% Safe", safetyPercentage));

            rippleColor = (distanceInMeters <= 500) ? Color.RED : Color.GREEN;

            float ratio;
            if (distanceInMeters <= 500) {
                ratio = 0.5f + (0.5f * (distanceInMeters / 500));
            } else {
                ratio = 0.5f * ((distanceInMeters - 500) / 1500);
            }

            int red = (int) (255 * ratio);
            int green = (int) (255 * (1 - ratio));
            int bannerColor = Color.argb(230, red, green, 0);
            nearestCrimeBanner.setCardBackgroundColor(bannerColor);

        } catch (NumberFormatException e) {
            e.printStackTrace();
            nearestCrimeDistance.setText(nearestCrime.getDistance() + " KM");
            safetyPercentageText.setText("N/A");
            rippleColor = Color.YELLOW;
            nearestCrimeBanner.setCardBackgroundColor(ContextCompat.getColor(this, R.color.yellow_dark));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mMap != null) {
            List<CrimeReport> currentCrimes = LocationDataManager.getInstance().getCurrentCrimes();
            if (currentCrimes != null && !currentCrimes.isEmpty()) {
                updateMapWithCrimes(currentCrimes);
            }
        }
        LocationDataManager.getInstance().forceUpdateAllLocations();
    }
}