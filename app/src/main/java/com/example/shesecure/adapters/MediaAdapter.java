package com.example.shesecure.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.shesecure.R;

import java.util.List;

public class MediaAdapter extends RecyclerView.Adapter<MediaAdapter.MediaViewHolder> {

    private List<String> mediaUrls;
    private boolean isPhoto;
    private OnMediaClickListener listener;

    public interface OnMediaClickListener {
        void onMediaClick(String mediaUrl);
    }

    public MediaAdapter(List<String> mediaUrls, boolean isPhoto, OnMediaClickListener listener) {
        this.mediaUrls = mediaUrls;
        this.isPhoto = isPhoto;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MediaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_crime_photos, parent, false);
        return new MediaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MediaViewHolder holder, int position) {
        String mediaUrl = mediaUrls.get(position);

        Glide.with(holder.itemView.getContext())
                .load(mediaUrl)
                .placeholder(isPhoto ? R.drawable.image : R.drawable.video)
                .into(holder.ivMedia);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMediaClick(mediaUrl);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mediaUrls.size();
    }

    static class MediaViewHolder extends RecyclerView.ViewHolder {
        ImageView ivMedia;

        public MediaViewHolder(@NonNull View itemView) {
            super(itemView);
            ivMedia = itemView.findViewById(R.id.ivMedia);
        }
    }
}