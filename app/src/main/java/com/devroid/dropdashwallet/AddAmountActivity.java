package com.devroid.dropdashwallet;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.util.Locale;


public class AddAmountActivity extends BaseActivity {
    ImageButton btnBack;
    TextView walletBalance;
    ImageView btnRetry;
    TextView walletUserName, walletId;
    Switch dProwSwitch;
    LinearLayout dProwEnabledContainer;
    TextInputEditText etEnterAmount;
    MaterialButton btn1, btn2, btn3, btn4, btn5, btn6, btn7, btn8, btn9, btn0, btnDot, btnDel;
    MaterialButton btnContinue;
    private SharedPreferences prefs;
    private long currentBalance = 0;
    private boolean isAmountVisible = false;
    String type, name, amount, esewaId, merchantId;
    String fetchedName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_amount);

        btnBack = findViewById(R.id.btnBack);
        walletBalance = findViewById(R.id.walletBalance);
        btnRetry = findViewById(R.id.btnRetry);

        walletUserName = findViewById(R.id.walletUserName);
        walletId = findViewById(R.id.walletId);

        dProwSwitch = findViewById(R.id.dProwSwitch);
        dProwEnabledContainer = findViewById(R.id.dProwEnabledContainer);

        etEnterAmount = findViewById(R.id.etEnterAmount);

        type = getIntent().getStringExtra("type");
        name = getIntent().getStringExtra("name");
        amount = getIntent().getStringExtra("amount");
        esewaId = getIntent().getStringExtra("dPay_id");
        merchantId = getIntent().getStringExtra("merchant_id");

        if ("user".equals(type)) {
            walletId.setText(esewaId);
            if (name != null && !name.isEmpty()) {
                walletUserName.setText(maskName(name));
            } else {
                fetchUserNameFromFirebase(esewaId, type);
            }

        } else if ("merchant".equals(type)) {
            walletId.setText(merchantId);

            if (name != null && !name.isEmpty()) {
                walletUserName.setText(name);
            } else {
                fetchUserNameFromFirebase(merchantId, type);
            }
        }



        if (amount != null && !amount.isEmpty()) {
            etEnterAmount.setText(amount);
        }

        btn1 = findViewById(R.id.btn1);
        btn2 = findViewById(R.id.btn2);
        btn3 = findViewById(R.id.btn3);
        btn4 = findViewById(R.id.btn4);
        btn5 = findViewById(R.id.btn5);
        btn6 = findViewById(R.id.btn6);
        btn7 = findViewById(R.id.btn7);
        btn8 = findViewById(R.id.btn8);
        btn9 = findViewById(R.id.btn9);
        btn0 = findViewById(R.id.btn0);
        btnDot = findViewById(R.id.btnDot);
        btnDel = findViewById(R.id.btnDel);

        btnContinue = findViewById(R.id.btnContinue);

        btnBack.setOnClickListener(v -> finish());


        etEnterAmount.setShowSoftInputOnFocus(false); // disable keyboard
        etEnterAmount.setCursorVisible(true);         // optional (show cursor)
        etEnterAmount.setFocusable(true);
        etEnterAmount.setFocusableInTouchMode(true);


        prefs = this.getSharedPreferences("wallet", Context.MODE_PRIVATE);

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

        btn1.setOnClickListener(v -> appendText("1"));
        btn2.setOnClickListener(v -> appendText("2"));
        btn3.setOnClickListener(v -> appendText("3"));
        btn4.setOnClickListener(v -> appendText("4"));
        btn5.setOnClickListener(v -> appendText("5"));
        btn6.setOnClickListener(v -> appendText("6"));
        btn7.setOnClickListener(v -> appendText("7"));
        btn8.setOnClickListener(v -> appendText("8"));
        btn9.setOnClickListener(v -> appendText("9"));
        btn0.setOnClickListener(v -> appendText("0"));
        btnDot.setOnClickListener(v -> appendText("."));

        btnDel.setOnClickListener(v -> deleteLast());

        btnContinue.setOnClickListener(v -> {
            double amount = getEnteredAmount();

            if (amount > 0) {
                Intent intent = new Intent(AddAmountActivity.this, RemarksActivity.class);
                intent.putExtra("type", type);
                intent.putExtra("id", walletId.getText().toString());
                intent.putExtra("name", walletUserName.getText().toString());
                intent.putExtra("amount", amount);
                startActivity(intent);
            } else {
                etEnterAmount.setError("Enter valid amount");
            }
        });


// Load saved state (default OFF)
        boolean isDproEnabled = prefs.getBoolean("dprow_enabled", false);

