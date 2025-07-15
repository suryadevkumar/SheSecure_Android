package com.example.shesecure.activities;

import android.Manifest;
import android.app.ProgressDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.shesecure.R;
import com.example.shesecure.SheSecureApp;
import com.example.shesecure.adapters.PlaceAutoCompleteAdapter;
import com.example.shesecure.models.ReportLocation;
import com.example.shesecure.models.Suspect;
import com.example.shesecure.models.Witness;
import com.example.shesecure.services.ApiService;
import com.example.shesecure.utils.ApiUtils;
import com.example.shesecure.utils.SecurePrefs;
import com.example.shesecure.utils.SimpleTextWatcher;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CrimeReportSubmissionActivity extends BaseActivity {

    private static final int PICK_FIR_IMAGE = 1;
    private static final int PICK_PHOTO_EVIDENCE = 2;
    private static final int PICK_VIDEO_EVIDENCE = 3;
    private static final int PICK_SUSPECT_PHOTO = 4;
    private static final int PICK_WITNESS_PHOTO = 5;
    private static final int LOCATION_SELECTION_REQUEST = 6;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private PlacesClient placesClient;
    private AutocompleteSessionToken sessionToken;
    private GoogleMap googleMap;
    private Marker marker;
    private boolean isMapInitialized = false;
    private String mapsApiKey;

    private Spinner crimeTypeSpinner;
    private TextInputEditText descriptionEditText;
    private TextInputEditText dateEditText;
    private EditText locationEditText;
    private TextView firFileNameText;
    private TextView photoEvidenceText;
    private TextView videoEvidenceText;
    private LinearLayout suspectsContainer;
    private LinearLayout witnessesContainer;
    private Button submitButton;

    private String selectedCrimeType;
    private Uri firFileUri;
    private List<Uri> photoEvidenceUris = new ArrayList<>();
    private List<Uri> videoEvidenceUris = new ArrayList<>();
    private List<Suspect> suspects = new ArrayList<>();
    private List<Witness> witnesses = new ArrayList<>();
    private ReportLocation reportLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crime_report_submission);
        try {
            SecurePrefs securePrefs = ((SheSecureApp) getApplication()).getSecurePrefs();
            mapsApiKey = securePrefs.getGoogleMapsApiKey();
        } catch (Exception e) {
            throw new IllegalStateException("Google Maps API key not found");
        }

        initializeViews();
        setupCrimeTypeSpinner();
        setupDatePicker();
        initializePlaces();
        setupMap();
        setupListeners();
    }

    private void initializeViews() {
        crimeTypeSpinner = findViewById(R.id.crimeTypeSpinner);
        descriptionEditText = findViewById(R.id.descriptionEditText);
        dateEditText = findViewById(R.id.dateEditText);
        locationEditText = findViewById(R.id.locationEditText);
        firFileNameText = findViewById(R.id.firFileNameText);
        photoEvidenceText = findViewById(R.id.photoEvidenceText);
        videoEvidenceText = findViewById(R.id.videoEvidenceText);
        suspectsContainer = findViewById(R.id.suspectsContainer);
        witnessesContainer = findViewById(R.id.witnessesContainer);
        submitButton = findViewById(R.id.submitButton);
    }

    private void setupCrimeTypeSpinner() {
        String[] crimeTypes = getResources().getStringArray(R.array.crime_types);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, crimeTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        crimeTypeSpinner.setAdapter(adapter);

        crimeTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCrimeType = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedCrimeType = "";
            }
        });
    }

    private void setupDatePicker() {
        dateEditText.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();

            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    CrimeReportSubmissionActivity.this,
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        String formattedDate = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear);
                        dateEditText.setText(formattedDate);
                    },
                    year, month, day
            );

            // Sirf aaj tak ka date allow karo (future disable)
            datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());

            datePickerDialog.show();
        });
    }

    private void initializePlaces() {
        Places.initialize(getApplicationContext(), mapsApiKey);
        placesClient = Places.createClient(this);
        sessionToken = AutocompleteSessionToken.newInstance();
    }

    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);

        if (mapFragment == null) {
            Toast.makeText(this, "Map fragment not found", Toast.LENGTH_SHORT).show();
            return;
        }

        mapFragment.getMapAsync(googleMap -> {
            this.googleMap = googleMap;
            isMapInitialized = true;

            // Set initial position (India)
            LatLng india = new LatLng(20.5937, 78.9629);
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(india, 5f));

            // Add marker with proper initialization
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(india)
                    .draggable(true)
                    .title("Crime Location");

            marker = googleMap.addMarker(markerOptions);

            // Check if marker was created successfully
            if (marker == null) {
                Toast.makeText(this, "Failed to create map marker", Toast.LENGTH_SHORT).show();
                return;
            }

            // Set map click listener
            googleMap.setOnMapClickListener(latLng -> {
                updateMarkerPosition(latLng);
                reverseGeocode(latLng);
            });

            // Set marker drag listener - this should work now
            googleMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
                @Override
                public void onMarkerDragStart(Marker marker) {
                    // Optional: Handle drag start
                    Log.d("MapDrag", "Marker drag started");
                }

                @Override
                public void onMarkerDrag(Marker marker) {
                    // Optional: Handle during drag
                    // You can update UI here if needed
                }

                @Override
                public void onMarkerDragEnd(Marker marker) {
                    Log.d("MapDrag", "Marker drag ended at: " + marker.getPosition());
                    reverseGeocode(marker.getPosition());
                }
            });
        });
    }

    private void setupLocationAutocomplete() {
        AutoCompleteTextView locationEditText = findViewById(R.id.locationEditText);

        // Set bounds for India
        LatLngBounds bounds = new LatLngBounds(
                new LatLng(6.7559528, 68.1097), // SW corner
                new LatLng(35.6745457, 97.4025614766) // NE corner
        );

        PlaceAutoCompleteAdapter adapter = new PlaceAutoCompleteAdapter(this, placesClient, bounds);
        locationEditText.setAdapter(adapter);

        locationEditText.setOnItemClickListener((parent, view, position, id) -> {
            AutocompletePrediction item = adapter.getItem(position);
            if (item != null) {
                handleSelectedPlace(item);
            }
        });
    }

    private void handleSelectedPlace(AutocompletePrediction prediction) {
        List<Place.Field> placeFields = Arrays.asList(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.LAT_LNG,
                Place.Field.ADDRESS
        );

        FetchPlaceRequest request = FetchPlaceRequest.newInstance(
                prediction.getPlaceId(),
                placeFields
        );

        placesClient.fetchPlace(request).addOnSuccessListener(response -> {
            Place place = response.getPlace();

            // Update UI
            EditText locationEditText = findViewById(R.id.locationEditText);
            locationEditText.setText(place.getAddress());

            // Update form data
            if (place.getLatLng() != null) {
                reportLocation = new ReportLocation(
                        place.getLatLng().latitude,
                        place.getLatLng().longitude,
                        place.getAddress(),
                        null
                );

                // Update map if visible
                updateMapLocation(place.getLatLng());
            }
        }).addOnFailureListener(exception -> {
            Toast.makeText(this, "Error getting place details", Toast.LENGTH_SHORT).show();
        });
    }

    private void updateMapLocation(LatLng latLng) {
        if (googleMap != null && marker != null) {
            marker.setPosition(latLng);
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f));
        }
    }

    private void updateMarkerPosition(LatLng latLng) {
        if (marker != null) {
            marker.setPosition(latLng);
        }

        // Update form data
        reportLocation = new ReportLocation(
                latLng.latitude,
                latLng.longitude,
                null,
                null
        );
    }

    private void reverseGeocode(LatLng latLng) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(
                    latLng.latitude,
                    latLng.longitude,
                    1
            );

            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String addressText = address.getAddressLine(0);

                // Update UI
                EditText locationEditText = findViewById(R.id.locationEditText);
                locationEditText.setText(addressText);

                // Update form data
                if (reportLocation != null) {
                    reportLocation.setFormattedAddress(addressText);
                }
            }
        } catch (IOException e) {
            Toast.makeText(this, "Error getting address", Toast.LENGTH_SHORT).show();
        }
    }

    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                    updateMarkerPosition(latLng);
                    reverseGeocode(latLng);

                    if (googleMap != null) {
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f));
                    }
                }
            });
        } else {
            // Request location permission
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE
            );
        }
    }

    private void setupListeners() {
        findViewById(R.id.uploadFirButton).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, PICK_FIR_IMAGE);
        });

        findViewById(R.id.uploadPhotoEvidenceButton).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(intent, PICK_PHOTO_EVIDENCE);
        });

        findViewById(R.id.uploadVideoEvidenceButton).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(intent, PICK_VIDEO_EVIDENCE);
        });

        findViewById(R.id.toggleMapButton).setOnClickListener(v -> {
            FrameLayout mapContainer = findViewById(R.id.mapContainer);
            Button toggleMapButton = findViewById(R.id.toggleMapButton);

            if (mapContainer.getVisibility() == View.VISIBLE) {
                mapContainer.setVisibility(View.GONE);
                toggleMapButton.setText("Show map to select location");
            } else {
                mapContainer.setVisibility(View.VISIBLE);
                toggleMapButton.setText("Hide map");

                // If we have a location, center the map on it
                if (reportLocation != null) {
                    LatLng latLng = new LatLng(reportLocation.getLatitude(), reportLocation.getLongitude());
                    updateMapLocation(latLng);
                }
            }
        });

        findViewById(R.id.useCurrentLocationButton).setOnClickListener(v -> {
            getCurrentLocation();
        });

        findViewById(R.id.doneMapButton).setOnClickListener(v -> {
            FrameLayout mapContainer = findViewById(R.id.mapContainer);
            Button toggleMapButton = findViewById(R.id.toggleMapButton);

            mapContainer.setVisibility(View.GONE);
            toggleMapButton.setText("Show map to select location");
        });

        findViewById(R.id.addSuspectButton).setOnClickListener(v -> addSuspectForm());
        findViewById(R.id.addWitnessButton).setOnClickListener(v -> addWitnessForm());

        setupLocationAutocomplete();
        submitButton.setOnClickListener(v -> validateAndSubmitReport());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == PICK_FIR_IMAGE) {
                firFileUri = data.getData();
                firFileNameText.setText(getFileName(firFileUri));
                firFileNameText.setVisibility(View.VISIBLE);
            }
            else if (requestCode == PICK_PHOTO_EVIDENCE) {
                if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    for (int i = 0; i < count; i++) {
                        photoEvidenceUris.add(data.getClipData().getItemAt(i).getUri());
                    }
                } else if (data.getData() != null) {
                    photoEvidenceUris.add(data.getData());
                }
                updatePhotoEvidenceText();
            }
            else if (requestCode == PICK_VIDEO_EVIDENCE) {
                if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    for (int i = 0; i < count; i++) {
                        videoEvidenceUris.add(data.getClipData().getItemAt(i).getUri());
                    }
                } else if (data.getData() != null) {
                    videoEvidenceUris.add(data.getData());
                }
                updateVideoEvidenceText();
            }
            else if (requestCode == PICK_SUSPECT_PHOTO) {
                int suspectIndex = data.getIntExtra("suspectIndex", -1);
                if (suspectIndex != -1 && data.getData() != null) {
                    suspects.get(suspectIndex).setPhotoUri(data.getData());
                    updateSuspectPhotoView(suspectIndex);
                }
            }
            else if (requestCode == PICK_WITNESS_PHOTO) {
                int witnessIndex = data.getIntExtra("witnessIndex", -1);
                if (witnessIndex != -1 && data.getData() != null) {
                    witnesses.get(witnessIndex).setPhotoUri(data.getData());
                    updateWitnessPhotoView(witnessIndex);
                }
            }
            else if (requestCode == LOCATION_SELECTION_REQUEST) {
                // Handle location selection result
                if (data != null && data.hasExtra("location")) {
                    reportLocation = (ReportLocation) data.getSerializableExtra("location");
                    locationEditText.setText(reportLocation.getFormattedAddress());
                }
            }
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME));
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private void updatePhotoEvidenceText() {
        photoEvidenceText.setText(getString(R.string.photo_evidence_count, photoEvidenceUris.size()));
        photoEvidenceText.setVisibility(View.VISIBLE);
    }

    private void updateVideoEvidenceText() {
        videoEvidenceText.setText(getString(R.string.video_evidence_count, videoEvidenceUris.size()));
        videoEvidenceText.setVisibility(View.VISIBLE);
    }

    private void addSuspectForm() {
        View suspectView = getLayoutInflater().inflate(R.layout.item_suspect_form, null);
        int suspectIndex = suspects.size();

        EditText nameEditText = suspectView.findViewById(R.id.suspectNameEditText);
        Spinner genderSpinner = suspectView.findViewById(R.id.suspectGenderSpinner);
        ImageView photoImageView = suspectView.findViewById(R.id.suspectPhotoImageView);
        Button removeButton = suspectView.findViewById(R.id.removeSuspectButton);

        // Create new suspect with empty values
        Suspect suspect = new Suspect("", "", null);
        suspects.add(suspect);

        // Setup gender spinner
        ArrayAdapter<CharSequence> genderAdapter = ArrayAdapter.createFromResource(this,
                R.array.gender_options, android.R.layout.simple_spinner_item);
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        genderSpinner.setAdapter(genderAdapter);

        genderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                suspect.setGender(parent.getItemAtPosition(position).toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                suspect.setGender("");
            }
        });

        nameEditText.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                suspect.setName(s.toString());
            }
        });

        photoImageView.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.putExtra("suspectIndex", suspectIndex);
            startActivityForResult(intent, PICK_SUSPECT_PHOTO);
        });

        removeButton.setOnClickListener(v -> {
            suspects.remove(suspectIndex);
            suspectsContainer.removeView(suspectView);
        });

        suspectsContainer.addView(suspectView);
    }

    private void updateSuspectPhotoView(int suspectIndex) {
        View suspectView = suspectsContainer.getChildAt(suspectIndex);
        if (suspectView != null) {
            ImageView photoImageView = suspectView.findViewById(R.id.suspectPhotoImageView);
            photoImageView.setImageURI(suspects.get(suspectIndex).getPhotoUri());
        }
    }

    private void addWitnessForm() {
        View witnessView = getLayoutInflater().inflate(R.layout.item_witness_form, null);
        int witnessIndex = witnesses.size();

        EditText nameEditText = witnessView.findViewById(R.id.witnessNameEditText);
        Spinner genderSpinner = witnessView.findViewById(R.id.witnessGenderSpinner);
        EditText contactEditText = witnessView.findViewById(R.id.witnessContactEditText);
        EditText addressEditText = witnessView.findViewById(R.id.witnessAddressEditText);
        ImageView photoImageView = witnessView.findViewById(R.id.witnessPhotoImageView);
        Button removeButton = witnessView.findViewById(R.id.removeWitnessButton);

        // Create new witness with empty values
        Witness witness = new Witness("", "", "", "", null);
        witnesses.add(witness);

        // Setup gender spinner
        ArrayAdapter<CharSequence> genderAdapter = ArrayAdapter.createFromResource(this,
                R.array.gender_options, android.R.layout.simple_spinner_item);
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        genderSpinner.setAdapter(genderAdapter);

        genderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                witness.setGender(parent.getItemAtPosition(position).toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                witness.setGender("");
            }
        });

        nameEditText.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                witness.setName(s.toString());
            }
        });

        contactEditText.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                witness.setContactNumber(s.toString());
            }
        });

        addressEditText.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                witness.setAddress(s.toString());
            }
        });

        photoImageView.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.putExtra("witnessIndex", witnessIndex);
            startActivityForResult(intent, PICK_WITNESS_PHOTO);
        });

        removeButton.setOnClickListener(v -> {
            witnesses.remove(witnessIndex);
            witnessesContainer.removeView(witnessView);
        });

        witnessesContainer.addView(witnessView);
    }

    private void updateWitnessPhotoView(int witnessIndex) {
        View witnessView = witnessesContainer.getChildAt(witnessIndex);
        if (witnessView != null) {
            ImageView photoImageView = witnessView.findViewById(R.id.witnessPhotoImageView);
            photoImageView.setImageURI(witnesses.get(witnessIndex).getPhotoUri());
        }
    }

    private void validateAndSubmitReport() {
        String description = descriptionEditText.getText().toString().trim();
        String date = dateEditText.getText().toString().trim();

        if (selectedCrimeType == null || selectedCrimeType.equals("Select Crime Type")) {
            Toast.makeText(this, "Please select a valid crime type", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(description)) {
            descriptionEditText.setError("Please enter description");
            return;
        }

        if (TextUtils.isEmpty(date)) {
            dateEditText.setError("Please select date");
            return;
        }

        if (reportLocation == null) {
            Toast.makeText(this, "Please select location", Toast.LENGTH_SHORT).show();
            return;
        }

        if (firFileUri == null) {
            Toast.makeText(this, "Please upload FIR copy", Toast.LENGTH_SHORT).show();
            return;
        }

        submitReport();
    }

    private void submitReport() {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Submitting report...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        try {
            // Prepare multipart request
            MultipartBody.Part firPart = prepareFilePart("FIR", firFileUri);

            List<MultipartBody.Part> photoParts = new ArrayList<>();
            for (Uri uri : photoEvidenceUris) {
                photoParts.add(prepareFilePart("crimePhotos", uri));
            }

            List<MultipartBody.Part> videoParts = new ArrayList<>();
            for (Uri uri : videoEvidenceUris) {
                videoParts.add(prepareFilePart("crimeVideos", uri));
            }

            List<MultipartBody.Part> suspectPhotoParts = new ArrayList<>();
            for (int i = 0; i < suspects.size(); i++) {
                if (suspects.get(i).getPhotoUri() != null) {
                    suspectPhotoParts.add(prepareFilePart("suspectPhotos[" + i + "]", suspects.get(i).getPhotoUri()));
                }
            }

            List<MultipartBody.Part> witnessPhotoParts = new ArrayList<>();
            for (int i = 0; i < witnesses.size(); i++) {
                if (witnesses.get(i).getPhotoUri() != null) {
                    witnessPhotoParts.add(prepareFilePart("witnessPhotos[" + i + "]", witnesses.get(i).getPhotoUri()));
                }
            }

            // Prepare other form data
            RequestBody crimeType = RequestBody.create(selectedCrimeType, MediaType.parse("text/plain"));
            RequestBody description = RequestBody.create(descriptionEditText.getText().toString(), MediaType.parse("text/plain"));
            RequestBody date = RequestBody.create(dateEditText.getText().toString(), MediaType.parse("text/plain"));

            // Prepare location data
            RequestBody latitude = RequestBody.create(String.valueOf(reportLocation.getLatitude()), MediaType.parse("text/plain"));
            RequestBody longitude = RequestBody.create(String.valueOf(reportLocation.getLongitude()), MediaType.parse("text/plain"));
            RequestBody address = RequestBody.create(reportLocation.getFormattedAddress(), MediaType.parse("text/plain"));

            // Prepare suspects and witnesses as JSON
            RequestBody suspectsJson = RequestBody.create(
                    convertSuspectsToJson(suspects), MediaType.parse("application/json"));

            RequestBody witnessesJson = RequestBody.create(
                    convertWitnessesToJson(witnesses), MediaType.parse("application/json"));

            // Get API service instance
            apiService = ApiUtils.initializeApiService(this, ApiService.class);

            // Make API call
            Call<ResponseBody> call = apiService.submitCrimeReport(
                    crimeType, description, date,
                    latitude, longitude, address,
                    firPart, photoParts, videoParts,
                    suspectsJson, suspectPhotoParts,
                    witnessesJson, witnessPhotoParts);

            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    progressDialog.dismiss();
                    if (response.isSuccessful()) {
                        Toast.makeText(CrimeReportSubmissionActivity.this,
                                "Report submitted successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(CrimeReportSubmissionActivity.this,
                                "Failed to submit report", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    progressDialog.dismiss();
                    Toast.makeText(CrimeReportSubmissionActivity.this,
                            "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        } catch (Exception e) {
            progressDialog.dismiss();
            Toast.makeText(this, "Error preparing files: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private MultipartBody.Part prepareFilePart(String partName, Uri fileUri) {
        File file = new File(fileUri.getPath());
        RequestBody requestFile = RequestBody.create(file, MediaType.parse("multipart/form-data"));
        return MultipartBody.Part.createFormData(partName, file.getName(), requestFile);
    }

    private String convertSuspectsToJson(List<Suspect> suspects) throws JSONException {
        JSONArray jsonArray = new JSONArray();
        for (Suspect suspect : suspects) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("name", suspect.getName());
            jsonObject.put("gender", suspect.getGender());
            jsonArray.put(jsonObject);
        }
        return jsonArray.toString();
    }

    private String convertWitnessesToJson(List<Witness> witnesses) throws JSONException {
        JSONArray jsonArray = new JSONArray();
        for (Witness witness : witnesses) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("name", witness.getName());
            jsonObject.put("gender", witness.getGender());
            jsonObject.put("contactNumber", witness.getContactNumber());
            jsonObject.put("address", witness.getAddress());
            jsonArray.put(jsonObject);
        }
        return jsonArray.toString();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}