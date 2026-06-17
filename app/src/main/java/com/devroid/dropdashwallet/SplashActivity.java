package com.devroid.dropdashwallet;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends BaseActivity {

    private boolean hasNavigated = false; // 🔐 IMPORTANT
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable navigateRunnable = this::navigateOnce;

    @Override
    protected boolean shouldCheckInternet() {
        return false; // 👈 Splash controls navigation itself
    }

    @Override
    protected void setupStatusBarColor(int color) {
        super.setupStatusBarColor(Color.TRANSPARENT);
    }

    protected void setupNavigationBarColor(int color) {
        super.setupNavigationBarColor(Color.TRANSPARENT);
    }

    @Override
    protected void setupSystemBarsIcons(boolean darkIcons) {
        super.setupSystemBarsIcons(false);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // ✅ REQUIRED: initialize Firebase before using FirebaseAuth
        com.google.firebase.FirebaseApp.initializeApp(this);

        handler.postDelayed(navigateRunnable, 3000);

    }
    private synchronized void navigateOnce() {
        if (hasNavigated) return; // block double execution
        hasNavigated = true;

        Intent intent;

        if (!isInternetAvailable()) {
            intent = new Intent(this, NoInternetActivity.class);
            intent.putExtra("previous_activity", "FROM_SPLASH");
        } else {
            SessionManager sessionManager = new SessionManager(this);
            FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

            if (sessionManager.isLoggedIn() && firebaseUser != null) {
                intent = new Intent(this, MainActivity.class);
            } else {
                intent = new Intent(this, LoginActivity.class);
            }
        }

        startActivity(intent);
        finish(); // ✅ Only call finish once
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(navigateRunnable);
        super.onDestroy();
    }





}
