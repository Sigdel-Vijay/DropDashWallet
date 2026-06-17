package com.devroid.dropdashwallet;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;

import com.airbnb.lottie.LottieAnimationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Random;

public class NoInternetActivity extends BaseActivity {

    private LottieAnimationView noInternetAnim;
    private RelativeLayout rootLayout;
    private Button retryBtn;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private int lastIndex = -1;

    private ConnectivityManager.NetworkCallback networkCallback;
    private boolean hasRedirected = false; // 🔐 IMPORTANT FLAG

    private final Random random = new Random();

    // Background colors
    private final int[] colors = {
            Color.WHITE,
            Color.WHITE,
            Color.WHITE
    };

    // Lottie animations
    private final int[] animations = {
            R.raw.no_internet1,
            R.raw.no_internet2,
            R.raw.no_internet3
    };

    private final Runnable autoChangeRunnable = new Runnable() {
        @Override
        public void run() {
            showRandomAnimation();
            handler.postDelayed(this, 5000);
        }
    };

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_no_internet);

        noInternetAnim = findViewById(R.id.noInternetAnim);
        rootLayout = findViewById(R.id.rootLayout);
        retryBtn = findViewById(R.id.retryBtn);

        noInternetAnim.setAlpha(0f);
        showRandomAnimation();

        handler.postDelayed(autoChangeRunnable, 5000);

        retryBtn.setOnClickListener(v -> {
            if (isInternetAvailable()) {
                safeRedirect();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        // Internet already restored → exit immediately (once)
        if (isInternetAvailable()) {
            safeRedirect();
            return;
        }

        // Listen for internet restore
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                runOnUiThread(() -> safeRedirect());
            }

            @Override
            public void onLost(@NonNull Network network) {
                // no action
            }
        };

        cm.registerDefaultNetworkCallback(networkCallback);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterNetworkCallback();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(autoChangeRunnable);
    }

    /** ====================== INTERNET CHECK ====================== */
    protected boolean isInternetAvailable() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        Network network = cm.getActiveNetwork();
        if (network == null) return false;

        NetworkCapabilities nc = cm.getNetworkCapabilities(network);
        if (nc == null) return false;

        return nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    /** ====================== SAFE REDIRECT ====================== */
    private synchronized void safeRedirect() {
        if (hasRedirected) return; // ❌ block double calls
        hasRedirected = true;

        unregisterNetworkCallback();
        BaseActivity.noInternetActivityOpen = false;
        goBackToApp();
    }

    private void unregisterNetworkCallback() {
        if (networkCallback != null) {
            ConnectivityManager cm =
                    (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            try {
                cm.unregisterNetworkCallback(networkCallback);
            } catch (Exception ignored) {
            }
            networkCallback = null;
        }
    }

    /** ====================== NAVIGATION ====================== */
    private void goBackToApp() {

        String prev = getIntent().getStringExtra("previous_activity");
        Intent intent;

        if ("FROM_SPLASH".equals(prev)) {

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                intent = new Intent(this, MainActivity.class);
            } else {
                intent = new Intent(this, LoginActivity.class);
            }

        } else if (prev != null) {
            try {
                Class<?> clazz = Class.forName(prev);
                intent = new Intent(this, clazz);
            } catch (Exception e) {
                intent = new Intent(this, MainActivity.class);
            }
        } else {
            intent = new Intent(this, MainActivity.class);
        }

        startActivity(intent);
        finish();
    }

    /** ====================== ANIMATION ====================== */
    private void showRandomAnimation() {

        int index;
        do {
            index = random.nextInt(animations.length);
        } while (index == lastIndex);
        lastIndex = index;

        rootLayout.setBackgroundColor(colors[random.nextInt(colors.length)]);

        noInternetAnim.cancelAnimation();
        noInternetAnim.setAnimation(animations[index]);
        noInternetAnim.setAlpha(0f);
        noInternetAnim.playAnimation();
        noInternetAnim.animate().alpha(1f).setDuration(600).start();
    }
}
