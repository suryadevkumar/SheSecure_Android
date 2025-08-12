package com.example.shesecure.activities;

import android.Manifest;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.shesecure.R;
import com.example.shesecure.SheSecureApp;
import com.example.shesecure.adapters.CommentsAdapter;
import com.example.shesecure.adapters.CrimeInfoWindowAdapter;
import com.example.shesecure.adapters.ImageGridAdapter;
import com.example.shesecure.adapters.PlaceAdapter;
import com.example.shesecure.adapters.VideoGridAdapter;
import com.example.shesecure.dialogs.ContactSelectionDialog;
import com.example.shesecure.models.AdditionalDetails;
import com.example.shesecure.models.CrimeReport;
import com.example.shesecure.models.EmergencyContact;
import com.example.shesecure.models.EmergencyContactResponse;
import com.example.shesecure.models.User;
import com.example.shesecure.services.ApiService;
import com.example.shesecure.services.LiveLocationService;
import com.example.shesecure.socket.SocketManager;
import com.example.shesecure.utils.ApiUtils;
import com.example.shesecure.utils.AuthManager;
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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserDashboardActivity extends BaseActivity implements OnMapReadyCallback, CrimeInfoWindowAdapter.OnInfoWindowClickListener {

    private GoogleMap mMap;
    private Marker userMarker;
    private FusedLocationProviderClient fusedLocationClient;
    private boolean isSOSActive = false;
    private boolean isLocationShared = false;
    private boolean isLoading = false;
    private SocketManager socketManager;
    private String shareId;
    private String firstName, lastName, profileImage, authToken;
    private ApiService apiService;
    private CrimeInfoWindowAdapter infoWindowAdapter;
    private List<ApiService.CrimeInteractionStats> crimeInteractions = new ArrayList<>();
    private Map<String, Marker> crimeMarkers = new HashMap<>();
    private List<CrimeReport> crimes = new ArrayList<>();
    // Animation duration in milliseconds
    private static final int MARKER_ANIMATION_DURATION = 1000;
    private static final int CAMERA_ANIMATION_DURATION = 1500;
    private static final float DEFAULT_ZOOM = 15f;
    private Circle safetyRadiusCircle;
    private ValueAnimator radiusAnimator;
    private Circle staticSafetyCircle;
    private Circle animatedRippleCircle;
    private CrimeReport nearestCrime;
    private RecyclerView policeStationsList;
    private RecyclerView hospitalsList;
    private PlaceAdapter policeStationsAdapter;
    private PlaceAdapter hospitalsAdapter;
    private CardView nearestCrimeBanner;
    private TextView nearestCrimeType, nearestCrimeDescription, safetyPercentageText, nearestCrimeDistance;
    private int rippleColor = Color.GREEN;
    private final MutableLiveData<Location> locationObserver = new MutableLiveData<>();
    private static final int SMS_PERMISSION_REQUEST_CODE = 1001;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_dashboard);

        apiService = ApiUtils.initializeApiService(this, ApiService.class);
        SecurePrefs securePrefs = ((SheSecureApp) this.getApplicationContext()).getSecurePrefs();
        String googleApiKey = securePrefs.getGoogleMapsApiKey();

        authToken = "Bearer " + authManager.getToken();
        firstName = authManager.getFirstName();
        lastName = authManager.getLastName();
        profileImage = authManager.getProfileImage();

        // Initialize CrimeLocationManager
        LocationDataManager.getInstance().initialize(apiService, authToken, googleApiKey);

        if (LocationDataManager.getInstance().getCurrentCrimes() != null &&
                !LocationDataManager.getInstance().getCurrentCrimes().isEmpty()) {
            LocationDataManager.getInstance().forceUpdateAllLocations();
        }

        // Observe crime locations
        observeCrimeLocations();

        policeStationsList = findViewById(R.id.police_stations_list);
        hospitalsList = findViewById(R.id.hospitals_list);

        policeStationsAdapter = new PlaceAdapter(new ArrayList<>(), place -> {});;
        hospitalsAdapter = new PlaceAdapter(new ArrayList<>(), place -> {});;

        policeStationsList.setLayoutManager(new LinearLayoutManager(this));
        hospitalsList.setLayoutManager(new LinearLayoutManager(this));

        policeStationsList.setAdapter(policeStationsAdapter);
        hospitalsList.setAdapter(hospitalsAdapter);

        observePlacesData();

        nearestCrimeBanner = findViewById(R.id.nearest_crime_banner);
        nearestCrimeType = findViewById(R.id.nearest_crime_type);
        nearestCrimeDescription = findViewById(R.id.nearest_crime_description);
        safetyPercentageText = findViewById(R.id.safety_percentage);
        nearestCrimeDistance = findViewById(R.id.nearest_crime_distance);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        socketManager = SocketManager.getInstance(this);
        isLocationShared = AuthManager.getLiveLocationShareId(this) != null;
        updateLocationButton();
        setupMap();
        setupButtons();
    }

    @Override
    protected void onLocationUpdated(@NonNull Location location) {
        Log.d("LOCATION_UPDATE", "New location: " + location.getLatitude() + ", " + location.getLongitude());
        updateMapWithLocation(location);
    }

    private void observeCrimeLocations() {
        // Remove any existing observers to avoid duplicates
        LocationDataManager.getInstance().getNearestCrimes().removeObservers(this);

        // Observe crime locations
        LocationDataManager.getInstance().getNearestCrimes().observe(this, newCrimes -> {
            Log.d("CRIME_OBSERVER", "Crime data updated: " + (newCrimes != null ? newCrimes.size() : "null"));

            if (newCrimes != null && !newCrimes.isEmpty()) {
                crimes = newCrimes;
                if (mMap != null) {  // Ensure map is ready
                    updateMapWithCrimes(newCrimes);
                }
            } else {
                // If no new crimes, try to use current crimes
                List<CrimeReport> current = LocationDataManager.getInstance().getCurrentCrimes();
                if (current != null && !current.isEmpty()) {
                    crimes = current;
                    if (mMap != null) {  // Ensure map is ready
                        updateMapWithCrimes(current);
                    }
                }
            }
        });

        // Observe our local location updates
        locationObserver.removeObservers(this);
        locationObserver.observe(this, location -> {
            if (location != null) {
                LocationDataManager.getInstance().updateUserLocation(location);
            }
        });
    }

    private void observePlacesData() {
        // Observe police stations
        LocationDataManager.getInstance().getNearestPoliceStations().observe(this, policeStations -> {
            if (policeStations != null && !policeStations.isEmpty()) {
                policeStationsAdapter.updatePlaces(policeStations);
                findViewById(R.id.police_stations_card).setVisibility(View.VISIBLE);
            } else {
                policeStationsAdapter.updatePlaces(new ArrayList<>());
                findViewById(R.id.police_stations_card).setVisibility(View.GONE);
            }
        });

        // Observe hospitals
        LocationDataManager.getInstance().getNearestHospitals().observe(this, hospitals -> {
            if (hospitals != null && !hospitals.isEmpty()) {
                hospitalsAdapter.updatePlaces(hospitals);
                findViewById(R.id.hospitals_card).setVisibility(View.VISIBLE);
            } else {
                hospitalsAdapter.updatePlaces(new ArrayList<>());
                findViewById(R.id.hospitals_card).setVisibility(View.GONE);
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

        // Initialize adapter with click listener
        infoWindowAdapter = new CrimeInfoWindowAdapter(this, this);
        mMap.setInfoWindowAdapter(infoWindowAdapter);

        mMap.setOnMarkerClickListener(marker -> {
            if (crimeMarkers.containsValue(marker)) {
                String crimeId = getCrimeIdFromMarker(marker);
                if (crimeId != null) {
                    CrimeReport crime = getCrimeById(crimeId);
                    ApiService.CrimeInteractionStats stats = getStatsForCrime(crimeId);

                    if (crime != null && stats != null) {
                        infoWindowAdapter.setCrimeData(crime, stats, marker);
                        marker.showInfoWindow();
                        return true;
                    }
                }
            }
            return false;
        });

        // Info window click listener
        mMap.setOnInfoWindowClickListener(marker -> {
            if (marker.getTag() instanceof CrimeReport) {
                CrimeReport crime = (CrimeReport) marker.getTag();
                onInfoWindowClick(crime);
            }
        });

        loadCrimeInteractions();

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

        // ADD THIS: Check if we already have crime data and update map
        List<CrimeReport> existingCrimes = LocationDataManager.getInstance().getCurrentCrimes();
        if (existingCrimes != null && !existingCrimes.isEmpty()) {
            Log.d("MAP_READY", "Updating map with existing crimes: " + existingCrimes.size());
            updateMapWithCrimes(existingCrimes);
        }
    }

    @Override
    public void onInfoWindowClick(CrimeReport crime) {
        showCrimeDetailsDialog(crime);
    }

    private void updateMapWithLocation(Location location) {
        if (mMap != null) {
            LatLng newPosition  = new LatLng(location.getLatitude(), location.getLongitude());

            if (userMarker == null) {
                userMarker = mMap.addMarker(new MarkerOptions()
                        .position(newPosition )
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

    private void updateMapWithCrimes(List<CrimeReport> crimes) {
        if (mMap == null || crimes == null) return;

        // Clear existing crime markers
        for (Marker marker : crimeMarkers.values()) {
            if (marker != null) {
                marker.remove();
            }
        }
        crimeMarkers.clear();

        // Variables to track nearest crime
        CrimeReport nearestCrime = null;
        float minDistance = Float.MAX_VALUE;

        // Add new crime markers and find nearest crime in single iteration
        for (CrimeReport crime : crimes) {
            if (crime != null && crime.getLocation() != null && crime.getId() != null) {
                LatLng crimeLatLng = new LatLng(
                        crime.getLocation().getLatitude(),
                        crime.getLocation().getLongitude()
                );

                // Create marker
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
                    Log.e("MAP_MARKER", "Failed to create marker icon", e);
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                }

                Marker marker = mMap.addMarker(markerOptions);
                crimeMarkers.put(crime.getId(), marker);

                // Check if this is the nearest crime
                try {
                    float distance = Float.parseFloat(crime.getDistance());
                    if (distance < minDistance) {
                        minDistance = distance;
                        nearestCrime = crime;
                    }
                } catch (NumberFormatException e) {
                    Log.e("CRIME_DISTANCE", "Invalid distance format for crime: " + crime.getId(), e);
                }
            }
        }

        // Update nearest crime
        this.nearestCrime = nearestCrime;
        updateNearestCrimeDisplay();
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

            // Format distance display - meters when <1km, km when >=1km
            String distanceText;
            if (distanceInKm < 1) {
                distanceText = String.format(Locale.getDefault(), "%d M", (int)distanceInMeters);
            } else {
                distanceText = String.format(Locale.getDefault(), "%.2f KM", distanceInKm);
            }
            nearestCrimeDistance.setText(distanceText);

            // Non-linear safety calculation
            float safetyPercentage;
            if (distanceInMeters <= 500) {
                safetyPercentage = 50 * (distanceInMeters / 500);
            } else {
                safetyPercentage = 50 + (50 * ((distanceInMeters - 500) / 1500));
            }
            safetyPercentage = Math.max(0, Math.min(100, safetyPercentage));
            safetyPercentageText.setText(String.format(Locale.getDefault(), "%.0f%% Safe", safetyPercentage));

            // Set ripple color - pure red if within 500m, otherwise pure green
            rippleColor = (distanceInMeters <= 500) ? Color.RED : Color.GREEN;

            // Banner color
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

    private void loadCrimeInteractions() {
        apiService.getCrimeInteractions(authToken).enqueue(new Callback<List<ApiService.CrimeInteractionStats>>() {
            @Override
            public void onResponse(Call<List<ApiService.CrimeInteractionStats>> call,
                                   Response<List<ApiService.CrimeInteractionStats>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    crimeInteractions = response.body();
                    updateCrimeMarkersWithStats();
                }
            }

            @Override
            public void onFailure(Call<List<ApiService.CrimeInteractionStats>> call, Throwable t) {
                Log.e("CrimeInteractions", "Failed to load crime interactions", t);
            }
        });
    }

    private void updateCrimeMarkersWithStats() {
        for (Map.Entry<String, Marker> entry : crimeMarkers.entrySet()) {
            String crimeId = entry.getKey();
            Marker marker = entry.getValue();
            ApiService.CrimeInteractionStats stats = getStatsForCrime(crimeId);

            // You can update marker appearance based on stats if needed
        }
    }

    private CrimeReport getCrimeById(String crimeId) {
        if (crimeId == null || crimes == null) {
            return null;
        }

        for (CrimeReport crime : crimes) {
            if (crime != null && crimeId.equals(crime.getId())) {
                return crime;
            }
        }
        return null;
    }

    private String getCrimeIdFromMarker(Marker marker) {
        if (marker == null || crimeMarkers == null) {
            return null;
        }

        for (Map.Entry<String, Marker> entry : crimeMarkers.entrySet()) {
            if (entry.getValue() != null && entry.getValue().equals(marker)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public void showCrimeDetailsDialog(CrimeReport crime) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_crime_details, null);
        builder.setView(dialogView);

        // Initialize views
        TextView crimeType = dialogView.findViewById(R.id.crime_type);
        TextView crimeDesc = dialogView.findViewById(R.id.crime_description);
        RecyclerView imagesRecycler = dialogView.findViewById(R.id.images_recycler);
        RecyclerView videosRecycler = dialogView.findViewById(R.id.videos_recycler);
        TextView imagesLabel = dialogView.findViewById(R.id.images_label);
        TextView videosLabel = dialogView.findViewById(R.id.videos_label);
        TextView likesCount = dialogView.findViewById(R.id.likes_count);
        TextView dislikesCount = dialogView.findViewById(R.id.dislikes_count);
        Button likeBtn = dialogView.findViewById(R.id.like_button);
        Button dislikeBtn = dialogView.findViewById(R.id.dislike_button);
        RecyclerView commentsRecycler = dialogView.findViewById(R.id.comments_recycler);
        EditText commentInput = dialogView.findViewById(R.id.comment_input);
        Button postCommentBtn = dialogView.findViewById(R.id.post_comment_button);

        // Set crime data
        crimeType.setText(crime.getTypeOfCrime());
        crimeDesc.setText(crime.getDescription());

        // Setup images grid
        if (crime.getPhotoUrls() != null && !crime.getPhotoUrls().isEmpty()) {
            imagesLabel.setVisibility(View.VISIBLE);
            imagesRecycler.setVisibility(View.VISIBLE);

            imagesRecycler.setLayoutManager(new GridLayoutManager(this, 3));
            ImageGridAdapter imageAdapter = new ImageGridAdapter(this, crime.getPhotoUrls());
            imagesRecycler.setAdapter(imageAdapter);
        }

        // Setup videos grid (assuming you have a getVideoUrls() method)
        if (crime.getVideoUrls() != null && !crime.getVideoUrls().isEmpty()) {
            videosLabel.setVisibility(View.VISIBLE);
            videosRecycler.setVisibility(View.VISIBLE);

            videosRecycler.setLayoutManager(new GridLayoutManager(this, 2));
            VideoGridAdapter videoAdapter = new VideoGridAdapter(this, crime.getVideoUrls());
            videosRecycler.setAdapter(videoAdapter);
        }

        // Load interaction details
        loadCrimeInteractionDetails(crime.getId(), likesCount, dislikesCount,
                likeBtn, dislikeBtn, commentsRecycler);

        // Set up comment posting
        postCommentBtn.setOnClickListener(v -> {
            String comment = commentInput.getText().toString().trim();
            if (!comment.isEmpty()) {
                postCrimeComment(crime.getId(), comment, commentsRecycler);
                commentInput.setText("");
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void loadCrimeInteractionDetails(String crimeId, TextView likesCount,
                                             TextView dislikesCount, Button likeBtn, Button dislikeBtn,
                                             RecyclerView commentsRecycler) {

        apiService.getCrimeInteractionDetails(authToken, crimeId)
                .enqueue(new Callback<ApiService.CrimeInteractionDetails>() {
                    @Override
                    public void onResponse(Call<ApiService.CrimeInteractionDetails> call,
                                           Response<ApiService.CrimeInteractionDetails> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            ApiService.CrimeInteractionDetails details = response.body();

                            likesCount.setText(String.valueOf(details.supports));
                            dislikesCount.setText(String.valueOf(details.unsupports));

                            // Set up like/dislike buttons
                            setupInteractionButtons(likeBtn, dislikeBtn, details.userInteraction, crimeId, likesCount, dislikesCount);

                            // Set up comments recycler
                            setupCommentsRecycler(commentsRecycler, details.comments);
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiService.CrimeInteractionDetails> call, Throwable t) {
                        Log.e("CrimeDetails", "Failed to load interaction details", t);
                    }
                });
    }

    private void setupInteractionButtons(Button likeBtn, Button dislikeBtn,
                                         ApiService.UserInteraction userInteraction,
                                         String crimeId,
                                         TextView likesCount,
                                         TextView dislikesCount) {

        // Set initial state
        if (userInteraction != null) {
            if ("Support".equals(userInteraction.supportStatus)) {
                likeBtn.setSelected(true);
                dislikeBtn.setSelected(false);
                likeBtn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.blue_600)));
                dislikeBtn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.red_400)));
            } else if ("Unsupport".equals(userInteraction.supportStatus)) {
                likeBtn.setSelected(false);
                dislikeBtn.setSelected(true);
                likeBtn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.blue_400)));
                dislikeBtn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.red_600)));
            }
        } else {
            likeBtn.setSelected(false);
            dislikeBtn.setSelected(false);
            likeBtn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.blue_400)));
            dislikeBtn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.red_400)));
        }

        likeBtn.setOnClickListener(v -> {
            if (likeBtn.isSelected()) {
                // Already liked - remove like
                interactWithCrime(crimeId, "None", likeBtn, dislikeBtn, likesCount, dislikesCount);
            } else {
                // Like the post
                interactWithCrime(crimeId, "Support", likeBtn, dislikeBtn, likesCount, dislikesCount);
            }
        });

        dislikeBtn.setOnClickListener(v -> {
            if (dislikeBtn.isSelected()) {
                // Already disliked - remove dislike
                interactWithCrime(crimeId, "None", likeBtn, dislikeBtn, likesCount, dislikesCount);
            } else {
                // Dislike the post
                interactWithCrime(crimeId, "Unsupport", likeBtn, dislikeBtn, likesCount, dislikesCount);
            }
        });
    }

    private void interactWithCrime(String crimeId, String action,
                                   Button likeBtn, Button dislikeBtn,
                                   TextView likesCount, TextView dislikesCount) {
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("supportStatus", action);

            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json"),
                    requestBody.toString()
            );

            apiService.interactWithCrime(authToken, crimeId, body)
                    .enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call,
                                               Response<ResponseBody> response) {
                            if (response.isSuccessful()) {
                                // Update UI based on action
                                int currentLikes = Integer.parseInt(likesCount.getText().toString());
                                int currentDislikes = Integer.parseInt(dislikesCount.getText().toString());

                                switch (action) {
                                    case "Support":
                                        // If already liked, this is a toggle to remove like
                                        if (likeBtn.isSelected()) {
                                            likesCount.setText(String.valueOf(currentLikes - 1));
                                            likeBtn.setSelected(false);
                                            likeBtn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(UserDashboardActivity.this, R.color.blue_400)));
                                        } else {
                                            // Like the post
                                            likesCount.setText(String.valueOf(currentLikes + 1));
                                            likeBtn.setSelected(true);
                                            likeBtn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(UserDashboardActivity.this, R.color.blue_600)));

                                            // If was previously disliked, remove dislike
                                            if (dislikeBtn.isSelected()) {
                                                dislikesCount.setText(String.valueOf(currentDislikes - 1));
                                                dislikeBtn.setSelected(false);
                                                dislikeBtn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(UserDashboardActivity.this, R.color.red_400)));
                                            }
                                        }
                                        break;

                                    case "Unsupport":
                                        // If already disliked, this is a toggle to remove dislike
                                        if (dislikeBtn.isSelected()) {
                                            dislikesCount.setText(String.valueOf(currentDislikes - 1));
                                            dislikeBtn.setSelected(false);
                                            dislikeBtn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(UserDashboardActivity.this, R.color.red_400)));
                                        } else {
                                            // Dislike the post
                                            dislikesCount.setText(String.valueOf(currentDislikes + 1));
                                            dislikeBtn.setSelected(true);
                                            dislikeBtn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(UserDashboardActivity.this, R.color.blue_600)));

                                            // If was previously liked, remove like
                                            if (likeBtn.isSelected()) {
                                                likesCount.setText(String.valueOf(currentLikes - 1));
                                                likeBtn.setSelected(false);
                                                likeBtn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(UserDashboardActivity.this, R.color.red_400)));
                                            }
                                        }
                                        break;

                                    case "None":
                                        // Remove any existing interaction
                                        if (likeBtn.isSelected()) {
                                            likesCount.setText(String.valueOf(currentLikes - 1));
                                            likeBtn.setSelected(false);
                                            likeBtn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(UserDashboardActivity.this, R.color.blue_400)));
                                        } else if (dislikeBtn.isSelected()) {
                                            dislikesCount.setText(String.valueOf(currentDislikes - 1));
                                            dislikeBtn.setSelected(false);
                                            dislikeBtn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(UserDashboardActivity.this, R.color.red_400)));
                                        }
                                        break;
                                }
                            }
                        }

                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                            Log.e("CrimeInteraction", "Failed to interact with crime", t);
                            Toast.makeText(UserDashboardActivity.this, "Failed to update interaction", Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupCommentsRecycler(RecyclerView recyclerView, List<ApiService.CrimeComment> comments) {
        CommentsAdapter adapter = new CommentsAdapter(comments);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void postCrimeComment(String crimeId, String comment, RecyclerView commentsRecycler) {
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("comment", comment);

            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json"),
                    requestBody.toString()
            );

            apiService.postCrimeComment(authToken, crimeId, body)
                    .enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call,
                                               Response<ResponseBody> response) {
                            if (response.isSuccessful()) {
                                // Create new comment
                                ApiService.CrimeComment newComment = new ApiService.CrimeComment();
                                newComment.text = comment;
                                newComment.createdAt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(new Date());

                                // Create user from model package
                                User user = new User();
                                user.setFirstName(firstName);
                                user.setLastName(lastName);

                                // Create additional details
                                AdditionalDetails details = new AdditionalDetails();
                                details.setImage(profileImage);
                                user.setAdditionalDetails(details);

                                // Set user to comment
                                newComment.user = user;

                                // Update UI
                                CommentsAdapter adapter = (CommentsAdapter) commentsRecycler.getAdapter();
                                if (adapter != null) {
                                    adapter.addComment(newComment);
                                    commentsRecycler.smoothScrollToPosition(0);
                                }
                            } else {
                                Toast.makeText(UserDashboardActivity.this,
                                        "Failed to post comment", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                            Log.e("CrimeComment", "Failed to post comment", t);
                            Toast.makeText(UserDashboardActivity.this,
                                    "Network error", Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error creating comment", Toast.LENGTH_SHORT).show();
        }
    }

    private ApiService.CrimeInteractionStats getStatsForCrime(String crimeId) {
        for (ApiService.CrimeInteractionStats stats : crimeInteractions) {
            if (stats.crimeId.equals(crimeId)) {
                return stats;
            }
        }
        return null;
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

    private void setupButtons() {
        CardView trustedContactsBtn = findViewById(R.id.trustedContactsBtn);
        trustedContactsBtn.setOnClickListener(v -> {
            startActivity(new Intent(this, EmergencyContactsActivity.class));
        });

        CardView sosBtn = findViewById(R.id.sosBtn);
        sosBtn.setOnClickListener(v -> {
            isSOSActive = !isSOSActive;
            updateSOSButton();
            if (isSOSActive) {
                startSOS();
            } else {
                stopSOS();
            }
        });

        CardView locationBtn = findViewById(R.id.locationBtn);
        locationBtn.setOnClickListener(v -> {
            if (isLocationShared) {
                stopLiveLocationSharing();
            } else {
                isLoading = true;
                updateLocationButton();
                shareLiveLocation();
            }
        });

        CardView helplineBtn = findViewById(R.id.helplineBtn);
        helplineBtn.setOnClickListener(v -> {
            startActivity(new Intent(this, HelplineActivity.class));
        });
    }

    private void updateSOSButton() {
        CardView sosCard = findViewById(R.id.sosBtn);
        TextView textView = sosCard.findViewById(R.id.emergencySOSText);
        ImageView iconView = sosCard.findViewById(R.id.emergencySOSIcon);

        if (isSOSActive) {
            sosCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.red_600));
            textView.setTextColor(ContextCompat.getColor(this, R.color.white));
            iconView.setColorFilter(ContextCompat.getColor(this, R.color.white));
            // Update the circle background if needed
            iconView.setBackground(ContextCompat.getDrawable(this, R.drawable.circle_bg));
        } else {
            sosCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.white));
            textView.setTextColor(ContextCompat.getColor(this, R.color.black));
            iconView.setColorFilter(ContextCompat.getColor(this, R.color.red_600));
            // Update the circle background if needed
            iconView.setBackground(ContextCompat.getDrawable(this, R.drawable.circle_bg));
        }
    }

    private void updateLocationButton() {
        runOnUiThread(() -> {
            CardView locationCard = findViewById(R.id.locationBtn);
            TextView textView = locationCard.findViewById(R.id.liveLocationText);
            ImageView iconView = locationCard.findViewById(R.id.liveLocationIcon);

            if (isLoading) {
                textView.setText("Starting...");
                locationCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.gray_400));
                iconView.setColorFilter(ContextCompat.getColor(this, R.color.gray_600));
            } else if (isLocationShared) {
                locationCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.blue_600));
                textView.setTextColor(ContextCompat.getColor(this, R.color.white));
                textView.setText("Stop Sharing");
                iconView.setColorFilter(ContextCompat.getColor(this, R.color.white));
            } else {
                locationCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.white));
                textView.setTextColor(ContextCompat.getColor(this, R.color.black));
                textView.setText("Share Live Location");
                iconView.setColorFilter(ContextCompat.getColor(this, R.color.blue));
            }
        });
    }

    private void startSOS() {
        Toast.makeText(this, "Emergency SOS Activated", Toast.LENGTH_SHORT).show();
        // Implement actual SOS functionality
    }

    private void stopSOS() {
        Toast.makeText(this, "Emergency SOS Deactivated", Toast.LENGTH_SHORT).show();
        // Implement stop SOS functionality
    }

    private void shareLiveLocation() {
        isLocationShared = AuthManager.getLiveLocationShareId(this) != null;
        if (!isLocationShared) {
            // Check SMS permission first
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                    == PackageManager.PERMISSION_GRANTED) {
                showContactSelectionDialog();
            } else {
                // Request SMS permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.SEND_SMS},
                        SMS_PERMISSION_REQUEST_CODE);
            }
        } else {
            stopLiveLocationSharing();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with location sharing
                showContactSelectionDialog();
            } else {
                // Permission denied
                Toast.makeText(this, "SMS permission is required to share location", Toast.LENGTH_SHORT).show();
                isLoading = false;
                updateLocationButton();
            }
        }
    }

    private void showContactSelectionDialog() {
        // Load contacts first
        loadContacts(new ContactsLoadCallback() {
            @Override
            public void onContactsLoaded(List<EmergencyContact> contacts) {
                if (contacts.isEmpty()) {
                    Toast.makeText(UserDashboardActivity.this,
                            "No emergency contacts found", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Show dialog to select contacts
                ContactSelectionDialog dialog = new ContactSelectionDialog(
                        UserDashboardActivity.this,
                        contacts,
                        new ContactSelectionDialog.ContactSelectionListener() {
                            @Override
                            public void onContactsSelected(List<String> selectedContactNumbers) {
                                startLiveLocationSharing(selectedContactNumbers);
                            }

                            @Override
                            public void onCancelled() {
                                isLoading = false;
                                updateLocationButton();
                            }
                        });

                dialog.show();
            }

            @Override
            public void onLoadFailed(String error) {
                Toast.makeText(UserDashboardActivity.this,
                        "Failed to load contacts: " + error, Toast.LENGTH_SHORT).show();
                isLoading = false;
                updateLocationButton();
            }
        });
    }

    private void startLiveLocationSharing(List<String> contactNumbers) {
        // Double check permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "SMS permission required", Toast.LENGTH_SHORT).show();
            isLoading = false;
            updateLocationButton();
            return;
        }

        isLoading = true;
        updateLocationButton();

        // Generate share ID
        shareId = UUID.randomUUID().toString();

        // Start the live location service
        Intent serviceIntent = new Intent(this, LiveLocationService.class);
        serviceIntent.setAction("START_SHARING");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        // Send SMS to selected contacts
        sendLocationShareSMS(contactNumbers, shareId);

        // Update UI
        isLocationShared = true;
        isLoading = false;
        updateLocationButton();
    }

    private void stopLiveLocationSharing() {
        // Stop the live location service
        Intent serviceIntent = new Intent(this, LiveLocationService.class);
        serviceIntent.setAction("STOP_SHARING");
        startService(serviceIntent);

        // Clear the share ID
        AuthManager.clearLiveLocationShareId(this);
        shareId = null;

        // Update state
        isLocationShared = false;
        isLoading = false;
        updateLocationButton();

        Toast.makeText(this, "Location sharing stopped", Toast.LENGTH_SHORT).show();
    }

    private void sendLocationShareSMS(List<String> contactNumbers, String shareId) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "SMS permission required", Toast.LENGTH_SHORT).show();
            return;
        }

        String message = "I'm sharing my live location with you for safety. " +
                "Track my location: " +
                "https://shesecure.vercel.app/live-location/?shareId=" + shareId;

        SmsManager smsManager = SmsManager.getDefault();

        for (String number : contactNumbers) {
            try {
                String formattedNumber = number.replaceAll("[^0-9+]", "");
                if (formattedNumber.isEmpty()) continue;
                smsManager.sendTextMessage(formattedNumber, null, message, null, null);

            } catch (Exception e) {
                Log.e("SMS_ERROR", "Failed to send SMS to " + number, e);
            }
        }

    }

    interface ContactsLoadCallback {
        void onContactsLoaded(List<EmergencyContact> contacts);
        void onLoadFailed(String error);
    }

    private void loadContacts(ContactsLoadCallback callback) {
        Call<EmergencyContactResponse> call = apiService.getEmergencyContacts(authToken);

        call.enqueue(new Callback<EmergencyContactResponse>() {
            @Override
            public void onResponse(Call<EmergencyContactResponse> call,
                                   Response<EmergencyContactResponse> response) {

                if (response.isSuccessful() && response.body() != null) {
                    List<EmergencyContact> contacts = response.body().getContacts();
                    callback.onContactsLoaded(contacts);
                } else {
                    callback.onLoadFailed("Server error");
                }
            }

            @Override
            public void onFailure(Call<EmergencyContactResponse> call, Throwable t) {
                callback.onLoadFailed(t.getMessage());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Force update crime locations when returning to dashboard
        if (mMap != null) {
            List<CrimeReport> currentCrimes = LocationDataManager.getInstance().getCurrentCrimes();
            if (currentCrimes != null && !currentCrimes.isEmpty()) {
                Log.d("ON_RESUME", "Updating map with current crimes: " + currentCrimes.size());
                updateMapWithCrimes(currentCrimes);
                updateNearestCrimeDisplay();
            }
        }

        // Also trigger a force update from CrimeLocationManager
        LocationDataManager.getInstance().forceUpdateAllLocations();
    }
}