package com.devroid.dropdashwallet;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

public class MyQrBottomSheet extends BottomSheetDialogFragment {

    private String dpayId;
    private String userName;

    public MyQrBottomSheet() {
    }

    public static MyQrBottomSheet newInstance(String dpayId, String userName) {

        MyQrBottomSheet sheet = new MyQrBottomSheet();

        Bundle args = new Bundle();

        args.putString("dPay_id", dpayId);
        args.putString("name", userName);

        sheet.setArguments(args);

        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.bottom_sheet_my_qr, container, false);

        LinearLayout downloadContainer = view.findViewById(R.id.downloadContainer);
        ImageView qrImage = view.findViewById(R.id.qrImage);

        downloadContainer.setOnClickListener(v -> {

            saveImageToGallery(qrImage);
        });

        TextView qrUserName = view.findViewById(R.id.qrUserName);

        TextView qrDpayId = view.findViewById(R.id.qrDpayId);

        if (getArguments() != null) {

            dpayId = getArguments().getString("dPay_id");

            userName = getArguments().getString("name");

            qrUserName.setText(userName);

            qrDpayId.setText(dpayId);

            String qrData =
                    "{"
                            + "\"dPay_id\":\"" + dpayId + "\","
                            + "\"name\":\"" + userName + "\""
                            + "}";

            generateQrCode(qrImage, qrData);
        }

        return view;
    }

    private void saveImageToGallery(ImageView imageView) {

        imageView.setDrawingCacheEnabled(true);
        Bitmap bitmap = imageView.getDrawingCache();

        try {

            String filename = "dPay_QR_" + System.currentTimeMillis() + ".png";

            android.content.ContentValues values = new android.content.ContentValues();

            values.put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, filename);
            values.put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png");

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {

                values.put(android.provider.MediaStore.Images.Media.RELATIVE_PATH,
                        "Pictures/dPayQR");

            }

            android.content.ContentResolver resolver =
                    requireContext().getContentResolver();

            android.net.Uri uri = resolver.insert(
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    values
            );

            if (uri != null) {

                java.io.OutputStream out = resolver.openOutputStream(uri);

                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);

                if (out != null) {
                    out.close();
                }

                Toast.makeText(requireContext(),
                        "QR Saved to Gallery",
                        Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            e.printStackTrace();

            Toast.makeText(requireContext(),
                    "Failed to save QR",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void generateQrCode(
            ImageView imageView,
            String text
    ) {

        try {

            BitMatrix matrix =
                    new MultiFormatWriter().encode(
                            text,
                            BarcodeFormat.QR_CODE,
                            800,
                            800
                    );

            int width = matrix.getWidth();

            int height = matrix.getHeight();

            Bitmap bitmap = Bitmap.createBitmap(
                    width,
                    height,
                    Bitmap.Config.RGB_565
            );

            for (int x = 0; x < width; x++) {

                for (int y = 0; y < height; y++) {

                    bitmap.setPixel(
                            x,
                            y,
                            matrix.get(x, y)
                                    ? Color.BLACK
                                    : Color.WHITE
                    );
                }
            }

            imageView.setImageBitmap(bitmap);

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    @Override
    public void onStart() {

        super.onStart();

        View bottomSheet = getDialog().findViewById(com.google.android.material.R.id.design_bottom_sheet);

        if (bottomSheet != null) {

            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);

            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);

            behavior.setPeekHeight(500);

            behavior.setHideable(true);

            behavior.setDraggable(true);

            behavior.setSkipCollapsed(false);
        }
    }
}