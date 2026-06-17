package com.devroid.dropdashwallet;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;
import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService{
    private static final String LOGIN_CHANNEL_ID = "login_channel";
    private static final String PAYMENT_CHANNEL_ID = "payment_channel";

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.d("FCM", "New token: " + token);

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

            Map<String, Object> tokenMap = new HashMap<>();
            tokenMap.put(token, true);

            FirebaseDatabase.getInstance()
                    .getReference("fcmTokens")
                    .child("users")
                    .child(uid)
                    .updateChildren(tokenMap);
        }
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            Log.w("FCM", "Notification permission not granted");
            return;
        }

        String title = null;
        String body = null;
        String type = null;
        String amount = null;
        String senderName = null;
        String receiverName = null;
        String transactionType = null;
        String transactionId = null;

        // ✅ DATA payload
        if (!remoteMessage.getData().isEmpty()) {
            title = remoteMessage.getData().get("title");
            body = remoteMessage.getData().get("body");
            type = remoteMessage.getData().get("type");
            amount = remoteMessage.getData().get("amount");
            senderName = remoteMessage.getData().get("senderName");
            receiverName = remoteMessage.getData().get("receiverName");
            transactionType = remoteMessage.getData().get("transactionType");
            transactionId = remoteMessage.getData().get("transactionId");
        }

        // ✅ NOTIFICATION fallback
        if (title == null && remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            body = remoteMessage.getNotification().getBody();
            type = "payment";
        }

        if (title == null) return;

        boolean isLogin = "login".equals(type);

        String channelId = isLogin ? LOGIN_CHANNEL_ID : PAYMENT_CHANNEL_ID;
        String channelName = isLogin ? "Login Notifications" : "Payment Notifications";

        Uri soundUri = isLogin
                ? Uri.parse("android.resource://" + getPackageName() + "/raw/login_sound")
                : Uri.parse("android.resource://" + getPackageName() + "/raw/payment_sound");

        NotificationManager manager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    channelName,
                    NotificationManager.IMPORTANCE_HIGH
            );

            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build();

            channel.setSound(soundUri, audioAttributes);
            manager.createNotificationChannel(channel);
        }

        Intent intent;

        if ("payment".equals(type)) {
            intent = new Intent(this, PaymentNotificationActivity.class);

            // ✅ Pass data to activity
            intent.putExtra("amount", amount);
            intent.putExtra("senderName", senderName);
            intent.putExtra("receiverName", receiverName);
            intent.putExtra("transactionType", transactionType);
            intent.putExtra("transactionId", transactionId);

        } else {
            intent = new Intent(this, MainActivity.class);
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                (int) System.currentTimeMillis(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build();

        manager.notify((int) System.currentTimeMillis(), notification);
    }

}
