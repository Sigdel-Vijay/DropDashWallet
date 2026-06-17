package com.devroid.dropdashwallet;

import android.content.Context;
import android.content.SharedPreferences;

public class ApiConfig {

    // Your PC IP server (BlueStacks / real phone use this)
    public static final String LAN_SERVER = "http://192.168.1.91:8080";

    // fallback (optional production)
    public static final String PROD_SERVER = "https://yourdomain.com";

    public static String getServerUrl(Context context) {

        SharedPreferences prefs = context.getSharedPreferences("config", Context.MODE_PRIVATE);
        boolean useProd = prefs.getBoolean("useProd", false);

        return useProd ? PROD_SERVER : LAN_SERVER;
    }
}
