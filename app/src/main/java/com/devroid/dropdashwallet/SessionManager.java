package com.devroid.dropdashwallet;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME = "WalletSession";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_IDENTIFIER = "identifier";
    private static final String KEY_NAME = "name";
    private static final String KEY_PROFILE_IMAGE = "profile_image_uri";

    private SharedPreferences pref;
    private SharedPreferences.Editor editor;

    private Context context;

    public SessionManager(Context context) {
        this.context = context;
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    // Save login session
    public void createLoginSession(String identifier, String name, boolean rememberMe) {
        editor.putBoolean(KEY_IS_LOGGED_IN, rememberMe);
        editor.putString(KEY_IDENTIFIER, identifier);
        editor.putString(KEY_NAME, name);
        editor.commit();
    }

    // Check login status
    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    // Get saved email
    public String getIdentifier() {
        return pref.getString(KEY_IDENTIFIER, null);
    }

    public String getName() {
        return pref.getString(KEY_NAME, null);
    }

    // ===== Name =====
    public void setName(String name) {
        editor.putString(KEY_NAME, name);
        editor.apply();
    }

    // Logout user
    public void logout() {
        editor.clear();
        editor.apply();
    }

    // Save profile image URI
    public void setProfileImageUri(String uri) {
        editor.putString(KEY_PROFILE_IMAGE, uri);
        editor.apply();
    }

    // Get profile image URI
    public String getProfileImageUri() {
        return pref.getString(KEY_PROFILE_IMAGE, null);
    }
}
