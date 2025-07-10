package com.example.shesecure.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
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
import com.google.android.gms.maps.model.MarkerOptions;

public class UserDashboardActivity extends BaseActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    // Button states
    private boolean isSOSActive = false;
    private boolean isLocationShared = false;
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_dashboard);

        setStatusBarColor();
        setupMap();
        setupButtons();
    }

    private void setStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.white));
        }
    }

    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    private void setupButtons() {
        // Trusted Contacts Button
        CardView trustedContactsBtn = findViewById(R.id.trustedContactsBtn);
        trustedContactsBtn.setOnClickListener(v -> {
            // Start TrustedContactsActivity
            Intent intent = new Intent(UserDashboardActivity.this, HelplineActivity.class);
            startActivity(intent);
        });

        // SOS Button
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

        // Location Button
        CardView locationBtn = findViewById(R.id.locationBtn);
        locationBtn.setOnClickListener(v -> {
            if (!isLoading) {
                isLoading = true;
                updateLocationButton();
                handleLocationClick();
            }
        });

        // Helpline Button
        CardView helplineBtn = findViewById(R.id.helplineBtn);
        helplineBtn.setOnClickListener(v -> {
            // Start HelplineActivity
            Intent intent = new Intent(UserDashboardActivity.this, HelplineActivity.class);
            startActivity(intent);
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
        // Implement actual SOS functionality here
        // For now just showing a toast
        Toast.makeText(this, "Emergency SOS Activated", Toast.LENGTH_SHORT).show();

        // You would typically:
        // 1. Send emergency alerts to trusted contacts
        // 2. Start sharing live location
        // 3. Maybe play an alarm sound
    }

    private void stopSOS() {
        // Implement stop SOS functionality
        Toast.makeText(this, "Emergency SOS Deactivated", Toast.LENGTH_SHORT).show();

        // You would typically:
        // 1. Stop sending alerts
        // 2. Stop sharing location
        // 3. Stop any alarm sounds
    }

    private void handleLocationClick() {
        // Check location permission first
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            // Simulate location sharing starting (replace with actual implementation)
            new Handler().postDelayed(() -> {
                isLoading = false;
                isLocationShared = true;
                updateLocationButton();
                Toast.makeText(this, "Live Location Sharing Started", Toast.LENGTH_SHORT).show();

                // Actual implementation would:
                // 1. Start sending location updates to server/contacts
                // 2. Update UI accordingly
            }, 1500);

        } else {
            // Request permission if not granted
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            isLoading = false;
            updateLocationButton();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        enableMyLocation();
    }

    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            getLastKnownLocation();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void getLastKnownLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null && mMap != null) {
                            LatLng currentLatLng = new LatLng(location.getLatitude(),
                                    location.getLongitude());
                            mMap.addMarker(new MarkerOptions()
                                    .position(currentLatLng)
                                    .title("Your Location"));
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));
                        }
                    });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation();
                if (isLoading) {
                    // Retry location sharing if permission was just granted
                    handleLocationClick();
                }
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
                isLoading = false;
                updateLocationButton();
            }
        }
    }
}