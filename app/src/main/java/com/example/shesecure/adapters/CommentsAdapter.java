package com.example.shesecure.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.shesecure.R;
import com.example.shesecure.services.ApiService;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.CommentViewHolder> {
    private List<ApiService.CrimeComment> comments;

    public CommentsAdapter(List<ApiService.CrimeComment> comments) {
        this.comments = comments != null ? comments : new ArrayList<>();
    }

    // Add this method to handle new comments
    public void addComment(ApiService.CrimeComment comment) {
        this.comments.add(0, comment); // Add at beginning to show newest first
        notifyItemInserted(0);

        // Scroll to the new comment
        if (getItemCount() > 0) {
            // You might need to pass a reference to the RecyclerView to scroll it
        }
    }

    // Rest of your adapter code remains the same
    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        ApiService.CrimeComment comment = comments.get(position);
        holder.bind(comment);
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        private ImageView userImage;
        private TextView userName;
        private TextView commentText;
        private TextView commentTime;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            userImage = itemView.findViewById(R.id.user_image);
            userName = itemView.findViewById(R.id.user_name);
            commentText = itemView.findViewById(R.id.comment_text);
            commentTime = itemView.findViewById(R.id.comment_time);
        }

        public void bind(ApiService.CrimeComment comment) {
            userName.setText(comment.user.getFirstName() + " " + comment.user.getLastName());
            commentText.setText(comment.text);

            // Format time
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, hh:mm a");
            try {
                Date date = inputFormat.parse(comment.createdAt);
                commentTime.setText(outputFormat.format(date));
            } catch (ParseException e) {
                commentTime.setText(comment.createdAt);
            }

            // Load user image
            if (comment.user.additionalDetails != null &&
                    comment.user.additionalDetails.image != null) {

                Glide.with(itemView.getContext())
                        .load(comment.user.additionalDetails.image)
                        .circleCrop()
                        .into(userImage);
            } else {
                // Set a default image if no image is available
                userImage.setImageResource(R.drawable.person);
            }
        }
    }
}