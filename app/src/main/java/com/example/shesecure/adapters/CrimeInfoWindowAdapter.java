package com.example.shesecure.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.shesecure.R;
import com.example.shesecure.models.CrimeReport;
import com.example.shesecure.services.ApiService;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

public class CrimeInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {
    private final Context context;
    private CrimeReport crime;
    private ApiService.CrimeInteractionStats stats;
    private OnInfoWindowClickListener listener;

    public interface OnInfoWindowClickListener {
        void onInfoWindowClick(CrimeReport crime);
    }

    public CrimeInfoWindowAdapter(Context context, OnInfoWindowClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setCrimeData(CrimeReport crime, ApiService.CrimeInteractionStats stats, Marker marker) {
        this.crime = crime;
        this.stats = stats;
        if (marker != null) {
            marker.setTag(crime); // Crime data store in marker tag
        }
    }

    @Override
    public View getInfoWindow(Marker marker) {
        // Use default info window background (null return karein)
        return null;
    }

    @Override
    public View getInfoContents(Marker marker) {
        // Get crime data from marker tag if needed
        if (marker.getTag() instanceof CrimeReport) {
            crime = (CrimeReport) marker.getTag();
        }

        if (crime == null) return null;

        View view = LayoutInflater.from(context).inflate(R.layout.crime_marker_info, null);

        // Initialize views
        TextView crimeType = view.findViewById(R.id.crime_type);
        TextView crimeDesc = view.findViewById(R.id.crime_description);
        TextView likesCount = view.findViewById(R.id.likes_count);
        TextView dislikesCount = view.findViewById(R.id.dislikes_count);
        ImageView crimeImage = view.findViewById(R.id.crime_image);

        // Set data
        crimeType.setText(crime.getTypeOfCrime());
        crimeDesc.setText(crime.getDescription());
        likesCount.setText(String.valueOf(stats != null ? stats.supports : 0));
        dislikesCount.setText(String.valueOf(stats != null ? stats.unsupports : 0));

        // Load image if available
        if (crime.getPhotoUrls() != null && !crime.getPhotoUrls().isEmpty()) {
            Glide.with(context)
                    .load(crime.getPhotoUrls().get(0))
                    .override(300, 500)
                    .centerCrop()
                    .placeholder(R.drawable.person)
                    .into(crimeImage);
        } else {
            crimeImage.setVisibility(View.GONE);
        }

        return view;
    }
}