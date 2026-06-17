package com.devroid.dropdashwallet;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class EnterIdToSendActivity extends BaseActivity {
    ImageButton btnBack;
    TextView walletBalance;
    ImageView btnRetry;
    RecyclerView recentTransfersRecyclerView;
    RecentTransfersAdapter recentTransfersAdapter;
    List<RecentTransfer> recentTransferList;
    LinearLayout selectionMobileContainer, selectionEmailContainer;
    LinearLayout recentTransfersContainer;
    ImageView selectionMobileIcon, selectionEmailIcon;
    TextView selectionMobile, selectionEmail;
    View selectionMobileView, selectionEmailView;
    TextInputLayout mobileNumberContainer, emailAddressContainer;
    TextInputEditText etMobileNumber, etEmailAddress;
    MaterialButton btnProceed;
    private SharedPreferences prefs;
    private long currentBalance = 0;
    private boolean isAmountVisible = false;
    String selection;
    String input;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enter_id_to_send);

        btnBack = findViewById(R.id.btnBack);
        walletBalance = findViewById(R.id.walletBalance);
        btnRetry = findViewById(R.id.btnRetry);
        btnProceed = findViewById(R.id.btnProceed);

        selectionMobileContainer = findViewById(R.id.selectionMobileContainer);
        selectionEmailContainer = findViewById(R.id.selectionEmailContainer);
        selectionMobileIcon = findViewById(R.id.selectionMobileIcon);
        selectionEmailIcon = findViewById(R.id.selectionEmailIcon);
        selectionMobile = findViewById(R.id.selectionMobile);
        selectionEmail = findViewById(R.id.selectionEmail);
        selectionMobileView = findViewById(R.id.selectionMobileView);
        selectionEmailView = findViewById(R.id.selectionEmailView);
        etMobileNumber = findViewById(R.id.etMobileNumber);
        etEmailAddress = findViewById(R.id.etEmailAddress);

        mobileNumberContainer = findViewById(R.id.mobileNumberContainer);
        emailAddressContainer = findViewById(R.id.emailAddressContainer);

        selection = "mobile";

        selectionMobileIcon.setColorFilter(getColor(R.color.app_primary));
        selectionMobile.setTextColor(getColor(R.color.app_primary));
        selectionMobileView.setBackgroundColor(getColor(R.color.app_primary));

        selectionEmailIcon.setColorFilter(Color.BLACK);
        selectionEmail.setTextColor(Color.BLACK);

        selectionMobileContainer.setOnClickListener(v -> selectMobile());

        selectionEmailContainer.setOnClickListener(v -> selectEmail());



        recentTransfersContainer = findViewById(R.id.recentTransfersContainer);

        recentTransfersRecyclerView = findViewById(R.id.recentTransfersRecyclerView);

        recentTransferList = new ArrayList<>();

        recentTransfersAdapter = new RecentTransfersAdapter(
                this,
                recentTransferList,
                transfer -> {

                    String idValue;

                    // 🔥 Decide what to use
                    if (transfer.getNumber() != null && !transfer.getNumber().isEmpty()) {
                        idValue = transfer.getNumber();
                    } else if (transfer.getEmail() != null && !transfer.getEmail().isEmpty()) {
                        idValue = transfer.getEmail();
                    } else {
                        Toast.makeText(this, "No valid ID found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // 🔥 AUTO FILL BASED ON TYPE
                    if (idValue.contains("@")) {
                        selectEmail();
                        etEmailAddress.setHint(null);
                        etEmailAddress.setText(idValue);
                        etMobileNumber.setText("");
                    } else {
                        selectMobile();
                        etMobileNumber.setHint(null);
                        etMobileNumber.setText(idValue);
                        etEmailAddress.setText("");
                    }

                    // 🔥 NEXT ACTIVITY
                    Intent intent = new Intent(EnterIdToSendActivity.this, AddAmountActivity.class);

                    intent.putExtra("name", transfer.getName());
                    intent.putExtra("amount", String.valueOf(transfer.getAmount()));
                    intent.putExtra("type", "user");
                    intent.putExtra("dPay_id", idValue);

                    startActivity(intent);
                }
        );

        recentTransfersRecyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        );

        recentTransfersRecyclerView.setAdapter(recentTransfersAdapter);

        btnBack.setOnClickListener(v-> finish());

        prefs = this.getSharedPreferences("wallet_prefs", MODE_PRIVATE);
        isAmountVisible = prefs.getBoolean("showAmount", false);

        // 🔥 Toggle click
        walletBalance.setOnClickListener(v -> {
            isAmountVisible = !isAmountVisible;
            prefs.edit().putBoolean("showAmount", isAmountVisible).apply();
            updateAmountUI();
        });

        btnRetry.setOnClickListener(v -> {
            isAmountVisible = !isAmountVisible;
            prefs.edit().putBoolean("showAmount", isAmountVisible).apply();
            fetchWalletData();
            updateAmountUI();
        });

        btnProceed.setOnClickListener(v -> {

            if (selection.equals("mobile")) {
                input = etMobileNumber.getText().toString().trim();

                if (input.isEmpty() || input.length() < 10) {
                    etMobileNumber.setError("Enter valid mobile number");
                    return;
                }
            } else if (selection.equals("email")) {
                input = etEmailAddress.getText().toString().trim();

                if (input.isEmpty() || !input.contains("@")) {
                    etEmailAddress.setError("Enter valid email address");
                    return;
                }
            }

            checkUserExists(input);

        });

        updateAmountUI();
        fetchWalletData();
        loadRecentTransfers();
    }


    private void selectMobile() {
        selection = "mobile";

        selectionMobileIcon.setColorFilter(getColor(R.color.app_primary));
        selectionMobile.setTextColor(getColor(R.color.app_primary));
        selectionMobileView.setBackgroundColor(getColor(R.color.app_primary));

        selectionEmailIcon.setColorFilter(Color.BLACK);
        selectionEmail.setTextColor(Color.BLACK);

        mobileNumberContainer.setVisibility(View.VISIBLE);
        emailAddressContainer.setVisibility(View.GONE);

        selectionMobileView.setVisibility(View.VISIBLE);
        selectionEmailView.setVisibility(View.GONE);
    }

    private void selectEmail() {
        selection = "email";

        selectionEmailIcon.setColorFilter(getColor(R.color.app_primary));
        selectionEmail.setTextColor(getColor(R.color.app_primary));
        selectionEmailView.setBackgroundColor(getColor(R.color.app_primary));

        selectionMobileIcon.setColorFilter(Color.BLACK);
        selectionMobile.setTextColor(Color.BLACK);

        emailAddressContainer.setVisibility(View.VISIBLE);
        mobileNumberContainer.setVisibility(View.GONE);

        selectionEmailView.setVisibility(View.VISIBLE);
        selectionMobileView.setVisibility(View.GONE);
    }

    private void loadRecentTransfers() {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) return;

        String uid = user.getUid();

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("recentTransfers")
                .child(uid);

        ref.orderByChild("timestamp")
                .limitToLast(50) // get enough records
                .addValueEventListener(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        recentTransferList.clear();

                        // Keep only latest transfer for each user
                        java.util.HashMap<String, RecentTransfer> uniqueTransfers =
                                new java.util.HashMap<>();

                        for (DataSnapshot ds : snapshot.getChildren()) {

                            RecentTransfer transfer =
                                    ds.getValue(RecentTransfer.class);

                            if (transfer == null) continue;

                            // Use mobile if available, otherwise email
                            String uniqueId = "";

                            if (transfer.getNumber() != null &&
                                    !transfer.getNumber().trim().isEmpty()) {

                                uniqueId = transfer.getNumber().trim();

                            } else if (transfer.getEmail() != null &&
                                    !transfer.getEmail().trim().isEmpty()) {

                                uniqueId = transfer.getEmail().trim();
                            }

                            if (uniqueId.isEmpty()) continue;

                            // Keep latest transfer only
                            if (!uniqueTransfers.containsKey(uniqueId)) {

                                uniqueTransfers.put(uniqueId, transfer);

                            } else {

                                RecentTransfer existing =
                                        uniqueTransfers.get(uniqueId);

                                long existingTime =
                                        existing.getTimestamp();

                                long newTime =
                                        transfer.getTimestamp();

                                if (newTime > existingTime) {
                                    uniqueTransfers.put(uniqueId, transfer);
                                }
                            }
                        }

                        recentTransferList.addAll(uniqueTransfers.values());

                        // Sort latest first
                        Collections.sort(recentTransferList,
                                (a, b) -> Long.compare(
                                        b.getTimestamp(),
                                        a.getTimestamp()
                                ));

                        if (recentTransferList.isEmpty()) {
                            recentTransfersContainer.setVisibility(View.GONE);
                        } else {
                            recentTransfersContainer.setVisibility(View.VISIBLE);
                        }

                        recentTransfersAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                        Toast.makeText(
                                EnterIdToSendActivity.this,
                                "Failed to load transfers",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }

    private void fetchWalletData() {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(EnterIdToSendActivity.this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = user.getUid();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("wallets").child(uid);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (snapshot.exists()) {

                    Long balance = snapshot.child("balance").getValue(Long.class);

                    currentBalance = (balance != null) ? balance : 0;

                } else {
                    currentBalance = 0;
                }

                updateAmountUI();

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(EnterIdToSendActivity.this, "Failed to load wallet", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateAmountUI() {

        walletBalance.animate().alpha(0f).setDuration(120).withEndAction(() -> {

            if (isAmountVisible) {
                walletBalance.setText(formatAmount(currentBalance));
            } else {
                walletBalance.setText("XXXX.XX");
            }

            walletBalance.animate().alpha(1f).setDuration(120).start();

        }).start();
    }

    private void checkUserExists(String receiverId) {

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("wallets");

        ref.orderByChild("walletId") // 🔥 change "id" based on your DB field (mobile/email)
                .equalTo(receiverId)
                .addValueEventListener(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        if (snapshot.exists()) {
                            // ✅ USER FOUND → go next
                            Intent iNext = new Intent(EnterIdToSendActivity.this, AddAmountActivity.class);
                            iNext.putExtra("type", "user");
                            iNext.putExtra("dPay_id", receiverId);
                            startActivity(iNext);

                        } else {
                            // ❌ USER NOT FOUND
                            if ("mobile".equals(selection)) {
                                showError(etMobileNumber, mobileNumberContainer, "User not found");
                            } else {
                                showError(etEmailAddress, emailAddressContainer, "User not found");
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(EnterIdToSendActivity.this,
                                "Database error", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showError(TextInputEditText editText,
                           TextInputLayout container,
                           String message) {

        // 🔴 Set error text
        container.setError(message);

        // 🎯 Focus on field
        editText.requestFocus();

        // 📳 Shake animation
        Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);
        editText.startAnimation(shake);

        // 🔥 Optional: temporary red stroke flash
        container.setBoxStrokeColor(Color.RED);

        new Handler().postDelayed(() -> {
            container.setBoxStrokeColor(getColor(R.color.input_stroke)); // back to normal
        }, 800);
    }

    // 🔥 Number formatting (1,23,456)
    private String formatAmount(long amount) {
        NumberFormat formatter = NumberFormat.getInstance(new Locale("en", "IN"));
        return formatter.format(amount);
    }
}
