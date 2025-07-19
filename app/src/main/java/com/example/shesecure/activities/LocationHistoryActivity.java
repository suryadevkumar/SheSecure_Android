package com.example.shesecure.activities;

import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.shesecure.R;
import com.example.shesecure.SheSecureApp;
import com.example.shesecure.models.Location;
import com.example.shesecure.services.ApiService;
import com.example.shesecure.utils.ApiUtils;
import com.example.shesecure.utils.SecurePrefs;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.navigation.NavigationView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LocationHistoryActivity extends BaseActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener, NavigationView.OnNavigationItemSelectedListener {

    private GoogleMap mMap;
    private TextView datePick, startTime, endTime;
    private DrawerLayout drawerLayout;
    private ListView historyListView;
    private ApiService apiService;
    private List<Location> allLocationData = new ArrayList<>();
    private String authToken, mapsApiKey;
    private Calendar startTimeCalendar = Calendar.getInstance();
    private Calendar endTimeCalendar = Calendar.getInstance();
    private SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private SimpleDateFormat apiDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
    private SimpleDateFormat displayDateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    // Track currently open info window
    private Marker currentOpenMarker = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_history);

        // Initialize toolbar and navigation drawer
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Initialize map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Initialize views
        datePick = findViewById(R.id.datePick);
        startTime = findViewById(R.id.startTime);
        endTime = findViewById(R.id.endTime);
        historyListView = findViewById(R.id.historyListView);

        // Get auth token and maps API key
        SharedPreferences prefs = getSharedPreferences("SheSecurePrefs", MODE_PRIVATE);
        authToken = "Bearer " + prefs.getString("token", null);
        SecurePrefs securePrefs = ((SheSecureApp) getApplication()).getSecurePrefs();
        mapsApiKey = securePrefs.getGoogleMapsApiKey();

        // Initialize API service
        apiService = ApiUtils.initializeApiService(this, ApiService.class);

        // Set default time range (whole day)
        startTimeCalendar.set(Calendar.HOUR_OF_DAY, 0);
        startTimeCalendar.set(Calendar.MINUTE, 0);
        endTimeCalendar.set(Calendar.HOUR_OF_DAY, 23);
        endTimeCalendar.set(Calendar.MINUTE, 59);
        updateTimeButtons();

        // Set click listeners
        datePick.setOnClickListener(v -> showMaterialDatePicker());
        startTime.setOnClickListener(v -> showTimePicker(true));
        endTime.setOnClickListener(v -> showTimePicker(false));

        // Initially set today's date
        datePick.setText(displayDateFormat.format(Calendar.getInstance().getTime()));

        // Initially fetch today's data
        fetchLocationHistory();
    }

    private void showMaterialDatePicker() {
        // Set today as the maximum selectable date
        CalendarConstraints.Builder constraintsBuilder = new CalendarConstraints.Builder();
        constraintsBuilder.setEnd(MaterialDatePicker.todayInUtcMilliseconds());

        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select date")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .setCalendarConstraints(constraintsBuilder.build())
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(selection);

            // Update the button text
            datePick.setText(displayDateFormat.format(calendar.getTime()));

            // Fetch data for the selected date
            fetchLocationHistory();
        });

        datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMarkerClickListener(this);

        // Customize map appearance
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);

        // Default location (Delhi coordinates)
        LatLng defaultLocation = new LatLng(28.6139, 77.2090);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12));
    }

    private void showTimePicker(boolean isStartTime) {
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    if (isStartTime) {
                        startTimeCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        startTimeCalendar.set(Calendar.MINUTE, minute);
                    } else {
                        endTimeCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        endTimeCalendar.set(Calendar.MINUTE, minute);
                    }
                    updateTimeButtons();
                    filterAndDisplayLocationsWithTracking();
                    updateHistoryList(); // Update list when time changes
                },
                isStartTime ? startTimeCalendar.get(Calendar.HOUR_OF_DAY) : endTimeCalendar.get(Calendar.HOUR_OF_DAY),
                isStartTime ? startTimeCalendar.get(Calendar.MINUTE) : endTimeCalendar.get(Calendar.MINUTE),
                false
        );
        timePickerDialog.show();
    }

    private void updateTimeButtons() {
        startTime.setText(timeFormat.format(startTimeCalendar.getTime()));
        endTime.setText(timeFormat.format(endTimeCalendar.getTime()));
    }

    private void fetchLocationHistory() {
        // Get selected date from button text
        String selectedDateStr = datePick.getText().toString();
        Date selectedDate;
        try {
            selectedDate = displayDateFormat.parse(selectedDateStr);
        } catch (ParseException e) {
            e.printStackTrace();
            selectedDate = new Date(); // fallback to today
        }

        String apiFormattedDate = dateFormat.format(selectedDate);

        Call<ResponseBody> call = apiService.fetchLocationHistory(apiFormattedDate, authToken);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseString = response.body().string();
                        JSONObject jsonObject = new JSONObject(responseString);
                        JSONArray dataArray = jsonObject.getJSONArray("data");

                        allLocationData.clear();
                        for (int i = 0; i < dataArray.length(); i++) {
                            JSONObject locationObj = dataArray.getJSONObject(i);
                            Location location = new Location(locationObj);
                            allLocationData.add(location);
                        }

                        updateHistoryList();
                        filterAndDisplayLocationsWithTracking();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(LocationHistoryActivity.this, "Error parsing data", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(LocationHistoryActivity.this, "No data found for selected date", Toast.LENGTH_SHORT).show();
                    allLocationData.clear();
                    updateHistoryList();
                    if (mMap != null) mMap.clear();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(LocationHistoryActivity.this, "Failed to fetch data: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateHistoryList() {
        // Get filtered locations based on time range
        List<Location> filteredLocations = filterLocationsByTime();

        ArrayAdapter<Location> adapter = new ArrayAdapter<Location>(this,
                android.R.layout.simple_list_item_1, filteredLocations) {
            @Override
            public android.view.View getView(int position, android.view.View convertView, android.view.ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view.findViewById(android.R.id.text1);

                Location location = filteredLocations.get(position);
                String time = "";
                String date = "";
                try {
                    Date dateObj = apiDateFormat.parse(location.getCreatedAt());
                    time = timeFormat.format(dateObj);
                    date = displayDateFormat.format(dateObj);
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                textView.setText(location.getDisplayName() + "\n" + date + " " + time);
                return view;
            }
        };

        historyListView.setAdapter(adapter);
        historyListView.setOnItemClickListener((parent, view, position, id) -> {
            Location location = filteredLocations.get(position);
            showLocationDetailsFromListUpdated(location);
            drawerLayout.closeDrawer(GravityCompat.START);
        });
    }

    private void filterAndDisplayLocations() {
        if (mMap == null) return;

        mMap.clear();
        currentOpenMarker = null; // Reset current open marker

        List<Location> filteredLocations = filterLocationsByTime();

        if (filteredLocations.isEmpty()) {
            Toast.makeText(this, "No locations found for selected time range", Toast.LENGTH_SHORT).show();
            return;
        }

        // Draw path and markers
        PolylineOptions polylineOptions = new PolylineOptions();
        for (Location location : filteredLocations) {
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

            // Create small red dot marker
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(latLng)
                    .icon(createRedDotIcon())
                    .title(location.getDisplayName())
                    .snippet(getDetailedInfo(location));

            // Store the location object as the marker's tag
            Marker marker = mMap.addMarker(markerOptions);
            marker.setTag(location);

            polylineOptions.add(latLng);
        }

        // Customize polyline (red color)
        polylineOptions.width(15)
                .color(Color.RED)
                .geodesic(true);

        mMap.addPolyline(polylineOptions);

        // Move camera to show all markers if available
        if (!filteredLocations.isEmpty()) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(filteredLocations.get(0).getLatitude(),
                            filteredLocations.get(0).getLongitude()), 12));
        }
    }

    // Create a custom red dot icon
    private BitmapDescriptor createRedDotIcon() {
        int dotSize = 50; // Size of the dot in pixels
        Bitmap bitmap = Bitmap.createBitmap(dotSize, dotSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setAntiAlias(true);

        // Draw filled circle
        canvas.drawCircle(dotSize / 2, dotSize / 2, dotSize / 2 - 2, paint);

        // Add white border
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        canvas.drawCircle(dotSize / 2, dotSize / 2, dotSize / 2 - 2, paint);

        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    private String getDetailedInfo(Location location) {
        try {
            Date dateObj = apiDateFormat.parse(location.getCreatedAt());
            String time = timeFormat.format(dateObj);
            String date = displayDateFormat.format(dateObj);

            return "Address: " + location.getFormattedAddress() +
                    "\nDate: " + date +
                    "\nTime: " + time;
        } catch (ParseException e) {
            e.printStackTrace();
            return "";
        }
    }

    private List<Location> filterLocationsByTime() {
        List<Location> filteredList = new ArrayList<>();

        for (Location location : allLocationData) {
            try {
                Date timestamp = apiDateFormat.parse(location.getCreatedAt());
                Calendar cal = Calendar.getInstance();
                cal.setTime(timestamp);

                if (isTimeInRange(cal, startTimeCalendar, endTimeCalendar)) {
                    filteredList.add(location);
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        return filteredList;
    }

    private boolean isTimeInRange(Calendar timeToCheck, Calendar startTime, Calendar endTime) {
        int hourToCheck = timeToCheck.get(Calendar.HOUR_OF_DAY);
        int minuteToCheck = timeToCheck.get(Calendar.MINUTE);

        int startHour = startTime.get(Calendar.HOUR_OF_DAY);
        int startMinute = startTime.get(Calendar.MINUTE);

        int endHour = endTime.get(Calendar.HOUR_OF_DAY);
        int endMinute = endTime.get(Calendar.MINUTE);

        int checkInMinutes = hourToCheck * 60 + minuteToCheck;
        int startInMinutes = startHour * 60 + startMinute;
        int endInMinutes = endHour * 60 + endMinute;

        return checkInMinutes >= startInMinutes && checkInMinutes <= endInMinutes;
    }

    // Method called when clicking from the list
    private void showLocationDetailsFromList(Location location) {
        // Move camera to the location
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));

        // Find the marker for this location and show its info window
        for (Marker marker : getAllMarkersFromMap()) {
            Location markerLocation = (Location) marker.getTag();
            if (markerLocation != null && markerLocation.equals(location)) {
                // Close any currently open info window
                if (currentOpenMarker != null) {
                    currentOpenMarker.hideInfoWindow();
                }

                // Show info window for this marker
                marker.showInfoWindow();
                currentOpenMarker = marker;
                break;
            }
        }
    }

    // Helper method to get all markers from map (since GoogleMap doesn't provide this directly)
    private List<Marker> getAllMarkersFromMap() {
        List<Marker> markers = new ArrayList<>();
        // Since we can't directly get all markers, we'll use the filtered locations
        List<Location> filteredLocations = filterLocationsByTime();

        return markers;
    }

    // Modified approach: track markers in a list
    private List<Marker> mapMarkers = new ArrayList<>();

    private void filterAndDisplayLocationsWithTracking() {
        if (mMap == null) return;

        mMap.clear();
        mapMarkers.clear(); // Clear our marker tracking list
        currentOpenMarker = null; // Reset current open marker

        List<Location> filteredLocations = filterLocationsByTime();

        if (filteredLocations.isEmpty()) {
            Toast.makeText(this, "No locations found for selected time range", Toast.LENGTH_SHORT).show();
            return;
        }

        // Draw path and markers
        PolylineOptions polylineOptions = new PolylineOptions();
        for (Location location : filteredLocations) {
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

            // Create small red dot marker
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(latLng)
                    .icon(createRedDotIcon())
                    .title(location.getDisplayName())
                    .snippet(getDetailedInfo(location));

            // Store the location object as the marker's tag
            Marker marker = mMap.addMarker(markerOptions);
            marker.setTag(location);
            mapMarkers.add(marker); // Track the marker

            polylineOptions.add(latLng);
        }

        // Customize polyline (red color)
        polylineOptions.width(15)
                .color(Color.RED)
                .geodesic(true);

        mMap.addPolyline(polylineOptions);

        // Move camera to show all markers if available
        if (!filteredLocations.isEmpty()) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(filteredLocations.get(0).getLatitude(),
                            filteredLocations.get(0).getLongitude()), 12));
        }
    }

    // Updated method called when clicking from the list
    private void showLocationDetailsFromListUpdated(Location location) {
        // Move camera to the location
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));

        // Find the marker for this location and show its info window
        for (Marker marker : mapMarkers) {
            Location markerLocation = (Location) marker.getTag();
            if (markerLocation != null && markerLocation.equals(location)) {
                // Close any currently open info window
                if (currentOpenMarker != null) {
                    currentOpenMarker.hideInfoWindow();
                }

                // Show info window for this marker
                marker.showInfoWindow();
                currentOpenMarker = marker;
                break;
            }
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        // Toggle info window behavior
        if (currentOpenMarker != null && currentOpenMarker.equals(marker)) {
            // If clicking on the same marker that's already open, close it
            marker.hideInfoWindow();
            currentOpenMarker = null;
        } else {
            // Close any currently open info window
            if (currentOpenMarker != null) {
                currentOpenMarker.hideInfoWindow();
            }

            // Show info window for clicked marker
            marker.showInfoWindow();
            currentOpenMarker = marker;
        }

        return true; // Prevent default behavior
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}