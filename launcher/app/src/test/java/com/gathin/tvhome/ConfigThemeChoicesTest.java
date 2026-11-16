package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.SharedPreferences;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The choices made on the TV and what a new theme takes back: the tick "Random abspielen" lives with them, so a
 * theme push resets it and "Theme default" counts it, while installing over the launcher (same files, same signature)
 * keeps it.
 */
public class ConfigThemeChoicesTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    /** A SharedPreferences in memory: no Android needed. */
    private static final class Prefs implements SharedPreferences {
        final Map<String, Object> values = new HashMap<>();

        @Override public Map<String, ?> getAll() { return new HashMap<>(values); }
        @Override public String getString(String key, String def) { return values.containsKey(key) ? (String) values.get(key) : def; }
        @Override public Set<String> getStringSet(String key, Set<String> def) { return def; }
        @Override public int getInt(String key, int def) { return values.containsKey(key) ? (Integer) values.get(key) : def; }
        @Override public long getLong(String key, long def) { return values.containsKey(key) ? (Long) values.get(key) : def; }
        @Override public float getFloat(String key, float def) { return def; }
        @Override public boolean getBoolean(String key, boolean def) { return values.containsKey(key) ? (Boolean) values.get(key) : def; }
        @Override public boolean contains(String key) { return values.containsKey(key); }
        @Override public Editor edit() { return new Ed(); }
        @Override public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l) { }
        @Override public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l) { }

        final class Ed implements Editor {
            private final Map<String, Object> puts = new HashMap<>();
            private final Set<String> removes = new HashSet<>();

            @Override public Editor putString(String key, String value) { puts.put(key, value); return this; }
            @Override public Editor putStringSet(String key, Set<String> value) { puts.put(key, value); return this; }
            @Override public Editor putInt(String key, int value) { puts.put(key, value); return this; }
            @Override public Editor putLong(String key, long value) { puts.put(key, value); return this; }
            @Override public Editor putFloat(String key, float value) { puts.put(key, value); return this; }
            @Override public Editor putBoolean(String key, boolean value) { puts.put(key, value); return this; }
            @Override public Editor remove(String key) { removes.add(key); return this; }
            @Override public Editor clear() { removes.addAll(values.keySet()); return this; }
            @Override public boolean commit() { apply(); return true; }
            @Override public void apply() {
                for (String key : removes) values.remove(key);
                values.putAll(puts);
            }
        }
    }

    private File themeFolder(String revision) throws IOException {
        File dir = folder.newFolder();
        if (revision != null) Files.write(new File(dir, "theme_revision").toPath(), revision.getBytes(StandardCharsets.US_ASCII));
        return dir;
    }

    private static final String REVISION = "0123456789abcdef0123456789abcdef";   // 32 characters, as theme.sh writes it

    // ---- what a new theme takes back ----

    @Test
    public void aNewThemeTakesBackTheRandomTick() throws IOException {
        Prefs prefs = new Prefs();
        prefs.values.put("theme_signature", "absent:older");
        prefs.values.put(Config.WALLPAPER_RANDOM_KEY, true);
        Config.syncThemeChoices(prefs, themeFolder(REVISION), null);
        assertFalse(prefs.contains(Config.WALLPAPER_RANDOM_KEY));
    }

    @Test
    public void installingOverTheLauncherKeepsTheRandomTick() throws IOException {
        Prefs prefs = new Prefs();
        prefs.values.put("theme_signature", "absent:" + REVISION);   // the same files: the signature does not move
        prefs.values.put(Config.WALLPAPER_RANDOM_KEY, true);
        Config.syncThemeChoices(prefs, themeFolder(REVISION), null);
        assertTrue(prefs.getBoolean(Config.WALLPAPER_RANDOM_KEY, false));
    }

    @Test
    public void theFirstStartAfterAThemePushTakesBackTheRandomTick() throws IOException {
        Prefs prefs = new Prefs();
        prefs.values.put(Config.WALLPAPER_RANDOM_KEY, true);
        Config.syncThemeChoices(prefs, themeFolder(REVISION), null);
        assertFalse(prefs.contains(Config.WALLPAPER_RANDOM_KEY));
    }

    @Test
    public void theFirstStartWithoutAThemePushKeepsTheRandomTick() throws IOException {
        Prefs prefs = new Prefs();
        prefs.values.put(Config.WALLPAPER_RANDOM_KEY, true);
        Config.syncThemeChoices(prefs, themeFolder(null), null);
        assertTrue(prefs.getBoolean(Config.WALLPAPER_RANDOM_KEY, false));
    }

    @Test
    public void aNewThemeStillTakesBackThePictureChoice() throws IOException {
        Prefs prefs = new Prefs();
        prefs.values.put("theme_signature", "absent:older");
        prefs.values.put(Config.WALLPAPER_CHOICE_KEY, "a.jpg");
        Config.syncThemeChoices(prefs, themeFolder(REVISION), null);
        assertFalse(prefs.contains(Config.WALLPAPER_CHOICE_KEY));
    }

    @Test
    public void takingBackTheChoicesRemovesTheRandomTick() {
        Prefs prefs = new Prefs();
        prefs.values.put(Config.WALLPAPER_RANDOM_KEY, true);
        Config.removeThemeChoices(prefs.edit()).commit();
        assertFalse(prefs.contains(Config.WALLPAPER_RANDOM_KEY));
    }

    // ---- "Theme default" counts the tick ----

    @Test
    public void onlyTheRandomTickIsAnOnTvChoice() {
        Prefs prefs = new Prefs();
        prefs.values.put(Config.WALLPAPER_RANDOM_KEY, true);
        assertTrue(Config.hasOnTvChoices(prefs));
    }

    @Test
    public void withoutAnyChoiceThereIsNothingToTakeBack() {
        assertFalse(Config.hasOnTvChoices(new Prefs()));
    }

    // ---- the library order is no theme choice ----

    @Test
    public void libraryOrderSurvivesAThemePush() throws IOException {
        Prefs prefs = new Prefs();
        prefs.values.put("theme_signature", "absent:older");
        prefs.values.put(Config.LIBRARY_ORDER_KEY, "a.b\nc.d\n");
        prefs.values.put(Config.WALLPAPER_CHOICE_KEY, "a.jpg");
        Config.syncThemeChoices(prefs, themeFolder(REVISION), null);
        assertEquals("a.b\nc.d\n", prefs.getString(Config.LIBRARY_ORDER_KEY, null));
        assertFalse(prefs.contains(Config.WALLPAPER_CHOICE_KEY));   // the theme still takes the picture back
    }

    @Test
    public void onlyReorderingDoesNotShowThemeDefault() {
        Prefs prefs = new Prefs();
        prefs.values.put(Config.LIBRARY_ORDER_KEY, "a.b\n");
        assertFalse(Config.hasOnTvChoices(prefs));
        prefs.values.put(Config.WALLPAPER_CHOICE_KEY, "a.jpg");
        assertTrue(Config.hasOnTvChoices(prefs));
    }
}
