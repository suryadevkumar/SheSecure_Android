// CommentsAdapter.java
package com.example.shesecure.adapters;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.shesecure.R;
import com.example.shesecure.models.Comment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.CommentViewHolder> {

    private List<Comment> comments;
    private String currentUserId;
    private Map<String, Boolean> expandedComments = new HashMap<>();

    public CommentsAdapter(List<Comment> comments, String currentUserId) {
        this.comments = comments;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = comments.get(position);
        holder.bind(comment, position);
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    class CommentViewHolder extends RecyclerView.ViewHolder {

        private ImageView ivUserImage, ivSupportIcon;
        private TextView tvUserName, tvCommentText, tvTimeAgo, tvSupportStatus, tvReadMore;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            ivUserImage = itemView.findViewById(R.id.ivUserImage);
            ivSupportIcon = itemView.findViewById(R.id.ivSupportIcon);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvCommentText = itemView.findViewById(R.id.tvCommentText);
            tvTimeAgo = itemView.findViewById(R.id.tvTimeAgo);
            tvSupportStatus = itemView.findViewById(R.id.tvSupportStatus);
            tvReadMore = itemView.findViewById(R.id.tvReadMore);
        }

        public void bind(Comment comment, int position) {
            // Set user name
            String userName = comment.getUserName();
            if (currentUserId != null && currentUserId.equals(comment.getUserId())) {
                userName += " (You)";
            }
            tvUserName.setText(userName);

            // Set user image
            if (!TextUtils.isEmpty(comment.getUserImage())) {
                Glide.with(itemView.getContext())
                        .load(comment.getUserImage())
                        .placeholder(R.drawable.default_user)
                        .error(R.drawable.default_user)
                        .circleCrop()
                        .into(ivUserImage);
            } else {
                ivUserImage.setImageResource(R.drawable.default_user);
            }

            // Set comment text with read more functionality
            String commentText = comment.getText();
            boolean isLongComment = commentText.length() > 150;
            String commentId = comment.getId();
            Boolean isExpanded = expandedComments.get(commentId);

            if (isLongComment) {
                if (isExpanded != null && isExpanded) {
                    tvCommentText.setText(commentText);
                    tvReadMore.setText("Read less");
                } else {
                    tvCommentText.setText(commentText.substring(0, 150) + "...");
                    tvReadMore.setText("Read more");
                }
                tvReadMore.setVisibility(View.VISIBLE);
                tvReadMore.setOnClickListener(v -> {
                    boolean currentlyExpanded = expandedComments.getOrDefault(commentId, false);
                    expandedComments.put(commentId, !currentlyExpanded);
                    notifyItemChanged(position);
                });
            } else {
                tvCommentText.setText(commentText);
                tvReadMore.setVisibility(View.GONE);
            }

            // Set time ago
            tvTimeAgo.setText(getTimeAgo(comment.getCreatedAt()));

            // Set support status
            if (!TextUtils.isEmpty(comment.getSupportStatus())) {
                tvSupportStatus.setVisibility(View.VISIBLE);
                ivSupportIcon.setVisibility(View.VISIBLE);

                if ("Support".equals(comment.getSupportStatus())) {
                    tvSupportStatus.setText("Supported");
                    tvSupportStatus.setTextColor(itemView.getContext().getColor(R.color.blue_600));
                    ivSupportIcon.setImageResource(R.drawable.ic_thumb_up);
                    ivSupportIcon.setColorFilter(itemView.getContext().getColor(R.color.blue_600));
                } else if ("Unsupport".equals(comment.getSupportStatus())) {
                    tvSupportStatus.setText("Unsupported");
                    tvSupportStatus.setTextColor(itemView.getContext().getColor(R.color.red_600));
                    ivSupportIcon.setImageResource(R.drawable.ic_thumb_down);
                    ivSupportIcon.setColorFilter(itemView.getContext().getColor(R.color.red_600));
                }
            } else {
                tvSupportStatus.setVisibility(View.GONE);
                ivSupportIcon.setVisibility(View.GONE);
            }
        }

        private String getTimeAgo(String createdAt) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                Date date = sdf.parse(createdAt);
                long seconds = (System.currentTimeMillis() - date.getTime()) / 1000;

                if (seconds < 60) {
                    return seconds + " seconds ago";
                } else if (seconds < 3600) {
                    int minutes = (int) (seconds / 60);
                    return minutes + (minutes == 1 ? " minute ago" : " minutes ago");
                } else if (seconds < 86400) {
                    int hours = (int) (seconds / 3600);
                    return hours + (hours == 1 ? " hour ago" : " hours ago");
                } else if (seconds < 2592000) {
                    int days = (int) (seconds / 86400);
                    return days + (days == 1 ? " day ago" : " days ago");
                } else if (seconds < 31536000) {
                    int months = (int) (seconds / 2592000);
                    return months + (months == 1 ? " month ago" : " months ago");
                } else {
                    int years = (int) (seconds / 31536000);
                    return years + (years == 1 ? " year ago" : " years ago");
                }
            } catch (Exception e) {
                return "Unknown";
            }
        }
    }
}