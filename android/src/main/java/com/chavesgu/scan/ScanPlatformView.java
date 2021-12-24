package com.chavesgu.scan;

import android.app.Activity;
import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;

import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.platform.PlatformView;

public class ScanPlatformView implements PlatformView, MethodChannel.MethodCallHandler, ScanViewNew.CaptureListener {
    private final MethodChannel channel;
    private final Context context;
    private final Activity activity;
    private final ActivityPluginBinding activityPluginBinding;
    private ParentView parentView;
    private ScanViewNew scanViewNew;
    private ScanDrawView scanDrawView;
    private boolean flashlight;

    ScanPlatformView(@NonNull BinaryMessenger messenger, @NonNull Context context, @NonNull Activity activity, ActivityPluginBinding activityPluginBinding, int viewId, @Nullable Map<String, Object> args) {
        channel = new MethodChannel(messenger, "chavesgu/scan/method_"+viewId);
        channel.setMethodCallHandler(this);
        this.context = context;
        this.activity = activity;
        this.activityPluginBinding = activityPluginBinding;
        initForBinding(args);
    }

    private void initForBinding(Map<String, Object> args) {
        this.scanViewNew = new ScanViewNew(context, activity, activityPluginBinding,  args);
        this.scanViewNew.setCaptureListener(this);

        this.scanDrawView = new ScanDrawView(context, activity, args);

        this.parentView = new ParentView(context);
        this.parentView.addView(this.scanViewNew);
        this.parentView.addView(this.scanDrawView);
    }

    @Override
    public View getView() {
        return this.parentView;
    }

    @Override
    public void dispose() {
        this.scanViewNew.dispose();
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull MethodChannel.Result result) {
        switch (call.method) {
            case "resume":
                resume();
                break;
            case "pause":
                pause();
                break;
            case "toggleTorchMode":
                toggleTorchMode((Boolean) call.arguments);
                break;
        }
    }

    private void resume() {
        this.scanViewNew.resume();
        this.scanDrawView.resume();
    }
    private void pause() {
        this.scanViewNew.pause();
        this.scanDrawView.pause();
    }
    private void toggleTorchMode(@Nullable Boolean state) {
        boolean to = !flashlight;
        if (state != null) {
            to = state;
        }
        this.scanViewNew.toggleTorchMode(to);
        flashlight = to;
    }

    @Override
    public void onCapture(String text) {
        channel.invokeMethod("onCaptured", text);
        pause();
    }

    @Override
    public void onBrightnessChange(Double val) {
        channel.invokeMethod("onBrightnessChange", val);
    }
}
