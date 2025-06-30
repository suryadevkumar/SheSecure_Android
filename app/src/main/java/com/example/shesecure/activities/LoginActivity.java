package com.example.shesecure.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.shesecure.MainActivity;
import com.example.shesecure.R;
import com.example.shesecure.services.ApiService;
import com.example.shesecure.utils.ApiUtils;
import com.example.shesecure.utils.LocationHelper;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    // Views
    private EditText etEmail;
    private LinearLayout layoutOTP, otpContainer;
    private Button btnSendOTP, btnLogin;
    private TextView tvSignupLink;
    private ProgressBar progressBar;

    // OTP Variables
    private List<EditText> otpEditTexts = new ArrayList<>();
    private static final int OTP_LENGTH = 6;
    private CountDownTimer countDownTimer;
    private boolean isOtpVisible = false;

    // API Service
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initializeViews();
        setupOTPInput();

        apiService = ApiUtils.initializeApiService(this);

        btnSendOTP.setOnClickListener(v -> handleSendOTPClick());
        btnLogin.setOnClickListener(v -> handleLogin());
        tvSignupLink.setOnClickListener(v -> navigateToSignup());
    }

    private void initializeViews() {
        etEmail = findViewById(R.id.etEmail);
        layoutOTP = findViewById(R.id.layoutOTP);
        otpContainer = findViewById(R.id.otpContainer);
        btnSendOTP = findViewById(R.id.btnSendOTP);
        btnLogin = findViewById(R.id.btnLogin);
        tvSignupLink = findViewById(R.id.tvSignupLink);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupOTPInput() {
        otpContainer.removeAllViews();
        otpEditTexts.clear();

        for (int i = 0; i < OTP_LENGTH; i++) {
            EditText editText = new EditText(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    dpToPx(45),
                    dpToPx(45)
            );
            params.setMargins(dpToPx(4), 0, dpToPx(4), 0);
            editText.setLayoutParams(params);

            editText.setBackgroundResource(R.drawable.otp_box_background);
            editText.setTextColor(ContextCompat.getColor(this, R.color.black));
            editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            editText.setInputType(InputType.TYPE_CLASS_TEXT);
            editText.setGravity(Gravity.CENTER);
            editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(1)});

            // Add text changed listener to move focus
            final int position = i;
            editText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1 && position < OTP_LENGTH - 1) {
                        otpEditTexts.get(position + 1).requestFocus();
                    }
                    updateLoginButtonState();
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });

            // Handle backspace to move to previous box
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

    private void handleSendOTPClick() {
        if (!isOtpVisible) {
            // First click - validate email and send OTP
            String email = etEmail.getText().toString().trim();

            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email!", Toast.LENGTH_SHORT).show();
                etEmail.setError("Email is required");
                return;
            }

            checkUserExists(email);
        } else {
            sendEmailOTP();
        }
    }

    private void checkUserExists(String email) {
        progressBar.setVisibility(View.VISIBLE);
        btnSendOTP.setEnabled(false);

        String json = String.format("{\"email\":\"%s\"}", email);
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);

        Call<ResponseBody> call = apiService.checkEmailExists(body);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                progressBar.setVisibility(View.GONE);
                btnSendOTP.setEnabled(true);

                try {
                    String responseBody = null;

                    // Handle both successful and error responses
                    if (response.body() != null) {
                        responseBody = response.body().string();
                    } else if (response.errorBody() != null) {
                        responseBody = response.errorBody().string();
                    }

                    if (responseBody != null && !responseBody.isEmpty()) {
                        JSONObject jsonObject = new JSONObject(responseBody);

                        // Check the success field in JSON (not HTTP status)
                        if (jsonObject.has("success") && jsonObject.getBoolean("success")) {
                            // User exists and can login, show OTP section and send OTP
                            Toast.makeText(LoginActivity.this, "User found! Sending OTP...", Toast.LENGTH_SHORT).show();
                            showOTPSection();
                            sendEmailOTP();
                        } else {
                            // Show error message (user doesn't exist or other error)
                            String message = jsonObject.optString("message", "User not found");
                            Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, "Empty response from server", Toast.LENGTH_SHORT).show();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(LoginActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnSendOTP.setEnabled(true);
                Toast.makeText(LoginActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showOTPSection() {
        isOtpVisible = true;
        layoutOTP.setVisibility(View.VISIBLE);
        btnLogin.setVisibility(View.VISIBLE);
        etEmail.setEnabled(false);
        etEmail.setBackgroundColor(ContextCompat.getColor(this, R.color.gray_300));

        // Focus on first OTP input
        if (!otpEditTexts.isEmpty()) {
            otpEditTexts.get(0).requestFocus();
        }

        updateLoginButtonState();
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
                    Toast.makeText(LoginActivity.this, "OTP sent to your email", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(LoginActivity.this, "Failed to send OTP", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(LoginActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startOTPTimer() {
        btnSendOTP.setEnabled(false);
        countDownTimer = new CountDownTimer(59000, 1000) {
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

    private void updateLoginButtonState() {
        String otp = getEnteredOTP();
        if (otp.length() == OTP_LENGTH) {
            btnLogin.setEnabled(true);
            btnLogin.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.pink_600));
        } else {
            btnLogin.setEnabled(false);
            btnLogin.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.pink_400));
        }
    }

    private String getEnteredOTP() {
        StringBuilder otp = new StringBuilder();
        for (EditText editText : otpEditTexts) {
            otp.append(editText.getText().toString());
        }
        return otp.toString();
    }

    private void handleLogin() {
        String otp = getEnteredOTP();
        if (otp.length() != OTP_LENGTH) {
            Toast.makeText(this, "Please enter the complete OTP", Toast.LENGTH_SHORT).show();
            return;
        }

        String email = etEmail.getText().toString().trim();
        loginUser(email, otp);
    }

    private void loginUser(String email, String emailOTP) {
        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        // First verify OTP
        String otpJson = String.format("{\"emailOTP\":\"%s\"}", emailOTP);
        RequestBody otpBody = RequestBody.create(MediaType.parse("application/json"), otpJson);

        Call<ResponseBody> verifyCall = apiService.verifyEmailOTP(otpBody);
        verifyCall.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    // OTP verified, now login
                    performLogin(email);
                } else {
                    progressBar.setVisibility(View.GONE);
                    btnLogin.setEnabled(true);
                    Toast.makeText(LoginActivity.this, "Invalid or expired OTP", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnLogin.setEnabled(true);
                Toast.makeText(LoginActivity.this, "Failed to verify OTP", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void performLogin(String email) {
        String loginJson = String.format("{\"email\":\"%s\"}", email);
        RequestBody loginBody = RequestBody.create(MediaType.parse("application/json"), loginJson);

        Call<ResponseBody> loginCall = apiService.login(loginBody);
        loginCall.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                progressBar.setVisibility(View.GONE);
                btnLogin.setEnabled(true);

                try {
                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        JSONObject jsonObject = new JSONObject(responseBody);

                        if (jsonObject.has("token")) {
                            String token = jsonObject.getString("token");
                            JSONObject user = jsonObject.getJSONObject("user");

                            // Save token and user data to SharedPreferences
                            SharedPreferences preferences = getSharedPreferences("SheSecurePrefs", MODE_PRIVATE);
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putString("token", token);
                            editor.putString("user", user.toString());
                            editor.apply();

                            Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();

                            // Check if we should start location tracking
                            if (LocationHelper.shouldTrackLocation(LoginActivity.this)) {
                                if (!LocationHelper.checkLocationPermissions(LoginActivity.this)) {
                                    LocationHelper.requestLocationPermissions(LoginActivity.this);
                                } else {
                                    LocationHelper.startLocationService(LoginActivity.this);
                                }
                            }

                            // Navigate to dashboard
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        }
                    }
                } catch (Exception e) {
                    Toast.makeText(LoginActivity.this, "Error processing login response", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnLogin.setEnabled(true);
                Toast.makeText(LoginActivity.this, "Network error during login", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateToSignup() {
        Intent intent = new Intent(this, SignupActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}