// Apply state to switch
        dProwSwitch.setChecked(isDproEnabled);

// Apply visibility based on saved state
        dProwEnabledContainer.setVisibility(isDproEnabled ? View.VISIBLE : View.GONE);

        dProwSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {

            prefs.edit().putBoolean("dprow_enabled", isChecked).apply();

            if (isChecked) {
                dProwEnabledContainer.setAlpha(0f);
                dProwEnabledContainer.setVisibility(View.VISIBLE);
                dProwEnabledContainer.animate().alpha(1f).setDuration(200).start();
            } else {
                dProwEnabledContainer.animate().alpha(0f).setDuration(200).withEndAction(() -> dProwEnabledContainer.setVisibility(View.GONE)).start();
            }
        });


        updateAmountUI();

        fetchWalletData();

    }

    private void fetchUserNameFromFirebase(String receiverId, String type) {

        if (receiverId == null || receiverId.isEmpty()) {
            walletUserName.setText("Unknown User");
            return;
        }

        if ("user".equals(type)) {

            DatabaseReference ref = FirebaseDatabase.getInstance()
                    .getReference("wallets");

            ref.orderByChild("walletId")
                    .equalTo(receiverId)
                    .addValueEventListener(new ValueEventListener() {

                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {

                            if (snapshot.exists()) {

                                for (DataSnapshot ds : snapshot.getChildren()) {

                                    fetchedName = ds.child("name").getValue(String.class);

                                    if (fetchedName != null && !fetchedName.isEmpty()) {
                                        walletUserName.setText(maskName(fetchedName));
                                    } else {
                                        walletUserName.setText("Unknown User");
                                    }

                                    return;
                                }
                            } else {
                                walletUserName.setText("User not found");
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            walletUserName.setText("Error loading name");
                        }
                    });

        } else if ("merchant".equals(type)) {

            DatabaseReference ref = FirebaseDatabase.getInstance()
                    .getReference("merchants");

            ref.orderByChild("merchantId")
                    .equalTo(receiverId)
                    .addValueEventListener(new ValueEventListener() {

                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {

                            if (snapshot.exists()) {

                                for (DataSnapshot ds : snapshot.getChildren()) {

                                    fetchedName = ds.child("businessName").getValue(String.class);

                                    if (fetchedName != null && !fetchedName.isEmpty()) {
                                        walletUserName.setText(fetchedName);
                                    } else {
                                        walletUserName.setText("Unknown Merchant");
                                    }

                                    return;
                                }
                            } else {
                                walletUserName.setText("Merchant not found");
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            walletUserName.setText("Error loading name");
                        }
                    });
        }
    }

    private void fetchWalletData() {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(AddAmountActivity.this, "User not logged in", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(AddAmountActivity.this, "Failed to load wallet", Toast.LENGTH_SHORT).show();
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

    private void appendText(String value) {
        String current = etEnterAmount.getText().toString();

        // Prevent multiple dots
        if (value.equals(".") && current.contains(".")) return;

        // Prevent leading zero like 000
        if (current.equals("0") && !value.equals(".")) {
            current = "";
        }

        etEnterAmount.setText(current + value);
        etEnterAmount.setSelection(etEnterAmount.getText().length()); // move cursor to end
    }

    private void deleteLast() {
        String current = etEnterAmount.getText().toString();

        if (!current.isEmpty()) {
            current = current.substring(0, current.length() - 1);
            etEnterAmount.setText(current);
            etEnterAmount.setSelection(current.length());
        }
    }

    private double getEnteredAmount() {
        String text = etEnterAmount.getText().toString().trim();

        if (text.isEmpty() || text.equals(".")) return 0;

        try {
            return Double.parseDouble(text);
        } catch (Exception e) {
            return 0;
        }
    }

    // 🔥 Number formatting (1,23,456)
    private String formatAmount(long amount) {
        NumberFormat formatter = NumberFormat.getInstance(new Locale("en", "IN"));
        return formatter.format(amount);
    }

    private String maskName(String fullName) {

        if (fullName == null || fullName.isEmpty()) return "Unknown User";

        String[] parts = fullName.split(" ");

        if (parts.length == 1) {
            // Only one name (e.g. Bijay)
            String name = parts[0];

            if (name.length() <= 3) return name;

            return name.substring(0, 3) + "**";
        }

        // First name + Last name
        String firstName = parts[0];
        String lastName = parts[parts.length - 1];

        String maskedFirst;

        if (firstName.length() <= 3) {
            maskedFirst = firstName;
        } else {
            maskedFirst = firstName.substring(0, 3) + "**";
        }

        return maskedFirst + " " + lastName;
    }
}