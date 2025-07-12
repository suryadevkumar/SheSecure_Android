package com.example.shesecure.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

public class SecurePrefs {

    private static final String PREF_NAME = "secure_prefs";

    private final SharedPreferences sharedPreferences;

    public SecurePrefs(Context context) throws Exception {
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

    // Save keys
    public void saveKeys() {
        sharedPreferences.edit()
                .putString("api", "http://10.0.2.2:3000/api/")
                .apply();
    }

    // Access keys
    public String getGoogleMapsApiKey() {
        return sharedPreferences.getString("api", null);
    }

}
