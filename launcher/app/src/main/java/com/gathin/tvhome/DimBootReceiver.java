package com.gathin.tvhome;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Brings the dimming layer back after a restart of the TV, but only when "keep settings" is ticked and the
 * switch is on. It reacts to the system's boot broadcast and nothing else, and reads nothing from the broadcast.
 */
public final class DimBootReceiver extends BroadcastReceiver {
    private static final String TAG = "TvHome.Dim";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Dim.isBootCompleted(intent.getAction())) return;
        SharedPreferences prefs = DimStore.prefs(context);
        if (!DimStore.shouldStartAtBoot(prefs)) return;
        try {
            DimService.start(context);
        } catch (RuntimeException e) {
            Log.e(TAG, "The dimming service could not be started at boot: " + e.getMessage(), e);
            DimStore.saveOff(prefs, DimStore.ERROR_SERVICE);
        }
    }
}
