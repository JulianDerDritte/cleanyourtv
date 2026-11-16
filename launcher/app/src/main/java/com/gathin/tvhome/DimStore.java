package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Process;

/**
 * What the TV remembers about dimming, in the launcher's own choices file next to wallpaper_choice.
 * The value stays. Without "keep settings" the switch is "on" only in the power-on session and the process it was
 * switched on in: it is stored with the boot count and the start time of the process, so a restart of the TV, a
 * standby (the service clears it) or a killed process all leave it off. With "keep settings" the switch stays on
 * through standby and restart (dim_on). None of these keys is in Config.removeThemeChoices or hasOnTvChoices.
 */
final class DimStore {
    static final String VALUE_KEY = "dim_percent";
    static final String ON_BOOT_KEY = "dim_on_boot";
    static final String ON_RUN_KEY = "dim_on_run";
    /** The switch is on, for the case "keep settings" is ticked. */
    static final String ON_KEY = "dim_on";
    /** "Keep settings" is ticked. Off unless the user ticked it. */
    static final String KEEP_KEY = "dim_keep";
    static final String ERROR_KEY = "dim_error";

    static final String ERROR_BOOT_UNKNOWN = "boot_unknown";
    static final String ERROR_PERMISSION = "permission";
    static final String ERROR_SERVICE = "service";

    enum Result { OK, BOOT_UNKNOWN }

    private DimStore() {
    }

    static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE);
    }

    /** The marker of this process: it differs in every new process, also within the same boot. */
    static long runNow() {
        return Process.getStartElapsedRealtime();
    }

    /** The stored value, at most 95: an older, higher value (100 of the first version) is cut when read. */
    static int value(SharedPreferences p) {
        return Dim.clamp(p.getInt(VALUE_KEY, Dim.DEFAULT_VALUE));
    }

    static void saveValue(SharedPreferences p, int value) {
        p.edit().putInt(VALUE_KEY, Dim.clamp(value)).apply();
    }

    static boolean keep(SharedPreferences p) {
        return p.getBoolean(KEEP_KEY, false);
    }

    /**
     * Ticks or unticks "keep settings" and leaves the switch as it stood, also after a restart and for an install that
     * only knew the first version (switch stored by the markers alone). The switch is read before the change.
     */
    static Result saveKeep(SharedPreferences p, boolean keep, int bootNow, long runNow) {
        boolean wasOn = isOn(p, bootNow, runNow);
        SharedPreferences.Editor editor = p.edit().putBoolean(KEEP_KEY, keep);
        Result result = Result.OK;
        if (wasOn) {
            if (keep) {
                editor.putBoolean(ON_KEY, true);
            } else if (bootNow >= 0) {
                // From now on the markers carry the switch: stamp them for this boot and process.
                editor.putInt(ON_BOOT_KEY, bootNow).putLong(ON_RUN_KEY, runNow);
            } else {
                editor.remove(ON_KEY).remove(ON_BOOT_KEY).remove(ON_RUN_KEY).putString(ERROR_KEY, ERROR_BOOT_UNKNOWN);
                result = Result.BOOT_UNKNOWN;
            }
        } else {
            // Off stays off: a leftover dim_on from before a restart must not turn the switch on when it is ticked.
            editor.remove(ON_KEY);
        }
        editor.apply();
        return result;
    }

    /** With "keep settings": on as stored. Without: only when stored in this boot and this process, both known. */
    static boolean isOn(SharedPreferences p, int bootNow, long runNow) {
        if (keep(p)) return p.getBoolean(ON_KEY, false);
        if (bootNow < 0 || runNow < 0) return false;
        int boot = p.getInt(ON_BOOT_KEY, -1);
        long run = p.getLong(ON_RUN_KEY, -1);
        return boot >= 0 && run >= 0 && boot == bootNow && run == runNow;
    }

    /** Switches on for this boot and process. A boot count below 0 is unknown: the switch stays off, with the reason. */
    static Result saveOn(SharedPreferences p, int bootNow, long runNow) {
        if (bootNow < 0) {
            p.edit().remove(ON_BOOT_KEY).remove(ON_RUN_KEY).remove(ON_KEY).putString(ERROR_KEY, ERROR_BOOT_UNKNOWN).apply();
            return Result.BOOT_UNKNOWN;
        }
        p.edit().putInt(ON_BOOT_KEY, bootNow).putLong(ON_RUN_KEY, runNow).putBoolean(ON_KEY, true)
                .remove(ERROR_KEY).apply();
        return Result.OK;
    }

    /** Switches off; the value stays. */
    static void saveOff(SharedPreferences p) {
        p.edit().remove(ON_BOOT_KEY).remove(ON_RUN_KEY).remove(ON_KEY).apply();
    }

    /** The user switched off on purpose: off, and an old failure reason is gone with it. */
    static void saveOffByUser(SharedPreferences p) {
        p.edit().remove(ON_BOOT_KEY).remove(ON_RUN_KEY).remove(ON_KEY).remove(ERROR_KEY).apply();
    }

    /** The reason of a failure is shown only while the switch is off because of it, never next to a working layer. */
    static String shownError(SharedPreferences p, boolean on) {
        return on ? null : error(p);
    }

    /** Switches off because of a failure and keeps the reason, for the status line. */
    static void saveOff(SharedPreferences p, String error) {
        p.edit().remove(ON_BOOT_KEY).remove(ON_RUN_KEY).remove(ON_KEY).putString(ERROR_KEY, error).apply();
    }

    /** The screen went off (standby): off when the TV wakes, the value stays; with "keep settings" nothing changes. */
    static void afterScreenOff(SharedPreferences p) {
        if (keep(p)) return;
        saveOff(p);
    }

    /** What the service decided: no permission switches off, with the reason (also at the boot start). */
    static void applyDecision(SharedPreferences p, Dim.Decision decision) {
        if (decision == Dim.Decision.DENIED) saveOff(p, ERROR_PERMISSION);
    }

    /** The boot receiver starts the layer only for "keep settings" and the switch on. */
    static boolean shouldStartAtBoot(SharedPreferences p) {
        return keep(p) && p.getBoolean(ON_KEY, false);
    }

    static String error(SharedPreferences p) {
        return p.getString(ERROR_KEY, null);
    }
}
