package com.devroid.dropdashwallet;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.util.Size;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.mlkit.vision.barcode.*;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import org.json.JSONObject;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QrFragment extends Fragment {

    private PreviewView previewView;
    private boolean isScanned = false;
    private Camera camera;
    String dpay_id, userName;
    boolean isWalletLoaded = false;
    private ProcessCameraProvider cameraProvider;
    private boolean cameraStarted = false;

    public QrFragment() {
        super(R.layout.fragment_qr);
    }

    private androidx.activity.result.ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
        if (uri != null) {
            scanImageFromGallery(uri);
        }
    });

    private final ExecutorService cameraExecutor =
            Executors.newSingleThreadExecutor();

    private final androidx.activity.result.ActivityResultLauncher<String> requestGalleryPermission = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
        if (granted) {
            pickImageLauncher.launch("image/*");
        } else {
            Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show();
        }
    });


    // 📷 Permission
    private final androidx.activity.result.ActivityResultLauncher<String> requestPermission = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
        if (granted) startCamera();
    });

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        previewView = view.findViewById(R.id.previewView);

        // 🔦 Flash button
        ImageView flashBtn = view.findViewById(R.id.flashBtn);
        if (flashBtn != null) {
            flashBtn.setOnClickListener(v -> toggleFlash(flashBtn));
        }

        // Gallery Button
        ImageView galleryBtn = view.findViewById(R.id.galleryBtn);


        LinearLayout myQrCodeContainer = view.findViewById(R.id.myQrCodeContainer);

        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) scanImageFromGallery(uri);
        });
        if (galleryBtn != null) {
            galleryBtn.setOnClickListener(v -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // No permission needed
                    pickImageLauncher.launch("image/*");
                } else {
                    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        pickImageLauncher.launch("image/*");
                    } else {
                        requestGalleryPermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                    }
                }
            });
        }

        // 🎯 Tap to focus
        previewView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                focusCamera(event.getX(), event.getY());
            }
            return true;
        });

        // 🔴 Start scan line animation
        View scanLine = view.findViewById(R.id.scanLine);
        if (scanLine != null) {
            startScanAnimation(scanLine);
        }

        // Camera permission
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestPermission.launch(Manifest.permission.CAMERA);
        }

        animateCorners(view.findViewById(R.id.topLeft));
        animateCorners(view.findViewById(R.id.topRight));
        animateCorners(view.findViewById(R.id.bottomLeft));
        animateCorners(view.findViewById(R.id.bottomRight));

        Toast.makeText(requireContext(), "Wallet loading, please wait...", Toast.LENGTH_SHORT).show();
        fetchWalletData();

        myQrCodeContainer.setOnClickListener(v -> {

            if (!isWalletLoaded || dpay_id == null || userName == null) {
                Toast.makeText(requireContext(), "Loading wallet...", Toast.LENGTH_SHORT).show();
                return;
            }

            MyQrBottomSheet sheet = MyQrBottomSheet.newInstance(dpay_id, userName);

            sheet.show(getParentFragmentManager(), "MyQrBottomSheet");
        });

    }

    private void fetchWalletData() {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = user.getUid();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("wallets").child(uid);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (snapshot.exists()) {

                    dpay_id = snapshot.child("walletId").getValue(String.class);
                    userName = snapshot.child("name").getValue(String.class);

                }

                isWalletLoaded = true;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load wallet", Toast.LENGTH_SHORT).show();
            }
        });
    }


    // 🚀 Camera setup
    @OptIn(markerClass = ExperimentalGetImage.class)
    private void startCamera() {

        if (cameraStarted) return;

        cameraStarted = true;

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {

            if (!isFragmentActive()) return;

            try {

                cameraProvider = cameraProviderFuture.get();

                cameraProvider.unbindAll();

                Preview preview = new Preview.Builder().build();

                preview.setSurfaceProvider(
                        previewView.getSurfaceProvider()
                );

                ImageAnalysis analysis = new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(1280, 720))
                        .setBackpressureStrategy(
                                ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                        )
                        .build();

                BarcodeScannerOptions options =
                        new BarcodeScannerOptions.Builder()
                                .setBarcodeFormats(
                                        Barcode.FORMAT_QR_CODE
                                )
                                .build();

                BarcodeScanner scanner =
                        BarcodeScanning.getClient(options);

                analysis.setAnalyzer(cameraExecutor, imageProxy -> {

                    if (!isFragmentActive() || isScanned) {
                        imageProxy.close();
                        return;
                    }

                    if (imageProxy.getImage() == null) {
                        imageProxy.close();
                        return;
                    }

                    try {

                        InputImage image =
                                InputImage.fromMediaImage(
                                        imageProxy.getImage(),
                                        imageProxy.getImageInfo()
                                                .getRotationDegrees()
                                );

                        scanner.process(image)
                                .addOnSuccessListener(barcodes -> {

                                    if (!isFragmentActive()) return;

                                    for (Barcode barcode : barcodes) {

                                        String value =
                                                barcode.getRawValue();

                                        if (value != null && !isScanned) {

                                            isScanned = true;

                                            handleScannedResult(value);
                                            break;
                                        }
                                    }
                                })
                                .addOnCompleteListener(
                                        task -> imageProxy.close()
                                );

                    } catch (Exception e) {

                        imageProxy.close();

                        Log.e("QR_CAMERA",
                                "Analyzer Error", e);
                    }
                });

                camera = cameraProvider.bindToLifecycle(
                        getViewLifecycleOwner(),
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis
                );

                camera.getCameraControl().setLinearZoom(0.25f);

            } catch (Exception e) {

                cameraStarted = false;

                Log.e("QR_CAMERA",
                        "Camera init failed", e);
            }

        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private boolean isFragmentActive() {
        return isAdded() && getActivity() != null && !isDetached();
    }


    // 📳 Vibrate on scan
    @SuppressLint("ObsoleteSdkInt")
    private void vibrate() {
        Vibrator vibrator = (Vibrator) requireContext().getSystemService(Context.VIBRATOR_SERVICE);

        if (vibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(200);
            }
        }
    }

    private void handleScannedResult(String value) {

        if (!isWalletLoaded || dpay_id == null) {
            Toast.makeText(requireContext(), "Wallet still loading", Toast.LENGTH_SHORT).show();

            isScanned = false;
            return;
        }

        try {

            JSONObject obj = new JSONObject(value);

            String type = obj.optString("type", "user");

            vibrate();
            playBeep();

            if ("merchant".equals(type)) {

                String merchantId = obj.optString("merchant_id", "");
                String name = obj.optString("name", "");

                Log.d("QR_SCAN", "Merchant: " + merchantId + " | " + name);

                openAddAmount("merchant", name, merchantId);

            } else if ("merchant_payment".equals(type)) {

                String qrId = obj.optString("qr_id", "");

                if (qrId.isEmpty()) {

                    Toast.makeText(requireContext(), "Invalid payment QR", Toast.LENGTH_SHORT).show();

                    isScanned = false;
                    return;
                }

                FirebaseDatabase.getInstance().getReference("PaymentRequests").child(qrId).get().addOnSuccessListener(snapshot -> {

                    if (!snapshot.exists()) {

                        Toast.makeText(requireContext(), "Payment request not found", Toast.LENGTH_SHORT).show();

                        isScanned = false;
                        return;
                    }

                    String status = snapshot.child("status").getValue(String.class);

                    if ("PAID".equals(status)) {

                        Toast.makeText(requireContext(), "Already paid", Toast.LENGTH_SHORT).show();

                        isScanned = false;
                        return;
                    }

                    Long expiry = snapshot.child("expiry").getValue(Long.class);

                    long currentTime = System.currentTimeMillis() / 1000;

                    if (expiry != null && currentTime > expiry) {

                        Toast.makeText(requireContext(), "QR Expired", Toast.LENGTH_SHORT).show();

                        isScanned = false;
                        return;
                    }

                    String merchantId = snapshot.child("merchantId").getValue(String.class);

                    String merchantName = snapshot.child("merchantName").getValue(String.class);

                    String orderId = snapshot.child("orderId").getValue(String.class);

                    String orderNumber = snapshot.child("orderNumber").getValue(String.class);

                    String remarks = snapshot.child("remarks").getValue(String.class);

                    String purpose = snapshot.child("purpose").getValue(String.class);

                    Double amountObj = snapshot.child("amount").getValue(Double.class);

                    double amount = amountObj == null ? 0 : amountObj;

                    openMakePaymentActivity("merchant_payment", merchantId, merchantName, amount, orderId, orderNumber, remarks, purpose);
                }).addOnFailureListener(e -> {

                    Toast.makeText(requireContext(), "Failed to load payment", Toast.LENGTH_SHORT).show();

                    isScanned = false;
                });
            } else {

                String scannedDpayId = obj.optString("dPay_id", "");
                String name = obj.optString("name", "");

                // Prevent self payment
                if (dpay_id != null && scannedDpayId.equalsIgnoreCase(dpay_id)) {

                    Toast.makeText(requireContext(), "Self payment not allowed", Toast.LENGTH_SHORT).show();

                    isScanned = false; // allow scanning again

                    return;
                }

                Log.d("QR_SCAN", "User: " + scannedDpayId + " | " + name);

                openAddAmount("user", name, scannedDpayId);
            }

        } catch (Exception e) {

            e.printStackTrace();

            Toast.makeText(requireContext(), "Invalid QR Code", Toast.LENGTH_SHORT).show();
        }
    }

    // 🔦 Toggle flash
    private void toggleFlash(ImageView flashBtn) {
        if (camera != null) {
            CameraControl control = camera.getCameraControl();
            CameraInfo info = camera.getCameraInfo();

            boolean isOn = info.getTorchState().getValue() != null && info.getTorchState().getValue() == TorchState.ON;

            control.enableTorch(!isOn);

            flashBtn.setImageResource(isOn ? R.drawable.flash_on : R.drawable.flash_off);
        }
    }

    // 🎯 Tap to focus
    private void focusCamera(float x, float y) {
        if (camera == null) return;

        MeteringPointFactory factory = previewView.getMeteringPointFactory();
        MeteringPoint point = factory.createPoint(x, y);

        FocusMeteringAction action = new FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF).setAutoCancelDuration(2, java.util.concurrent.TimeUnit.SECONDS).build();

        camera.getCameraControl().startFocusAndMetering(action);
    }

    // 🔴 Scan line animation
    private void startScanAnimation(View scanLine) {
        scanLine.post(() -> {
            float range = 500f; // matches box height

            ObjectAnimator animator = ObjectAnimator.ofFloat(scanLine, "translationY", -range / 2, range / 2);

            animator.setDuration(1200);
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.setRepeatMode(ValueAnimator.REVERSE);
            animator.start();
        });
    }

    // 🚀 Open AddAmountActivity
    private void openAddAmount(String type, String name, String id) {

        Intent intent = new Intent(requireContext(), AddAmountActivity.class);

        intent.putExtra("type", type);
        intent.putExtra("name", name);

        if ("merchant".equals(type)) {
            intent.putExtra("merchant_id", id);
        } else {
            intent.putExtra("dPay_id", id);
        }

        startActivity(intent);
    }

    private void openMakePaymentActivity(String type, String merchantId, String merchantName, double amount, String orderId, String orderNumber, String remarks, String purpose) {

        Intent intent = new Intent(requireContext(), MakePaymentActivity.class);

        intent.putExtra("type", type);

        intent.putExtra("merchant_id", merchantId);

        intent.putExtra("userName", merchantName);

        intent.putExtra("amount", amount);

        intent.putExtra("order_id", orderId);

        intent.putExtra("order_number", orderNumber);

        intent.putExtra("remarks", remarks);

        intent.putExtra("purpose", purpose);

        intent.putExtra("prefilled", false);

        startActivity(intent);
    }


    private void playBeep() {
        MediaPlayer mp = MediaPlayer.create(requireContext(), R.raw.beep);
        mp.start();
    }

    private boolean isInsideScanBox(Barcode barcode) {
        if (barcode.getBoundingBox() == null) return false;

        View scanBox = requireView().findViewById(R.id.scanBox);

        int[] location = new int[2];
        scanBox.getLocationOnScreen(location);

        int left = location[0];
        int top = location[1];
        int right = left + scanBox.getWidth();
        int bottom = top + scanBox.getHeight();

        Rect box = barcode.getBoundingBox();

        return box.centerX() > left && box.centerX() < right && box.centerY() > top && box.centerY() < bottom;
    }

    private void animateCorners(View view) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.3f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.3f);

        scaleX.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);

        scaleX.setRepeatMode(ValueAnimator.REVERSE);
        scaleY.setRepeatMode(ValueAnimator.REVERSE);

        scaleX.setDuration(800);
        scaleY.setDuration(800);

        scaleX.start();
        scaleY.start();
    }

    private void scanImageFromGallery(@NonNull android.net.Uri uri) {
        try {
            isScanned = false;
            InputImage image = InputImage.fromFilePath(requireContext(), uri);
            BarcodeScanner scanner = BarcodeScanning.getClient();

            scanner.process(image).addOnSuccessListener(barcodes -> {
                if (barcodes.isEmpty()) {
                    Toast.makeText(requireContext(), "No QR code found in image", Toast.LENGTH_SHORT).show();
                    return;
                }
                for (Barcode barcode : barcodes) {
                    String value = barcode.getRawValue();
                    if (value != null) {
                        Log.d("QR_RESULT", value);
                        handleScannedResult(value);
                        break;
                    }
                }
            }).addOnFailureListener(e -> {
                e.printStackTrace();
                Toast.makeText(requireContext(), "Failed to scan image", Toast.LENGTH_SHORT).show();
            });
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Failed to load image", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        isScanned = false;

        if (!cameraStarted) {
            startCamera();
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        isScanned = true;

        cameraStarted = false;

        if (cameraProvider != null) {
            cameraProvider.unbindAll();
            cameraProvider = null;
        }

        camera = null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        camera = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (!cameraExecutor.isShutdown()) {
            cameraExecutor.shutdown();
        }
    }


    @Override
    public void onPause() {
        super.onPause();
        Log.d("QR_CAMERA", "onPause");
    }


}