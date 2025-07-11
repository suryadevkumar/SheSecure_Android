package com.example.shesecure.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import com.bumptech.glide.Glide;
import com.example.shesecure.R;
import com.example.shesecure.models.User;
import com.example.shesecure.services.ApiService;
import com.example.shesecure.utils.ApiUtils;
import org.json.JSONObject;
import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends BaseActivity {

    private CircleImageView profileImage;
    private TextView tvName, tvEmail, tvPhone, tvGender, tvDob, tvAddress, tvUserType;
    private Button btnEditProfile, btnEmergencyContacts;
    private ApiService apiService;
    private String authToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        apiService = ApiUtils.initializeApiService(this, ApiService.class);

        initializeViews();
        loadProfileData();
    }

    private void initializeViews() {
        profileImage = findViewById(R.id.profile_image);
        tvName = findViewById(R.id.tv_name);
        tvEmail = findViewById(R.id.tv_email);
        tvPhone = findViewById(R.id.tv_phone);
        tvGender = findViewById(R.id.tv_gender);
        tvDob = findViewById(R.id.tv_dob);
        tvAddress = findViewById(R.id.tv_address);
        tvUserType = findViewById(R.id.tv_user_type);

        btnEditProfile = findViewById(R.id.btn_edit_profile);
        btnEmergencyContacts = findViewById(R.id.btn_emergency_contacts);

        btnEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
            startActivity(intent);
        });

        btnEmergencyContacts.setOnClickListener(v -> {
            startActivity(new Intent(this, EmergencyContactsActivity.class));
        });
    }

    private void loadProfileData() {
        SharedPreferences prefs = getSharedPreferences("SheSecurePrefs", MODE_PRIVATE);
        authToken = "Bearer " + prefs.getString("token", "");

        // First try to load from SharedPreferences
        String userJson = prefs.getString("userJson", null);
        if (userJson != null) {
            try {
                displayUserData(new JSONObject(userJson));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Then fetch fresh data from API
        Call<ResponseBody> call = apiService.getUserDetails(authToken);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    try {
                        String responseData = response.body().string();
                        JSONObject userObj = new JSONObject(responseData);
                        displayUserData(userObj);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(ProfileActivity.this, "Failed to load profile", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(ProfileActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayUserData(JSONObject responseObj) {
        try {
            // Get the user object from the response
            JSONObject userObj = responseObj.getJSONObject("user");

            // Basic info
            String name = userObj.optString("firstName", "") + " " +
                    userObj.optString("lastName", "");
            String email = userObj.optString("email", "");
            String phone = String.valueOf(userObj.optLong("mobileNumber", 0));
            String userType = userObj.optString("userType", "");

            tvName.setText(name);
            tvEmail.setText(email);
            tvPhone.setText(phone);
            tvUserType.setText(userType);

            // Additional details
            if (userObj.has("additionalDetails")) {
                JSONObject additional = userObj.getJSONObject("additionalDetails");
                String gender = additional.optString("gender", "Not specified");
                String dob = additional.optString("dob", "Not specified").split("T")[0];
                String address = additional.optString("address", "Not specified");
                String imageUrl = additional.optString("image", "");

                tvGender.setText(gender);
                tvDob.setText(dob);
                tvAddress.setText(address);

                if (!imageUrl.isEmpty()) {
                    Glide.with(this)
                            .load(imageUrl)
                            .placeholder(R.drawable.person)
                            .into(profileImage);
                }
            }

            // Show/hide emergency contacts button based on user type
            btnEmergencyContacts.setVisibility(
                    userType.equals("User") ? View.VISIBLE : View.GONE
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}