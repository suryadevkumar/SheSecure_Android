package com.example.shesecure.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ScrollView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shesecure.R;
import com.example.shesecure.adapters.CommentsAdapter;
import com.example.shesecure.adapters.CrimePhotosAdapter;
import com.example.shesecure.models.Comment;
import com.example.shesecure.models.Crime;
import com.example.shesecure.models.CrimeInteraction;
import com.example.shesecure.services.ApiService;
import com.example.shesecure.utils.ApiUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CrimeDetailsActivity extends AppCompatActivity {

    // Views
    private TextView tvCrimeType, tvCrimeDescription, tvReportedDate, tvErrorMessage;
    private TextView tvSupportCount, tvUnsupportCount, tvCommentsCount;
    private Button btnSupport, btnUnsupport, btnPostComment;
    private EditText etComment;
    private ImageButton btnClose;
    private ProgressBar progressBar;
    private RecyclerView rvComments, rvCrimePhotos;
    private LinearLayout layoutError, layoutInteractions, layoutCommentForm;
    private ScrollView scrollViewMain;

    // Data
    private Crime crime;
    private CrimeInteraction interactions;
    private List<Comment> commentsList = new ArrayList<>();
    private List<String> crimePhotosList = new ArrayList<>();
    private CommentsAdapter commentsAdapter;
    private CrimePhotosAdapter crimePhotosAdapter;

    // API
    private ApiService apiService;
    private String token;
    private String currentUserId;

    // State
    private boolean isLoading = false;
    private String userInteractionStatus = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crime_details);

        initializeViews();
        setupRecyclerViews();
        getCrimeDataFromIntent();
        setupClickListeners();

        // Initialize API service and get token
        apiService = ApiUtils.initializeApiService(this);
        getTokenFromPreferences();

        if (crime != null && crime.getId() != null) {
            fetchInteractions();
        }
    }

    private void initializeViews() {
        // TextViews
        tvCrimeType = findViewById(R.id.tvCrimeType);
        tvCrimeDescription = findViewById(R.id.tvCrimeDescription);
        tvReportedDate = findViewById(R.id.tvReportedDate);
        tvErrorMessage = findViewById(R.id.tvErrorMessage);
        tvSupportCount = findViewById(R.id.tvSupportCount);
        tvUnsupportCount = findViewById(R.id.tvUnsupportCount);
        tvCommentsCount = findViewById(R.id.tvCommentsCount);

        // Buttons
        btnSupport = findViewById(R.id.btnSupport);
        btnUnsupport = findViewById(R.id.btnUnsupport);
        btnPostComment = findViewById(R.id.btnPostComment);
        btnClose = findViewById(R.id.btnClose);

        // EditText
        etComment = findViewById(R.id.etComment);

        // Other views
        progressBar = findViewById(R.id.progressBar);
        rvComments = findViewById(R.id.rvComments);
        rvCrimePhotos = findViewById(R.id.rvCrimePhotos);
        layoutError = findViewById(R.id.layoutError);
        layoutInteractions = findViewById(R.id.layoutInteractions);
        layoutCommentForm = findViewById(R.id.layoutCommentForm);
        scrollViewMain = findViewById(R.id.scrollViewMain);
    }

    private void setupRecyclerViews() {
        // Comments RecyclerView
        commentsAdapter = new CommentsAdapter(commentsList, currentUserId);
        rvComments.setLayoutManager(new LinearLayoutManager(this));
        rvComments.setAdapter(commentsAdapter);

        // Crime Photos RecyclerView
        crimePhotosAdapter = new CrimePhotosAdapter(crimePhotosList);
        rvCrimePhotos.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvCrimePhotos.setAdapter(crimePhotosAdapter);
    }

    private void getCrimeDataFromIntent() {
        // Get crime data from intent
        String crimeJson = getIntent().getStringExtra("crime_data");
        if (crimeJson != null) {
            try {
                JSONObject crimeObj = new JSONObject(crimeJson);
                crime = parseCrimeFromJson(crimeObj);
                displayCrimeDetails();
            } catch (Exception e) {
                e.printStackTrace();
                showError("Error loading crime details");
            }
        } else {
            showError("No crime data provided");
        }
    }

    private Crime parseCrimeFromJson(JSONObject jsonObject) throws Exception {
        Crime crime = new Crime();
        crime.setId(jsonObject.getString("_id"));
        crime.setTypeOfCrime(jsonObject.getString("typeOfCrime"));
        crime.setDescription(jsonObject.getString("description"));
        crime.setCreatedAt(jsonObject.getString("createdAt"));

        // Parse crime photos
        if (jsonObject.has("crimePhotos")) {
            JSONArray photosArray = jsonObject.getJSONArray("crimePhotos");
            List<String> photos = new ArrayList<>();
            for (int i = 0; i < photosArray.length(); i++) {
                photos.add(photosArray.getString(i));
            }
            crime.setCrimePhotos(photos);
        }

        return crime;
    }

    private void displayCrimeDetails() {
        if (crime != null) {
            tvCrimeType.setText(crime.getTypeOfCrime());
            tvCrimeDescription.setText(crime.getDescription());

            // Format and display date
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault());
                Date date = inputFormat.parse(crime.getCreatedAt());
                tvReportedDate.setText("Reported: " + outputFormat.format(date));
            } catch (Exception e) {
                tvReportedDate.setText("Reported: " + crime.getCreatedAt());
            }

            // Display crime photos
            if (crime.getCrimePhotos() != null && !crime.getCrimePhotos().isEmpty()) {
                crimePhotosList.clear();
                crimePhotosList.addAll(crime.getCrimePhotos());
                crimePhotosAdapter.notifyDataSetChanged();
                rvCrimePhotos.setVisibility(View.VISIBLE);
            } else {
                rvCrimePhotos.setVisibility(View.GONE);
            }
        }
    }

    private void setupClickListeners() {
        btnClose.setOnClickListener(v -> finish());

        btnSupport.setOnClickListener(v -> handleInteraction("Support"));

        btnUnsupport.setOnClickListener(v -> handleInteraction("Unsupport"));

        btnPostComment.setOnClickListener(v -> handleCommentSubmit());
    }

    private void getTokenFromPreferences() {
        SharedPreferences preferences = getSharedPreferences("SheSecurePrefs", MODE_PRIVATE);
        token = preferences.getString("token", null);

        // Get current user ID
        String userJson = preferences.getString("user", null);
        if (userJson != null) {
            try {
                JSONObject userObj = new JSONObject(userJson);
                currentUserId = userObj.getString("_id");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void fetchInteractions() {
        if (isLoading || crime == null) return;

        setLoading(true);
        hideError();

        Call<ResponseBody> call = apiService.getCrimeInteractions(
                "Bearer " + token,
                crime.getId()
        );

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                setLoading(false);

                try {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        JSONObject jsonObject = new JSONObject(responseBody);

                        if (jsonObject.has("success") && jsonObject.getBoolean("success")) {
                            JSONObject data = jsonObject.getJSONObject("data");
                            parseInteractionsData(data);
                            updateUI();
                        } else {
                            showError(jsonObject.optString("message", "Failed to load interactions"));
                        }
                    } else if (response.errorBody() != null) {
                        String errorBody = response.errorBody().string();
                        JSONObject errorObj = new JSONObject(errorBody);
                        showError(errorObj.optString("message", "Failed to load interactions"));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    showError("Error processing response");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                setLoading(false);
                showError("Network error: " + t.getMessage());
            }
        });
    }

    private void parseInteractionsData(JSONObject data) throws Exception {
        interactions = new CrimeInteraction();
        interactions.setSupports(data.optInt("supports", 0));
        interactions.setUnsupports(data.optInt("unsupports", 0));

        // Parse user interaction
        if (data.has("userInteraction") && !data.isNull("userInteraction")) {
            JSONObject userInteraction = data.getJSONObject("userInteraction");
            userInteractionStatus = userInteraction.optString("supportStatus", null);
        } else {
            userInteractionStatus = null;
        }

        // Parse comments
        commentsList.clear();
        if (data.has("comments")) {
            JSONArray commentsArray = data.getJSONArray("comments");
            for (int i = 0; i < commentsArray.length(); i++) {
                JSONObject commentObj = commentsArray.getJSONObject(i);
                Comment comment = parseCommentFromJson(commentObj);
                commentsList.add(comment);
            }
        }
    }

    private Comment parseCommentFromJson(JSONObject commentObj) throws Exception {
        Comment comment = new Comment();
        comment.setId(commentObj.getString("_id"));
        comment.setText(commentObj.getString("text"));
        comment.setCreatedAt(commentObj.getString("createdAt"));

        if (commentObj.has("supportStatus")) {
            comment.setSupportStatus(commentObj.getString("supportStatus"));
        }

        if (commentObj.has("user") && !commentObj.isNull("user")) {
            JSONObject userObj = commentObj.getJSONObject("user");
            comment.setUserId(userObj.getString("_id"));
            comment.setUserName(getUserName(userObj));
            comment.setUserImage(getUserImage(userObj));
        }

        return comment;
    }

    private String getUserName(JSONObject userObj) {
        try {
            String firstName = userObj.optString("firstName", "");
            String lastName = userObj.optString("lastName", "");
            String fullName = (firstName + " " + lastName).trim();
            return fullName.isEmpty() ? "Anonymous" : fullName;
        } catch (Exception e) {
            return "Anonymous";
        }
    }

    private String getUserImage(JSONObject userObj) {
        try {
            if (userObj.has("additionalDetails")) {
                JSONObject additionalDetails = userObj.getJSONObject("additionalDetails");
                return additionalDetails.optString("image", "");
            }
        } catch (Exception e) {
            // Return empty string for default handling
        }
        return "";
    }

    private void updateUI() {
        if (interactions != null) {
            // Update counts
            tvSupportCount.setText(String.valueOf(interactions.getSupports()));
            tvUnsupportCount.setText(String.valueOf(interactions.getUnsupports()));
            tvCommentsCount.setText("Comments (" + commentsList.size() + ")");

            // Update button states
            updateInteractionButtons();

            // Update comments
            commentsAdapter.notifyDataSetChanged();

            // Show interactions layout
            layoutInteractions.setVisibility(View.VISIBLE);
        }
    }

    private void updateInteractionButtons() {
        // Reset button colors
        btnSupport.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.gray_400));
        btnUnsupport.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.gray_400));

        // Highlight selected button
        if ("Support".equals(userInteractionStatus)) {
            btnSupport.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.blue_600));
        } else if ("Unsupport".equals(userInteractionStatus)) {
            btnUnsupport.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.red_600));
        }
    }

    private void handleInteraction(String supportStatus) {
        if (isLoading || crime == null) return;

        // Toggle behavior - if already selected, remove selection
        String action = supportStatus.equals(userInteractionStatus) ? null : supportStatus;

        setLoading(true);
        hideError();

        try {
            JSONObject requestJson = new JSONObject();
            if (action != null) {
                requestJson.put("supportStatus", action);
            } else {
                requestJson.put("supportStatus", JSONObject.NULL);
            }

            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json"),
                    requestJson.toString()
            );

            Call<ResponseBody> call = apiService.interactWithCrime(
                    "Bearer " + token,
                    crime.getId(),
                    body
            );

            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    setLoading(false);

                    try {
                        if (response.isSuccessful()) {
                            Toast.makeText(CrimeDetailsActivity.this, "Interaction updated", Toast.LENGTH_SHORT).show();
                            fetchInteractions(); // Refresh data
                        } else if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            JSONObject errorObj = new JSONObject(errorBody);
                            showError(errorObj.optString("message", "Failed to update interaction"));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        showError("Error updating interaction");
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    setLoading(false);
                    showError("Network error: " + t.getMessage());
                }
            });

        } catch (Exception e) {
            setLoading(false);
            showError("Error creating request");
        }
    }

    private void handleCommentSubmit() {
        String commentText = etComment.getText().toString().trim();

        if (TextUtils.isEmpty(commentText)) {
            Toast.makeText(this, "Please enter a comment", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isLoading || crime == null) return;

        setLoading(true);
        hideError();
        btnPostComment.setEnabled(false);
        btnPostComment.setText("Posting...");

        try {
            JSONObject requestJson = new JSONObject();
            requestJson.put("comment", commentText);

            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json"),
                    requestJson.toString()
            );

            Call<ResponseBody> call = apiService.postCrimeComment(
                    "Bearer " + token,
                    crime.getId(),
                    body
            );

            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    setLoading(false);
                    btnPostComment.setEnabled(true);
                    btnPostComment.setText("Post");

                    try {
                        if (response.isSuccessful()) {
                            etComment.setText(""); // Clear comment field
                            Toast.makeText(CrimeDetailsActivity.this, "Comment posted successfully", Toast.LENGTH_SHORT).show();
                            fetchInteractions(); // Refresh data
                        } else if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            JSONObject errorObj = new JSONObject(errorBody);
                            showError(errorObj.optString("message", "Failed to post comment"));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        showError("Error posting comment");
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    setLoading(false);
                    btnPostComment.setEnabled(true);
                    btnPostComment.setText("Post");
                    showError("Network error: " + t.getMessage());
                }
            });

        } catch (Exception e) {
            setLoading(false);
            btnPostComment.setEnabled(true);
            btnPostComment.setText("Post");
            showError("Error creating request");
        }
    }

    private void setLoading(boolean loading) {
        isLoading = loading;
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void showError(String message) {
        tvErrorMessage.setText(message);
        layoutError.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        layoutError.setVisibility(View.GONE);
    }
}