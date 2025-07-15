package com.example.shesecure.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

public class SecurePrefs {

    private static final String PREF_NAME = "secure_prefs";
    private static SecurePrefs instance;
    private final SharedPreferences sharedPreferences;

    // Private constructor
    private SecurePrefs(Context context) throws Exception {
        MasterKey masterKey = new MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();

        sharedPreferences = EncryptedSharedPreferences.create(
                context,
                PREF_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );
    }

    // Singleton pattern to ensure single instance
    public static synchronized SecurePrefs getInstance(Context context) throws Exception {
        if (instance == null) {
            instance = new SecurePrefs(context);
        }
        return instance;
    }

    public void saveApiKeys(String apiBaseUrl, String googleMapsApiKey) {
        sharedPreferences.edit()
                .putString("api_base_url", apiBaseUrl)
                .putString("google_maps_api_key", googleMapsApiKey)
                .apply();
    }

    public String getApiBaseUrl() {
        return sharedPreferences.getString("api_base_url", null);
    }

    public String getGoogleMapsApiKey() {
        return sharedPreferences.getString("google_maps_api_key", null);
    }

    // Clear all secure preferences if needed
    public void clearAll() {
        sharedPreferences.edit().clear().apply();
    }
}