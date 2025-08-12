package com.example.shesecure.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AutoCompleteTextView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.widget.Toolbar;

import com.example.shesecure.MainActivity;
import com.example.shesecure.R;
import com.example.shesecure.models.Course;
import com.example.shesecure.models.User;
import com.example.shesecure.services.ApiService;
import com.example.shesecure.utils.ApiUtils;
import com.google.android.material.navigation.NavigationView;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignupActivity extends AppCompatActivity {

    // Basic Info Views
    private EditText etFirstName, etLastName, etEmail, etMobile;
    private AutoCompleteTextView spinnerUserType;
    private Button btnNextBasicInfo;

    // Course Info Views
    private LinearLayout layoutCourseInfo;
    private EditText etCourseName, etPercentage, etCertificatePath;
    private Button btnAddCourse, btnNextCourseInfo, btnBackToPrevious, btnChooseCertificate;
    private TextView tvCoursesList;
    private LinearLayout layoutAddedCourses, courseListContainer;
    private List<Course> coursesData = new ArrayList<>();
    private File selectedCertificate;

    // OTP Views
    private LinearLayout layoutOTP, otpContainer;
    private EditText etEmailDisplay;
    private Button btnSendOTP, btnSignup, btnBackToUserInfo;
    private ProgressBar progressBar;
    private TextView tvLoginLink;

    // OTP Variables
    private List<EditText> otpEditTexts = new ArrayList<>();
    private static final int OTP_LENGTH = 6;

    // State Variables
    private CountDownTimer countDownTimer;
    private ApiService apiService;
    private String userType = "";
    private int currentPage = 1;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    // File picker for certificate
    private ActivityResultLauncher<String> filePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        setStatusBarColor();

        setupNavigationDrawer();
        initializeViews();
        setupUserTypeSpinner();
        setupOTPInput();
        setupFilePicker();

        apiService = ApiUtils.initializeApiService(this, ApiService.class);

        btnNextBasicInfo.setOnClickListener(v -> validateBasicInfo());
        btnAddCourse.setOnClickListener(v -> addCourse());
        btnChooseCertificate.setOnClickListener(v -> openFilePicker());
        btnNextCourseInfo.setOnClickListener(v -> validateCourses());
        btnBackToPrevious.setOnClickListener(v -> goToPreviousPage());
        btnBackToUserInfo.setOnClickListener(v -> goToPreviousPage());
        btnSendOTP.setOnClickListener(v -> sendEmailOTP());
        btnSignup.setOnClickListener(v -> signUp());
        tvLoginLink.setOnClickListener(v -> navigateToLogin());
    }

    private void setStatusBarColor() {
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.pink_600));
        }
    }

    private void setupNavigationDrawer() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.menu);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        navigationView.getMenu().clear();
        navigationView.inflateMenu(R.menu.nav_logout_menu);

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }
            else if (id == R.id.nav_signup) {
                drawerLayout.closeDrawer(GravityCompat.START);
            }
            else if (id == R.id.nav_login) {
                startActivity(new Intent(this, LoginActivity.class));
                finish();
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            drawerLayout.openDrawer(GravityCompat.START);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initializeViews() {
        // Basic Info Views
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etEmail = findViewById(R.id.etEmail);
        etMobile = findViewById(R.id.etMobile);
        spinnerUserType = findViewById(R.id.spinnerUserType);
        btnNextBasicInfo = findViewById(R.id.btnNextBasicInfo);

        // Course Info Views
        layoutCourseInfo = findViewById(R.id.layoutCourseInfo);
        etCourseName = findViewById(R.id.etCourseName);
        etPercentage = findViewById(R.id.etPercentage);
        etCertificatePath = findViewById(R.id.etCertificatePath);
        btnAddCourse = findViewById(R.id.btnAddCourse);
        btnNextCourseInfo = findViewById(R.id.btnNextCourseInfo);
        btnBackToPrevious = findViewById(R.id.btnBackToPrevious);
        btnChooseCertificate = findViewById(R.id.btnChooseCertificate);
        tvCoursesList = findViewById(R.id.tvCoursesList);
        layoutAddedCourses = findViewById(R.id.layoutAddedCourses);
        courseListContainer = findViewById(R.id.courseListContainer);

        // OTP Views
        layoutOTP = findViewById(R.id.layoutOTP);
        otpContainer = findViewById(R.id.otpContainer);
        etEmailDisplay = findViewById(R.id.etEmailDisplay);
        btnSendOTP = findViewById(R.id.btnSendOTP);
        btnSignup = findViewById(R.id.btnSignup);
        btnBackToUserInfo = findViewById(R.id.btnBackToUserInfo);
        tvLoginLink = findViewById(R.id.tvLoginLink);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupOTPInput() {
        otpContainer.removeAllViews();
        otpEditTexts.clear();

        for (int i = 0; i < OTP_LENGTH; i++) {
            EditText editText = new EditText(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    dpToPx(42),
                    dpToPx(42)
            );
            params.setMargins(dpToPx(4), 0, dpToPx(4), 0);
            editText.setLayoutParams(params);

            editText.setBackgroundResource(R.drawable.otp_box_background);
            editText.setTextColor(ContextCompat.getColor(this, R.color.black));
            editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            editText.setInputType(InputType.TYPE_CLASS_TEXT);
            editText.setGravity(Gravity.CENTER);
            editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(1)});

            final int position = i;
            editText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1 && position < OTP_LENGTH - 1) {
                        otpEditTexts.get(position + 1).requestFocus();
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });

            editText.setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (editText.getText().toString().isEmpty() && position > 0) {
                        EditText previousEditText = otpEditTexts.get(position - 1);
                        previousEditText.requestFocus();
                        previousEditText.setSelection(previousEditText.getText().length());
                        return true;
                    }
                    else if (!editText.getText().toString().isEmpty()) {
                        editText.setText("");
                        return true;
                    }
                }
                return false;
            });

            otpContainer.addView(editText);
            otpEditTexts.add(editText);
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void setupFilePicker() {
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        try {
                            String fileName = getFileNameFromUri(uri);
                            selectedCertificate = createFileFromUri(uri, fileName);
                            etCertificatePath.setText(fileName);
                        } catch (Exception e) {
                            Toast.makeText(this, "Error selecting file", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    private String getFileNameFromUri(Uri uri) {
        String fileName = "unknown_file";
        String scheme = uri.getScheme();
        if (scheme.equals("content")) {
            try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (columnIndex != -1) {
                        fileName = cursor.getString(columnIndex);
                    }
                }
            } catch (Exception e) {
                fileName = "certificate_" + System.currentTimeMillis();
            }
        }
        return fileName;
    }

    private File createFileFromUri(Uri uri, String fileName) throws Exception {
        File destinationFile = new File(getCacheDir(), fileName);
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             FileOutputStream outputStream = new FileOutputStream(destinationFile)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
        return destinationFile;
    }

    private void openFilePicker() {
        filePickerLauncher.launch("*/*");
    }

    private void setupUserTypeSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.user_types,
                android.R.layout.simple_dropdown_item_1line
        );
        spinnerUserType.setAdapter(adapter);
        spinnerUserType.setOnItemClickListener((parent, view, position, id) -> {
            userType = (String) parent.getItemAtPosition(position);
        });
    }

    private void validateBasicInfo() {
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String mobile = etMobile.getText().toString().trim();

        if (firstName.isEmpty()) {
            etFirstName.setError("First name is required");
            return;
        }
        if (lastName.isEmpty()) {
            etLastName.setError("Last name is required");
            return;
        }
        if (email.isEmpty()) {
            etEmail.setError("Email is required");
            return;
        }
        if (mobile.isEmpty()) {
            etMobile.setError("Mobile number is required");
            return;
        }
        if (userType.isEmpty()) {
            Toast.makeText(this, "Please select user type", Toast.LENGTH_SHORT).show();
            return;
        }

        checkEmailExists(email, () -> {
            if (userType.equals("User")) {
                currentPage = 3;
                showPage(currentPage);
                etEmailDisplay.setText(email);
                sendEmailOTP();
            } else {
                currentPage = 2;
                showPage(currentPage);
            }
        });
    }

    private void checkEmailExists(String email, Runnable onSuccess) {
        progressBar.setVisibility(View.VISIBLE);
        btnNextBasicInfo.setEnabled(false);

        String json = String.format("{\"email\":\"%s\"}", email);
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);

        Call<ResponseBody> call = apiService.checkEmailExists(body);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                progressBar.setVisibility(View.GONE);
                btnNextBasicInfo.setEnabled(true);

                try {
                    if (response.isSuccessful()) {
                        onSuccess.run();
                    } else {
                        String errorBody = response.errorBody().string();
                        JSONObject jsonObject = new JSONObject(errorBody);
                        String message = jsonObject.getString("message");

                        Toast.makeText(SignupActivity.this, message, Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(SignupActivity.this, "Error checking email", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnNextBasicInfo.setEnabled(true);
                Toast.makeText(SignupActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addCourse() {
        String courseName = etCourseName.getText().toString().trim();
        String percentage = etPercentage.getText().toString().trim();

        if (courseName.isEmpty()) {
            etCourseName.setError("Course name is required");
            return;
        }
        if (selectedCertificate == null) {
            Toast.makeText(this, "Certificate is required", Toast.LENGTH_SHORT).show();
            return;
        }

        Course course = new Course(courseName, percentage, selectedCertificate);
        coursesData.add(course);
        updateCoursesList();

        if (layoutAddedCourses.getVisibility() == View.GONE) {
            layoutAddedCourses.setVisibility(View.VISIBLE);
        }

        etCourseName.setText("");
        etPercentage.setText("");
        etCertificatePath.setText("");
        selectedCertificate = null;
        Toast.makeText(this, "Course added", Toast.LENGTH_SHORT).show();
    }

    private void updateCoursesList() {
        courseListContainer.removeAllViews();
        for (int i = 0; i < coursesData.size(); i++) {
            Course course = coursesData.get(i);
            TextView courseView = new TextView(this);
            courseView.setTextColor(getResources().getColor(R.color.gray_600));

            StringBuilder sb = new StringBuilder();
            sb.append("• ").append(course.getCourseName());
            if (!course.getPercentage().isEmpty()) {
                sb.append(" (").append(course.getPercentage()).append("%)");
            }
            courseView.setText(sb.toString());
            courseView.setPadding(0, 5, 0, 5);
            courseListContainer.addView(courseView);
        }

        StringBuilder sb = new StringBuilder();
        for (Course course : coursesData) {
            sb.append("• ").append(course.getCourseName());
            if (!course.getPercentage().isEmpty()) {
                sb.append(" (").append(course.getPercentage()).append("%)");
            }
            sb.append("\n");
        }
        tvCoursesList.setText(sb.toString());
    }

    private void validateCourses() {
        if (coursesData.isEmpty()) {
            Toast.makeText(this, "Please add at least one course", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Proceeding with the added courses", Toast.LENGTH_SHORT).show();
        currentPage = 3;
        showPage(currentPage);
        etEmailDisplay.setText(etEmail.getText().toString().trim());
        sendEmailOTP();
    }

    private void sendEmailOTP() {
        String email = etEmail.getText().toString().trim();

        if (email.isEmpty()) {
            Toast.makeText(this, "Email is required", Toast.LENGTH_SHORT).show();
            return;
        }

        startOTPTimer();

        progressBar.setVisibility(View.VISIBLE);
        btnSendOTP.setEnabled(false);

        String json = String.format("{\"email\":\"%s\"}", email);
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);

        Call<ResponseBody> call = apiService.sendEmailOTP(body);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful()) {
                    Toast.makeText(SignupActivity.this, "OTP sent to your email", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(SignupActivity.this, "Failed to send OTP", Toast.LENGTH_SHORT).show();
                    btnSendOTP.setEnabled(true);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnSendOTP.setEnabled(true);
                Toast.makeText(SignupActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startOTPTimer() {
        btnSendOTP.setEnabled(false);
        countDownTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                btnSendOTP.setText("Resend in " + millisUntilFinished / 1000 + "s");
            }

            @Override
            public void onFinish() {
                btnSendOTP.setText("Resend OTP");
                btnSendOTP.setEnabled(true);
            }
        }.start();
    }

    private String getEnteredOTP() {
        StringBuilder otp = new StringBuilder();
        for (EditText editText : otpEditTexts) {
            otp.append(editText.getText().toString());
        }
        return otp.toString();
    }

    private void signUp() {
        String otp = getEnteredOTP();
        if (otp.length() != OTP_LENGTH) {
            Toast.makeText(this, "Please enter the 6-digit OTP sent to your email", Toast.LENGTH_LONG).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSignup.setEnabled(false);

        String json = String.format("{\"emailOTP\":\"%s\"}", otp);
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);

        Call<ResponseBody> verifyCall = apiService.verifyEmailOTP(body);
        verifyCall.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    performSignup();
                } else {
                    progressBar.setVisibility(View.GONE);
                    btnSignup.setEnabled(true);
                    Toast.makeText(SignupActivity.this, "The OTP you entered is invalid or expired", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnSignup.setEnabled(true);
                Toast.makeText(SignupActivity.this, "Failed to verify OTP. Please try again", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void performSignup() {
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String mobile = etMobile.getText().toString().trim();

        User user = new User(firstName, lastName, email, mobile, userType);

        if (userType.equals("User")) {
            Call<ResponseBody> call = apiService.signup(user);
            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    progressBar.setVisibility(View.GONE);
                    btnSignup.setEnabled(true);

                    if (response.isSuccessful()) {
                        Toast.makeText(SignupActivity.this, "Signup successful!", Toast.LENGTH_SHORT).show();
                        navigateToLogin();
                    } else {
                        Toast.makeText(SignupActivity.this, "Signup failed", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    progressBar.setVisibility(View.GONE);
                    btnSignup.setEnabled(true);
                    Toast.makeText(SignupActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            uploadWithQualifications(user);
        }
    }

    private void uploadWithQualifications(User user) {
        try {
            RequestBody firstNameBody = RequestBody.create(MediaType.parse("text/plain"), user.getFirstName());
            RequestBody lastNameBody = RequestBody.create(MediaType.parse("text/plain"), user.getLastName());
            RequestBody emailBody = RequestBody.create(MediaType.parse("text/plain"), user.getEmail());
            RequestBody mobileBody = RequestBody.create(MediaType.parse("text/plain"), user.getMobileNumber());
            RequestBody userTypeBody = RequestBody.create(MediaType.parse("text/plain"), user.getUserType());

            StringBuilder qualificationsJson = new StringBuilder("[");
            for (int i = 0; i < coursesData.size(); i++) {
                Course course = coursesData.get(i);
                if (i > 0) qualificationsJson.append(",");
                qualificationsJson.append("{")
                        .append("\"courseName\":\"").append(course.getCourseName()).append("\",")
                        .append("\"percentage\":\"").append(course.getPercentage()).append("\"")
                        .append("}");
            }
            qualificationsJson.append("]");

            RequestBody qualificationsBody = RequestBody.create(
                    MediaType.parse("text/plain"),
                    qualificationsJson.toString()
            );

            List<MultipartBody.Part> certificateParts = new ArrayList<>();
            for (int i = 0; i < coursesData.size(); i++) {
                Course course = coursesData.get(i);
                File certificateFile = course.getCertificateFile();

                RequestBody requestFile = RequestBody.create(
                        MediaType.parse("multipart/form-data"),
                        certificateFile
                );

                String partName = String.format("qualifications[%d].certificate", i);
                certificateParts.add(MultipartBody.Part.createFormData(
                        partName,
                        certificateFile.getName(),
                        requestFile
                ));
            }

            Call<ResponseBody> call = apiService.signupWithQualifications(
                    firstNameBody,
                    lastNameBody,
                    emailBody,
                    mobileBody,
                    userTypeBody,
                    qualificationsBody,
                    certificateParts
            );

            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    progressBar.setVisibility(View.GONE);
                    btnSignup.setEnabled(true);

                    if (response.isSuccessful()) {
                        Toast.makeText(SignupActivity.this, "Signup successful!", Toast.LENGTH_SHORT).show();
                        navigateToLogin();
                    } else {
                        try {
                            String errorBody = response.errorBody().string();
                            JSONObject jsonObject = new JSONObject(errorBody);
                            String message = jsonObject.getString("message");
                            Toast.makeText(SignupActivity.this, message, Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            Toast.makeText(SignupActivity.this, "Signup failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    progressBar.setVisibility(View.GONE);
                    btnSignup.setEnabled(true);
                    Toast.makeText(SignupActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                }
            });

        } catch (Exception e) {
            progressBar.setVisibility(View.GONE);
            btnSignup.setEnabled(true);
            Toast.makeText(this, "Error preparing data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void goToPreviousPage() {
        for (EditText editText : otpEditTexts) {
            editText.setText("");
        }
        showPage(1);
    }

    private void showPage(int page) {
        findViewById(R.id.layoutBasicInfo).setVisibility(View.GONE);
        layoutCourseInfo.setVisibility(View.GONE);
        layoutOTP.setVisibility(View.GONE);

        switch (page) {
            case 1:
                findViewById(R.id.layoutBasicInfo).setVisibility(View.VISIBLE);
                currentPage = 1;
                break;
            case 2:
                layoutCourseInfo.setVisibility(View.VISIBLE);
                currentPage = 2;
                break;
            case 3:
                layoutOTP.setVisibility(View.VISIBLE);
                currentPage = 3;
                if (!otpEditTexts.isEmpty()) {
                    otpEditTexts.get(0).requestFocus();
                }
                break;
        }
    }

    private void navigateToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}