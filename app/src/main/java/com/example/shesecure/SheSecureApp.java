package com.example.shesecure;

import android.app.Application;
import com.example.shesecure.utils.SecurePrefs;

public class SheSecureApp extends Application {
    private SecurePrefs securePrefs;

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            securePrefs = SecurePrefs.getInstance(this);

            // Initialize with your keys if they're not already set
            if (securePrefs.getGoogleMapsApiKey() == null) {
                securePrefs.saveApiKeys(
                        "http://10.0.2.2:3000/api/",
                        "AIzaSyAqJW62Rkv5azC1er-_jr-3AolJffDmHp8"
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public SecurePrefs getSecurePrefs() {
        return securePrefs;
    }
}