package com.example.shesecure.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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
        apiService = ApiUtils.initializeApiService(this);
        loadUserData();
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

        // Profile image setup
        loadProfileImage(profileImage);
        profileImage.setOnClickListener(v -> showProfilePopup(v));
    }

    private void loadUserData() {
        SharedPreferences prefs = getSharedPreferences("SheSecurePrefs", MODE_PRIVATE);
        String userJson = prefs.getString("userJson", null);

        try {
            if (userJson != null) {
                userData = new JSONObject(userJson);
                userType = userData.optString("userType", "");

                // Save parsed name and email
                String name = userData.optString("firstName", "") + " " +
                        userData.optString("lastName", "");
                String email = userData.optString("email", "");

                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("parsedName", name);
                editor.putString("parsedEmail", email);
                editor.apply();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadProfileImage(CircleImageView profileImage) {
        SharedPreferences prefs = getSharedPreferences("SheSecurePrefs", MODE_PRIVATE);
        String userJson = prefs.getString("userJson", null);

        if (userJson != null) {
            try {
                JSONObject userObj = new JSONObject(userJson);
                if (userObj.has("additionalDetails")) {
                    JSONObject additional = userObj.getJSONObject("additionalDetails");
                    String imageUrl = additional.optString("image");
                    Glide.with(this)
                            .load(imageUrl)
                            .placeholder(R.drawable.person)
                            .into(profileImage);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

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
        LinearLayout optionEditProfile = popupView.findViewById(R.id.optionEditProfile);
        LinearLayout optionLogout = popupView.findViewById(R.id.optionLogout);

        // Set user data
        SharedPreferences prefs = getSharedPreferences("SheSecurePrefs", MODE_PRIVATE);
        tvUserName.setText(prefs.getString("parsedName", "User"));
        tvUserEmail.setText(prefs.getString("parsedEmail", "example@example.com"));

        // Set click listeners
        optionEditProfile.setOnClickListener(v -> {
//            profilePopupWindow.dismiss();
//            // Handle edit profile
//            startActivity(new Intent(this, EditProfileActivity.class));
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