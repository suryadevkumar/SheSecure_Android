package com.example.shesecure.activities;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.shesecure.R;
import com.example.shesecure.services.ApiService;
import com.example.shesecure.utils.ApiUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FeedbackActivity extends BaseActivity {

    private static final String TAG = "FeedbackActivity";
    private ImageView[] stars = new ImageView[5];
    private TextInputEditText etReview;
    private MaterialButton btnSubmit;
    private TextView tvCharCount, tvRatingError;
    private ApiService apiService;
    private String authToken;
    private ProgressDialog progressDialog;
    private int selectedRating = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize API service
        apiService = ApiUtils.initializeApiService(this, ApiService.class);
        authToken = "Bearer " + getSharedPreferences("SheSecurePrefs", MODE_PRIVATE)
                .getString("token", "");
        checkExistingFeedback();
    }

    private void initializeFeedbackForm() {
        setContentView(R.layout.activity_feedback);

        // Initialize views
        stars[0] = findViewById(R.id.star1);
        stars[1] = findViewById(R.id.star2);
        stars[2] = findViewById(R.id.star3);
        stars[3] = findViewById(R.id.star4);
        stars[4] = findViewById(R.id.star5);
        etReview = findViewById(R.id.et_review);
        btnSubmit = findViewById(R.id.btn_submit);
        tvCharCount = findViewById(R.id.tv_char_count);
        tvRatingError = findViewById(R.id.tv_rating_error);

        setupCharacterCounter();
        setupRatingStars();

        btnSubmit.setOnClickListener(v -> submitFeedback());
    }

    private void setupCharacterCounter() {
        etReview.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String counterText = s.length() + "/200";
                tvCharCount.setText(counterText);
                tvCharCount.setTextColor(getResources().getColor(
                        s.length() == 200 ? R.color.red_500 : R.color.blue_500
                ));
            }
        });
    }

    private void setupRatingStars() {
        for (int i = 0; i < stars.length; i++) {
            final int rating = i + 1;
            stars[i].setOnClickListener(v -> {
                selectedRating = rating;
                updateStars();
                tvRatingError.setVisibility(View.GONE);
            });
        }
    }

    private void updateStars() {
        for (int i = 0; i < stars.length; i++) {
            stars[i].setImageResource(
                    i < selectedRating ? R.drawable.filled_star : R.drawable.empty_star
            );
        }
    }

    private void checkExistingFeedback() {
        showProgressDialog("Checking for existing feedback...");

        Call<ResponseBody> call = apiService.getUserFeedback(authToken);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                dismissProgressDialog();
                if (response.isSuccessful()) {
                    try {
                        String responseData = response.body().string();
                        Log.d(TAG, "Feedback response: " + responseData);

                        JSONObject jsonResponse = new JSONObject(responseData);
                        if (jsonResponse.getBoolean("success")) {
                            JSONObject feedbackData = jsonResponse.getJSONObject("data");
                            showSubmittedFeedback(feedbackData);
                            return;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing feedback", e);
                    }
                }
                // If we reach here, show empty feedback form
                initializeFeedbackForm();
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                dismissProgressDialog();
                Log.e(TAG, "Failed to check feedback", t);
                initializeFeedbackForm();
            }
        });
    }

    private void showSubmittedFeedback(JSONObject feedbackData) {
        setContentView(R.layout.activity_feedback_submitted);

        try {
            // Extract feedback data
            int rating = feedbackData.getInt("rating");
            String review = feedbackData.getString("review");
            String dateStr = feedbackData.getString("createdAt");

            // Initialize views
            LinearLayout starsLayout = findViewById(R.id.stars_submitted);
            TextView tvReview = findViewById(R.id.tv_review);
            TextView tvDate = findViewById(R.id.tv_date);

            // Set rating stars
            for (int i = 0; i < starsLayout.getChildCount() && i < 5; i++) {
                View starView = starsLayout.getChildAt(i);
                if (starView instanceof ImageView) {
                    ((ImageView) starView).setImageResource(
                            i < rating ? R.drawable.filled_star : R.drawable.empty_star
                    );
                }
            }

            // Set review text
            tvReview.setText(review);

            // Format and set date
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
            Date date = inputFormat.parse(dateStr);
            tvDate.setText("Submitted on " + outputFormat.format(date));

        } catch (Exception e) {
            Log.e(TAG, "Error showing feedback", e);
            Toast.makeText(this, "Error displaying feedback", Toast.LENGTH_SHORT).show();
            initializeFeedbackForm();
        }
    }

    private boolean validateForm() {
        boolean isValid = true;

        if (selectedRating == 0) {
            tvRatingError.setVisibility(View.VISIBLE);
            isValid = false;
        }

        if (etReview.getText().toString().trim().isEmpty()) {
            etReview.setError("Please write your review");
            isValid = false;
        }

        return isValid;
    }

    private void submitFeedback() {
        if (!validateForm()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgressDialog("Submitting feedback...");

        try {
            JSONObject feedbackJson = new JSONObject();
            feedbackJson.put("rating", selectedRating);
            feedbackJson.put("review", etReview.getText().toString().trim());

            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json"),
                    feedbackJson.toString()
            );

            Call<ResponseBody> call = apiService.submitFeedback(authToken, body);
            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    dismissProgressDialog();
                    if (response.isSuccessful()) {
                        try {
                            String responseData = response.body().string();
                            JSONObject jsonResponse = new JSONObject(responseData);
                            if (jsonResponse.getBoolean("success")) {
                                showSubmittedFeedback(jsonResponse.getJSONObject("data"));
                                Toast.makeText(FeedbackActivity.this,
                                        "Thank you for your feedback!",
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing response", e);
                        }
                    }
                    Toast.makeText(FeedbackActivity.this,
                            "Feedback submitted successfully!",
                            Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    dismissProgressDialog();
                    Toast.makeText(FeedbackActivity.this,
                            "Error: " + t.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            dismissProgressDialog();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showProgressDialog(String message) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setCancelable(false);
        }
        progressDialog.setMessage(message);
        progressDialog.show();
    }

    private void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        dismissProgressDialog();
        super.onDestroy();
    }
}