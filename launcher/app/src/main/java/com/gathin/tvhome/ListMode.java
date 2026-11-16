package com.gathin.tvhome;

import java.util.Collections;
import java.util.List;

/**
 * list_mode: Home as one vertical list of white app symbols on the left instead of tiles; the name shows only behind
 * the focused symbol. Everything that can be decided
 * without a TV lives here (fixed sizes, the shadow, the layout values the mode forces, which row Up and
 * Down reach, where focus goes back to), so a JVM test can pin it. The views themselves are built in MainActivity
 * and AppAdapter from these values.
 */
final class ListMode {
    private ListMode() { }

    /** Up on the first row leaves for the top bar. */
    static final int TOP_BAR = -1;
    /** Down on the last row has nowhere to go. */
    static final int END = -2;

    /** The list is this wide, starting at the root's own left padding (activity_main.xml, 56dp). */
    static final int ROOT_PADDING_DP = 56;
    static final int GRID_WIDTH_DP = 360;
    /** One row: the card (the symbol field, 72 px tall), and this much air above and below it: 128 px per row. */
    static final int CARD_DP = 36;
    static final int ROW_PAD_DP = 14;
    /** The symbol field, 96 x 72 px at density 2.0. */
    static final int ICON_W_DP = 48;
    static final int ICON_H_DP = 36;
    static final int NAME_SP = 22;
    /** The name starts this far from the list's left edge: the symbol field plus 16 dp (x = 240 px at density 2.0). */
    static final int NAME_START_DP = 64;
    /**
     * Space under the last row. With the rows above it fixes where the lowest symbol ends: 15 dp puts the smile of the
     * Prime Video symbol 996 px down, as far from the bottom edge as the clock digits are from the top (84 px). The one
     * value to adjust at the TV.
     */
    static final float BOTTOM_PAD_DP = 15f;
    /** Pixels between the smile's lowest point and the bottom of its symbol field (Prime Video, 25 px drawable, 72 px field). */
    static final int SMILE_INSET_PX = 26;
    static final int SCREEN_HEIGHT_PX = 1080;
    /** Top of the clock digits, measured at the TV (home-vor-a003.png): the list's bottom edge keeps the same distance. */
    static final int CLOCK_TOP_PX = 84;
    /** Gap between the two top buttons: 40 px at density 2.0 (the gear's start margin). */
    static final int HEADER_GAP_DP = 20;
    /** Focus is on a top button (see {@link #returnTo(int, int, int, int)}). */
    static final int HEADER = -3;

    /** The name of an app ends at this many dp from the screen's left edge (x = 640 px at density 2.0). */
    static final int NAME_END_DP = 320;

    /**
     * Home darkens nothing in list_mode. Text and symbols carry a soft black shadow instead: blur radius, drop (down) and
     * colour, in px at density 2.0 (scaled with the density where drawn).
     */
    static final int SHADOW_RADIUS_PX = 6;
    static final int SHADOW_DY_PX = 2;
    static final int SHADOW_COLOR = 0xCC000000;

    // ---- navigation ---------------------------------------------------------------------------------------

    /** The row after Up or Down from {@code position}: one row, or {@link #TOP_BAR} / {@link #END} at the ends. */
    static int step(int position, int count, boolean up) {
        if (up) return position <= 0 ? TOP_BAR : position - 1;
        return position >= count - 1 ? END : position + 1;
    }

    /**
     * The row focus goes back to after an app was open: the app's own row when it is still there, else the row it had,
     * kept inside the list. 0 for an empty list.
     */
    static int returnTo(int positionOfKey, int slot, int count) {
        if (positionOfKey >= 0) return positionOfKey;
        return Math.max(0, Math.min(slot, count - 1));
    }

    /** The circle list (circle_list) is shown only together with list_mode; alone it changes nothing. */
    static boolean circleActive(boolean listMode, boolean circleList) {
        return listMode && circleList;
    }

    /**
     * Holding OK opens the tile menu (with Move) when tile_menu says so, and always in the circle list: there the menu
     * is the way to change the order, and App info stays one row of it. The straight list keeps tile_menu as it is.
     */
    static boolean circleTileMenu(boolean tileMenu, boolean listMode, boolean circleList) {
        return tileMenu || circleActive(listMode, circleList);
    }

    /**
     * The row focus goes back to when Down (or Left on the first top button) leaves a top button: the row that was
     * chosen before, kept inside the list. 0 for an empty list.
     */
    static int afterHeaderDown(int lastFocused, int count) {
        if (count <= 0) return 0;
        return Math.max(0, Math.min(lastFocused, count - 1));
    }

