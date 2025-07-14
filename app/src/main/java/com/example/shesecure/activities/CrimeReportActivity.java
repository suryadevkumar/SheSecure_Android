package com.example.shesecure.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.shesecure.R;
import com.example.shesecure.adapters.CrimeReportAdapter;
import com.example.shesecure.models.CrimeReport;
import com.example.shesecure.services.ApiService;
import com.example.shesecure.utils.ApiUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CrimeReportActivity extends BaseActivity
        implements CrimeReportAdapter.OnReportActionListener {

    private static final String TAG = "CrimeReportActivity";
    private RecyclerView reportsRecyclerView;
    private CrimeReportAdapter adapter;
    private List<CrimeReport> reportList = new ArrayList<>();
    private ProgressBar progressBar;
    private TextView emptyStateText;
    private FloatingActionButton fabNewReport;
    private SwipeRefreshLayout swipeRefreshLayout;
    private String userType;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crime_report);

        initializeViews();
        setupUserType();
        setupRecyclerView();
        setupClickListeners();
        fetchReports();
    }

    private void initializeViews() {
        progressBar = findViewById(R.id.progressBar);
        emptyStateText = findViewById(R.id.emptyStateText);
        reportsRecyclerView = findViewById(R.id.reportsRecyclerView);
        fabNewReport = findViewById(R.id.fabNewReport);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
    }

    private void setupUserType() {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        userType = prefs.getString("userType", "User");
        apiService = ApiUtils.initializeApiService(this, ApiService.class);
    }

    private void setupRecyclerView() {
        reportsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CrimeReportAdapter(reportList, userType, this);
        reportsRecyclerView.setAdapter(adapter);
    }

    private void setupClickListeners() {
        fabNewReport.setOnClickListener(v -> {
            if (userType.equals("User")) {
                startActivity(new Intent(this, CrimeReportActivity.class));
            } else {
                Toast.makeText(this, "Only users can create reports", Toast.LENGTH_SHORT).show();
            }
        });

        swipeRefreshLayout.setOnRefreshListener(this::fetchReports);
    }

    private void fetchReports() {
        progressBar.setVisibility(View.VISIBLE);
        emptyStateText.setVisibility(View.GONE);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String token = "Bearer " + prefs.getString("token", "");

        Call<ResponseBody> call = userType.equals("User")
                ? apiService.getUserCrimeReports(token)
                : apiService.getAllCrimeReports(token);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);

                try {
                    if (response.isSuccessful() && response.body() != null) {
                        handleSuccessfulResponse(response.body().string());
                    } else {
                        showError("Server error: " + response.code());
                    }
                } catch (Exception e) {
                    handleResponseError(e);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                handleNetworkError(t);
            }
        });
    }

    private void handleSuccessfulResponse(String responseString) throws JSONException {
        Log.d(TAG, "Response: " + responseString);
        JSONObject jsonResponse = new JSONObject(responseString);

        if (jsonResponse.getBoolean("success")) {
            parseReports(jsonResponse.getJSONArray("reports"));
        } else {
            showError(jsonResponse.optString("message", "Error fetching reports"));
        }
    }

    private void parseReports(JSONArray reportsArray) throws JSONException {
        reportList.clear();
        for (int i = 0; i < reportsArray.length(); i++) {
            try {
                CrimeReport report = new CrimeReport(reportsArray.getJSONObject(i));
                reportList.add(report);
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing report at index " + i, e);
            }
        }

        adapter.notifyDataSetChanged();
        emptyStateText.setVisibility(reportList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void handleResponseError(Exception e) {
        Log.e(TAG, "Error processing response", e);
        showError("Error processing data");
    }

    private void handleNetworkError(Throwable t) {
        progressBar.setVisibility(View.GONE);
        swipeRefreshLayout.setRefreshing(false);
        Log.e(TAG, "Network error", t);
        showError("Network error: " + t.getMessage());
    }

    private void showError(String message) {
        emptyStateText.setVisibility(View.VISIBLE);
        emptyStateText.setText(message);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onVerifyReport(String reportId) {
        new AlertDialog.Builder(this)
                .setTitle("Verify Report")
                .setMessage("Are you sure you want to verify this report?")
                .setPositiveButton("Verify", (dialog, which) -> verifyReport(reportId))
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onDeleteReport(String reportId) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Report")
                .setMessage("Are you sure you want to delete this report?")
                .setPositiveButton("Delete", (dialog, which) -> deleteReport(reportId))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void verifyReport(String reportId) {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String token = "Bearer " + prefs.getString("token", "");

        apiService.verifyCrimeReport(token, reportId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                handleVerificationResponse(response);
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                handleVerificationFailure(t);
            }
        });
    }

    private void handleVerificationResponse(Response<ResponseBody> response) {
        try {
            if (response.isSuccessful() && response.body() != null) {
                JSONObject jsonResponse = new JSONObject(response.body().string());
                showToastAndRefresh(jsonResponse.getString("message"));
            } else {
                showToast("Verification failed: " + response.code());
            }
        } catch (Exception e) {
            handleVerificationError(e);
        }
    }

    private void handleVerificationFailure(Throwable t) {
        Log.e(TAG, "Network error verifying report", t);
        showToast("Network error");
    }

    private void deleteReport(String reportId) {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String token = "Bearer " + prefs.getString("token", "");

        apiService.deleteCrimeReport(token, reportId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                handleDeletionResponse(response);
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                handleDeletionFailure(t);
            }
        });
    }

    private void handleDeletionResponse(Response<ResponseBody> response) {
        try {
            if (response.isSuccessful() && response.body() != null) {
                JSONObject jsonResponse = new JSONObject(response.body().string());
                showToastAndRefresh(jsonResponse.getString("message"));
            } else {
                showToast("Deletion failed: " + response.code());
            }
        } catch (Exception e) {
            handleDeletionError(e);
        }
    }

    private void handleDeletionFailure(Throwable t) {
        Log.e(TAG, "Network error deleting report", t);
        showToast("Network error");
    }

    @Override
    public void onPhotoClicked(String photoUrl) {
        Intent intent = new Intent(this, FullScreenImageActivity.class);
        intent.putExtra("photo_url", photoUrl);
        startActivity(intent);
    }

    @Override
    public void onVideoClicked(String videoUrl) {
        Intent intent = new Intent(this, VideoPlayerActivity.class);
        intent.putExtra("video_url", videoUrl);
        startActivity(intent);
    }

    private void showToastAndRefresh(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        fetchReports();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void handleVerificationError(Exception e) {
        Log.e(TAG, "Error verifying report", e);
        showToast("Error verifying report");
    }

    private void handleDeletionError(Exception e) {
        Log.e(TAG, "Error deleting report", e);
        showToast("Error deleting report");
    }
}