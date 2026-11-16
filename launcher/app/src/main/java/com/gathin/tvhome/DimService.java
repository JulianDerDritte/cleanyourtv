package com.gathin.tvhome;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

/**
 * Holds the dimming layer: one black full-screen window over every app, started only by DimSettingsActivity.
 * It draws what the stored state says, removes the layer and stops when the switch goes off, and switches off
 * when the screen goes off (standby). It is not restarted by the system (START_NOT_STICKY) and no start command or
 * bound caller can set the state: it reads the stored state only.
 */
public final class DimService extends Service {
    private static final String TAG = "TvHome.Dim";
    private static final String CHANNEL = "dimming";
    private static final int NOTIFICATION_ID = 7;

    private WindowManager wm;
    /** The one layer window, or null. */
    private View layer;
    private SharedPreferences prefs;
    private BroadcastReceiver screenOff;
    /** Kept in a field: SharedPreferences holds its listeners weakly. */
    private final SharedPreferences.OnSharedPreferenceChangeListener changes = (p, key) -> apply();

    /** True while this process runs the service; a new process starts with false. Read by the submenu. */
    private static volatile boolean running;

    static boolean isRunning() {
        return running;
    }

    /** Starts the service. Throws when Android refuses; the caller shows the reason. */
    static void start(Context context) {
        context.startForegroundService(new Intent(context, DimService.class));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Android ends the process when a foreground start is not followed by startForeground in time: the channel
        // (which the notification needs) is made, then startForeground follows at once, before the window, the
        // receiver and the store are touched.
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel(CHANNEL, getString(R.string.dim_label),
                NotificationManager.IMPORTANCE_MIN));
        Notification n = new Notification.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.ic_dim)
                .setContentTitle(getString(R.string.dim_label))
                .setOngoing(true)
                .build();
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIFICATION_ID, n);
        }
        running = true;
        wm = getSystemService(WindowManager.class);
        prefs = DimStore.prefs(this);
        prefs.registerOnSharedPreferenceChangeListener(changes);
        screenOff = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!Dim.switchesOffOn(intent.getAction())) return;
                DimStore.afterScreenOff(prefs);
                apply();
            }
        };
        registerReceiver(screenOff, new IntentFilter(Intent.ACTION_SCREEN_OFF));
        // Started while the screen is already off: the broadcast has gone by, so switch off now.
        PowerManager power = getSystemService(PowerManager.class);
        if (power != null && !power.isInteractive()) {
            DimStore.afterScreenOff(prefs);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // The state comes from the store only. A restart by the system (intent == null) checks it the same way.
        apply();
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        running = false;
        prefs.unregisterOnSharedPreferenceChangeListener(changes);
        unregisterReceiver(screenOff);
        removeLayer();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /** Draws or removes the layer for the stored state; stops the service when the switch is off. */
    private void apply() {
        boolean on = DimStore.isOn(prefs, OpenOnStart.boot(this), DimStore.runNow());
        Dim.Decision decision = Dim.decide(on, DimStore.value(prefs), Settings.canDrawOverlays(this));
        switch (decision) {
            case DENIED:
                Log.e(TAG, "Dimming is on but 'display over other apps' (SYSTEM_ALERT_WINDOW) is not allowed: switching off");
                DimStore.applyDecision(prefs, decision);
                removeLayer();
                stopSelf();
                return;
            case SHOW:
                showLayer(Dim.color(DimStore.value(prefs)));
                break;
            default:
                removeLayer();
                break;
        }
        if (!Dim.wantsService(on)) stopSelf();
    }

    private void showLayer(int color) {
        Dim.LayerAction action = Dim.layerAction(layer != null, true);
        if (action == Dim.LayerAction.UPDATE) {
            layer.setBackgroundColor(color);
            return;
        }
        View view = new View(this);
        view.setBackgroundColor(color);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
                Dim.TYPE, Dim.flagsFor(DimStore.value(prefs)), PixelFormat.TRANSLUCENT);
        params.setTitle("TvHomeDim");
        try {
            wm.addView(view, params);
            layer = view;
        } catch (RuntimeException e) {
            Log.e(TAG, "The dimming layer could not be added: " + e.getMessage(), e);
            DimStore.saveOff(prefs, DimStore.ERROR_SERVICE);
            stopSelf();
        }
    }

    private void removeLayer() {
        if (Dim.layerAction(layer != null, false) != Dim.LayerAction.REMOVE) return;
        try {
            wm.removeView(layer);
        } catch (RuntimeException e) {
            Log.e(TAG, "The dimming layer could not be removed: " + e.getMessage(), e);
        }
        layer = null;
    }
}
