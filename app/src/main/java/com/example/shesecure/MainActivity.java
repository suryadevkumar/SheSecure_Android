package com.example.shesecure;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.core.widget.NestedScrollView;

import com.bumptech.glide.Glide;
import com.example.shesecure.activities.LoginActivity;
import com.example.shesecure.activities.SignupActivity;
import com.example.shesecure.models.Feedback;
import com.example.shesecure.models.FeedbackResponse;
import com.example.shesecure.services.ApiService;
import com.example.shesecure.utils.ApiUtils;
import com.example.shesecure.utils.LocationHelper;
import com.google.android.material.navigation.NavigationView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private NestedScrollView scrollView;
    private Button getStarted;

    // Feedback views
    private LinearLayout feedbackItemsContainer;
    private ProgressBar feedbackProgress;
    private TextView feedbackEmptyMessage;
    private HorizontalScrollView feedbackScrollView;
    private Handler autoScrollHandler = new Handler();
    private Runnable autoScrollRunnable;

    // API Service
    private ApiService apiService;

    // Feedback data
    private List<Feedback> feedbackList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize API service
        apiService = ApiUtils.initializeApiService(this);

        View homeSection = findViewById(R.id.home_section);
        getStarted = homeSection.findViewById(R.id.getStarted);

        // Initialize views
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        scrollView = findViewById(R.id.scroll_view);

        // Initialize feedback views
        initializeFeedbackViews();

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.menu);

        // Setup navigation drawer
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                scrollToSection(R.id.home_section);
            } else if (id == R.id.nav_features) {
                scrollToSection(R.id.features_section);
            } else if (id == R.id.nav_about) {
                scrollToSection(R.id.about_section);
            } else if (id == R.id.nav_contact) {
                scrollToSection(R.id.contact_section);
            } else if (id == R.id.nav_login) {
                startActivity(new Intent(this, LoginActivity.class));
            } else if (id == R.id.nav_signup) {
                startActivity(new Intent(this, SignupActivity.class));
            }

            drawerLayout.closeDrawers();
            return true;
        });

        getStarted.setOnClickListener(view -> {
            startActivity(new Intent(this, SignupActivity.class));
        });

        // Load feedbacks
        loadFeedbacks();

        // Check if we should track location
        if (LocationHelper.shouldTrackLocation(this)) {
            if (!LocationHelper.checkLocationPermissions(this)) {
                LocationHelper.requestLocationPermissions(this);
            } else {
                LocationHelper.startLocationService(this);
            }
        }
    }

    private void initializeFeedbackViews() {
        feedbackItemsContainer = findViewById(R.id.feedbackItemsContainer);
        feedbackProgress = findViewById(R.id.feedbackProgress);
        feedbackEmptyMessage = findViewById(R.id.feedbackEmptyMessage);
        feedbackScrollView = findViewById(R.id.feedbackScrollView);
    }

    private void loadFeedbacks() {
        showLoading();

        Call<FeedbackResponse> call = apiService.getAllFeedbacks();
        call.enqueue(new Callback<FeedbackResponse>() {
            @Override
            public void onResponse(Call<FeedbackResponse> call, Response<FeedbackResponse> response) {
                hideLoading();

                if (response.isSuccessful() && response.body() != null) {
                    FeedbackResponse feedbackResponse = response.body();

                    if (feedbackResponse.isSuccess() && feedbackResponse.getData() != null) {
                        feedbackList = feedbackResponse.getData();

                        if (feedbackList.isEmpty()) {
                            showEmptyState();
                        } else {
                            displayFeedbacks();
                            startAutoScroll();
                        }
                    } else {
                        showEmptyState();
                    }
                } else {
                    showEmptyState();
                    Toast.makeText(MainActivity.this, "Failed to load feedbacks", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<FeedbackResponse> call, Throwable t) {
                hideLoading();
                showEmptyState();
                Toast.makeText(MainActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLoading() {
        feedbackProgress.setVisibility(View.VISIBLE);
        feedbackScrollView.setVisibility(View.GONE);
        feedbackEmptyMessage.setVisibility(View.GONE);
    }

    private void hideLoading() {
        feedbackProgress.setVisibility(View.GONE);
    }

    private void showEmptyState() {
        feedbackEmptyMessage.setVisibility(View.VISIBLE);
        feedbackScrollView.setVisibility(View.GONE);
    }

    private void displayFeedbacks() {
        feedbackScrollView.setVisibility(View.VISIBLE);
        feedbackEmptyMessage.setVisibility(View.GONE);

        feedbackItemsContainer.removeAllViews();

        // Create doubled list for infinite scroll effect
        List<Feedback> doubledList = new ArrayList<>(feedbackList);
        doubledList.addAll(feedbackList);

        for (Feedback feedback : doubledList) {
            View feedbackItem = createFeedbackItem(feedback);
            feedbackItemsContainer.addView(feedbackItem);
        }
    }

    private View createFeedbackItem(Feedback feedback) {
        View itemView = LayoutInflater.from(this).inflate(R.layout.feedback_card, null);

        // Find views
        ImageView ivProfileImage = itemView.findViewById(R.id.ivProfileImage);
        TextView tvUserName = itemView.findViewById(R.id.tvUserName);
        TextView tvDate = itemView.findViewById(R.id.tvDate);
        TextView tvReview = itemView.findViewById(R.id.tvReview);
        TextView tvTruncateNotice = itemView.findViewById(R.id.tvTruncateNotice);

        // Star rating views
        ImageView[] stars = {
                itemView.findViewById(R.id.star1),
                itemView.findViewById(R.id.star2),
                itemView.findViewById(R.id.star3),
                itemView.findViewById(R.id.star4),
                itemView.findViewById(R.id.star5)
        };

        // Set user data
        if (feedback.getUserId() != null) {
            String fullName = (feedback.getUserId().getFirstName() != null ? feedback.getUserId().getFirstName() : "") +
                    " " + (feedback.getUserId().getLastName() != null ? feedback.getUserId().getLastName() : "");
            tvUserName.setText(fullName.trim());

            // Load profile image
            if (feedback.getUserId().getAdditionalDetails() != null &&
                    feedback.getUserId().getAdditionalDetails().getImage() != null) {
                Glide.with(this)
                        .load(feedback.getUserId().getAdditionalDetails().getImage())
                        .placeholder(R.drawable.person)
                        .circleCrop()
                        .into(ivProfileImage);
            }
        }

        // Set rating stars
        for (int i = 0; i < 5; i++) {
            if (i < feedback.getRating()) {
                stars[i].setColorFilter(ContextCompat.getColor(this, R.color.yellow_400));
            } else {
                stars[i].setColorFilter(ContextCompat.getColor(this, R.color.gray_300));
            }
        }

        // Set date
        String formattedDate = formatDate(feedback.getCreatedAt());
        tvDate.setText(formattedDate);

        // Set review text (truncate if necessary)
        String review = feedback.getReview();
        String truncatedReview = truncateReview(review);
        tvReview.setText(truncatedReview);

        // Show truncation notice if review was truncated
        if (!review.equals(truncatedReview)) {
            tvTruncateNotice.setVisibility(View.VISIBLE);
        }

        return itemView;
    }

    private String truncateReview(String text) {
        if (text == null) return "";

        String[] words = text.split(" ");
        if (words.length > 200) {
            StringBuilder truncated = new StringBuilder();
            for (int i = 0; i < 200; i++) {
                truncated.append(words[i]).append(" ");
            }
            return truncated.toString().trim() + "...";
        }
        return text;
    }

    private String formatDate(String dateString) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
            Date date = inputFormat.parse(dateString);
            return outputFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return dateString;
        }
    }

    private void startAutoScroll() {
        autoScrollRunnable = new Runnable() {
            @Override
            public void run() {
                if (feedbackScrollView != null && feedbackItemsContainer != null) {
                    int scrollX = feedbackScrollView.getScrollX();
                    int itemWidth = 600;
                    int maxScroll = feedbackItemsContainer.getWidth() - feedbackScrollView.getWidth();

                    if (scrollX < maxScroll) {
                        feedbackScrollView.smoothScrollBy(itemWidth, 0);
                    } else {
                        // Reset to beginning for infinite scroll effect
                        feedbackScrollView.post(() -> feedbackScrollView.scrollTo(0, 0));
                    }

                    autoScrollHandler.postDelayed(this, 3000);
                }
            }
        };
        autoScrollHandler.postDelayed(autoScrollRunnable, 3000);
    }

    private void scrollToSection(int sectionId) {
        scrollView.post(() -> {
            View section = findViewById(sectionId);
            if (section != null) {
                scrollView.smoothScrollTo(0, section.getTop());
            }
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (autoScrollHandler != null && autoScrollRunnable != null) {
            autoScrollHandler.removeCallbacks(autoScrollRunnable);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LocationHelper.LOCATION_PERMISSION_REQUEST_CODE) {
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (allPermissionsGranted && LocationHelper.shouldTrackLocation(this)) {
                LocationHelper.startLocationService(this);
            } else {
                Toast.makeText(this,
                        "Location permissions are required for safety features",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (LocationHelper.shouldTrackLocation(this) &&
                LocationHelper.checkLocationPermissions(this)) {
            LocationHelper.startLocationService(this);
        }
    }
}