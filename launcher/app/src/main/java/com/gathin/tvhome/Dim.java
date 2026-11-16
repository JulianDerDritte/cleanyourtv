package com.gathin.tvhome;

import android.content.Intent;
import android.view.WindowManager;

/**
 * The decisions of the dimming layer, kept free of Android calls so plain JVM tests can check them.
 * The layer is one black full-screen window over every app; its opacity is the chosen value from 0 to 95 percent.
 */
final class Dim {
    static final int MIN = 0;
    /** The top of the scale: 95 percent, so the picture never goes fully black (alpha at most 242 of 255). */
    static final int MAX = 95;
    /** The value offered when none was ever chosen. */
    static final int DEFAULT_VALUE = 50;

    /** Over every app. Needs the display-over-other-apps permission (SYSTEM_ALERT_WINDOW). */
    static final int TYPE = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
    /**
     * Never takes focus or touch, so every remote key and click goes on to the app below. Exactly these four flags
     * (0x318), the same for every value.
     */
    static final int FLAGS = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;

    private Dim() {
    }

    /** The window flags for a value: the same set for every value. */
    static int flagsFor(int value) {
        return FLAGS;
    }

    static int clamp(int value) {
        return Math.max(MIN, Math.min(MAX, value));
    }

    /** Opacity 0..255 of the black layer for a value; 0 gives 0, 100 gives fully opaque (255). */
    static int alphaOf(int value) {
        return (clamp(value) * 255 + 50) / 100;
    }

    /** The value that gives this opacity (the inverse of {@link #alphaOf} on the 101 values). */
    static int valueOf(int alpha) {
        return (Math.max(0, Math.min(255, alpha)) * 100 + 127) / 255;
    }

    /** The layer colour: black with the opacity of the value. */
    static int color(int value) {
        return alphaOf(value) << 24;
    }

    /** True when a layer is to be drawn: the switch is on and the value is above 0. */
    static boolean showsLayer(boolean on, int value) {
        return on && clamp(value) > 0;
    }

    /** The service runs only while the switch is on. */
    static boolean wantsService(boolean on) {
        return on;
    }

    enum Decision { NONE, SHOW, DENIED }

    /**
     * What the service does with the stored state. Switch on without the permission is DENIED (nothing is drawn, the
     * service logs it, switches off and stops). On with value 0 is NONE, not an error.
     */
    static Decision decide(boolean on, int value, boolean allowed) {
        if (!on) return Decision.NONE;
        if (!allowed) return Decision.DENIED;
        return showsLayer(true, value) ? Decision.SHOW : Decision.NONE;
    }

    enum LayerAction { NONE, ADD, UPDATE, REMOVE }

    /** Whether a window has to be added, updated or removed, so that there is never more than one. */
    static LayerAction layerAction(boolean present, boolean wanted) {
        if (wanted) return present ? LayerAction.UPDATE : LayerAction.ADD;
        return present ? LayerAction.REMOVE : LayerAction.NONE;
    }

    /** The boot receiver acts on the boot broadcast only. */
    static boolean isBootCompleted(String action) {
        return Intent.ACTION_BOOT_COMPLETED.equals(action);
    }

    /** Opening the submenu starts a layer that "keep settings" and the switch want but that is not running. */
    static boolean shouldRestartOnOpen(boolean keep, boolean on, boolean running) {
        return keep && on && !running;
    }

    /** Only the screen going off (standby) switches the layer off; app changes and the screensaver do not. */
    static boolean switchesOffOn(String action) {
        return Intent.ACTION_SCREEN_OFF.equals(action);
    }
}
