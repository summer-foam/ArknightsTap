package com.icebem.akt.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Intent;
import android.graphics.Path;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

import com.icebem.akt.BuildConfig;
import com.icebem.akt.R;
import com.icebem.akt.app.CoreApplication;
import com.icebem.akt.object.PreferencesManager;
import com.icebem.akt.util.RandomUtil;

import java.lang.ref.WeakReference;

public class GestureService extends AccessibilityService {
    private static final int GESTURE_DURATION = 120;
    private int time;
    private boolean timerTimeout;
    private PreferencesManager manager;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        manager = new PreferencesManager(this);
        if (!manager.dataUpdated()) {
            disableSelf();
            return;
        }
        ((CoreApplication) getApplication()).setGestureService(this);
        if (packageInstalled("com.hypergryph.arknights") && !packageInstalled("com.hypergryph.arknights.bilibili")) {
            try {
                startActivity(new Intent().setClassName("com.hypergryph.arknights", "com.u8.sdk.U8UnityContext").setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (Exception e) {
                Log.w(getClass().getSimpleName(), e);
            }
        }
        new Thread(this::performGestures, "gesture").start();
        time = manager.getTimerTime();
        if (time > 0) {
            Handler handler = new UIHandler(this);
            new Thread(() -> {
                try {
                    while (!timerTimeout && time > 0) {
                        handler.sendEmptyMessage(time);
                        Thread.sleep(60000);
                        time--;
                    }
                } catch (Exception e) {
                    Log.w(getClass().getSimpleName(), e);
                }
                timerTimeout = true;
            }, "timer").start();
        } else {
            Toast.makeText(this, R.string.info_gesture_connected, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (BuildConfig.DEBUG)
            Log.d(getClass().getSimpleName(), "onAccessibilityEvent: " + event.toString());
    }

    @Override
    public void onInterrupt() {
        if (BuildConfig.DEBUG)
            Log.d(getClass().getSimpleName(), "onInterrupt");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (timerTimeout)
            performGlobalAction(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ? AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN : AccessibilityService.GLOBAL_ACTION_HOME);
        else
            timerTimeout = true;
        Toast.makeText(this, manager.dataUpdated() ? R.string.info_gesture_disconnected : R.string.info_resolution_unsupported, Toast.LENGTH_SHORT).show();
        return super.onUnbind(intent);
    }

    private void performGestures() {
        try {
            Thread.sleep(manager.getUpdateTime());
            if (Settings.canDrawOverlays(this))
                startService(new Intent(this, OverlayService.class));
            Path path = new Path();
            while (!timerTimeout) {
                GestureDescription.Builder builder = new GestureDescription.Builder();
                path.moveTo(RandomUtil.randomP(manager.getA()), RandomUtil.randomP(manager.getB()));
                builder.addStroke(new GestureDescription.StrokeDescription(path, 0, RandomUtil.randomP(GESTURE_DURATION)));
                path.moveTo(RandomUtil.randomP(manager.getW()), RandomUtil.randomP(manager.getH()));
                builder.addStroke(new GestureDescription.StrokeDescription(path, RandomUtil.randomT(manager.getUpdateTime() / 2), RandomUtil.randomP(GESTURE_DURATION)));
                path.moveTo(RandomUtil.randomP(manager.getX()), RandomUtil.randomP(manager.getY()));
                builder.addStroke(new GestureDescription.StrokeDescription(path, RandomUtil.randomT(manager.getUpdateTime()), RandomUtil.randomP(GESTURE_DURATION)));
                dispatchGesture(builder.build(), null, null);
                Thread.sleep(RandomUtil.randomT(manager.getUpdateTime() * 2));
            }
        } catch (Exception e) {
            Log.e(getClass().getSimpleName(), Log.getStackTraceString(e));
            timerTimeout = true;
        }
        disableSelf();
    }

    private boolean packageInstalled(String packageName) {
        try {
            return getPackageManager().getPackageGids(packageName) != null;
        } catch (Exception e) {
            return false;
        }
    }

    private static final class UIHandler extends Handler {
        private final WeakReference<GestureService> ref;

        private UIHandler(GestureService service) {
            ref = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(@androidx.annotation.NonNull Message msg) {
            super.handleMessage(msg);
            GestureService service = ref.get();
            if (service != null)
                Toast.makeText(service, String.format(service.getString(R.string.info_gesture_running), msg.what), Toast.LENGTH_SHORT).show();
        }
    }
}