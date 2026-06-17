package com.devroid.dropdashwallet;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;
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

public class RemarksActivity extends BaseActivity {
    ImageButton btnBack;
    TextView walletBalance;
    ImageView btnRetry;
    TextView walletUserName, esewaId, transfer_amount;
    ImageView edit_amount;
    MaterialButton btnBillSharing, btnFamilyExpenses, btnGroceries, btnLendBorrow, btnPersonalUse, btnRideSharing;
    TextInputEditText addRemarks;
    MaterialButton btnContinue;
    private SharedPreferences prefs;
    private long currentBalance = 0;
    private boolean isAmountVisible = false;
    String esewa_id, userName, type;
    Double amount;
    private String selectedPurpose = "Bill Sharing";
    String formattedAmount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remarks);

        btnBack = findViewById(R.id.btnBack);
        walletBalance = findViewById(R.id.walletBalance);
        btnRetry = findViewById(R.id.btnRetry);

        walletUserName = findViewById(R.id.walletUserName);
        esewaId = findViewById(R.id.esewaId);
        transfer_amount = findViewById(R.id.transfer_amount);
        edit_amount = findViewById(R.id.edit_amount);

        btnBillSharing = findViewById(R.id.btnBillSharing);
        btnFamilyExpenses = findViewById(R.id.btnFamilyExpenses);
        btnGroceries = findViewById(R.id.btnGroceries);
        btnLendBorrow = findViewById(R.id.btnLendBorrow);
        btnPersonalUse = findViewById(R.id.btnPersonalUse);
        btnRideSharing = findViewById(R.id.btnRideSharing);

        addRemarks = findViewById(R.id.addRemarks);

        addRemarks.requestFocus();

        btnContinue = findViewById(R.id.btnContinue);

        btnBack.setOnClickListener(v-> finish());

        esewa_id = getIntent().getStringExtra("id");
        userName = getIntent().getStringExtra("name");
        amount = getIntent().getDoubleExtra("amount", 0);
        type = getIntent().getStringExtra("type");

        formattedAmount = String.format("%.2f", amount);

        if (esewa_id != null && !esewa_id.isEmpty()) {
            esewaId.setText(esewa_id);
        } else {
            esewaId.setText("Unknown Id");
        }

        if ("user".equals(type)) {
            if (userName != null && !userName.isEmpty()) {
                walletUserName.setText(maskName(userName));
            } else {
                walletUserName.setText("Unknown User");
            }
        } else if ("merchant".equals(type)) {
            if (userName != null && !userName.isEmpty()) {
                walletUserName.setText(userName);
            } else {
                walletUserName.setText("Unknown User");
            }
        }

        if (amount != null) {
            transfer_amount.setText(formattedAmount);
        } else {
            transfer_amount.setText("0");
        }

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

        edit_amount.setOnClickListener(v -> finish());

        btnContinue.setOnClickListener(v -> {
            String remarks = addRemarks.getText().toString().trim();

            if (remarks == null || remarks.equals("") || remarks.length() < 4) {
                addRemarks.setError("Enter valid remarks");
            } else {
                Intent iNext = new Intent(RemarksActivity.this, MakePaymentActivity.class);
                iNext.putExtra("type", type);
                iNext.putExtra("dpayId", esewa_id);
                iNext.putExtra("userName", userName);
                iNext.putExtra("amount", amount);
                iNext.putExtra("purpose", selectedPurpose);
                iNext.putExtra("remarks", remarks);
                startActivity(iNext);
            }
        });


        MaterialButton[] purposeButtons = {
                btnBillSharing,
                btnFamilyExpenses,
                btnGroceries,
                btnLendBorrow,
                btnPersonalUse,
                btnRideSharing
        };


        selectPurpose(btnBillSharing, purposeButtons);

        for (MaterialButton btn : purposeButtons) {
            btn.setOnClickListener(v -> selectPurpose(btn, purposeButtons));
        }

        Log.d("Selected Purpose", selectedPurpose);

    }

    private void fetchWalletData() {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(RemarksActivity.this, "User not logged in", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(RemarksActivity.this, "Failed to load wallet", Toast.LENGTH_SHORT).show();
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

    private void selectPurpose(MaterialButton selectedBtn, MaterialButton[] allButtons) {

        for (MaterialButton btn : allButtons) {

            btn.setChecked(btn == selectedBtn);

            if (btn == selectedBtn) {
//                btn.setIconResource(R.drawable.ic_selected);
                btn.setTextColor(getResources().getColor(R.color.green)); // optional
                selectedPurpose = selectedBtn.getText().toString();
                Log.d("Selected Purpose", selectedPurpose);
            }
            else {
                btn.setTextColor(getResources().getColor(R.color.black)); // optional
            }
        }
    }

}