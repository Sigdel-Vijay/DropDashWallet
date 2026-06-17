package com.devroid.dropdashwallet;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
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
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class LoadMoneyActivity extends BaseActivity {

    private TextInputEditText etAmount;
    private RadioGroup sourceGroup;
    private MaterialButton btn500, btn1000, btn2000, btnLoadMoney;

    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load_money);

        ImageButton btnBack = findViewById(R.id.btnBack);
        etAmount = findViewById(R.id.etAmount);
        sourceGroup = findViewById(R.id.sourceGroup);
        btn500 = findViewById(R.id.btn500);
        btn1000 = findViewById(R.id.btn1000);
        btn2000 = findViewById(R.id.btn2000);
        btnLoadMoney = findViewById(R.id.btnLoadMoney);

        btnBack.setOnClickListener(v -> finish());
        btn500.setOnClickListener(v -> setAmount("500"));
        btn1000.setOnClickListener(v -> setAmount("1000"));
        btn2000.setOnClickListener(v -> setAmount("2000"));
        btnLoadMoney.setOnClickListener(v -> validateAndLoadMoney());
    }

    private void setAmount(String amount) {
        etAmount.setText(amount);
        etAmount.setSelection(amount.length());
    }

    private void validateAndLoadMoney() {
        if (isLoading) {
            return;
        }

        String amountText = etAmount.getText() == null ? "" : etAmount.getText().toString().trim();
        double amount;

        try {
            amount = Double.parseDouble(amountText);
        } catch (Exception e) {
            amount = 0;
        }

        if (amount <= 0) {
            etAmount.setError("Enter valid amount");
            etAmount.requestFocus();
            return;
        }

        int sourceId = sourceGroup.getCheckedRadioButtonId();
        if (sourceId == -1) {
            Toast.makeText(this, "Choose load source", Toast.LENGTH_SHORT).show();
            return;
        }

        RadioButton selectedSource = findViewById(sourceId);
        creditWallet(Math.round(amount), selectedSource.getText().toString());
    }

    private void creditWallet(long amount, String source) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        isLoading = true;
        btnLoadMoney.setEnabled(false);
        btnLoadMoney.setText("Loading...");

        DatabaseReference walletRef = FirebaseDatabase.getInstance()
                .getReference("wallets")
                .child(user.getUid());

        walletRef.child("balance").runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                Long currentBalance = currentData.getValue(Long.class);
                currentData.setValue((currentBalance == null ? 0 : currentBalance) + amount);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed, DataSnapshot currentData) {
                if (error != null || !committed) {
                    resetLoading();
                    Toast.makeText(LoadMoneyActivity.this, "Failed to load money", Toast.LENGTH_SHORT).show();
                    return;
                }

                saveLoadMoneyTransaction(user.getUid(), amount, source);
            }
        });
    }

    private void saveLoadMoneyTransaction(String uid, long amount, String source) {
        String transactionId = UUID.randomUUID().toString();

        Map<String, Object> data = new HashMap<>();
        data.put("transactionId", transactionId);
        data.put("amount", amount);
        data.put("source", source);
        data.put("status", "SUCCESS");
        data.put("type", "load_money");
        data.put("timestamp", System.currentTimeMillis());

        FirebaseDatabase.getInstance()
                .getReference("loadMoneyTransactions")
                .child(uid)
                .child(transactionId)
                .setValue(data)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Money loaded successfully", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(this, TransactionSuccessfulActivity.class);
                    intent.putExtra("status", "SUCCESS");
                    intent.putExtra("transactionId", transactionId);
                    intent.putExtra("amount", (double) amount);
                    intent.putExtra("receiverName", String.format(Locale.US, "Loaded from %s", source));
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    resetLoading();
                    Toast.makeText(this, "Money loaded, but receipt was not saved", Toast.LENGTH_LONG).show();
                });
    }

    private void resetLoading() {
        isLoading = false;
        btnLoadMoney.setEnabled(true);
        btnLoadMoney.setText("Load Money");
    }
}
