package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The rows of the Launcher settings screen and the gear menu in the four states of the two switches (A: both off, B: only
 * circle_list, C: both on, D: only list_mode), and the German texts of those screens.
 */
public class SettingsMenuTest {
    private static final int[] WITHOUT_EFFECT = {SettingsMenu.COLUMNS, SettingsMenu.ROWS_BELOW_DOCK, SettingsMenu.DOCK_OPACITY,
            SettingsMenu.ADVANCED_EFFECTS};
    private static final int[] WITH_EFFECT = {SettingsMenu.CLOCK, SettingsMenu.WALLPAPER, SettingsMenu.DIM,
            SettingsMenu.ANIMATIONS, SettingsMenu.FOLLOW_THEME, SettingsMenu.DEFAULT_HOME, SettingsMenu.TV_SETTINGS};
    /** A, B, C, D as {list_mode, circle_list}. */
    private static final boolean[][] STATES = {{false, false}, {false, true}, {true, true}, {true, false}};

    private static boolean circleActive(boolean[] state) {
        return ListMode.circleActive(state[0], state[1]);
    }

    @Test
    public void maskDropsTheRowsWithoutEffectWithListMode() {
        for (boolean[] state : STATES) {
            for (int row : WITHOUT_EFFECT) {
                // Tiles (A, B) keep the row; both lists (C, D) force the values it sets, so the row goes.
                assertEquals("row " + row + " with list_mode=" + state[0] + " circle_list=" + state[1], !state[0],
                        SettingsMenu.maskShows(row, state[0]));
            }
        }
        assertFalse("state D: the straight list", SettingsMenu.maskShows(SettingsMenu.COLUMNS, true));
        assertTrue("state B: circle_list alone shows the tiles", SettingsMenu.maskShows(SettingsMenu.COLUMNS, false));
    }

    @Test
    public void maskKeepsTheRowsThatDoSomethingInEveryState() {
        for (boolean[] state : STATES) {
            for (int row : WITH_EFFECT) {
                assertTrue("row " + row + " in state " + state[0] + "/" + state[1], SettingsMenu.maskShows(row, state[0]));
            }
        }
    }

    @Test
    public void gearMenuDropsArrangeAppsOnlyInTheCircleList() {
        for (boolean[] state : STATES) {
            assertEquals(!circleActive(state), SettingsMenu.menuShowsReorder(circleActive(state)));
        }
    }

    @Test
    public void gearMenuShowsModuleCardsInTheCircleListOnlyWhenThereAreCards() {
        // Circle list (C): shown only with an installed card and a working card feature.
        assertFalse(SettingsMenu.menuShowsModuleCards(true, true, 0, true));
        assertTrue(SettingsMenu.menuShowsModuleCards(true, true, 1, true));
        assertFalse(SettingsMenu.menuShowsModuleCards(true, true, 3, false));
        // Every other state: as before, whether or not a card is installed.
        for (boolean[] state : new boolean[][]{{false, false}, {false, true}, {true, false}}) {
            assertTrue(SettingsMenu.menuShowsModuleCards(true, circleActive(state), 0, true));
        }
        // module_cards off: never.
        for (boolean[] state : STATES) {
            assertFalse(SettingsMenu.menuShowsModuleCards(false, circleActive(state), 2, true));
        }
    }

    // ---- German texts ----

    private static Map<String, String> strings(String folder) throws IOException {
        // The unit tests run in the module's folder (launcher/app).
        Path file = Paths.get("src", "main", "res", folder, "strings.xml");
        assertTrue("not found: " + file.toAbsolutePath(), Files.isRegularFile(file));
        String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        Map<String, String> out = new HashMap<>();
        Matcher m = Pattern.compile("<string name=\"([^\"]+)\"[^>]*>(.*?)</string>", Pattern.DOTALL).matcher(text);
        while (m.find()) out.put(m.group(1), m.group(2));
        return out;
    }

    /** Every text the gear menu, the Launcher settings screen, the tile menu and the Dim screen show. */
    private static final String[] VISIBLE = {"launcher_settings", "columns_label", "rows_below_dock_label", "dock_opacity_label",
            "dream_name", "wallpaper_label", "wallpaper_state", "wallpaper_none", "wallpaper_applied", "wallpaper_theme_default",
            "wallpaper_random", "animations_state", "advanced_effects", "advanced_effects_state", "advanced_effects_warning",
            "follow_theme", "follow_theme_hint", "home_settings", "home_settings_help", "tv_settings", "reorder", "reorder_hint",
            "module_cards", "no_module_cards", "quick_network", "tile_move", "library_remove_shortcut", "library_app_info",
            "circle_move_hint", "dim_label", "dim_state", "dim_switch_state", "dim_value_state", "dim_permission_missing",
            "dim_error_boot", "dim_error_service", "dim_layer_active", "dim_keep", "dim_hint"};

    @Test
    public void germanTextsOfTheMenusAndTheMaskAreThereAndNotEnglish() throws IOException {
        Map<String, String> english = strings("values");
        Map<String, String> german = strings("values-de");
        for (String key : VISIBLE) {
            assertNotNull("values has no " + key, english.get(key));
            assertNotNull("values-de has no " + key, german.get(key));
            assertNotEquals("values-de still has the English text for " + key, english.get(key), german.get(key));
        }
        assertEquals("Hintergrund", german.get("wallpaper_label"));
    }
}
