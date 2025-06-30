package com.example.shesecure.utils;

import android.content.Context;
import android.widget.Toast;

import com.example.shesecure.services.ApiService;
import com.example.shesecure.services.RetrofitClient;

public class ApiUtils {
    public static ApiService initializeApiService(Context context) {
        try {
            SecurePrefs securePrefs = new SecurePrefs(context);
            securePrefs.saveKeys();
            String baseUrl = securePrefs.getAPI();
            return RetrofitClient.getClient(baseUrl).create(ApiService.class);
        } catch (Exception e) {
            Toast.makeText(context, "Error initializing app", Toast.LENGTH_SHORT).show();
            return null;
        }
    }
}