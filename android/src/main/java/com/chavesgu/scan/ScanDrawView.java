package com.chavesgu.scan;

import static java.lang.Math.floor;
import static java.lang.Math.max;
import static java.lang.Math.min;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Region;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.Nullable;

import java.util.Map;

@SuppressLint("ViewConstructor")
public class ScanDrawView extends SurfaceView implements SurfaceHolder.Callback {
    private final String LOG_TAG = "scan";
    private final Activity activity;
    private double vw;
    private double vh;
    private double areaX;
    private double areaY;
    private double areaWidth;
    private final int scanLineColor;
    private boolean transparentScanLine = false;
    private final double scale;
    private float dpi;
    private boolean running;

    private ValueAnimator positionAnimator;
    private float scanLinePositionValue;

    public ScanDrawView(Context context, Activity activity, @Nullable Map<String, Object> args) {
        super(context);

        assert args != null;
        scale = (double) args.get("scale");
        final int r = (int) args.get("r");
        final int g = (int) args.get("g");
        final int b = (int) args.get("b");
        final double alpha = (double) args.get("a");
        final int a = max(0, min(255, (int) floor(alpha * 256.0)));
        if (a == 0) transparentScanLine = true;
        scanLineColor = Color.argb(a, r, g, b);
        this.activity = activity;

        getHolder().addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        setWillNotDraw(false);
        setZOrderOnTop(true);
        surfaceHolder.setFormat(PixelFormat.TRANSPARENT);
        running = true;
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int width, int height) {
        vw = width;
        vh = height;
        areaWidth = min(vw, vh) * scale;
        areaX = (vw - areaWidth) / 2;
        areaY = (vh - areaWidth) / 2;

        DisplayMetrics dm = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        dpi = dm.density;

        // init animate
        final float scanLineWidth = (float) (areaWidth * 0.8);
        final long duration = (long) (areaWidth / 175 / dpi * 1.5 * 1000);
        positionAnimator = ValueAnimator.ofFloat(0, scanLineWidth);
        positionAnimator.setDuration(duration);
        positionAnimator.setInterpolator(null);
        positionAnimator.setRepeatMode(ValueAnimator.RESTART);
        positionAnimator.setRepeatCount(ValueAnimator.INFINITE);
        positionAnimator.addUpdateListener(valueAnimator -> {
            scanLinePositionValue = (float) valueAnimator.getAnimatedValue();
            Log.i(LOG_TAG, "scanLinePositionValue:" + scanLinePositionValue);
            invalidate();
        });
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        if (positionAnimator != null) {
            positionAnimator.removeAllUpdateListeners();
            positionAnimator = null;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (positionAnimator != null && !positionAnimator.isStarted()) positionAnimator.start();
        drawing(canvas);
    }

    private void drawing(Canvas canvas) {
        final float x = (float) areaX;
        final float y = (float) areaY;
        final float width = (float) areaWidth;
        final float shortWidth = (float) (areaWidth * 0.1);
        final float scanLineWidth = (float) (areaWidth * 0.8);
        final float scanLineX = (float) (vw - scanLineWidth) / 2;
        final float scanLineY = (float) (vh - scanLineWidth) / 2;

        if (scale < 1) {
            Paint paint = new Paint();
            paint.setColor(scanLineColor);
            paint.setStrokeWidth(2 * dpi);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawLine(x, y, x + shortWidth, y, paint);
            canvas.drawLine(x, y, x, y + shortWidth, paint);

            canvas.drawLine(x + width, y, x + width - shortWidth, y, paint);
            canvas.drawLine(x + width, y, x + width, y + shortWidth, paint);

            canvas.drawLine(x + width, y + width, x + width - shortWidth, y + width, paint);
            canvas.drawLine(x + width, y + width, x + width, y + width - shortWidth, paint);

            canvas.drawLine(x, y + width, x + shortWidth, y + width, paint);
            canvas.drawLine(x, y + width, x, y + width - shortWidth, paint);

            // mask
            canvas.save();
            Path clipPath = new Path();
            clipPath.addRect(x - 2, y - 2, (float) (x + areaWidth + 2), (float) (y + areaWidth + 2), Path.Direction.CCW);
            canvas.clipPath(clipPath, Region.Op.DIFFERENCE);

            Paint maskPaint = new Paint();
            final int a = max(0, min(255, (int) floor(0.5 * 256.0)));
            maskPaint.setColor(Color.argb(a, 0, 0, 0));
            maskPaint.setStyle(Paint.Style.FILL);

            canvas.drawRect(0, 0, (float) vw, (float) vh, maskPaint);
            canvas.restore();
        }

        if (running && !transparentScanLine) {
            Paint scanPaint = new Paint();
            scanPaint.setColor(scanLineColor);
            scanPaint.setStrokeWidth(2 * dpi);
            scanPaint.setStrokeCap(Paint.Cap.ROUND);
            scanPaint.setStrokeJoin(Paint.Join.ROUND);
            scanPaint.setStyle(Paint.Style.STROKE);

            if (scanLinePositionValue / scanLineWidth < (float) (2.0 / 3.0)) {
                scanPaint.setAlpha(255);
            } else {
                final float a = 1 - (scanLinePositionValue / scanLineWidth - (float) (2.0 / 3.0)) * 3;
                final int alpha = max(0, min(255, (int) floor(a * 256.0)));
                scanPaint.setAlpha(alpha);
            }

            Path scanPath = new Path();
            scanPath.moveTo(scanLineX, scanLineY + scanLinePositionValue);
            scanPath.lineTo(scanLineX + scanLineWidth, scanLineY + scanLinePositionValue);
            scanPath.close();

            canvas.drawPath(scanPath, scanPaint);
        }
//        invalidate();
    }

    public void resume() {
        running = true;
        if (positionAnimator != null) positionAnimator.resume();
        invalidate();
    }

    public void pause() {
        running = false;
        if (positionAnimator != null) positionAnimator.pause();
        invalidate();
    }
}