    // ---- symbols and names ---------------------------------------------------------------------------------

    /**
     * The white logo for an app, or 0 when it has none (then the row draws a monogram, see
     * {@link #monogramLetter(String, String)}). The 15 packages listed in ic_list_sources.md; that file says where each comes from.
     */
    static int symbolFor(String packageName) {
        if (packageName == null) return 0;
        switch (packageName) {
            case "com.netflix.ninja":
                return R.drawable.ic_list_netflix;
            case "com.google.android.youtube.tv":
                return R.drawable.ic_list_youtube;
            case "com.amazon.amazonvideo.livingroom":
                return R.drawable.ic_list_primevideo;
            case "com.crunchyroll.crunchyroid":
                return R.drawable.ic_list_crunchyroll;
            case "com.spotify.tv.android":
                return R.drawable.ic_list_spotify;
            case "com.zdf.android.mediathek":
                return R.drawable.ic_list_zdf;
            case "com.valvesoftware.steamlink":
                return R.drawable.ic_list_steam;
            case "com.ted.android.tv":
                return R.drawable.ic_list_ted;
            case "com.android.vending":
                return R.drawable.ic_list_googleplay;
            case "de.telekom.magentatv.atv":
                return R.drawable.ic_list_magenta;
            case "de.swr.avp.ard.tv":
                return R.drawable.ic_list_ard;
            case "tv.arte.plus7":
                return R.drawable.ic_list_arte;
            case "com.disney.disneyplus":
                return R.drawable.ic_list_disneyplus;
            case "de.sky.online":
                return R.drawable.ic_list_wow;
            case "de.prosiebensat1digital.seventv":
                return R.drawable.ic_list_seventv;
            default:
                return 0;
        }
    }

    /** In list_mode every row has a symbol: its logo, or a monogram when {@code logo} is 0. So the name follows the focus. */
    static boolean listRowHasSymbol(int logo) {
        return true;
    }

    /** The monogram shown when a package has no letter at all (an "_" package, say). */
    static final String MONOGRAM_FALLBACK = "#";
    /** A monogram is 36 dp square like the logos; its ring is this thick. */
    static final int MONOGRAM_DP = 36;
    static final float MONOGRAM_RING_DP = 3f;

    /**
     * The letter in the monogram ring: the first letter or digit of the name, upper case; a name without one takes the
     * first letter of the package (underscores, dots and digits skipped); a package without a letter shows
     * {@link #MONOGRAM_FALLBACK}.
     */
    static String monogramLetter(String name, String packageName) {
        String fromName = firstCodePoint(name, true);
        if (fromName != null) return fromName;
        String fromPackage = firstCodePoint(packageName, false);
        return fromPackage != null ? fromPackage : MONOGRAM_FALLBACK;
    }

