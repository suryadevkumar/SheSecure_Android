package com.example.shesecure.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
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

public class CrimeReportActivity extends AppCompatActivity {

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

        // Initialize views
        progressBar = findViewById(R.id.progressBar);
        emptyStateText = findViewById(R.id.emptyStateText);
        reportsRecyclerView = findViewById(R.id.reportsRecyclerView);
        fabNewReport = findViewById(R.id.fabNewReport);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        // Get user type from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        userType = prefs.getString("userType", "User");

        // Initialize API service
        apiService = ApiUtils.initializeApiService(this, ApiService.class);

        // Set up RecyclerView
        reportsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CrimeReportAdapter(reportList, userType, this);
        reportsRecyclerView.setAdapter(adapter);

        // Set click listeners
        fabNewReport.setOnClickListener(v -> {
            if (userType.equals("User")) {
                startActivity(new Intent(this, CrimeReportActivity.class));
            } else {
                Toast.makeText(this, "Only users can create reports", Toast.LENGTH_SHORT).show();
            }
        });

        swipeRefreshLayout.setOnRefreshListener(this::fetchReports);

        // Fetch initial data
        fetchReports();
    }

    private void fetchReports() {
        progressBar.setVisibility(View.VISIBLE);
        emptyStateText.setVisibility(View.GONE);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String token = "Bearer " + prefs.getString("token", "");

        Call<ResponseBody> call;
        if (userType.equals("User")) {
            call = apiService.getUserCrimeReports(token);
        } else {
            call = apiService.getAllCrimeReports(token);
        }

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseString = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseString);
                        JSONArray reportsArray = jsonResponse.getJSONArray("reports");

                        reportList.clear();
                        for (int i = 0; i < reportsArray.length(); i++) {
                            CrimeReport report = new CrimeReport(reportsArray.getJSONObject(i));
                            reportList.add(report);
                        }
                        adapter.notifyDataSetChanged();

                        if (reportList.isEmpty()) {
                            emptyStateText.setVisibility(View.VISIBLE);
                        }
                    } else {
                        emptyStateText.setVisibility(View.VISIBLE);
                        Toast.makeText(CrimeReportActivity.this, "Error fetching reports", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    emptyStateText.setVisibility(View.VISIBLE);
                    Toast.makeText(CrimeReportActivity.this, "Error parsing data", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
                emptyStateText.setVisibility(View.VISIBLE);
                Toast.makeText(CrimeReportActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void verifyReport(String reportId) {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String token = "Bearer " + prefs.getString("token", "");

        apiService.verifyCrimeReport(token, reportId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        JSONObject jsonResponse = new JSONObject(response.body().string());
                        Toast.makeText(CrimeReportActivity.this, jsonResponse.getString("message"), Toast.LENGTH_SHORT).show();
                        fetchReports(); // Refresh the list
                    } else {
                        Toast.makeText(CrimeReportActivity.this, "Verification failed", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(CrimeReportActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void deleteReport(String reportId) {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String token = "Bearer " + prefs.getString("token", "");

        apiService.deleteCrimeReport(token, reportId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        JSONObject jsonResponse = new JSONObject(response.body().string());
                        Toast.makeText(CrimeReportActivity.this, jsonResponse.getString("message"), Toast.LENGTH_SHORT).show();
                        fetchReports(); // Refresh the list
                    } else {
                        Toast.makeText(CrimeReportActivity.this, "Deletion failed", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(CrimeReportActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}