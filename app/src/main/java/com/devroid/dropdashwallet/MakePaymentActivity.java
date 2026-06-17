package com.devroid.dropdashwallet;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONObject;
import org.mindrot.jbcrypt.BCrypt;

import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class MakePaymentActivity extends BaseActivity {

    private LinearLayout main;
    private ImageButton btnBack;
    private TextView walletBalance;
    private ImageView btnRetry;
    private MaterialButton btnConfirm;

    private SharedPreferences prefs;

    private long currentBalance = 0;
    private boolean isAmountVisible = false;

    private TextView transferAmount;
    private TextView dpayId, receiverName, purpose, remarks;
    String merchantId, orderId, orderNumber;
    boolean prefilled;
    private TextView payingAmount;

    private LinearLayout payViaContainer;
    private LinearLayout payViaDpayContainer, linkedBankAccountContainer;

    private TextView tvPayViaDpay, tvLinkedBankAccount;

    private ImageView imgPayViaDpay, imgLinkedBankAccount;

    private LinearLayout confirmPaymentContainer;

    private MaterialButton btnPayVia, btnConfirmPaymentViaDpay;

    private TextInputEditText etEnterMpin;

    private View dimBackground;

    private String type, dpay_id, userName, payment_purpose, rmks;

    private double amount;

    private static final String REQUEST_TAG = "PAYMENT_REQUEST";

    private RequestQueue requestQueue;

    private boolean isRequestRunning = false;

    private int retryCount = 0;

    private static final int MAX_RETRIES = 3;

    private final Handler retryHandler =
            new Handler(Looper.getMainLooper());

    private String currentClientTxnId;


    private static final String RAILWAY_URL =
            "https://wallet-pay-production.up.railway.app/pay";

    private static final String RENDER_URL =
            "https://wallet-pay.onrender.com/pay";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_make_payment);

        initViews();

        requestQueue = Volley.newRequestQueue(this);

        prefs = getSharedPreferences("wallet_prefs", MODE_PRIVATE);

        isAmountVisible =
                prefs.getBoolean("showAmount", false);

        loadIntentData();

        setupInitialUI();

        setupClickListeners();

        updateAmountUI();

        fetchWalletData();
    }

    private void initViews() {

        main = findViewById(R.id.main);

        btnBack = findViewById(R.id.btnBack);

        walletBalance = findViewById(R.id.walletBalance);

        btnRetry = findViewById(R.id.btnRetry);

        btnConfirm = findViewById(R.id.btnConfirm);

        transferAmount = findViewById(R.id.transferAmount);

        dpayId = findViewById(R.id.dpayId);

        receiverName = findViewById(R.id.receiverName);

        purpose = findViewById(R.id.purpose);

        remarks = findViewById(R.id.remarks);

        payingAmount = findViewById(R.id.payingAmount);

        payViaContainer = findViewById(R.id.payViaContainer);

        payViaDpayContainer =
                findViewById(R.id.payViaDpayContainer);

        linkedBankAccountContainer =
                findViewById(R.id.linkedBankAccountContainer);

        tvPayViaDpay =
                findViewById(R.id.tvPayViaDpay);

        tvLinkedBankAccount =
                findViewById(R.id.tvLinkedBankAccount);

        imgPayViaDpay =
                findViewById(R.id.imgPayViaDpay);

        imgLinkedBankAccount =
                findViewById(R.id.imgLinkedBankAccount);

        confirmPaymentContainer =
                findViewById(R.id.confirmPaymentContainer);

        btnPayVia = findViewById(R.id.btnPayVia);

        btnConfirmPaymentViaDpay =
                findViewById(R.id.btnConfirmPaymentViaDpay);

        etEnterMpin =
                findViewById(R.id.etEnterMpin);

        dimBackground =
                findViewById(R.id.dimBackground);
    }

    private void loadIntentData() {

        type =
                getIntent().getStringExtra("type");

        dpay_id =
                getIntent().getStringExtra("dpayId");

        userName =
                getIntent().getStringExtra("userName");

        amount =
                getIntent().getDoubleExtra("amount", 0);

        payment_purpose =
                getIntent().getStringExtra("purpose");

        rmks =
                getIntent().getStringExtra("remarks");

        merchantId = getIntent().getStringExtra("merchant_id");
        orderId = getIntent().getStringExtra("order_id");
        orderNumber = getIntent().getStringExtra("order_number");

        prefilled = getIntent().getBooleanExtra("prefilled", false);
    }

    private void setupInitialUI() {

        payViaContainer.setVisibility(View.GONE);

        if ("user".equals(type)) {
            receiverName.setText(maskName(userName));
            dpayId.setText(dpay_id);
        } else if ("merchant".equals(type)){
            receiverName.setText(userName);
            dpayId.setText(dpay_id);
        } else if ("merchant_payment".equals(type)) {
            receiverName.setText(userName);
            dpayId.setText(merchantId);
        }

        purpose.setText(payment_purpose);

        remarks.setText(rmks);

        String formattedAmount =
                String.format(Locale.US, "%.2f", amount);

        transferAmount.setText(formattedAmount);

        payingAmount.setText(formattedAmount);

        selectDpayOption();
    }

    private void setupClickListeners() {

        btnBack.setOnClickListener(v -> {

            if (isRequestRunning) {

                Toast.makeText(
                        this,
                        "Payment is processing",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            finish();
        });

        walletBalance.setOnClickListener(v -> {

            isAmountVisible = !isAmountVisible;

            prefs.edit()
                    .putBoolean(
                            "showAmount",
                            isAmountVisible
                    )
                    .apply();

            updateAmountUI();
        });

        btnRetry.setOnClickListener(v -> {

            fetchWalletData();

            Toast.makeText(
                    this,
                    "Refreshing balance...",
                    Toast.LENGTH_SHORT
            ).show();
        });

        btnConfirm.setOnClickListener(v -> {

            payViaContainer.setVisibility(View.VISIBLE);

            dimBackground.setVisibility(View.VISIBLE);
        });

        payViaDpayContainer.setOnClickListener(v ->
                selectDpayOption());

        linkedBankAccountContainer.setOnClickListener(v ->
                selectBankOption());

        btnPayVia.setOnClickListener(v -> {

            confirmPaymentContainer.setVisibility(View.VISIBLE);

            payViaContainer.setVisibility(View.GONE);

            etEnterMpin.requestFocus();
        });

        dimBackground.setOnClickListener(v -> {

            if (isRequestRunning) {
                return;
            }

            closeAllDialogs();
        });

        btnConfirmPaymentViaDpay.setOnClickListener(v ->
                validateAndPay());
    }

    private void verifyMpinAndProceed(String enteredMpin) {

        FirebaseUser currentUser =
                FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this,
                    "User not logged in",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference ref =
                FirebaseDatabase.getInstance()
                        .getReference("wallets")
                        .child(currentUser.getUid())
                        .child("mpinHash");

        ref.addListenerForSingleValueEvent(
                new ValueEventListener() {

                    @Override
                    public void onDataChange(
                            @NonNull DataSnapshot snapshot) {

                        String savedHash =
                                snapshot.getValue(String.class);

                        if (savedHash == null ||
                                savedHash.isEmpty()) {

                            Toast.makeText(
                                    MakePaymentActivity.this,
                                    "MPIN not set",
                                    Toast.LENGTH_SHORT
                            ).show();
                            return;
                        }

                        try {

                            boolean valid = BCrypt.checkpw(
                                    enteredMpin,
                                    savedHash
                            );

                            if (!valid) {

                                etEnterMpin.setError("Incorrect MPIN");
                                etEnterMpin.requestFocus();

                                return;
                            }

                            // MPIN correct
                            proceedWithPayment(enteredMpin);

                        } catch (Exception e) {

                            Toast.makeText(
                                    MakePaymentActivity.this,
                                    "Invalid MPIN data",
                                    Toast.LENGTH_SHORT
                            ).show();

                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onCancelled(
                            @NonNull DatabaseError error) {

                        Toast.makeText(
                                MakePaymentActivity.this,
                                "Failed to verify MPIN",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }


    private void proceedWithPayment(String mpin) {

        FirebaseUser currentUser =
                FirebaseAuth.getInstance()
                        .getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this,
                    "User not logged in",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference myWalletRef =
                FirebaseDatabase.getInstance()
                        .getReference("wallets")
                        .child(currentUser.getUid());

        myWalletRef.child("walletId")
                .get()
                .addOnSuccessListener(snapshot -> {

                    String myWalletId =
                            snapshot.getValue(String.class);

                    if (myWalletId != null &&
                            myWalletId.equals(dpay_id)) {

                        Toast.makeText(
                                this,
                                "Cannot send money to yourself",
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }

                    currentClientTxnId =
                            UUID.randomUUID().toString();

                    sendPaymentToServer(
                            mpin,
                            currentClientTxnId
                    );
                });
    }

    private void validateAndPay() {

        if (isRequestRunning) {
            return;
        }

        if (!isActivitySafe()) {
            return;
        }

        String mpin =
                etEnterMpin.getText()
                        .toString()
                        .trim();

        if (mpin.isEmpty()) {

            etEnterMpin.setError("Enter MPIN");

            etEnterMpin.requestFocus();

            return;
        }

        if (mpin.length() != 4) {

            etEnterMpin.setError("MPIN must be 4 digits");

            etEnterMpin.requestFocus();

            return;
        }

        if (amount <= 0) {

            Toast.makeText(
                    this,
                    "Invalid amount",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (amount > currentBalance) {

            Toast.makeText(
                    this,
                    "Insufficient balance",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        verifyMpinAndProceed(mpin);
    }

    private void sendPaymentToServer(
            String mpin,
            String clientTxnId
    ) {
        sendPaymentRequest(
                RAILWAY_URL,
                mpin,
                clientTxnId,
                true
        );
    }


    private void sendPaymentRequest(
            String url,
            String mpin,
            String clientTxnId,
            boolean allowFallback
    ) {


        if (isRequestRunning) {
            return;
        }

        isRequestRunning = true;

        setLoadingState(true);

        FirebaseUser user =
                FirebaseAuth.getInstance()
                        .getCurrentUser();

        if (user == null) {

            resetPaymentState();

            Toast.makeText(
                    this,
                    "User not logged in",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        user.getIdToken(true)
                .addOnCompleteListener(task -> {

                    if (!isActivitySafe()) {
                        return;
                    }

                    if (!task.isSuccessful()) {

                        resetPaymentState();

                        Toast.makeText(
                                this,
                                "Authentication failed",
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }

                    try {

                        String idToken =
                                task.getResult().getToken();

                        JSONObject body =
                                new JSONObject();

                        body.put("idToken", idToken);

                        body.put("walletId", dpayId.getText().toString());

                        body.put("type", type);

                        body.put("mpin", mpin);

                        body.put("amount", String.valueOf(amount));

                        body.put("purpose", payment_purpose);

                        body.put("remarks", rmks);

                        body.put(
                                "clientTxnId",
                                clientTxnId
                        );

                        if ("merchant_payment".equals(type)) {
                            body.put("orderId", orderId);
                            body.put("orderNumber", orderNumber);
                        }

                        JsonObjectRequest request =
                                new JsonObjectRequest(
                                        Request.Method.POST,
                                        url,
                                        body,

                                        response -> {
                                            handleSuccessResponse(response);
                                        },

                                        error -> {

                                            // Railway failed -> switch to Render
                                            if (allowFallback &&
                                                    url.contains("railway.app")) {

                                                isRequestRunning = false;

                                                Toast.makeText(
                                                        MakePaymentActivity.this,
                                                        "Railway unavailable. Switching server...",
                                                        Toast.LENGTH_SHORT
                                                ).show();

                                                sendPaymentRequest(
                                                        "https://wallet-pay.onrender.com/pay",
                                                        mpin,
                                                        clientTxnId,
                                                        false
                                                );

                                                return;
                                            }

                                            handleErrorResponse(
                                                    error,
                                                    mpin,
                                                    clientTxnId
                                            );
                                        }
                                ) {

                                    @Override
                                    public Map<String, String> getHeaders() {

                                        Map<String, String> headers =
                                                new HashMap<>();

                                        headers.put(
                                                "Content-Type",
                                                "application/json"
                                        );

                                        return headers;
                                    }
                                };

                        request.setRetryPolicy(
                                new DefaultRetryPolicy(
                                        25000,
                                        0,
                                        1f
                                )
                        );

                        request.setTag(REQUEST_TAG);

                        requestQueue.add(request);

                    } catch (Exception e) {

                        e.printStackTrace();

                        resetPaymentState();

                        Toast.makeText(
                                this,
                                "Failed to create request",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });

    }

    private void handleSuccessResponse(JSONObject response) {

        if (!isActivitySafe()) {
            return;
        }

        retryCount = 0;

        resetPaymentState();

        try {

            String status =
                    response.optString("status");

            if ("SUCCESS".equals(status)) {
                saveRecentTransfer();

                String transactionId =
                        response.optString("transactionId");

                Toast.makeText(
                        this,
                        "Payment Successful",
                        Toast.LENGTH_SHORT
                ).show();

                retryHandler.removeCallbacksAndMessages(null);

                closeAllDialogs();

                etEnterMpin.setText("");

                Intent intent =
                        new Intent(
                                MakePaymentActivity.this,
                                TransactionSuccessfulActivity.class
                        );

                intent.putExtra("status", status);

                intent.putExtra(
                        "transactionId",
                        transactionId
                );

                intent.putExtra("amount", amount);

                intent.putExtra(
                        "receiverName",
                        userName
                );

                startActivity(intent);

                finish();

            } else {

                String error =
                        response.optString(
                                "error",
                                "Payment failed"
                        );

                Toast.makeText(
                        this,
                        error,
                        Toast.LENGTH_LONG
                ).show();
            }

        } catch (Exception e) {

            e.printStackTrace();

            Toast.makeText(
                    this,
                    "Something went wrong",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void handleErrorResponse(
            com.android.volley.VolleyError error,
            String mpin,
            String clientTxnId
    ) {

        if (!isActivitySafe()) {
            return;
        }

        if (error instanceof TimeoutError ||
                error instanceof NoConnectionError ||
                error instanceof NetworkError) {

            if (retryCount < MAX_RETRIES) {

                retryCount++;

                isRequestRunning = false;

                Toast.makeText(
                        this,
                        "Waking secure server... (" +
                                retryCount +
                                "/" +
                                MAX_RETRIES +
                                ")",
                        Toast.LENGTH_SHORT
                ).show();

                retryHandler.postDelayed(() -> {

                    if (!isActivitySafe()) {
                        return;
                    }

                    sendPaymentToServer(
                            mpin,
                            clientTxnId
                    );

                }, 4000);

                return;
            }
        }

        retryCount = 0;

        resetPaymentState();

        String errorMessage =
                getVolleyErrorMessage(error);

        Toast.makeText(
                this,
                errorMessage,
                Toast.LENGTH_LONG
        ).show();
    }

    private String getVolleyErrorMessage(
            com.android.volley.VolleyError error
    ) {

        try {

            if (error.networkResponse != null &&
                    error.networkResponse.data != null) {

                String responseBody =
                        new String(
                                error.networkResponse.data,
                                StandardCharsets.UTF_8
                        );

                JSONObject obj =
                        new JSONObject(responseBody);

                return obj.optString(
                        "error",
                        "Payment failed"
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        if (error instanceof TimeoutError) {
            return "Server timeout";
        }

        if (error instanceof NoConnectionError) {
            return "No internet connection";
        }

        if (error instanceof ServerError) {
            return "Server error";
        }

        return "Payment failed";
    }

    private void resetPaymentState() {

        isRequestRunning = false;

        setLoadingState(false);
    }

    private void setLoadingState(boolean loading) {

        btnConfirmPaymentViaDpay.setEnabled(!loading);

        if (loading) {
            btnConfirmPaymentViaDpay.setText("Processing...");
        } else {
            btnConfirmPaymentViaDpay.setText("CONFIRM");
        }
    }

    private void fetchWalletData() {

        FirebaseUser user =
                FirebaseAuth.getInstance()
                        .getCurrentUser();

        if (user == null) {

            Toast.makeText(
                    this,
                    "User not logged in",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        DatabaseReference ref =
                FirebaseDatabase.getInstance()
                        .getReference("wallets")
                        .child(user.getUid());

        ref.addListenerForSingleValueEvent(
                new ValueEventListener() {

                    @Override
                    public void onDataChange(
                            @NonNull DataSnapshot snapshot
                    ) {

                        if (!isActivitySafe()) {
                            return;
                        }

                        if (snapshot.exists()) {

                            Long balance =
                                    snapshot.child("balance")
                                            .getValue(Long.class);

                            currentBalance =
                                    balance != null ? balance : 0;

                        } else {

                            currentBalance = 0;
                        }

                        updateAmountUI();
                    }

                    @Override
                    public void onCancelled(
                            @NonNull DatabaseError error
                    ) {

                        if (!isActivitySafe()) {
                            return;
                        }

                        Toast.makeText(
                                MakePaymentActivity.this,
                                "Failed to load wallet",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );
    }

    private void updateAmountUI() {

        if (!isActivitySafe()) {
            return;
        }

        walletBalance.animate()
                .alpha(0f)
                .setDuration(120)
                .withEndAction(() -> {

                    if (!isActivitySafe()) {
                        return;
                    }

                    if (isAmountVisible) {

                        walletBalance.setText(
                                formatAmount(currentBalance)
                        );

                    } else {

                        walletBalance.setText("XXXX.XX");
                    }

                    walletBalance.animate()
                            .alpha(1f)
                            .setDuration(120)
                            .start();
                })
                .start();
    }

    private String formatAmount(long amount) {

        NumberFormat formatter =
                NumberFormat.getInstance(
                        new Locale("en", "IN")
                );

        return formatter.format(amount);
    }

    private void selectDpayOption() {

        tvPayViaDpay.setTextColor(getColor(R.color.app_primary_dark));

        tvLinkedBankAccount.setTextColor(getColor(R.color.text_secondary));

        imgPayViaDpay.setVisibility(View.VISIBLE);

        imgLinkedBankAccount.setVisibility(View.GONE);
    }

    private void selectBankOption() {

        tvLinkedBankAccount.setTextColor(getColor(R.color.app_primary_dark));

        tvPayViaDpay.setTextColor(getColor(R.color.text_secondary));

        imgLinkedBankAccount.setVisibility(View.VISIBLE);

        imgPayViaDpay.setVisibility(View.GONE);
    }

    private void closeAllDialogs() {

        confirmPaymentContainer.setVisibility(View.GONE);

        payViaContainer.setVisibility(View.GONE);

        dimBackground.setVisibility(View.GONE);
    }

    private String maskName(String fullName) {

        if (fullName == null ||
                fullName.trim().isEmpty()) {

            return "Unknown User";
        }

        String[] parts =
                fullName.trim().split(" ");

        if (parts.length == 1) {

            String name = parts[0];

            if (name.length() <= 3) {
                return name;
            }

            return name.substring(0, 3) + "**";
        }

        String firstName = parts[0];

        String lastName =
                parts[parts.length - 1];

        String maskedFirst;

        if (firstName.length() <= 3) {

            maskedFirst = firstName;

        } else {

            maskedFirst =
                    firstName.substring(0, 3) + "**";
        }

        return maskedFirst + " " + lastName;
    }

    private boolean isActivitySafe() {

        return !(isFinishing() || isDestroyed());
    }

    @Override
    public void onBackPressed() {

        if (isRequestRunning) {

            Toast.makeText(
                    this,
                    "Payment is processing",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (confirmPaymentContainer.getVisibility()
                == View.VISIBLE) {

            confirmPaymentContainer
                    .setVisibility(View.GONE);

            payViaContainer
                    .setVisibility(View.VISIBLE);

            return;
        }

        if (payViaContainer.getVisibility()
                == View.VISIBLE) {

            payViaContainer
                    .setVisibility(View.GONE);

            dimBackground
                    .setVisibility(View.GONE);

            return;
        }

        super.onBackPressed();
    }

    private void saveRecentTransfer() {

        if (!"user".equals(type)) {
            return; // only save users, not merchants
        }

        FirebaseUser currentUser =
                FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) return;

        String uid = currentUser.getUid();

        String key = FirebaseDatabase.getInstance()
                .getReference("recentTransfers")
                .child(uid)
                .push()
                .getKey();

        if (key == null) return;

        Map<String, Object> map = new HashMap<>();

        map.put("name", userName);
        map.put("amount", amount);
        map.put("timestamp", System.currentTimeMillis());

        // Save wallet id
        map.put("number", dpay_id);

        FirebaseDatabase.getInstance()
                .getReference("recentTransfers")
                .child(uid)
                .child(key)
                .setValue(map);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (requestQueue != null) {
            requestQueue.cancelAll(REQUEST_TAG);
        }

        retryHandler.removeCallbacksAndMessages(null);
    }
}
