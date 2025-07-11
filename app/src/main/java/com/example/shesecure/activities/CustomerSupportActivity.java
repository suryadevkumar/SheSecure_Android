package com.example.shesecure.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import com.example.shesecure.R;
import com.example.shesecure.services.ApiService;
import com.example.shesecure.utils.ApiUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import org.json.JSONObject;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CustomerSupportActivity extends BaseActivity {

    private TextInputEditText etSubject, etMessage;
    private MaterialButton btnSubmit;
    private ApiService apiService;
    private String authToken;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_support);

        // Initialize views
        etSubject = findViewById(R.id.et_subject);
        etMessage = findViewById(R.id.et_message);
        btnSubmit = findViewById(R.id.btn_submit);

        // Initialize progress dialog (spinner)
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Submitting your request...");
        progressDialog.setCancelable(false);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);

        // Initialize API service
        apiService = ApiUtils.initializeApiService(this, ApiService.class);

        // Get auth token from shared preferences
        authToken = "Bearer " + getSharedPreferences("SheSecurePrefs", MODE_PRIVATE)
                .getString("token", "");

        // Set submit button click listener
        btnSubmit.setOnClickListener(v -> submitRequest());
    }

    public void onCallSupportClicked(View view) {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:+916204593307"));
        startActivity(intent);
    }

    private void submitRequest() {
        String subject = etSubject.getText().toString().trim();
        String message = etMessage.getText().toString().trim();

        // Validate inputs
        if (subject.isEmpty()) {
            etSubject.setError("Subject is required");
            return;
        }

        if (message.isEmpty()) {
            etMessage.setError("Message is required");
            return;
        }

        // Disable button and show loading state
        btnSubmit.setEnabled(false);
        btnSubmit.setText("Submitting...");
        progressDialog.show();

        try {
            // Create request body
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("subject", subject);
            jsonObject.put("message", message);

            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json"),
                    jsonObject.toString()
            );

            // Make API call
            Call<ResponseBody> call = apiService.submitCustomerCareRequest(authToken, body);
            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    resetButtonState();
                    progressDialog.dismiss();

                    if (response.isSuccessful()) {
                        Toast.makeText(CustomerSupportActivity.this,
                                "Your request has been submitted successfully!",
                                Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(CustomerSupportActivity.this,
                                "Failed to submit request",
                                Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    resetButtonState();
                    progressDialog.dismiss();
                    Toast.makeText(CustomerSupportActivity.this,
                            "Error: " + t.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            resetButtonState();
            progressDialog.dismiss();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void resetButtonState() {
        btnSubmit.setEnabled(true);
        btnSubmit.setText("Submit Request");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}