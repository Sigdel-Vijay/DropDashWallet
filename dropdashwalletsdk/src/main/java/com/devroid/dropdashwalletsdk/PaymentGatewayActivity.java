package com.devroid.dropdashwalletsdk;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.TimeoutError;
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
import java.util.UUID;

public class PaymentGatewayActivity extends AppCompatActivity {

    private static final String RAILWAY_URL = "https://payment-callback-backend-production.up.railway.app/pay";
    private static final String RENDER_URL = "https://payment-callback-backend.onrender.com/pay";

    TextView totalAmount, productAmount, chargeAmount, deliveryAmount, totalPayingAmount;
    TextInputEditText etWalletId, etPasswordMpin;
    CheckBox captchaCheckBox;
    TextView captchaText;
    MaterialButton btnWalletLogin;
    LinearLayout chargeAmountContainer, deliveryAmountContainer;
    ProgressBar progressBar;

    private boolean isRequestRunning = false;
    private int retryCount = 0;
    private static final int MAX_RETRIES = 3;
    private final Handler retryHandler = new Handler(Looper.getMainLooper());
    private String currentClientTxnId;
    private RequestQueue requestQueue;

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

        requestQueue = Volley.newRequestQueue(getApplicationContext());

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
            currentClientTxnId = UUID.randomUUID().toString();
            sendPaymentToServer(walletId, mpin, currentClientTxnId);
        });
    }

    private void startCaptcha() {
        captchaCheckBox.setChecked(true);
        btnWalletLogin.setEnabled(true);
    }

    private void sendPaymentToServer(String walletId, String mpin, String clientTxnId) {
        sendPaymentRequest(RAILWAY_URL, walletId, mpin, clientTxnId, true);
    }

    private void sendPaymentRequest(String url, String walletId, String mpin, String clientTxnId, boolean allowFallback) {
        if (isRequestRunning) {
            return;
        }

        isRequestRunning = true;
        setLoadingState(true);

        double amount;
        try {
            amount = Double.parseDouble(totalPayingAmount.getText().toString());
        } catch (Exception e) {
            Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
            resetPaymentState();
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            resetPaymentState();
            return;
        }

        user.getIdToken(true).addOnCompleteListener(task -> {
            if (!isActivitySafe()) {
                return;
            }

            if (!task.isSuccessful()) {
                Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show();
                resetPaymentState();
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
                jsonBody.put("amount", String.valueOf(amount));
                jsonBody.put("clientTxnId", clientTxnId);
            } catch (Exception e) {
                e.printStackTrace();
                resetPaymentState();
                return;
            }

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, jsonBody,
                    response -> {
                        if (!isActivitySafe()) return;
                        retryCount = 0;
                        resetPaymentState();

                        try {
                            String status = response.getString("status");
                            if ("SUCCESS".equals(status)) {
                                showResultDialog(true, response.getString("transactionId"), null);
                            } else {
                                String error = response.optString("error", "Payment Failed");
                                showResultDialog(false, null, error);
                            }
                        } catch (Exception e) {
                            showResultDialog(false, null, "Server error: " + e.getMessage());
                        }
                    },
                    error -> {
                        if (!isActivitySafe()) return;

                        // Railway failed -> switch to Render
                        if (allowFallback && url.contains("railway.app")) {
                            isRequestRunning = false;
                            Toast.makeText(this, "Railway unavailable. Switching server...", Toast.LENGTH_SHORT).show();
                            sendPaymentRequest(RENDER_URL, walletId, mpin, clientTxnId, false);
                            return;
                        }

                        // Handle Render cold start (Waking server)
                        if (error instanceof TimeoutError || error instanceof NoConnectionError || error instanceof NetworkError) {
                            if (retryCount < MAX_RETRIES) {
                                retryCount++;
                                isRequestRunning = false;
                                Toast.makeText(this, "Waking secure server... (" + retryCount + "/" + MAX_RETRIES + ")", Toast.LENGTH_SHORT).show();
                                
                                retryHandler.postDelayed(() -> {
                                    if (isActivitySafe()) {
                                        sendPaymentToServer(walletId, mpin, clientTxnId);
                                    }
                                }, 4000);
                                return;
                            }
                        }

                        retryCount = 0;
                        resetPaymentState();
                        showResultDialog(false, null, getVolleyErrorMessage(error));
                    }) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    return headers;
                }
            };

            request.setRetryPolicy(new DefaultRetryPolicy(25000, 0, 1f));
            requestQueue.add(request);
        });
    }

    private String getVolleyErrorMessage(com.android.volley.VolleyError error) {
        try {
            if (error.networkResponse != null && error.networkResponse.data != null) {
                String responseBody = new String(error.networkResponse.data, "UTF-8");
                JSONObject data = new JSONObject(responseBody);
                return data.optString("error", "Payment Failed");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (error instanceof TimeoutError) return "Server timeout. Please try again.";
        if (error instanceof NoConnectionError) return "No internet connection.";
        return "Payment failed. Please try again.";
    }

    private void resetPaymentState() {
        isRequestRunning = false;
        setLoadingState(false);
    }

    private void setLoadingState(boolean loading) {
        btnWalletLogin.setEnabled(!loading);
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnWalletLogin.setText(loading ? "Processing..." : "Login & Pay");
    }

    private boolean isActivitySafe() {
        return !(isFinishing() || isDestroyed());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (requestQueue != null) {
            requestQueue.cancelAll(this);
        }
        retryHandler.removeCallbacksAndMessages(null);
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
