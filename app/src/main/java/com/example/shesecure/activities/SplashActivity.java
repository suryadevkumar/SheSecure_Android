package com.example.shesecure.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import com.example.shesecure.MainActivity;
import com.example.shesecure.R;
import com.example.shesecure.utils.AuthManager;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler().postDelayed(() -> {
            AuthManager authManager = new AuthManager(this);

            if (authManager.isLoggedIn()) {
                // User is logged in, go to dashboard
                startActivity(new Intent(this, UserDashboardActivity.class));
            } else {
                // User is not logged in, go to main (logged out) screen
                startActivity(new Intent(this, MainActivity.class));
            }
            finish();
        }, 1500); // 1.5 seconds splash screen
    }
}