package com.devroid.dropdashwallet;

import android.os.Bundle;
import android.widget.TextView;

public class PaymentNotificationActivity extends BaseActivity {
    TextView paymentNotificationText, paymentTransactionId;
    String amount, senderName, receiverName, transactionType, transactionId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_notification);

        paymentNotificationText = findViewById(R.id.paymentNotificationText);
        paymentTransactionId = findViewById(R.id.paymentTransactionId);

        amount = getIntent().getStringExtra("amount");
        senderName = getIntent().getStringExtra("senderName");
        receiverName = getIntent().getStringExtra("receiverName");
        transactionType = getIntent().getStringExtra("transactionType");
        transactionId = getIntent().getStringExtra("transactionId");


        if ("sent".equals(transactionType)) {
            paymentNotificationText.setText(
                    "Payment Successful!\n\n" +
                            "You sent NPR " + amount + " to " + receiverName + ".\n" +
                            "Thank you for using dPay Wallet."
            );

        } else if ("received".equals(transactionType)) {
            paymentNotificationText.setText(
                    "Payment Received!\n\n" +
                            "NPR " + amount + " received from " + senderName + ".\n" +
                            "Amount has been added to your wallet."
            );
        }

        paymentTransactionId.setText(transactionId);

    }
}