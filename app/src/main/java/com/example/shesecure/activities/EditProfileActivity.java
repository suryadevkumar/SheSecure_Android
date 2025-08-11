package com.example.shesecure.activities;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;  // Add this import
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import com.bumptech.glide.Glide;
import com.example.shesecure.R;
import com.example.shesecure.services.ApiService;
import com.example.shesecure.utils.ApiUtils;
import org.json.JSONObject;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProfileActivity extends BaseActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private final Calendar myCalendar = Calendar.getInstance();

    private CircleImageView profileImage;
    private TextView tvName, tvEmail, tvPhone, tvGender, tvUserType;
    private EditText etDob, etAddress;
    private Button btnSave;
    private ImageButton btnSelectImage;  // Changed to ImageButton
    private ApiService apiService;
    private String authToken;
    private Uri imageUri;
    private ProgressDialog progressDialog;
    private String originalDob, originalAddress;
    private boolean isImageChanged = false;
    private Spinner genderSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        apiService = ApiUtils.initializeApiService(this, ApiService.class);

        initializeViews();
        setupDatePicker();
        loadProfileData();
    }

    private void initializeViews() {  // Removed @SuppressLint annotation
        profileImage = findViewById(R.id.profile_image);
        tvName = findViewById(R.id.tv_name);
        tvEmail = findViewById(R.id.tv_email);
        tvPhone = findViewById(R.id.tv_phone);
        tvGender = findViewById(R.id.tv_gender);
        tvUserType = findViewById(R.id.tv_user_type);
        etDob = findViewById(R.id.et_dob);
        etAddress = findViewById(R.id.et_address);
        btnSave = findViewById(R.id.btn_save);
        btnSelectImage = findViewById(R.id.btn_select_image);  // Now correctly casts to ImageButton

        btnSelectImage.setOnClickListener(v -> openImageChooser());
        btnSave.setOnClickListener(v -> updateProfile());

        // Make DOB field non-editable (only selectable via date picker)
        etDob.setKeyListener(null);
        etDob.setOnClickListener(v -> showDatePicker());
    }

    private void setupDatePicker() {
        DatePickerDialog.OnDateSetListener date = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int day) {
                myCalendar.set(Calendar.YEAR, year);
                myCalendar.set(Calendar.MONTH, month);
                myCalendar.set(Calendar.DAY_OF_MONTH, day);
                updateDobLabel();
            }
        };
    }

    private void showDatePicker() {
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            myCalendar.set(Calendar.YEAR, year);
            myCalendar.set(Calendar.MONTH, month);
            myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateDobLabel();
        },
                myCalendar.get(Calendar.YEAR),
                myCalendar.get(Calendar.MONTH),
                myCalendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateDobLabel() {
        String myFormat = "yyyy-MM-dd"; // ISO format
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.US);
        etDob.setText(sdf.format(myCalendar.getTime()));
    }

    private void showGenderSelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Gender");

        String[] genders = {"Male", "Female", "Other", "Prefer not to say"};

        builder.setItems(genders, (dialog, which) -> {
            String selectedGender = genders[which];
            tvGender.setText(selectedGender);
            tvGender.setVisibility(View.VISIBLE);
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void loadProfileData() {
        authToken = "Bearer " + authManager.getToken();

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
                    Toast.makeText(EditProfileActivity.this, "Failed to load profile", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(EditProfileActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayUserData(JSONObject responseObj) {
        try {
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
                String gender = additional.optString("gender", "");
                originalDob = additional.optString("dob", "").split("T")[0];
                originalAddress = additional.optString("address", "Not specified");
                String imageUrl = additional.optString("image", "");

                // Handle gender field
                if (gender == null || gender.isEmpty() || gender.equals("Not specified")) {
                    tvGender.setVisibility(View.VISIBLE);
                    tvGender.setText("Select Gender");
                    tvGender.setOnClickListener(v -> showGenderSelectionDialog());
                } else {
                    tvGender.setText(gender);
                    tvGender.setOnClickListener(null); // Remove click listener if gender is already set
                }

                etDob.setText(originalDob);
                etAddress.setText(originalAddress);

                if (!imageUrl.isEmpty()) {
                    Glide.with(this)
                            .load(imageUrl)
                            .placeholder(R.drawable.person)
                            .into(profileImage);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openImageChooser() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            profileImage.setImageURI(imageUri);
            isImageChanged = true;
        }
    }

    private void updateProfile() {
        String newDob = etDob.getText().toString();
        String newAddress = etAddress.getText().toString();
        String newGender = tvGender.getText().toString().equals("Select Gender") ?
                null : tvGender.getText().toString();

        // Check if anything has changed
        boolean dobChanged = !newDob.equals(originalDob);
        boolean addressChanged = !newAddress.equals(originalAddress);
        boolean genderChanged = newGender != null && !newGender.isEmpty();

        if (!isImageChanged && !dobChanged && !addressChanged && !genderChanged) {
            Toast.makeText(this, "No changes to update", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Updating profile...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        try {
            MultipartBody.Part imagePart = null;
            if (isImageChanged && imageUri != null) {
                File file = new File(getRealPathFromURI(imageUri));
                RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
                imagePart = MultipartBody.Part.createFormData("image", file.getName(), requestFile);
            }

            // Only include changed fields in the request
            RequestBody dob = dobChanged ?
                    RequestBody.create(MediaType.parse("text/plain"), newDob) : null;

            RequestBody address = addressChanged ?
                    RequestBody.create(MediaType.parse("text/plain"), newAddress) : null;

            RequestBody gender = genderChanged ?
                    RequestBody.create(MediaType.parse("text/plain"), newGender) : null;

            Call<ResponseBody> call = apiService.updateProfile(
                    authToken,
                    isImageChanged ? imagePart : null,
                    gender,
                    dob,
                    address
            );

            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    progressDialog.dismiss();
                    if (response.isSuccessful()) {
                        Toast.makeText(EditProfileActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();

                        try {
                            String responseData = response.body().string();
                            JSONObject responseObj = new JSONObject(responseData);
                            JSONObject userObj = responseObj.getJSONObject("user");

                            if (userObj.has("additionalDetails")) {
                                JSONObject additional = userObj.getJSONObject("additionalDetails");
                                String imageUrl = additional.optString("image", "");

                                if (!imageUrl.isEmpty()) {
                                    // Save the new image URL to SharedPreferences
                                    authManager.updateProfileImage(imageUrl);
                                }
                            }

                            finish();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        Toast.makeText(EditProfileActivity.this, "Failed to update profile", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    progressDialog.dismiss();
                    Toast.makeText(EditProfileActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            progressDialog.dismiss();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String getRealPathFromURI(Uri uri) {
        // Implement proper path resolution here
        return uri.getPath();
    }
}