package com.example.shesecure.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.shesecure.R;
import com.example.shesecure.services.ApiService;
import com.example.shesecure.utils.ApiUtils;

import org.json.JSONObject;

import de.hdodenhof.circleimageview.CircleImageView;

public class BaseActivity extends AppCompatActivity {

    protected ApiService apiService;
    protected JSONObject userData;
    protected String userType;
    protected PopupWindow profilePopupWindow;
    protected View navHeader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        apiService = ApiUtils.initializeApiService(this, ApiService.class);
        setStatusBarColor();
        SharedPreferences prefs = getSharedPreferences("SheSecurePrefs", MODE_PRIVATE);
        userType = prefs.getString("userType", null);
    }

    private void setStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.white));
        }
    }

    @Override
    public void setContentView(int layoutResID) {
        // Main container
        LinearLayout mainContainer = new LinearLayout(this);
        mainContainer.setOrientation(LinearLayout.VERTICAL);
        mainContainer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));

        // Add nav header
        LayoutInflater inflater = LayoutInflater.from(this);
        navHeader = inflater.inflate(R.layout.nav_login_header, mainContainer, false);
        mainContainer.addView(navHeader);

        // Add activity content
        View contentView = inflater.inflate(layoutResID, mainContainer, false);
        mainContainer.addView(contentView);

        super.setContentView(mainContainer);
        setupNavbar();
    }

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

        // Initialize PopupWindow
        profilePopupWindow = new PopupWindow(
                popupView,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                true
        );
        profilePopupWindow.setElevation(10f);
        profilePopupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        profilePopupWindow.setOutsideTouchable(true);

        // Find views
        TextView tvUserName = popupView.findViewById(R.id.tvUserName);
        TextView tvUserEmail = popupView.findViewById(R.id.tvUserEmail);
        LinearLayout optionDashboard = popupView.findViewById(R.id.optionDashboard);
        LinearLayout optionMyProfile = popupView.findViewById(R.id.optionMyProfile);
        LinearLayout optionEditProfile = popupView.findViewById(R.id.optionEditProfile);
        LinearLayout optionHelpline = popupView.findViewById(R.id.optionHelpline);
        LinearLayout optionCustomerSupport = popupView.findViewById(R.id.optionCustomerSupport);
        LinearLayout optionFeedback = popupView.findViewById(R.id.optionFeedback);
        LinearLayout optionLogout = popupView.findViewById(R.id.optionLogout);

        // Set user data
        SharedPreferences prefs = getSharedPreferences("SheSecurePrefs", MODE_PRIVATE);
        tvUserName.setText(prefs.getString("firstName", null) + " " +
                prefs.getString("lastName", null));
        tvUserEmail.setText(prefs.getString("email", "example@example.com"));

        // Set click listeners
        optionDashboard.setOnClickListener(v -> {
            profilePopupWindow.dismiss();
            // Handle edit profile
            startActivity(new Intent(this, UserDashboardActivity.class));
        });

        optionMyProfile.setOnClickListener(v -> {
            profilePopupWindow.dismiss();
            // Handle edit profile
            startActivity(new Intent(this, ProfileActivity.class));
        });

        optionEditProfile.setOnClickListener(v -> {
            profilePopupWindow.dismiss();
            // Handle edit profile
            startActivity(new Intent(this, EditProfileActivity.class));
        });

        optionHelpline.setOnClickListener(v -> {
            profilePopupWindow.dismiss();
            // Handle edit profile
            startActivity(new Intent(this, HelplineActivity.class));
        });

        optionCustomerSupport.setOnClickListener(v -> {
            profilePopupWindow.dismiss();
            // Handle edit profile
            startActivity(new Intent(this, CustomerSupportActivity.class));
        });

        optionFeedback.setOnClickListener(v -> {
            profilePopupWindow.dismiss();
            // Handle edit profile
            startActivity(new Intent(this, FeedbackActivity.class));
        });

        optionLogout.setOnClickListener(v -> {
            profilePopupWindow.dismiss();
            logout();
        });

        // Show popup
        profilePopupWindow.showAsDropDown(anchor, -200, 45);
    }

    private void triggerSOS() {
        // Implement SOS functionality
        Toast.makeText(this, "SOS Triggered!", Toast.LENGTH_SHORT).show();
    }

    protected void logout() {
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