// CrimePhotosAdapter.java
package com.example.shesecure.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.shesecure.R;

import java.util.List;

public class CrimePhotosAdapter extends RecyclerView.Adapter<CrimePhotosAdapter.PhotoViewHolder> {

    private List<String> crimePhotos;
    private OnPhotoClickListener onPhotoClickListener;

    public interface OnPhotoClickListener {
        void onPhotoClick(String photoUrl, int position);
    }

    public CrimePhotosAdapter(List<String> crimePhotos) {
        this.crimePhotos = crimePhotos;
    }

    public void setOnPhotoClickListener(OnPhotoClickListener listener) {
        this.onPhotoClickListener = listener;
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_crime_photo, parent, false);
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        String photoUrl = crimePhotos.get(position);
        holder.bind(photoUrl, position);
    }

    @Override
    public int getItemCount() {
        return crimePhotos.size();
    }

    class PhotoViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivCrimePhoto;

        public PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCrimePhoto = itemView.findViewById(R.id.ivCrimePhoto);
        }

        public void bind(String photoUrl, int position) {
            Glide.with(itemView.getContext())
                    .load(photoUrl)
                    .placeholder(R.drawable.image_placeholder)
                    .error(R.drawable.image_placeholder)
                    .centerCrop()
                    .into(ivCrimePhoto);

            ivCrimePhoto.setOnClickListener(v -> {
                if (onPhotoClickListener != null) {
                    onPhotoClickListener.onPhotoClick(photoUrl, position);
                }
            });
        }
    }
}