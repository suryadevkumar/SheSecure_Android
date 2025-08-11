package com.example.shesecure.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shesecure.R;
import com.example.shesecure.activities.VideoPlayerActivity;

import java.util.ArrayList;
import java.util.List;

public class VideoGridAdapter extends RecyclerView.Adapter<VideoGridAdapter.VideoViewHolder> {
    private List<String> videoUrls;
    private Context context;

    public VideoGridAdapter(Context context, List<String> videoUrls) {
        this.context = context;
        this.videoUrls = videoUrls != null ? videoUrls : new ArrayList<>();
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_video, parent, false);
        return new VideoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        String videoUrl = videoUrls.get(position);

        holder.playButton.setOnClickListener(v -> {
            Intent intent = new Intent(context, VideoPlayerActivity.class);
            intent.putExtra("video_url", videoUrl);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return videoUrls.size();
    }

    static class VideoViewHolder extends RecyclerView.ViewHolder {
        ImageView playButton;

        public VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            playButton = itemView.findViewById(R.id.play_button);
        }
    }
}