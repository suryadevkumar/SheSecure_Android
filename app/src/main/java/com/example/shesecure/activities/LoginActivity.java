package com.example.shesecure.activities;

import android.content.Intent;
import android.content.SharedPreferences;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.shesecure.MainActivity;
import com.example.shesecure.R;
import com.example.shesecure.services.ApiService;
import com.example.shesecure.utils.ApiUtils;
import com.example.shesecure.utils.AuthManager;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail;
    private LinearLayout layoutOTP, otpContainer;
    private Button btnSendOTP, btnLogin;
    private TextView tvSignupLink;
    private ProgressBar progressBar;
    private List<EditText> otpEditTexts = new ArrayList<>();
    private static final int OTP_LENGTH = 6;
    private CountDownTimer countDownTimer;
    private boolean isOtpVisible = false;
    private ApiService apiService;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        setStatusBarColor();

        setupNavigationDrawer();
        initializeViews();
        setupOTPInput();

        apiService = ApiUtils.initializeApiService(this, ApiService.class);

        btnSendOTP.setOnClickListener(v -> handleSendOTPClick());
        btnLogin.setOnClickListener(v -> handleLogin());
        tvSignupLink.setOnClickListener(v -> navigateToSignup());
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
                startActivity(new Intent(this, LoginActivity.class));
                finish();
            }
            else if (id == R.id.nav_login) {
                drawerLayout.closeDrawer(GravityCompat.START);
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
                    updateLoginButtonState();
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

    private void handleSendOTPClick() {
        if (!isOtpVisible) {
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
                    if (response.body() != null) {
                        responseBody = response.body().string();
                    } else if (response.errorBody() != null) {
                        responseBody = response.errorBody().string();
                    }

                    if (responseBody != null && !responseBody.isEmpty()) {
                        JSONObject jsonObject = new JSONObject(responseBody);
                        if (jsonObject.has("success") && jsonObject.getBoolean("success")) {
                            showOTPSection();
                            sendEmailOTP();
                        } else {
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
        TextInputLayout tilEmail = findViewById(R.id.tilEmail);
        tilEmail.setBoxStrokeColor(ContextCompat.getColor(this, R.color.gray_900));


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
            btnLogin.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.pink_300));
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

        String otpJson = String.format("{\"emailOTP\":\"%s\"}", emailOTP);
        RequestBody otpBody = RequestBody.create(MediaType.parse("application/json"), otpJson);

        Call<ResponseBody> verifyCall = apiService.verifyEmailOTP(otpBody);
        verifyCall.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
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
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        JSONObject jsonObject = new JSONObject(responseBody);

                        if (jsonObject.has("token") && jsonObject.has("user")) {
                            String token = jsonObject.getString("token");
                            JSONObject user = jsonObject.getJSONObject("user");

                            // Save all data using AuthManager
                            new AuthManager(LoginActivity.this).saveUserData(token, user);

                            proceedToDashboard();
                        }
                        else {
                            Toast.makeText(LoginActivity.this, "Invalid response format", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
                        Toast.makeText(LoginActivity.this, "Login failed: " + errorBody, Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(LoginActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnLogin.setEnabled(true);
                Toast.makeText(LoginActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void proceedToDashboard() {
        Intent intent = new Intent(LoginActivity.this, UserDashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
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