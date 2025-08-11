package com.example.shesecure.activities;

import android.Manifest;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MapService extends BaseActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Marker userMarker;
    private FusedLocationProviderClient fusedLocationClient;
    private String firstName, lastName, profileImage, authToken;
    private Map<String, Marker> crimeMarkers = new HashMap<>();
    private Map<String, Marker> policeMarkers = new HashMap<>();
    private Map<String, Marker> hospitalMarkers = new HashMap<>();
    private List<CrimeReport> crimes = new ArrayList<>();
    private static final int MARKER_ANIMATION_DURATION = 1000;
    private static final int CAMERA_ANIMATION_DURATION = 1500;
    private static final float DEFAULT_ZOOM = 15f;
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
        firstName = authManager.getFirstName();
        lastName = authManager.getLastName();
        profileImage = authManager.getProfileImage();

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
                    Drawable vectorDrawable = ContextCompat.getDrawable(this, R.drawable.police_ic);
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
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
                }

                Marker marker = mMap.addMarker(markerOptions);
                marker.setVisible(policeCheckbox.isChecked());
                policeMarkers.put(police.getDisplayName(), marker); // Using display name as ID
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
                    Drawable vectorDrawable = ContextCompat.getDrawable(this, R.drawable.health);
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
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                }

                Marker marker = mMap.addMarker(markerOptions);
                marker.setVisible(hospitalCheckbox.isChecked());
                hospitalMarkers.put(hospital.getDisplayName(), marker); // Using display name as ID
            }
        }
    }

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
            // If marker not found, create a temporary one
            destinationMarker = mMap.addMarker(new MarkerOptions()
                    .position(destinationLatLng)
                    .title(placeType));
        }
        destinationMarker.setVisible(true);

        // Draw path (in a real app, you would use Directions API here)
        currentPath = mMap.addPolyline(new PolylineOptions()
                .add(currentLocationLatLng, destinationLatLng)
                .width(5)
                .color(Color.BLUE));

        // Zoom to show both points
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(
                new LatLngBounds.Builder()
                        .include(currentLocationLatLng)
                        .include(destinationLatLng)
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
        if (safetyRadiusCircle != null) {
            safetyRadiusCircle.remove();
        }

        int backgroundColor;
        if (rippleColor == Color.RED) {
            backgroundColor = Color.argb(60, 255, 100, 100);
        } else {
            backgroundColor = Color.argb(60, 100, 255, 100);
        }

        safetyRadiusCircle = mMap.addCircle(new CircleOptions()
                .center(position)
                .radius(2000)
                .strokeWidth(2)
                .strokeColor(Color.argb(100,
                        Color.red(rippleColor),
                        Color.green(rippleColor),
                        Color.blue(rippleColor)))
                .fillColor(backgroundColor));

        startContinuousRippleAnimation(position);
    }

    private void updateSafetyRadiusCircle(LatLng newPosition) {
        if (safetyRadiusCircle != null) {
            safetyRadiusCircle.setCenter(newPosition);

            int backgroundColor;
            if (rippleColor == Color.RED) {
                backgroundColor = Color.argb(60, 255, 100, 100);
            } else {
                backgroundColor = Color.argb(60, 100, 255, 100);
            }
            safetyRadiusCircle.setFillColor(backgroundColor);

            safetyRadiusCircle.setStrokeColor(Color.argb(100,
                    Color.red(rippleColor),
                    Color.green(rippleColor),
                    Color.blue(rippleColor)));
        }
    }

    private void startContinuousRippleAnimation(LatLng center) {
        if (radiusAnimator != null) {
            radiusAnimator.cancel();
        }

        for (int i = 0; i < 4; i++) {
            final int rippleIndex = i;
            new Handler().postDelayed(() -> {
                createContinuousRipple(center, rippleIndex);
            }, i * 800);
        }
    }

    private void createContinuousRipple(LatLng center, int rippleIndex) {
        final Circle rippleCircle = mMap.addCircle(new CircleOptions()
                .center(center)
                .radius(100)
                .strokeWidth(4)
                .strokeColor(Color.argb(180,
                        Color.red(rippleColor),
                        Color.green(rippleColor),
                        Color.blue(rippleColor)))
                .fillColor(Color.argb(0, 0, 0, 0)));

        ValueAnimator animator = ValueAnimator.ofFloat(100, 2000);
        animator.setDuration(3000);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());

        animator.addUpdateListener(animation -> {
            float radius = (float) animation.getAnimatedValue();
            rippleCircle.setRadius(radius);

            float progress = (radius - 100) / (2000 - 100);
            float fadeProgress = 1 - (progress * progress);
            int alpha = (int) (180 * fadeProgress);

            rippleCircle.setStrokeColor(Color.argb(Math.max(0, alpha),
                    Color.red(rippleColor),
                    Color.green(rippleColor),
                    Color.blue(rippleColor)));
        });

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                rippleCircle.remove();
                new Handler().postDelayed(() -> {
                    if (mMap != null && currentLocation != null) {
                        LatLng currentPos = new LatLng(currentLocation.getLatitude(),
                                currentLocation.getLongitude());
                        createContinuousRipple(currentPos, rippleIndex);
                    }
                }, 300);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                rippleCircle.remove();
            }
        });

        animator.start();
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
            nearestCrimeBanner.setVisibility(View.GONE);
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