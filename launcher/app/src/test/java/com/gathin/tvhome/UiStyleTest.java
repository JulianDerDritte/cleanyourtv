package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The same look on every screen of the menus and settings: font, focus, corners, panel, spacing and row height. The tests
 * read the layouts, drawables and styles of the module (they run in launcher/app) and the constants of the two classes
 * that draw their corners in code.
 */
public class UiStyleTest {
    private static String res(String... path) throws IOException {
        Path file = Paths.get("src", "main", "res");
        for (String part : path) file = file.resolve(part);
        assertTrue("not found: " + file.toAbsolutePath(), Files.isRegularFile(file));
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }

    private static String java(String name) throws IOException {
        Path file = Paths.get("src", "main", "java", "com", "gathin", "tvhome", name);
        assertTrue("not found: " + file.toAbsolutePath(), Files.isRegularFile(file));
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }

    private static List<String> matches(String regex, String text) {
        List<String> out = new ArrayList<>();
        Matcher m = Pattern.compile(regex, Pattern.DOTALL).matcher(text);
        while (m.find()) out.add(m.group());
        return out;
    }

    // ---- K1: the font of the menus ----

    @Test
    public void everyTextOfTheSettingsScreensHasTheMenuFont() throws IOException {
        String styles = res("values", "styles.xml");
        String settingsRow = matches("<style name=\"SettingsRow\">.*?</style>", styles).get(0);
        assertTrue("SettingsRow has no font", settingsRow.contains("android:fontFamily\">@string/font_"));
        int without = 0;
        StringBuilder which = new StringBuilder();
        for (String layout : new String[]{"activity_launcher_settings.xml", "activity_dim_settings.xml"}) {
            for (String tag : matches("<(TextView|CheckBox)\\b[^>]*>", res("layout", layout))) {
                boolean ownFont = tag.contains("android:fontFamily=\"@string/font_");
                boolean styleFont = tag.contains("style=\"@style/SettingsRow\"") || tag.contains("style=\"@style/SettingsColumnChoice\"");
                if (!ownFont && !styleFont) {
                    without++;
                    which.append(layout).append(": ").append(tag.replace('\n', ' ')).append('\n');
                }
            }
        }
        assertEquals("texts without the menu font:\n" + which, 0, without);
        // Vietnamese keeps the system font through the same strings (Outfit has no glyphs for it).
        String vi = res("values-vi", "fonts.xml");
        assertTrue(vi.contains("name=\"font_medium\"") && vi.contains("name=\"font_regular\""));
    }

    // ---- K2: focus ----

    /** The item of a selector whose states are exactly these (e.g. "focused", "selected"); none: the item without states. */
    private static String stateItem(String drawable, String... states) {
        Set<String> wanted = new HashSet<>(java.util.Arrays.asList(states));
        for (String item : matches("<item\\b.*?</item>", drawable)) {
            String header = item.substring(0, item.indexOf('>'));
            Set<String> have = new HashSet<>();
            Matcher m = Pattern.compile("android:state_(\\w+)=\"true\"").matcher(header);
            while (m.find()) have.add(m.group(1));
            if (have.equals(wanted)) return item;
        }
        throw new AssertionError("no item with the states " + wanted);
    }

    private static String solid(String item) {
        Matcher m = Pattern.compile("<solid android:color=\"(#[0-9A-Fa-f]+)\"").matcher(item);
        assertTrue(item, m.find());
        return m.group(1).toUpperCase();
    }

    private static String stroke(String item) {
        Matcher m = Pattern.compile("<stroke android:width=\"(\\d+)dp\" android:color=\"(#[0-9A-Fa-f]+)\"").matcher(item);
        return m.find() ? m.group(1) + "dp " + m.group(2).toUpperCase() : null;
    }

    @Test
    public void focusedRowIsWhiteLikeTheMenusAndSelectedStaysGreen() throws IOException {
        String choice = res("drawable", "settings_choice_bg.xml");
        String menuRow = res("drawable", "input_row_bg.xml");
        String menuFocus = solid(stateItem(menuRow, "focused"));
        assertEquals("#F5FFFFFF", menuFocus);
        // Focus only: the white fill of the menus, no frame.
        String focused = stateItem(choice, "focused");
        assertEquals(menuFocus, solid(focused));
        assertEquals(null, stroke(focused));
        // Selected only: the green frame on the dark row.
        String selected = stateItem(choice, "selected");
        assertEquals("1dp #FF4DAE6D", stroke(selected));
        assertTrue(solid(selected).startsWith("#33"));
        // Both: the white fill with the green frame.
        String both = stateItem(choice, "focused", "selected");
        assertEquals(menuFocus, solid(both));
        assertEquals("2dp #FF4DAE6D", stroke(both));
        // Neither: the dark row.
        assertEquals("#FF242B34", solid(stateItem(choice)));
        // The text turns dark with focus, as in the menus and pills, for every row of the settings screens.
        assertTrue(res("drawable", "pill_text.xml").contains("state_focused=\"true\" android:color=\"#FF111111\""));
        String settingsRow = matches("<style name=\"SettingsRow\">.*?</style>", res("values", "styles.xml")).get(0);
        assertTrue(settingsRow.contains("android:textColor\">@drawable/pill_text<"));
    }

    // ---- K3, K4: corners and the panel ----

