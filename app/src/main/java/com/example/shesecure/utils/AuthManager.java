package com.example.shesecure.utils;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;

public class AuthManager {
    // Preference keys
    private static final String PREFS_NAME = "SheSecurePrefs";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_FIRST_NAME = "firstName";
    private static final String KEY_LAST_NAME = "lastName";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_USER_TYPE = "userType";
    private static final String KEY_PROFILE_IMAGE = "profileImage";
    private static final String LIVE_LOCATION_SHARE_ID = "live_location_share_id";
    // Add more fields as needed from your API response

    private final SharedPreferences prefs;

    public AuthManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveUserData(String token, JSONObject user) {
        SharedPreferences.Editor editor = prefs.edit();

        // Save authentication data
        editor.putString(KEY_TOKEN, token);
        editor.putBoolean(KEY_IS_LOGGED_IN, true);

        // Save user details
        try {
            editor.putString(KEY_USER_ID, user.optString("_id", ""));
            editor.putString(KEY_FIRST_NAME, user.optString("firstName", ""));
            editor.putString(KEY_LAST_NAME, user.optString("lastName", ""));
            editor.putString(KEY_EMAIL, user.optString("email", ""));
            editor.putString(KEY_USER_TYPE, user.optString("userType", ""));

            // Handle nested objects (like additionalDetails)
            if (user.has("additionalDetails")) {
                JSONObject additionalDetails = user.optJSONObject("additionalDetails");
                if (additionalDetails != null) {
                    editor.putString(KEY_PROFILE_IMAGE, additionalDetails.optString("image", ""));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        editor.apply();
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false) && getToken() != null;
    }

    // Getters for user data
    public String getUserId() {
        return prefs.getString(KEY_USER_ID, "");
    }

    public String getFirstName() {
        return prefs.getString(KEY_FIRST_NAME, "");
    }

    public String getLastName() {
        return prefs.getString(KEY_LAST_NAME, "");
    }

    public String getFullName() {
        return getFirstName() + " " + getLastName();
    }

    public String getEmail() {
        return prefs.getString(KEY_EMAIL, "");
    }

    public String getUserType() {
        return prefs.getString(KEY_USER_TYPE, "");
    }

    public String getProfileImage() {
        return prefs.getString(KEY_PROFILE_IMAGE, "");
    }

    public void updateProfileImage(String imageUrl) {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(KEY_PROFILE_IMAGE, imageUrl);
            editor.apply();
        }
    }

    public void clearAllData() {
        prefs.edit()
                .remove(KEY_TOKEN)
                .remove(KEY_IS_LOGGED_IN)
                .remove(KEY_USER_ID)
                .remove(KEY_FIRST_NAME)
                .remove(KEY_LAST_NAME)
                .remove(KEY_EMAIL)
                .remove(KEY_USER_TYPE)
                .remove(KEY_PROFILE_IMAGE)
                .apply();
    }

    public static void saveLiveLocationShareId(Context context, String shareId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(LIVE_LOCATION_SHARE_ID, shareId).apply();
    }

    public static String getLiveLocationShareId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(LIVE_LOCATION_SHARE_ID, null);
    }

    public static void clearLiveLocationShareId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(LIVE_LOCATION_SHARE_ID).apply();
    }
}
