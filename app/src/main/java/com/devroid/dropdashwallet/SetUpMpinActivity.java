package com.devroid.dropdashwallet;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.MotionEvent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import org.json.JSONObject;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SetUpMpinActivity extends AppCompatActivity {

    TextInputEditText etMpin, etConfirmMpin;
    MaterialButton btnSetMpin;

    FirebaseAuth mAuth;
    DatabaseReference dbRef;
    String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_up_mpin);

        etMpin = findViewById(R.id.etMpin);
        etConfirmMpin = findViewById(R.id.etConfirmMpin);
        btnSetMpin = findViewById(R.id.btnSetMpin);

        // Setup eye toggle for both fields
        setupPasswordToggle(etMpin);
        setupPasswordToggle(etConfirmMpin);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        token = getIntent().getStringExtra("token");

        if (user == null) {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        dbRef = FirebaseDatabase.getInstance().getReference("wallets").child(user.getUid());

        btnSetMpin.setOnClickListener(v -> {
            String mpin = etMpin.getText() == null ? "" : etMpin.getText().toString().trim();
            String confirm = etConfirmMpin.getText() == null ? "" : etConfirmMpin.getText().toString().trim();

            if (mpin.isEmpty() || confirm.isEmpty()) {
                Toast.makeText(this, "Enter MPIN", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!mpin.equals(confirm)) {
                Toast.makeText(this, "MPINs do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            saveMpin(mpin);
        });

        requestNotificationPermission();

    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(
                    new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                    1001
            );
        }
    }

    /**
     * Setup toggle logic for password/MPIN visibility (works for both numeric and alphanumeric)
     */
    private void setupPasswordToggle(TextInputEditText editText) {
        // Default hidden
        editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
        editText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.otp, 0, R.drawable.eye_closed, 0);

        editText.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                int drawableRight = 2; // Right drawable
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

                        // Keep cursor at the end
                        editText.setSelection(editText.getText().length());
                        return true;
                    }
                }
            }
            return false;
        });
    }

    private void saveMpin(String mpin) {
        // Hash MPIN securely using bcrypt
        String hashedMpin = BCrypt.hashpw(mpin, BCrypt.gensalt());

        dbRef.child("mpinHash").setValue(hashedMpin)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "MPIN set successfully!", Toast.LENGTH_SHORT).show();

                        if (token != null) {
                            sendLoginNotificationToBackend(token);
                        }

                        goToDashboard();
                    } else {
                        Toast.makeText(this, "Failed to set MPIN", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sendLoginNotificationToBackend(String fcmToken) {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        user.getIdToken(true).addOnSuccessListener(result -> {
            try {
                JSONObject json = new JSONObject();
                json.put("fcmToken", fcmToken);
                json.put("idToken", result.getToken());

                RequestBody body = RequestBody.create(
                        json.toString(),
                        MediaType.get("application/json")
                );

                Request request = new Request.Builder()
                        .url("https://notification-push-1.onrender.com/wallet-login-success")
                        .post(body)
                        .build();

                new OkHttpClient().newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) {
                    }
                });

            } catch (Exception ignored) {
            }
        });
    }

    private void goToDashboard() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}