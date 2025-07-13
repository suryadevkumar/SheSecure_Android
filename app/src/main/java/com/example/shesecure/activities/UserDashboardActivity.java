package com.example.shesecure.activities;

import android.Manifest;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.shesecure.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;

public class UserDashboardActivity extends BaseActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Marker userMarker;
    private FusedLocationProviderClient fusedLocationClient;
    private boolean isSOSActive = false;
    private boolean isLocationShared = false;
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_dashboard);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        setupMap();
        setupButtons();
    }

    @Override
    protected void onLocationUpdated(@NonNull Location location) {
        Log.d("LOCATION_UPDATE", "New location: " + location.getLatitude() + ", " + location.getLongitude());
        updateMapWithLocation(location);
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
    }

    private void updateMapWithLocation(Location location) {
        if (mMap != null) {
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

            if (userMarker == null) {
                userMarker = mMap.addMarker(new MarkerOptions()
                        .position(latLng)
                        .title("Your Location"));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
            } else {
                userMarker.setPosition(latLng);
            }
        }
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
            if (!isLocationShared) {
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
        LinearLayout sosLayout = (LinearLayout) sosCard.getChildAt(0);
        LinearLayout iconLayout = (LinearLayout) sosLayout.getChildAt(0);
        TextView textView = (TextView) sosLayout.getChildAt(1);

        if (isSOSActive) {
            sosCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.red_600));
            iconLayout.setBackground(ContextCompat.getDrawable(this, R.color.circle_bg_red_dark));
            textView.setTextColor(ContextCompat.getColor(this, R.color.white));
        } else {
            sosCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.white));
            iconLayout.setBackground(ContextCompat.getDrawable(this, R.drawable.circle_bg));
            textView.setTextColor(ContextCompat.getColor(this, R.color.black));
        }
    }

    private void updateLocationButton() {
        CardView locationCard = findViewById(R.id.locationBtn);
        LinearLayout locationLayout = (LinearLayout) locationCard.getChildAt(0);
        LinearLayout iconLayout = (LinearLayout) locationLayout.getChildAt(0);
        TextView textView = (TextView) locationLayout.getChildAt(1);

        if (isLoading) {
            textView.setText("Starting...");
        } else if (isLocationShared) {
            locationCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.blue_600));
            iconLayout.setBackground(ContextCompat.getDrawable(this, R.color.circle_bg_blue_dark));
            textView.setTextColor(ContextCompat.getColor(this, R.color.white));
            textView.setText("Location Shared");
        } else {
            locationCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.white));
            iconLayout.setBackground(ContextCompat.getDrawable(this, R.drawable.circle_bg));
            textView.setTextColor(ContextCompat.getColor(this, R.color.black));
            textView.setText("Share Live Location");
        }
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
        new Handler().postDelayed(() -> {
            isLoading = false;
            isLocationShared = true;
            updateLocationButton();
            Toast.makeText(this, "Live Location Sharing Started", Toast.LENGTH_SHORT).show();
        }, 1500);
    }
}