    @Test
    public void rowsAndCardsAre14dpAndPanelsAre20dp() throws IOException {
        for (String drawable : new String[]{"input_row_bg.xml", "settings_choice_bg.xml"}) {
            List<String> radii = matches("android:radius=\"\\d+dp\"", res("drawable", drawable));
            assertTrue(drawable, radii.size() >= 2);
            for (String radius : radii) assertEquals(drawable, "android:radius=\"14dp\"", radius);
        }
        assertTrue(res("drawable", "glass_panel.xml").contains("android:radius=\"20dp\""));
        assertEquals(14, WallpaperTiles.TILE_RADIUS_DP);
        assertEquals(14, AppLibraryDialog.CARD_RADIUS_DP);
        // The classes draw their corners from those constants.
        assertTrue(java("WallpaperTiles.java").contains("TILE_RADIUS_DP * density"));
        assertTrue(java("AppLibraryDialog.java").contains("CARD_RADIUS_DP));"));
    }

    @Test
    public void wallpaperTilesWindowIsTheGlassPanelOfTheMenus() throws IOException {
        String source = java("WallpaperTiles.java");
        assertFalse("the plain fill is gone", source.contains("0xF71C1E24"));
        assertTrue(source.contains("setBackgroundResource(R.drawable.glass_panel)"));
    }

    @Test
    public void followThemeRowIsNotMarkedSelectedBySomeStoredState() throws IOException {
        // "Follow theme" is an action row: it looks like any row at rest (the green frame is the active option of a row).
        assertFalse(java("LauncherSettingsActivity.java").contains("followThemeChoice.setSelected"));
    }

    // ---- K5: spacing of the settings screens ----

    @Test
    public void settingsSpacingIs24BetweenGroups10WithinAndSixAboveHints() throws IOException {
        Set<String> allowed = new HashSet<>();
        allowed.add("24dp");
        allowed.add("10dp");
        allowed.add("6dp");
        for (String layout : new String[]{"activity_launcher_settings.xml", "activity_dim_settings.xml"}) {
            for (String tag : matches("<(TextView|LinearLayout|CheckBox)\\b[^>]*>", res("layout", layout))) {
                Matcher m = Pattern.compile("android:layout_marginTop=\"([^\"]+)\"").matcher(tag);
                while (m.find()) assertTrue(layout + ": " + m.group(1) + " in " + tag.replace('\n', ' '), allowed.contains(m.group(1)));
            }
        }
        String launcher = res("layout", "activity_launcher_settings.xml");
        // Hintergrund, Abdunkeln, Animationen are one group of single rows: 10 dp apart, none of them a group gap.
        for (String id : new String[]{"dimChoice", "animationsChoice", "advancedChoice"}) {
            assertEquals(id, "10dp", marginTopOf(launcher, id));
        }
        for (String id : new String[]{"columnsLabel", "rowsLabel", "dockLabel", "clockLabel", "wallpaperChoice", "followThemeChoice",
                "defaultHomeChoice", "tvSettingsChoice"}) {
            assertEquals(id, "24dp", marginTopOf(launcher, id));
        }
        assertEquals("6dp", marginTopOf(launcher, "advancedHint"));
    }

    private static String marginTopOf(String layout, String id) {
        for (String tag : matches("<(TextView|LinearLayout)\\b[^>]*>", layout)) {
            if (tag.contains("android:id=\"@+id/" + id + "\"")) {
                Matcher m = Pattern.compile("android:layout_marginTop=\"([^\"]+)\"").matcher(tag);
                assertTrue(id, m.find());
                return m.group(1);
            }
        }
        throw new AssertionError("no view " + id);
    }

    @Test
    public void lastColumnOfARowHasNoEndMargin() throws IOException {
        int rows = 0;
        for (String layout : new String[]{"activity_launcher_settings.xml"}) {
            for (String row : matches("<LinearLayout\\b[^>]*android:orientation=\"horizontal\">.*?</LinearLayout>", res("layout", layout))) {
                List<String> columns = matches("<TextView\\b[^>]*SettingsColumnChoice[^>]*>", row);
                assertFalse(columns.isEmpty());
                rows++;
                for (int i = 0; i < columns.size(); i++) {
                    boolean zero = columns.get(i).contains("android:layout_marginEnd=\"0dp\"");
                    assertEquals("column " + i + " of " + columns.size() + " in " + row.replace('\n', ' '), i == columns.size() - 1, zero);
                }
            }
        }
        assertEquals("rows of columns: columns, rows below dock, dock opacity, clock", 4, rows);
        // The Dim screen already ends its row flush; its style default stays the gap between columns.
        assertTrue(res("layout", "activity_dim_settings.xml").contains("android:layout_marginEnd=\"0dp\""));
        assertTrue(matches("<style name=\"SettingsColumnChoice\".*?</style>", res("values", "styles.xml")).get(0)
                .contains("android:layout_marginEnd\">10dp<"));
    }

    // ---- K6: row height ----

    @Test
    public void menuAndSettingsRowsAre48dpHigh() throws IOException {
        Matcher menu = Pattern.compile("android:layout_height=\"(\\d+)dp\"").matcher(res("layout", "item_input.xml"));
        assertTrue(menu.find());
        assertEquals("48", menu.group(1));
        String settingsRow = matches("<style name=\"SettingsRow\">.*?</style>", res("values", "styles.xml")).get(0);
        assertTrue(settingsRow.contains("android:layout_height\">48dp<"));
    }
}
