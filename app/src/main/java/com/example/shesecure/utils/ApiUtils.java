package com.example.shesecure.utils;

import android.content.Context;
import android.widget.Toast;
import com.example.shesecure.services.ApiService;
import com.example.shesecure.services.RetrofitClient;

public class ApiUtils {
    public static ApiService initializeApiService(Context context, Class<ApiService> apiServiceClass) {
        try {
            SecurePrefs securePrefs = new SecurePrefs(context);
            String baseUrl = securePrefs.getApiBaseUrl();
            return RetrofitClient.getClient(baseUrl).create(ApiService.class);
        } catch (Exception e) {
            Toast.makeText(context, "Error initializing API service", Toast.LENGTH_SHORT).show();
            return null;
        }
    }
}