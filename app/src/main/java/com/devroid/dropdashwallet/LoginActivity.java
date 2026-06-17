package com.devroid.dropdashwallet;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class LoginActivity extends BaseActivity {
    LinearLayout selectionMobileContainer, selectionEmailContainer;
    ImageView selectionMobileIcon, selectionEmailIcon;
    TextView selectionMobile, selectionEmail;
    View selectionMobileView, selectionEmailView;
    LinearLayout mobileLoginContainer, emailLoginContainer;
    TextInputEditText etMobileNumber, etMobilePassword, etEmailAddress, etEmailPassword;
    CheckBox rememberMeCheckBox;
    TextView tvForgotMpin;
    MaterialButton btnLogin;
    FirebaseAuth mAuth;

    String selection;
    String verificationId;
    PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        selectionMobileContainer = findViewById(R.id.selectionMobileContainer);
        selectionEmailContainer = findViewById(R.id.selectionEmailContainer);
        selectionMobileIcon = findViewById(R.id.selectionMobileIcon);
        selectionEmailIcon = findViewById(R.id.selectionEmailIcon);
        selectionMobile = findViewById(R.id.selectionMobile);
        selectionEmail = findViewById(R.id.selectionEmail);
        selectionMobileView = findViewById(R.id.selectionMobileView);
        selectionEmailView = findViewById(R.id.selectionEmailView);
        mobileLoginContainer = findViewById(R.id.mobileLoginContainer);
        emailLoginContainer = findViewById(R.id.emailLoginContainer);
        etMobileNumber = findViewById(R.id.etMobileNumber);
        etMobilePassword = findViewById(R.id.etMobilePassword);
        etEmailAddress = findViewById(R.id.etEmailAddress);
        etEmailPassword = findViewById(R.id.etEmailPassword);
        rememberMeCheckBox = findViewById(R.id.rememberMeCheckBox);
        tvForgotMpin = findViewById(R.id.tvForgotMpin);
        btnLogin = findViewById(R.id.btnLogin);

        
        setupPasswordToggle(etMobilePassword);
        setupPasswordToggle(etEmailPassword);


        selection = "mobile";

        selectionMobileIcon.setColorFilter(getColor(R.color.app_primary));
        selectionMobile.setTextColor(getColor(R.color.app_primary));
        selectionMobileView.setBackgroundColor(getColor(R.color.app_primary));

        selectionEmailIcon.setColorFilter(getColor(R.color.icon_tint));
        selectionEmail.setTextColor(getColor(R.color.text_primary));

        mobileLoginContainer.setVisibility(View.VISIBLE);
        emailLoginContainer.setVisibility(View.GONE);

        selectionMobileView.setVisibility(View.VISIBLE);
        selectionEmailView.setVisibility(View.GONE);

        selectionMobileContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selection = "mobile";

                selectionMobileIcon.setColorFilter(getColor(R.color.app_primary));
                selectionMobile.setTextColor(getColor(R.color.app_primary));
                selectionMobileView.setBackgroundColor(getColor(R.color.app_primary));

                selectionEmailIcon.setColorFilter(getColor(R.color.icon_tint));
                selectionEmail.setTextColor(getColor(R.color.text_primary));

                mobileLoginContainer.setVisibility(View.VISIBLE);
                emailLoginContainer.setVisibility(View.GONE);

                selectionMobileView.setVisibility(View.VISIBLE);
                selectionEmailView.setVisibility(View.GONE);
            }
        });

        selectionEmailContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selection = "email";
                selectionEmailIcon.setColorFilter(getColor(R.color.app_primary));
                selectionEmail.setTextColor(getColor(R.color.app_primary));
                selectionEmailView.setBackgroundColor(getColor(R.color.app_primary));

                selectionMobileIcon.setColorFilter(getColor(R.color.icon_tint));
                selectionMobile.setTextColor(getColor(R.color.text_primary));

                emailLoginContainer.setVisibility(View.VISIBLE);
                mobileLoginContainer.setVisibility(View.GONE);

                selectionEmailView.setVisibility(View.VISIBLE);
                selectionMobileView.setVisibility(View.GONE);
            }
        });

        mAuth = FirebaseAuth.getInstance();




        btnLogin.setOnClickListener(v -> {

            if (selection.equals("mobile")) {
                String mobile = etMobileNumber.getText().toString().trim();
                String password = etMobilePassword.getText().toString().trim();

                if (mobile.isEmpty() || mobile.length() < 10) {
                    etMobileNumber.setError("Enter valid mobile number");
                    return;
                }

                if (password.isEmpty()) {
                    etMobilePassword.setError("Enter password");
                    return;
                }

                boolean rememberMe = rememberMeCheckBox.isChecked();

                callbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                    @Override
                    public void onVerificationCompleted(PhoneAuthCredential credential) {
                        // Auto verification (rare)
                    }

                    @Override
                    public void onVerificationFailed(FirebaseException e) {
                        Toast.makeText(LoginActivity.this,
                                e.getMessage(), Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onCodeSent(String verId,
                                           PhoneAuthProvider.ForceResendingToken token) {

                        verificationId = verId;

                        Intent intent = new Intent(
                                LoginActivity.this,
                                OtpVerificationActivity.class
                        );

                        intent.putExtra("verificationId", verificationId);
                        intent.putExtra("mobile", mobile);
                        intent.putExtra("walletId", mobile);
                        intent.putExtra("password", password);
                        intent.putExtra("selection", selection);
                        intent.putExtra("rememberMe", rememberMe);

                        startActivity(intent);
                    }
                };

                // 🔥 Send Firebase OTP
                sendOtp("+977" + mobile, rememberMe);   // Nepal country code


            } else {
                String email = etEmailAddress.getText().toString().trim();
                String password = etEmailPassword.getText().toString().trim();

                if (email.isEmpty()) {
                    etEmailAddress.setError("Enter valid email address");
                    return;
                }

                if (password.isEmpty()) {
                    etEmailPassword.setError("Enter password");
                    return;
                }

                boolean rememberMe = rememberMeCheckBox.isChecked();

                // Generate 6-digit OTP
                int otp = (int) (Math.random() * 900000) + 100000;

                sendOtpEmailStatic(this, email, password, String.valueOf(otp), rememberMe);

            }
        });

    }

    private void setupPasswordToggle(TextInputEditText editText) {
        // Default = hidden
        editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
        editText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.otp, 0, R.drawable.eye_closed, 0);

        editText.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                int drawableRight = 2;
                if (editText.getCompoundDrawables()[drawableRight] != null) {
                    int drawableWidth = editText.getCompoundDrawables()[drawableRight].getBounds().width();
                    if (event.getX() >= (editText.getWidth() - editText.getPaddingEnd() - drawableWidth)) {

                        if (editText.getTransformationMethod() instanceof PasswordTransformationMethod) {
                            // Show password
                            editText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                            editText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.otp, 0, R.drawable.eye_opened, 0);
                        } else {
                            // Hide password
                            editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                            editText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.otp, 0, R.drawable.eye_closed, 0);
                        }

                        // Keep cursor at end
                        editText.setSelection(editText.getText().length());
                        return true;
                    }
                }
            }
            return false;
        });
    }

    private void sendOtp(String phoneNumber, boolean rememberMe) {

        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(mAuth)
                        .setPhoneNumber(phoneNumber)
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(this)
                        .setCallbacks(callbacks)
                        .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    public void sendOtpEmailStatic(AppCompatActivity activity, String email, String password, String otp, boolean rememberMe) {
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL("https://api.emailjs.com/api/v1.0/email/send");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                // OTP expiry time example: current time + 15 minutes
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.MINUTE, 15);
                SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                String expiryTime = sdf.format(calendar.getTime());

                String json = "{"
                        + "\"service_id\":\"service_lj7m2qa\","
                        + "\"template_id\":\"template_fs7u93e\","
                        + "\"user_id\":\"Uwbn98hDYejgt4j0l\","
                        + "\"accessToken\":\"EzO4cq_DRuSqie3hVp-zH\","
                        + "\"template_params\":{"
                        + "\"to_email\":\"" + email + "\","
                        + "\"otp\":\"" + otp + "\","
                        + "\"time\":\"" + expiryTime + "\""
                        + "}"
                        + "}";

                OutputStream os = conn.getOutputStream();
                os.write(json.getBytes());
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                InputStream is = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
                StringBuilder response = new StringBuilder();
                int ch;
                while ((ch = is.read()) != -1) response.append((char) ch);
                is.close();

                String responseBody = response.toString();

                android.util.Log.d("EmailJS", "HTTP Response Code: " + responseCode);
                android.util.Log.d("EmailJS", "Response Body: " + responseBody);

                activity.runOnUiThread(() -> {
                    if (responseCode == 200) {
                        android.widget.Toast.makeText(activity, "OTP sent successfully!", android.widget.Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(activity, OtpVerificationActivity.class);
                        intent.putExtra("otp", otp);
                        intent.putExtra("email", email);
                        intent.putExtra("walletId", email);
                        intent.putExtra("password", password);
                        intent.putExtra("selection", selection);
                        intent.putExtra("rememberMe", rememberMe);
                        activity.startActivity(intent);
                        activity.finish();
                    } else {
                        android.widget.Toast.makeText(activity, "Failed to send OTP.", android.widget.Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                android.util.Log.e("EmailJS", "Exception sending OTP: ", e);
                activity.runOnUiThread(() -> android.widget.Toast.makeText(activity, "Error sending OTP.", android.widget.Toast.LENGTH_SHORT).show());
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }
}
