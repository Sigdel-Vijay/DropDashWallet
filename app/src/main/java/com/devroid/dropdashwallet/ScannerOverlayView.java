package com.devroid.dropdashwallet;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;

public class ScannerOverlayView extends View {

    private Paint overlayPaint;
    private Paint clearPaint;
    private Paint borderPaint;

    private RectF rect;

    public ScannerOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);

        overlayPaint = new Paint();
        overlayPaint.setColor(0xAA000000);

        clearPaint = new Paint();
        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        borderPaint = new Paint();
        borderPaint.setColor(Color.GREEN);
        borderPaint.setStrokeWidth(6f);
        borderPaint.setStyle(Paint.Style.STROKE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        int boxSize = 600;
        int left = (width - boxSize) / 2;
        int top = (height - boxSize) / 2;

        rect = new RectF(left, top, left + boxSize, top + boxSize);

        // Dark background
        canvas.drawRect(0, 0, width, height, overlayPaint);

        // Transparent cut-out
        canvas.drawRoundRect(rect, 30, 30, clearPaint);

        // Border
        canvas.drawRoundRect(rect, 30, 30, borderPaint);
    }
}