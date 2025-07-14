package com.example.shesecure.activities;

import android.os.Bundle;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.example.shesecure.R;

public class FullScreenImageActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen_image);

        ImageView fullScreenImageView = findViewById(R.id.fullScreenImageView);
        String imageUrl = getIntent().getStringExtra("photo_url");

        Glide.with(this)
                .load(imageUrl)
                .into(fullScreenImageView);

        fullScreenImageView.setOnClickListener(v -> finish());
    }
}