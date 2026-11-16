package com.gathin.tvhome;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.CheckBox;
import android.widget.TextView;

/**
 * The submenu "Abdunkeln": an on/off button and the tick "keep settings" side by side, and a value from 0 to
 * 95 percent in steps of 1. Every change is stored at once, so Back leaves the last value standing; the state shown
 * is always read from the store in onResume. The layer itself is drawn by DimService, which this screen starts.
 */
public final class DimSettingsActivity extends Activity {
    private static final String TAG = "TvHome.Dim";

    private SharedPreferences prefs;
    private TextView switchRow;
    private CheckBox keepRow;
    private TextView valueRow;
    private TextView statusRow;

    @Override
    protected void attachBaseContext(android.content.Context base) {
        super.attachBaseContext(LauncherLocale.wrap(base));   // locale
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        // The same dark ground as Launcher settings, set on the window so the layout paints no second background.
        getWindow().setBackgroundDrawable(new ColorDrawable(0xFF101419));
        setContentView(R.layout.activity_dim_settings);
        prefs = DimStore.prefs(this);
        switchRow = findViewById(R.id.dimSwitch);
        keepRow = findViewById(R.id.dimKeep);
        valueRow = findViewById(R.id.dimValue);
        statusRow = findViewById(R.id.dimStatus);
        switchRow.setOnClickListener(v -> toggle());
        // The box ticks itself on a click; the click listener (not a change listener) stores it, so refresh() can set it.
        keepRow.setOnClickListener(v -> {
            if (DimStore.saveKeep(prefs, keepRow.isChecked(), OpenOnStart.boot(this), DimStore.runNow())
                    != DimStore.Result.OK) {
                Log.e(TAG, "Unticking 'keep settings' switched the layer off: the TV does not report its start count");
            }
            refresh();
        });
        switchRow.requestFocus();
    }

    /** Left and Right on the value row change the value by one; every other key goes its usual way. */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (getCurrentFocus() == valueRow
                && (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT)) {
            int step = keyCode == KeyEvent.KEYCODE_DPAD_LEFT ? -1 : 1;
            DimStore.saveValue(prefs, DimStore.value(prefs) + step);
            refresh();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
        // "Keep settings" and the switch on, but no layer in this process (the system ended it): start it again.
        if (Dim.shouldRestartOnOpen(DimStore.keep(prefs), isOn(), DimService.isRunning())) startService();
    }

    private boolean isOn() {
        return DimStore.isOn(prefs, OpenOnStart.boot(this), DimStore.runNow());
    }

    private void toggle() {
        if (isOn()) {
            DimStore.saveOffByUser(prefs);
            refresh();
            return;
        }
        if (!Settings.canDrawOverlays(this)) {
            DimStore.saveOff(prefs, DimStore.ERROR_PERMISSION);
            refresh();
            return;
        }
        if (DimStore.saveOn(prefs, OpenOnStart.boot(this), DimStore.runNow()) != DimStore.Result.OK) {
            refresh();
            return;
        }
        startService();
        refresh();
    }

    private void startService() {
        try {
            DimService.start(this);
        } catch (RuntimeException e) {
            Log.e(TAG, "The dimming service could not be started: " + e.getMessage(), e);
            DimStore.saveOff(prefs, DimStore.ERROR_SERVICE);
            refresh();
        }
    }

    /** Reads the switch, the tick, the value and the reason for a refusal from the store. */
    private void refresh() {
        boolean on = isOn();
        switchRow.setText(getString(R.string.dim_switch_state, getString(on ? R.string.on : R.string.off)));
        keepRow.setChecked(DimStore.keep(prefs));
        valueRow.setText(getString(R.string.dim_value_state, DimStore.value(prefs)));
        statusRow.setText(statusText(on));
    }

    private String statusText(boolean on) {
        if (!Settings.canDrawOverlays(this)) return getString(R.string.dim_permission_missing);
        String error = DimStore.shownError(prefs, on);
        if (DimStore.ERROR_BOOT_UNKNOWN.equals(error)) return getString(R.string.dim_error_boot);
        if (DimStore.ERROR_SERVICE.equals(error)) return getString(R.string.dim_error_service);
        return on ? getString(R.string.dim_layer_active) : "";
    }
}
