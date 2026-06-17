package com.devroid.dropdashwallet;

import android.content.Intent;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.*;
import com.google.firebase.database.*;
import com.google.firebase.messaging.FirebaseMessaging;
import java.util.HashMap;
import java.util.Map;

public class OtpVerificationActivity extends BaseActivity {

    ImageButton btnBack;
    TextInputEditText etOtpCode;
    MaterialButton btnConfirmOtp;

    String email, otp;
    String verificationId, mobile, password;
    String selection;
    String walletId;
    boolean rememberMe;

    FirebaseAuth mAuth;
    DatabaseReference dbRef, tokenRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_verification);

        btnBack = findViewById(R.id.btnBack);
        etOtpCode = findViewById(R.id.etOtpCode);
        btnConfirmOtp = findViewById(R.id.btnConfirmOtp);

        btnBack.setOnClickListener(v -> finish());

        // Get data
        email = getIntent().getStringExtra("email");
        otp = getIntent().getStringExtra("otp");
        verificationId = getIntent().getStringExtra("verificationId");
        mobile = getIntent().getStringExtra("mobile");
        password = getIntent().getStringExtra("password");

        walletId = getIntent().getStringExtra("walletId");

        selection = getIntent().getStringExtra("selection");
        if (selection == null) selection = "mobile";

        rememberMe = getIntent().getBooleanExtra("rememberMe", false);

        mAuth = FirebaseAuth.getInstance();
        dbRef = FirebaseDatabase.getInstance().getReference("wallets");
        tokenRef = FirebaseDatabase.getInstance().getReference("fcmTokens");

        btnConfirmOtp.setOnClickListener(v -> {
            String otpCode = etOtpCode.getText() == null
                    ? "" : etOtpCode.getText().toString().trim();

            if (otpCode.isEmpty()) {
                etOtpCode.setError("Enter OTP");
                return;
            }

            if ("email".equals(selection)) {
                verifyEmailOtp(otpCode);
            } else {
                verifyFirebaseOtp(otpCode);
            }
        });
    }

    // =============================
    // ✅ PHONE OTP VERIFY
    // =============================
    private void verifyFirebaseOtp(String otpCode) {

        if (verificationId == null || verificationId.isEmpty()) {
            Toast.makeText(this, "Verification ID missing", Toast.LENGTH_SHORT).show();
            return;
        }

        PhoneAuthCredential credential =
                PhoneAuthProvider.getCredential(verificationId, otpCode);

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {

                        FirebaseUser user = mAuth.getCurrentUser();

                        if (user == null) {
                            Toast.makeText(this, "User error", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        createOrUpdateWallet(user);
                        getAndSaveFcmToken(user.getUid());

                    } else {
                        Toast.makeText(this,
                                "Invalid or expired OTP", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // =============================
    // ✅ EMAIL OTP VERIFY
    // =============================
    private void verifyEmailOtp(String enteredOtp) {

        if (!enteredOtp.equals(otp)) {
            etOtpCode.setError("Invalid OTP");
            return;
        }

        // First try login
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        createOrUpdateWallet(user);
                        getAndSaveFcmToken(user.getUid());
                    } else {
                        // If not exists → create
                        createNewEmailUser();
                    }
                });
    }

    private void createNewEmailUser() {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        createOrUpdateWallet(user);
                        getAndSaveFcmToken(user.getUid());
                    } else {
                        Toast.makeText(this,
                                "Auth failed: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // =============================
    // ✅ CREATE / UPDATE WALLET
    // =============================
    private void createOrUpdateWallet(FirebaseUser user) {

        if (user == null) return;

        String uid = user.getUid();

        DatabaseReference userRef = dbRef.child(uid);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                // 🔥 If wallet already exists → do nothing
                if (snapshot.exists()) return;

                userRef.child("uid").setValue(uid);
                userRef.child("balance").setValue(500);
                userRef.child("rewardPoints").setValue(0);
                userRef.child("createdAt").setValue(System.currentTimeMillis());

                if ("email".equals(selection)) {
                    userRef.child("email").setValue(email);
                    userRef.child("walletId").setValue(email);
                } else {
                    userRef.child("mobile").setValue(mobile);
                    userRef.child("walletId").setValue(mobile);
                }

                // ⚠️ MPIN will be set later in another screen
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(OtpVerificationActivity.this,
                        "Database error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getAndSaveFcmToken(String uid) {
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {

                    Map<String, Object> tokenMap = new HashMap<>();
                    tokenMap.put(token, ServerValue.TIMESTAMP);

                    tokenRef.child("users")
                            .child(uid)
                            .updateChildren(tokenMap)
                            .addOnSuccessListener(aVoid -> {
                                Log.d("DB_SAVE", "FCM token saved");
                                goToSetMpin(token);
                            })
                            .addOnFailureListener(e -> {
                                Log.d("DB_SAVE", "FCM token save failed", e);
                                goToSetMpin(token);
                            });
                })
                .addOnFailureListener(e -> Log.e("FCM", "Token fetch failed", e));
    }


    // 🔔 SEND TO BACKEND



    // =============================
    // ✅ NAVIGATION
    // =============================
    private void goToSetMpin(String token) {

        SessionManager sessionManager = new SessionManager(this);

        String identifier = "email".equals(selection) ? email : mobile;

        sessionManager.createLoginSession(identifier, "Wallet", rememberMe);

        Intent intent = new Intent(this, SetUpMpinActivity.class);
        intent.putExtra("token", token); // ✅ correct
        startActivity(intent);
        finish();
    }
}