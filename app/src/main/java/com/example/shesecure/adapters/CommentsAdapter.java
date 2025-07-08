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

public class CommentsAdapter {

    private List<Comment> comments;
    private String currentUserId;
    private Map<String, Boolean> expandedComments = new HashMap<>();

    public CommentsAdapter(List<Comment> comments, String currentUserId) {
        this.comments = comments;
        this.currentUserId = currentUserId;
    }




}