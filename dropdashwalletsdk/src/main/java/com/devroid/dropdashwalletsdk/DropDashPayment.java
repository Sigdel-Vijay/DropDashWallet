package com.devroid.dropdashwalletsdk;

import android.app.Activity;
import android.content.Intent;

public class DropDashPayment {


    public static Intent getPaymentIntent(Activity activity,
                                          String totalAmount,
                                          String productAmount,
                                          String chargeAmount,
                                          String deliveryAmount,
                                          String totalPayingAmount,
                                          String merchantId,
                                          String orderId) {

        Intent intent = new Intent(activity, PaymentGatewayActivity.class);

        intent.putExtra("totalAmount", totalAmount);
        intent.putExtra("productAmount", productAmount);
        intent.putExtra("chargeAmount", chargeAmount);
        intent.putExtra("deliveryAmount", deliveryAmount);
        intent.putExtra("totalPayingAmount", totalPayingAmount);
        intent.putExtra("merchantId", merchantId);
        intent.putExtra("orderId", orderId);

        return intent; // 🔥 RETURN instead of start
    }
}