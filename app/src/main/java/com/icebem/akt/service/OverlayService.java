package com.icebem.akt.service;

import android.app.Service;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Toast;

import com.icebem.akt.R;
import com.icebem.akt.app.CoreApplication;
import com.icebem.akt.model.HRViewer;
import com.icebem.akt.overlay.OverlayView;

import org.json.JSONException;

import java.io.IOException;

public class OverlayService extends Service {
    private OverlayView[] views;

    @Override
    public void onCreate() {
        super.onCreate();
        setTheme(R.style.AppTheme_Dark);
        views = new OverlayView[2];
        views[1] = new OverlayView(this, LayoutInflater.from(this).inflate(R.layout.content_overlay, null), Gravity.END | Gravity.TOP, false, null);
        views[1].getView().findViewById(R.id.action_disconnect).setOnClickListener(view -> stopSelf());
        views[1].getView().findViewById(R.id.action_collapse).setOnClickListener(view -> {
            views[1].remove();
            views[0].show();
        });
        HRViewer viewer = null;
        try {
            viewer = new HRViewer(this, (ViewGroup) views[1].getView());
        } catch (IOException | JSONException e) {
            Log.e(getClass().getSimpleName(), Log.getStackTraceString(e));
        }
        HRViewer hr = viewer;
        views[0] = new OverlayView(this, LayoutInflater.from(this).inflate(R.layout.fab_overlay, null), Gravity.CENTER_HORIZONTAL | Gravity.TOP, true, view -> {
            if (((CoreApplication) getApplication()).isGestureServiceRunning()) {
                ((CoreApplication) getApplication()).getGestureService().disableSelf();
            } else if (hr != null) {
                hr.resetTags();
                views[0].remove();
                views[1].show();
            } else
                stopSelf();
        }).show();
        Toast.makeText(this, R.string.info_overlay_connected, Toast.LENGTH_SHORT).show();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onConfigurationChanged(Configuration cfg) {
        super.onConfigurationChanged(cfg);
        for (OverlayView view : views)
            view.onConfigurationChanged(cfg);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        for (OverlayView view : views)
            view.remove();
        Toast.makeText(this, R.string.info_overlay_disconnected, Toast.LENGTH_SHORT).show();
    }
}