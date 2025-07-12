package com.example.shesecure.activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;

import com.bumptech.glide.Glide;
import com.example.shesecure.R;
import com.example.shesecure.services.ApiService;
import com.example.shesecure.services.LocationService;
import com.example.shesecure.utils.ApiUtils;

import org.json.JSONObject;

import de.hdodenhof.circleimageview.CircleImageView;

public class BaseActivity extends AppCompatActivity {

    protected ApiService apiService;
    protected JSONObject userData;
    protected String userType;
    protected PopupWindow profilePopupWindow;
    protected View navHeader;
    protected Location currentLocation;
    protected CardView locationRequirementCard;
    protected boolean isLocationRequired = false;
    protected LinearLayout mainContainer;
    protected View contentView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        apiService = ApiUtils.initializeApiService(this, ApiService.class);
        setStatusBarColor();

        SharedPreferences prefs = getSharedPreferences("SheSecurePrefs", MODE_PRIVATE);
        userType = prefs.getString("userType", null);
        isLocationRequired = "User".equals(userType);
    }

    @Override
    public void setContentView(int layoutResID) {
        // Main container
        mainContainer = new LinearLayout(this);
        mainContainer.setOrientation(LinearLayout.VERTICAL);
        mainContainer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));

        // Add location requirement card if needed
        if (isLocationRequired) {
            LayoutInflater inflater = LayoutInflater.from(this);
            locationRequirementCard = (CardView) inflater.inflate(R.layout.location_requirement_card, mainContainer, false);
            mainContainer.addView(locationRequirementCard);
            setupLocationRequirementCard();
            locationRequirementCard.setVisibility(View.GONE); // Initially hidden
        }

        // Add nav header
        LayoutInflater inflater = LayoutInflater.from(this);
        navHeader = inflater.inflate(R.layout.nav_login_header, mainContainer, false);
        mainContainer.addView(navHeader);

        // Add activity content
        contentView = inflater.inflate(layoutResID, mainContainer, false);
        mainContainer.addView(contentView);

        super.setContentView(mainContainer);
        setupNavbar();

        // Check location immediately after setting up views
        if (isLocationRequired) {
            checkLocationEnabled();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (isLocationRequired) {
            LocationService.getLiveLocation().observe(this, new Observer<Location>() {
                @Override
                public void onChanged(Location location) {
                    currentLocation = location;
                    onLocationUpdated(location);
                }
            });
            currentLocation = LocationService.getCurrentLocation();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isLocationRequired) {
            checkLocationEnabled();
        }
    }

    private void setupLocationRequirementCard() {
        locationRequirementCard.findViewById(R.id.btnEnableLocation).setOnClickListener(v -> {
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
        });
    }

    private void checkLocationEnabled() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean isLocationEnabled = false;
        try {
            isLocationEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception e) {
            Log.e("LocationCheck", "Error checking location status", e);
        }

        if (isLocationEnabled) {
            // Location is enabled - show all content except warning card
            if (locationRequirementCard != null) {
                locationRequirementCard.setVisibility(View.GONE);
            }
            enableAppFunctionality();
            if (!LocationService.isServiceRunning()) {
                startService(new Intent(this, LocationService.class));
            }
        } else {
            // Location is disabled - show only warning card
            if (locationRequirementCard != null) {
                locationRequirementCard.setVisibility(View.VISIBLE);
            }
            disableAppFunctionality();
            stopService(new Intent(this, LocationService.class));
        }
    }

    private void enableAppFunctionality() {
        // Show all views except the location requirement card
        for (int i = 0; i < mainContainer.getChildCount(); i++) {
            View child = mainContainer.getChildAt(i);
            if (child != locationRequirementCard) {
                child.setVisibility(View.VISIBLE);
            }
        }
    }

    private void disableAppFunctionality() {
        // Hide all views except the location requirement card
        for (int i = 0; i < mainContainer.getChildCount(); i++) {
            View child = mainContainer.getChildAt(i);
            if (child != locationRequirementCard) {
                child.setVisibility(View.GONE);
            }
        }
    }

    protected void onLocationUpdated(@NonNull Location location) {
        // Can be overridden by child activities
    }

    private void setStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.white));
        }
    }

    @SuppressLint("SetTextI18n")
    protected void setupNavbar() {
        CircleImageView profileImage = navHeader.findViewById(R.id.profileImage);
        Button sosButton = navHeader.findViewById(R.id.sosButton);

        // SOS button visibility
        sosButton.setVisibility(userType != null && userType.equals("User") ? View.VISIBLE : View.GONE);
        sosButton.setOnClickListener(v -> triggerSOS());

        SharedPreferences prefs = getSharedPreferences("SheSecurePrefs", MODE_PRIVATE);
        String imageUrl = prefs.getString("profileImage", "");
        Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.person)
                .into(profileImage);
        profileImage.setOnClickListener(v -> showProfilePopup(v));
    }

    @SuppressLint("SetTextI18n")
    private void showProfilePopup(View anchor) {
        View popupView = LayoutInflater.from(this).inflate(R.layout.popup_menu_user, null);

        int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.75);
        profilePopupWindow = new PopupWindow(
                popupView,
                width,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                true
        );

        profilePopupWindow.setElevation(10f);
        profilePopupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        profilePopupWindow.setOutsideTouchable(true);

        // Initialize popup views
        TextView tvUserName = popupView.findViewById(R.id.tvUserName);
        TextView tvUserEmail = popupView.findViewById(R.id.tvUserEmail);
        LinearLayout optionDashboard = popupView.findViewById(R.id.optionDashboard);
        LinearLayout optionMyProfile = popupView.findViewById(R.id.optionMyProfile);
        LinearLayout optionEditProfile = popupView.findViewById(R.id.optionEditProfile);
        LinearLayout optionHelpline = popupView.findViewById(R.id.optionHelpline);
        LinearLayout optionCustomerSupport = popupView.findViewById(R.id.optionCustomerSupport);
        LinearLayout optionFeedback = popupView.findViewById(R.id.optionFeedback);
        LinearLayout optionEmergencyContacts = popupView.findViewById(R.id.optionEmergencyContacts);
        LinearLayout optionLogout = popupView.findViewById(R.id.optionLogout);

        // Set user data
        SharedPreferences prefs = getSharedPreferences("SheSecurePrefs", MODE_PRIVATE);
        tvUserName.setText(prefs.getString("firstName", "") + " " + prefs.getString("lastName", ""));
        tvUserEmail.setText(prefs.getString("email", "example@example.com"));

        // Set click listeners
        optionDashboard.setOnClickListener(v -> navigateTo(UserDashboardActivity.class));
        optionMyProfile.setOnClickListener(v -> navigateTo(ProfileActivity.class));
        optionEditProfile.setOnClickListener(v -> navigateTo(EditProfileActivity.class));
        optionHelpline.setOnClickListener(v -> navigateTo(HelplineActivity.class));
        optionCustomerSupport.setOnClickListener(v -> navigateTo(CustomerSupportActivity.class));
        optionFeedback.setOnClickListener(v -> navigateTo(FeedbackActivity.class));
        optionEmergencyContacts.setOnClickListener(v -> navigateTo(EmergencyContactsActivity.class));
        optionLogout.setOnClickListener(v -> logout());

        // Show popup
        profilePopupWindow.showAsDropDown(anchor, -200, 45);
    }

    private void navigateTo(Class<?> cls) {
        if (profilePopupWindow != null) {
            profilePopupWindow.dismiss();
        }
        startActivity(new Intent(this, cls));
    }

    protected void triggerSOS() {
        if (currentLocation != null) {
            Toast.makeText(this,
                    "SOS Triggered at: " + currentLocation.getLatitude() + "," + currentLocation.getLongitude(),
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "SOS Triggered! Getting location...", Toast.LENGTH_SHORT).show();
        }
    }

    protected void logout() {
        stopService(new Intent(this, LocationService.class));

        SharedPreferences.Editor editor = getSharedPreferences("SheSecurePrefs", MODE_PRIVATE).edit();
        editor.clear();
        editor.apply();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        if (profilePopupWindow != null && profilePopupWindow.isShowing()) {
            profilePopupWindow.dismiss();
        }
        super.onDestroy();
    }
}