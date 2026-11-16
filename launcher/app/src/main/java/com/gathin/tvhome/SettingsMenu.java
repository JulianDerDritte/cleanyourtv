package com.gathin.tvhome;

/**
 * Which rows the Launcher settings screen and the gear menu show. With list_mode (the straight list and the circle
 * list) the layout values are forced (ListMode.apply: one column, no dock, no advanced effects), so the screen's rows
 * that set them change nothing there and are not shown; the tiles (list_mode off, with or without circle_list) keep
 * every row. The gear menu's two rows follow the circle list only. Pure, so a JVM test can pin it.
 */
final class SettingsMenu {
    private SettingsMenu() { }

    // ---- rows of the Launcher settings screen ----
    /** "Apps per row": ListMode.apply forces one column. */
    static final int COLUMNS = 1;
    /** "Rows below dock": forced to 0 (no dock). */
    static final int ROWS_BELOW_DOCK = 2;
    /** "Dock opacity": the dock band is not drawn without a dock row. */
    static final int DOCK_OPACITY = 3;
    /** "Advanced effects" and its sentence: forced off. */
    static final int ADVANCED_EFFECTS = 4;
    static final int CLOCK = 10;
    static final int WALLPAPER = 11;
    static final int DIM = 12;
    static final int ANIMATIONS = 13;
    /** "Follow theme": it resets the clock, wallpaper and animation choices, which stay. */
    static final int FOLLOW_THEME = 14;
    static final int DEFAULT_HOME = 15;
    static final int TV_SETTINGS = 16;

    /** True when the Launcher settings screen shows this row. {@code listMode} is Config.listMode (forced layout values). */
    static boolean maskShows(int row, boolean listMode) {
        if (!listMode) return true;
        return row != COLUMNS && row != ROWS_BELOW_DOCK && row != DOCK_OPACITY && row != ADVANCED_EFFECTS;
    }

    // ---- rows of the gear menu ----

    /** "Arrange apps": in the circle list the tile menu's Move (hold OK on an app) does it, so the row goes. */
    static boolean menuShowsReorder(boolean circleList) {
        return !circleList;
    }

    /**
     * "Module cards" opens the list of installed cards. Elsewhere it stays as it was (with no card installed it says so);
     * in the circle list it is shown only when there is something to list.
     */
    static boolean menuShowsModuleCards(boolean moduleCards, boolean circleList, int installedCards, boolean cardFeatureWorking) {
        if (!moduleCards) return false;
        if (!circleList) return true;
        return installedCards > 0 && cardFeatureWorking;
    }
}
