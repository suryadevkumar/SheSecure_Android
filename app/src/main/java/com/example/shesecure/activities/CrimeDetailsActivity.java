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


}