package com.devroid.dropdashwalletsdk;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class PaymentGatewayActivity extends AppCompatActivity {

    TextView totalAmount, productAmount, chargeAmount, deliveryAmount, totalPayingAmount;
    TextInputEditText etWalletId, etPasswordMpin;
    CheckBox captchaCheckBox;
    TextView captchaText;
    MaterialButton btnWalletLogin;
    LinearLayout chargeAmountContainer, deliveryAmountContainer;
    ProgressBar progressBar;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_gateway);

        totalAmount = findViewById(R.id.totalAmount);
        productAmount = findViewById(R.id.productAmount);
        chargeAmount = findViewById(R.id.chargeAmount);
        deliveryAmount = findViewById(R.id.deliveryAmount);
        totalPayingAmount = findViewById(R.id.totalPayingAmount);
        chargeAmountContainer = findViewById(R.id.chargeAmountContainer);
        deliveryAmountContainer = findViewById(R.id.deliveryAmountContainer);

        etWalletId = findViewById(R.id.etWalletId);
        etPasswordMpin = findViewById(R.id.etPasswordMpin);
        captchaCheckBox = findViewById(R.id.captchaCheckBox);
        captchaText = findViewById(R.id.captchaText);
        btnWalletLogin = findViewById(R.id.btnWalletLogin);

        progressBar = findViewById(R.id.progressBar);

        // Default = hidden password
        etPasswordMpin.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        etPasswordMpin.setCompoundDrawablesWithIntrinsicBounds(R.drawable.otp, 0, R.drawable.hide, 0);

        btnWalletLogin.setEnabled(false);

        // Set merchant amounts
        totalAmount.setText(getIntent().getStringExtra("totalAmount"));
        productAmount.setText(getIntent().getStringExtra("productAmount"));
        chargeAmount.setText(getIntent().getStringExtra("chargeAmount"));
        deliveryAmount.setText(getIntent().getStringExtra("deliveryAmount"));
        totalPayingAmount.setText(getIntent().getStringExtra("totalPayingAmount"));

        if (Objects.equals(getIntent().getStringExtra("chargeAmount"), "0")) {
            chargeAmount.setVisibility(View.GONE);
            chargeAmountContainer.setVisibility(View.GONE);
        } else {
            chargeAmount.setVisibility(View.VISIBLE);
            chargeAmountContainer.setVisibility(View.VISIBLE);
        }

        if (Objects.equals(getIntent().getStringExtra("deliveryAmount"), "0")) {
            deliveryAmount.setVisibility(View.GONE);
            deliveryAmountContainer.setVisibility(View.GONE);
        } else {
            deliveryAmount.setVisibility(View.VISIBLE);
            deliveryAmountContainer.setVisibility(View.VISIBLE);
        }

        // Captcha click
        captchaText.setOnClickListener(v -> startCaptcha());
        captchaCheckBox.setOnClickListener(v -> startCaptcha());

        // Password eye toggle
        etPasswordMpin.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {

                if (etPasswordMpin.getCompoundDrawables()[2] != null) {

                    int drawableWidth = etPasswordMpin.getCompoundDrawables()[2].getBounds().width();

                    if (event.getX() >= (etPasswordMpin.getWidth() - etPasswordMpin.getPaddingEnd() - drawableWidth)) {

                        int inputType = etPasswordMpin.getInputType();

                        // If password is visible → hide it
                        if ((inputType & InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD)
                                == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {

                            etPasswordMpin.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                            etPasswordMpin.setCompoundDrawablesWithIntrinsicBounds(R.drawable.otp, 0, R.drawable.show, 0);

                        } else {
                            // Show password
                            etPasswordMpin.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                            etPasswordMpin.setCompoundDrawablesWithIntrinsicBounds(R.drawable.otp, 0, R.drawable.hide, 0);
                        }

                        // Keep cursor at end
                        etPasswordMpin.setSelection(etPasswordMpin.getText().length());

                        return true;
                    }
                }
            }
            return false;
        });

        // Login & Pay
        btnWalletLogin.setOnClickListener(v -> {
            btnWalletLogin.setEnabled(false);
            progressBar.setVisibility(View.VISIBLE);
            String walletId = etWalletId.getText().toString().trim();
            String mpin = etPasswordMpin.getText().toString().trim();

            if (walletId.isEmpty()) {
                etWalletId.setError("Wallet ID required");
                etWalletId.requestFocus();
                return;
            }
            if (mpin.isEmpty()) {
                etPasswordMpin.setError("MPIN required");
                etPasswordMpin.requestFocus();
                return;
            }
            if (!captchaCheckBox.isChecked()) {
                Toast.makeText(this, "Please complete captcha", Toast.LENGTH_SHORT).show();
                return;
            }

            // Valid → call server
            sendPaymentToServer(walletId, mpin);
        });
    }

    private void startCaptcha() {
        captchaCheckBox.setChecked(true);
        btnWalletLogin.setEnabled(true);
    }

    private void sendPaymentToServer(String walletId, String mpin) {
        // Disable button immediately to prevent double tap
        btnWalletLogin.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        btnWalletLogin.setText("Processing...");

        String url = "https://payment-callback-backend.onrender.com/pay";
        int amount = Integer.parseInt(totalPayingAmount.getText().toString());

        // Generate a unique client-side transaction ID
        String clientTxnId = java.util.UUID.randomUUID().toString();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            btnWalletLogin.setEnabled(true);
            btnWalletLogin.setText("Login & Pay");
            return;
        }

        user.getIdToken(true).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Toast.makeText(this, "Failed to get Firebase ID token", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
                btnWalletLogin.setEnabled(true);
                btnWalletLogin.setText("Login & Pay");
                return;
            }

            String idToken = task.getResult().getToken();

            JSONObject jsonBody = new JSONObject();
            try {
                jsonBody.put("idToken", idToken);
                jsonBody.put("walletId", walletId);
                jsonBody.put("mpin", mpin);
                jsonBody.put("merchantId", getIntent().getStringExtra("merchantId"));
                jsonBody.put("orderId", getIntent().getStringExtra("orderId"));
                jsonBody.put("amount", amount);
                jsonBody.put("clientTxnId", clientTxnId); // <-- safe idempotency
            } catch (Exception e) {
                e.printStackTrace();
                progressBar.setVisibility(View.GONE);
                btnWalletLogin.setEnabled(true);
                btnWalletLogin.setText("Login & Pay");
                return;
            }

            RequestQueue queue = Volley.newRequestQueue(this);
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, jsonBody,
                    response -> {
                        progressBar.setVisibility(View.GONE);
                        btnWalletLogin.setEnabled(true);
                        btnWalletLogin.setText("Login & Pay");

                        try {
                            String status = response.getString("status");
                            if ("SUCCESS".equals(status)) {
                                showResultDialog(true, response.getString("transactionId"), null);
                            } else {
                                String error = response.optString("error", "Payment Failed");
                                String stack = response.optString("stack", "");
                                showResultDialog(false, null, error + (stack.isEmpty() ? "" : "\nCause: " + stack));
                            }
                        } catch (Exception e) {
                            showResultDialog(false, null, "Unexpected server error: " + e.getMessage());
                        }
                    },
                    error -> {
                        progressBar.setVisibility(View.GONE);
                        btnWalletLogin.setEnabled(true);
                        btnWalletLogin.setText("Login & Pay");

                        String errorMsg = "Server error. Please try again";

                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            try {
                                String responseBody = new String(error.networkResponse.data, "UTF-8");
                                JSONObject data = new JSONObject(responseBody);
                                String serverError = data.optString("error", null);
                                String stack = data.optString("stack", null);
                                if (serverError != null) {
                                    errorMsg = serverError;
                                    if (stack != null && !stack.isEmpty()) {
                                        errorMsg += "\nCause: " + stack;
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        showResultDialog(false, null, errorMsg);
                    }) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    return headers;
                }
            };

            // Add request to queue
            queue.add(request);
        });
    }

    private void showResultDialog(boolean success, String txnId, String errorMessage) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);

        if (success) {
            builder.setTitle("Payment Successful");
            builder.setMessage("Transaction ID: " + txnId);
            builder.setPositiveButton("OK", (dialog, which) -> {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("status", "SUCCESS");
                resultIntent.putExtra("transactionId", txnId);
                setResult(RESULT_OK, resultIntent);
                finish();
            });
        } else {
            builder.setTitle("Payment Failed");
            builder.setMessage(errorMessage);
            builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        }
        builder.show();
    }
}