package com.example.shesecure.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.example.shesecure.R;

public class HelplineActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_helpline);
        setStatusBarColor();
        setupEmergencyButtons();
    }

    private void setStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.white));
        }
    }

    private void setupEmergencyButtons() {
        setupEmergencyButton(R.id.police_call_btn, "100");
        setupEmergencyButton(R.id.fire_call_btn, "101");
        setupEmergencyButton(R.id.ambulance_call_btn, "102");
        setupEmergencyButton(R.id.pregnancy_call_btn, "108");
        setupEmergencyButton(R.id.national_call_btn, "112");
        setupEmergencyButton(R.id.women_call_btn, "1091");
        setupEmergencyButton(R.id.child_call_btn, "1098");
        setupEmergencyButton(R.id.road_call_btn, "1073");
        setupEmergencyButton(R.id.railway_call_btn, "182");
    }

    private void setupEmergencyButton(int buttonId, String number) {
        ImageView button = findViewById(buttonId);
        if (button != null) {
            button.setOnClickListener(v -> openDialerWithNumber(number));
        }
    }

    private void openDialerWithNumber(String number) {
        try {
            Intent dialIntent = new Intent(Intent.ACTION_DIAL);
            dialIntent.setData(Uri.parse("tel:" + number));
            startActivity(dialIntent);
        } catch (Exception e) {
            Toast.makeText(this, "Could not open dialer", Toast.LENGTH_SHORT).show();
        }
    }
}