package com.chavesgu.scan;

import static android.content.Context.VIBRATOR_SERVICE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static java.lang.Math.min;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.journeyapps.barcodescanner.BarcodeView;
import com.journeyapps.barcodescanner.DefaultDecoderFactory;
import com.journeyapps.barcodescanner.Size;

import java.util.Map;

import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.PluginRegistry;

@SuppressLint("ViewConstructor")
public class ScanViewNew extends BarcodeView implements PluginRegistry.RequestPermissionsResultListener, SensorEventListener {
    public interface CaptureListener {
        void onCapture(String text);

        void onBrightnessChange(Double val);
    }

    private CaptureListener captureListener;

    private final String LOG_TAG = "QRSCAN";
    private final int CAMERA_REQUEST_CODE = 6537;
    private final Context context;
    private final Activity activity;
    private double scale = .7;
    private SensorManager sensorManager = null;

    public ScanViewNew(Context context, Activity activity, @NonNull ActivityPluginBinding activityPluginBinding, @Nullable Map<String, Object> args) {
        super(context, null);

        this.context = context;
        this.activity = activity;
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        activityPluginBinding.addRequestPermissionsResultListener(this);
        if (args != null) {
            Object s = args.get("scale");
            if (s != null) {
                this.scale = (double) s;
            }
        }

        checkPermission();
    }

    private void start() {
        addListenLifecycle();
        this.setDecoderFactory(new DefaultDecoderFactory(QRCodeDecoder.allFormats, QRCodeDecoder.HINTS, "utf-8", 2));
        this.decodeContinuous(result -> {
            Log.i(LOG_TAG, "get result: " + result.getText());
            captureListener.onCapture(result.getText());
            Vibrator myVib = (Vibrator) context.getSystemService(VIBRATOR_SERVICE);
            if (myVib != null) {
                if (Build.VERSION.SDK_INT >= 26) {
                    myVib.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    myVib.vibrate(50);
                }
            }
        });

        _resume();
        Log.i(LOG_TAG, "start, camera is open? " + this.getCameraInstance().isOpen());
    }

    private void checkPermission() {
        if (hasPermission()) {
            start();
        } else {
            String[] permissions = new String[1];
            permissions[0] = Manifest.permission.CAMERA;
            ActivityCompat.requestPermissions(activity, permissions, CAMERA_REQUEST_CODE);
        }
    }

    private boolean hasPermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                activity.checkSelfPermission(Manifest.permission.CAMERA) == PERMISSION_GRANTED;
    }

    private void addListenLifecycle() {
        // activity.getApplication().registerActivityLifecycleCallbacks(lifecycleCallback);
        // 开启光线传感器
        this.sensorManager = (SensorManager) this.context.getSystemService(Context.SENSOR_SERVICE);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            double brightness = (double) event.values[0] - 300;
            Log.i(LOG_TAG, "光线强度: " + brightness);
            captureListener.onBrightnessChange(brightness);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void _resume() {
        if (this.sensorManager != null) {
            Sensor sensor = this.sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
            if (sensor != null) {
                this.sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST);
            }
        }
        this.resume();
    }

    public void _pause() {
        if (this.sensorManager != null) {
            this.sensorManager.unregisterListener(this);
        }
        this.pause();
    }

    public void toggleTorchMode(boolean mode) {
        this.setTorch(mode);
    }

    public void setCaptureListener(CaptureListener captureListener) {
        this.captureListener = captureListener;
    }

    public void dispose() {
        _pause();
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        double vw = getWidth();
        double vh = getHeight();
        if (scale < 1.0) {
            int s = (int) (min(vw, vh) * scale);
            this.setFramingRectSize(new Size(s, s));
        } else {
            this.setFramingRectSize(new Size((int) vw, (int) vh));
        }
    }

    @Override
    public boolean onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == CAMERA_REQUEST_CODE && grantResults[0] == PERMISSION_GRANTED) {
            start();
            Log.i(LOG_TAG, "onRequestPermissionsResult: true");
            return true;
        }
        Log.i(LOG_TAG, "onRequestPermissionsResult: false");
        return false;
    }
}