    private static String firstCodePoint(String text, boolean digitsToo) {
        if (text == null) return null;
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            if (Character.isLetter(cp) || digitsToo && Character.isDigit(cp)) {
                return new String(Character.toChars(Character.toUpperCase(cp)));
            }
            i += Character.charCount(cp);
        }
        return null;
    }

    /** The name shows behind the focused symbol; an app without a symbol shows it always. */
    static boolean nameVisible(boolean hasSymbol, boolean focused) {
        if (!hasSymbol) return true;
        return focused;
    }

    /** Where the smile of the lowest symbol ends, in px from the top, for this space under the last row. */
    static double smileBottomPx(float bottomPadDp) {
        return SCREEN_HEIGHT_PX - (bottomPadDp + ROW_PAD_DP) * 2 - SMILE_INSET_PX;
    }

    /** True when that edge is as far from the bottom as the clock digits are from the top, give or take 4 px. */
    static boolean bottomEdgeMatchesClock(float bottomPadDp) {
        return Math.abs(smileBottomPx(bottomPadDp) - (SCREEN_HEIGHT_PX - CLOCK_TOP_PX)) <= 4;
    }

    /** What the focus listener remembers: the id of the gear or the library button when one has focus, else 0. */
    static int headerFocusAfter(int focusedId, int settingsId, int libraryId) {
        if (focusedId == settingsId) return focusedId;
        if (focusedId == libraryId) return focusedId;
        return 0;
    }

    /**
     * Where focus goes back to when Home is rebuilt: {@link #HEADER} when a top button had it (the gear after the TV
     * settings, say), else the row as in {@link #returnTo(int, int, int)}.
     */
    static int returnTo(int headerFocusId, int positionOfKey, int slot, int count) {
        if (headerFocusId != 0) return HEADER;
        return returnTo(positionOfKey, slot, count);
    }

    // ---- the values the mode forces -----------------------------------------------------------------------

    /** The twelve layout values list_mode forces, plus one it never touches. */
    static final class Layout {
        final int columns, rowsBelowDock;
        final boolean dockRow, dockTitle, singleRow, ambientGlow, appGlow, cinematic, uniformTiles, fadeRows,
                alwaysShowNames, advancedEffects;
        /** Not one of the twelve: passes through unchanged. */
        final boolean animations;

        Layout(int columns, int rowsBelowDock, boolean dockRow, boolean dockTitle, boolean singleRow,
               boolean ambientGlow, boolean appGlow, boolean cinematic, boolean uniformTiles, boolean fadeRows,
               boolean alwaysShowNames, boolean advancedEffects, boolean animations) {
            this.columns = columns;
            this.rowsBelowDock = rowsBelowDock;
            this.dockRow = dockRow;
            this.dockTitle = dockTitle;
            this.singleRow = singleRow;
            this.ambientGlow = ambientGlow;
            this.appGlow = appGlow;
            this.cinematic = cinematic;
            this.uniformTiles = uniformTiles;
            this.fadeRows = fadeRows;
            this.alwaysShowNames = alwaysShowNames;
            this.advancedEffects = advancedEffects;
            this.animations = animations;
        }
    }

    /** With the mode off the values come back as given; on, the twelve are forced (one column, no dock, no glow). */
    static Layout apply(boolean listMode, Layout in) {
        if (!listMode) return in;
        return new Layout(1, 0, false, false, false, false, false, false, false, false, false, false, in.animations);
    }

    /** The top bar values list_mode forces: two round buttons, the library and the gear, and nothing else. */
    static final class Header {
        final boolean showInputsPill, showNetworkPill, topMenu, launcherSettingsButton, reorderButton, vpnPill,
                iconOnlyButtons, libraryButton, showLibraryPill;
        final List<String> topBarApps;

        Header(boolean showInputsPill, boolean showNetworkPill, boolean topMenu, boolean launcherSettingsButton,
               boolean reorderButton, boolean vpnPill, boolean iconOnlyButtons, boolean libraryButton,
               boolean showLibraryPill, List<String> topBarApps) {
            this.showInputsPill = showInputsPill;
            this.showNetworkPill = showNetworkPill;
            this.topMenu = topMenu;
            this.launcherSettingsButton = launcherSettingsButton;
            this.reorderButton = reorderButton;
            this.vpnPill = vpnPill;
            this.iconOnlyButtons = iconOnlyButtons;
            this.libraryButton = libraryButton;
            this.showLibraryPill = showLibraryPill;
            this.topBarApps = topBarApps;
        }
    }

    /**
     * With the mode off the values come back as given; on, the ten are forced: no Inputs or network pill (both live in
     * the menu behind the gear), no top menu, the gear opens the TV settings, no reorder button, no VPN pill, icon-only
     * buttons, the library as a top button without the row after the list, and no top-bar apps.
     */
    static Header applyHeader(boolean listMode, Header in) {
        if (!listMode) return in;
        return new Header(
                false,   // showInputsPill
                false,   // showNetworkPill
                false,   // topMenu
                false,   // launcherSettingsButton
                false,   // reorderButton
                false,   // vpnPill
                true,    // iconOnlyButtons
                true,    // libraryButton
                false,   // showLibraryPill
                Collections.<String>emptyList());   // topBarApps
    }

    // ---- the shadow --------------------------------------------------------------------------------------

    /** Room around a symbol for its blur and drop in the bitmap that carries the shadow (plus 2 px so no edge shows). */
    static int shadowMarginPx(int radiusPx, int dyPx) {
        return radiusPx + dyPx + 2;
    }

    /** Bytes of that bitmap (ARGB_8888, 4 bytes a pixel) for a symbol of this size: 72 x 72 with a margin of 10 gives 33,856. */
    static long shadowBitmapBytes(int symbolWidthPx, int symbolHeightPx, int marginPx) {
        return (long) (symbolWidthPx + 2 * marginPx) * (symbolHeightPx + 2 * marginPx) * 4;
    }

    /** The longest a name may be, in dp: it ends at {@link #NAME_END_DP}, and starts after the symbol or at the edge. */
    static int nameMaxWidthDp(boolean hasSymbol) {
        return NAME_END_DP - ROOT_PADDING_DP - (hasSymbol ? NAME_START_DP : 0);
    }
}
