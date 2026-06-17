package com.devroid.dropdashwallet;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;


public class BaseActivity extends AppCompatActivity {

    private ConnectivityManager.NetworkCallback networkCallback;
    private boolean isActivityVisible = false;
    static boolean noInternetActivityOpen = false; // track single instance
    Window window;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        window = getWindow();

        if (shouldSetupSystemBarsIcons()) {
            setupSystemBarsIcons(shouldUseDarkSystemBarIcons());
        }

        if (shouldSetupStatusBarColor()) {
            setupStatusBarColor(getColor(R.color.screen_background));
        }

        if (shouldSetupNavigationBarColor()) {
            setupNavigationBarColor(getColor(R.color.screen_background));
        }

        WindowCompat.setDecorFitsSystemWindows(window, true);

//        if (shouldHideSystemUI()) {
//            hideSystemUI();
//        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!shouldCheckInternet()) return; // 🔥 THIS IS THE FIX

        isActivityVisible = true;

        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        networkCallback = new ConnectivityManager.NetworkCallback() {

            @Override
            public void onLost(@NonNull Network network) {
                // redirect only if user is inside app
                if (isActivityVisible && !(BaseActivity.this instanceof NoInternetActivity)) {
                    noInternetActivityOpen = true; // prevent duplicates
                    Intent intent = new Intent(BaseActivity.this, NoInternetActivity.class);
                    intent.putExtra("previous_activity", BaseActivity.this.getClass().getName());
                    startActivity(intent);
                }
            }

            @Override
            public void onAvailable(@NonNull Network network) {
                // when coming back online, do nothing (smooth flow)
            }
        };

        // register default callback
        cm.registerDefaultNetworkCallback(networkCallback);

        // also check once initially

        // check immediately
        if (!isInternetAvailable() && !noInternetActivityOpen && !(this instanceof NoInternetActivity)) {
            noInternetActivityOpen = true;
            Intent intent = new Intent(this, NoInternetActivity.class);
            intent.putExtra("previous_activity", this.getClass().getName());
            startActivity(intent);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        isActivityVisible = false;

        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        if (networkCallback != null) {
            try {
                cm.unregisterNetworkCallback(networkCallback);
            } catch (Exception ignored) { }
        }
    }

    /** Simple capability-based internet check (no HTTP ping) */
    protected boolean isInternetAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        Network network = cm.getActiveNetwork();
        if (network == null) return false;

        NetworkCapabilities nc = cm.getNetworkCapabilities(network);
        if (nc == null) return false;

        return nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    /** immersive full screen */
//    private void hideSystemUI() {
//        View decorView = getWindow().getDecorView();
//        decorView.setSystemUiVisibility(
//                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
//                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
//                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//        );
//    }

    protected void setupSystemBarsIcons(boolean darkIcons) {

        // Modern way (Android 11+ and backward compatible)
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(window, window.getDecorView());

        if (controller != null) {
            controller.setAppearanceLightStatusBars(darkIcons);
            controller.setAppearanceLightNavigationBars(darkIcons);
        }

        // Fallback for older versions
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            int flags = window.getDecorView().getSystemUiVisibility();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (darkIcons) {
                    flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                } else {
                    flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (darkIcons) {
                    flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                } else {
                    flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                }
            }

            window.getDecorView().setSystemUiVisibility(flags);
        }
    }


    protected void setupStatusBarColor(@ColorInt int color) {
        window.setStatusBarColor(color);

    }

    protected void setupNavigationBarColor(@ColorInt int color) {
        window.setNavigationBarColor(color);
    }


    protected boolean shouldHideSystemUI() {
        return true; // default behavior for all activities
    }

    protected boolean shouldSetupSystemBarsIcons() {
        return true;
    }

    protected boolean shouldUseDarkSystemBarIcons() {
        int nightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightMode != Configuration.UI_MODE_NIGHT_YES;
    }

    protected boolean shouldCheckInternet() {
        return true; // default for all activities
    }

    protected boolean shouldSetupStatusBarColor(){
        return true;
    }

    protected boolean shouldSetupNavigationBarColor(){
        return true;
    }

    public void setNavigationBarForFragment(@ColorInt int status_color, @ColorInt int nav_color, boolean darkIcons) {
        setupStatusBarColor(status_color);
        setupNavigationBarColor(nav_color);
        setupSystemBarsIcons(darkIcons);
    }



}
