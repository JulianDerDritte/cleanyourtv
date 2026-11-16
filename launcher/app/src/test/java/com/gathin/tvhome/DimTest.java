package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.content.SharedPreferences;

import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Abdunkeln: the opacity of the layer, its window flags, what the service decides, and what the TV remembers
 * (value kept, switch off after standby, restart or a killed process). The activity and the service wiring are not
 * JVM-testable here (no Robolectric); the click path and the TV measurements cover them.
 */
public class DimTest {
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

    // ---- K1: value to opacity ----

    @Test
    public void alphaDerFuenfWerte() {
        assertEquals(0, Dim.alphaOf(0));
        assertEquals(3, Dim.alphaOf(1));
        assertEquals(128, Dim.alphaOf(50));
        assertEquals(240, Dim.alphaOf(94));
        assertEquals(242, Dim.alphaOf(95));
    }

    @Test
    public void werteAusserhalbWerdenBegrenzt() {
        assertEquals(0, Dim.alphaOf(-1));
        assertEquals(242, Dim.alphaOf(96));
        assertEquals(242, Dim.alphaOf(100));
        assertEquals(0, Dim.clamp(Integer.MIN_VALUE));
        assertEquals(95, Dim.clamp(Integer.MAX_VALUE));
        assertEquals(95, Dim.clamp(96));
        assertEquals(95, Dim.clamp(100));
        assertEquals(0, Dim.clamp(-1));
    }

    @Test
    public void deckkraftIstNieUeber95Prozent() {
        for (int v = -5; v <= 300; v++) assertTrue("alpha at " + v, Dim.alphaOf(v) <= 242);
        assertTrue(Dim.alphaOf(95) * 100 <= 95 * 255);
        assertEquals(95, Dim.MAX);
    }

    @Test
    public void abbildungIstFuerAlle96WerteUmkehrbar() {
        int last = -1;
        for (int v = 0; v <= 95; v++) {
            int alpha = Dim.alphaOf(v);
            assertTrue("alpha must rise with the value at " + v, alpha > last);
            last = alpha;
            assertEquals("round trip of " + v, v, Dim.valueOf(alpha));
        }
    }

    // ---- K3: the window flags ----

    @Test
    public void schichtFlagsSindGenau0x318FuerJedenWert() {
        assertEquals(0x318, Dim.FLAGS);
        for (int v = 1; v <= 95; v++) assertEquals("flags at " + v, 0x318, Dim.flagsFor(v));
        assertEquals(2038, Dim.TYPE);
    }

    // ---- K6, K10, K11: what is drawn, what runs ----

    @Test
    public void schalterAusOderWertNullZeigtKeineSchicht() {
        assertFalse(Dim.showsLayer(false, 70));
        assertFalse(Dim.showsLayer(true, 0));
        assertTrue(Dim.showsLayer(true, 1));
    }

    @Test
    public void keinDienstBeiSchalterAus() {
        assertFalse(Dim.wantsService(false));
        assertTrue(Dim.wantsService(true));
    }

    @Test
    public void ohneBerechtigungBeiEinWirdVerweigert() {
        assertEquals(Dim.Decision.DENIED, Dim.decide(true, 50, false));
        assertEquals(Dim.Decision.DENIED, Dim.decide(true, 0, false));
        assertEquals(Dim.Decision.NONE, Dim.decide(false, 50, false));
        assertEquals(Dim.Decision.SHOW, Dim.decide(true, 50, true));
    }

    @Test
    public void berechtigtMitWertNullZeigtNichtsUndMeldetKeinenFehler() {
        assertEquals(Dim.Decision.NONE, Dim.decide(true, 0, true));
    }

    @Test
    public void schichtWirdNurEinmalHinzugefuegt() {
        assertEquals(Dim.LayerAction.ADD, Dim.layerAction(false, true));
        assertEquals(Dim.LayerAction.UPDATE, Dim.layerAction(true, true));
        assertEquals(Dim.LayerAction.REMOVE, Dim.layerAction(true, false));
        assertEquals(Dim.LayerAction.NONE, Dim.layerAction(false, false));
    }

    // ---- K7: the value is kept ----

    @Test
    public void gespeicherterWertUeberlebtVerlassenUndZurueckkommen() {
        Prefs prefs = new Prefs();
        assertEquals(Dim.DEFAULT_VALUE, DimStore.value(prefs));
        DimStore.saveValue(prefs, 37);
        // Leave the submenu and come back: a new read of the same store.
        assertEquals(37, DimStore.value(prefs));
        // Switching on and off does not touch the value.
        DimStore.saveOn(prefs, 5, 1000L);
        DimStore.saveOff(prefs);
        assertEquals(37, DimStore.value(prefs));
        DimStore.saveValue(prefs, 500);
        assertEquals(95, DimStore.value(prefs));
    }

    @Test
    public void gespeicherterWertUeber95WirdBeimLesenBegrenzt() {
        Prefs prefs = new Prefs();
        prefs.values.put(DimStore.VALUE_KEY, 100);   // what the first version could store
        assertEquals(95, DimStore.value(prefs));
        prefs.values.put(DimStore.VALUE_KEY, 95);
        assertEquals(95, DimStore.value(prefs));
        prefs.values.put(DimStore.VALUE_KEY, 94);
        assertEquals(94, DimStore.value(prefs));
    }

    @Test
    public void grenze95GiltImClamp() {
        assertEquals(95, Dim.clamp(95));
        assertEquals(95, Dim.clamp(96));
        assertEquals(95, Dim.clamp(100));
        assertEquals(0, Dim.clamp(-1));
    }

    // ---- K4: off after restart, standby, a killed process ----

    @Test
    public void schalterStehtNachNeustartAufAus() {
        Prefs prefs = new Prefs();
        DimStore.saveValue(prefs, 37);
        assertEquals(DimStore.Result.OK, DimStore.saveOn(prefs, 5, 1000L));
        assertTrue(DimStore.isOn(prefs, 5, 1000L));
        assertFalse("another boot", DimStore.isOn(prefs, 6, 1000L));
        assertEquals("the value stays", 37, DimStore.value(prefs));
    }

    @Test
    public void einGiltNurImSelbenProzess() {
        Prefs prefs = new Prefs();
        DimStore.saveOn(prefs, 5, 1000L);
        assertFalse("same boot, new process", DimStore.isOn(prefs, 5, 2000L));
        assertTrue(DimStore.isOn(prefs, 5, 1000L));
    }

    @Test
    public void bildschirmAusSchaltetAusAberBehaeltDenWert() {
        Prefs prefs = new Prefs();
        DimStore.saveValue(prefs, 37);
        DimStore.saveOn(prefs, 5, 1000L);
        DimStore.afterScreenOff(prefs);
        assertFalse(DimStore.isOn(prefs, 5, 1000L));
        assertEquals(37, DimStore.value(prefs));
    }

    @Test
    public void nurBildschirmAusSchaltetAus() {
        assertTrue(Dim.switchesOffOn(Intent.ACTION_SCREEN_OFF));
        assertFalse(Dim.switchesOffOn(Intent.ACTION_SCREEN_ON));
        assertFalse(Dim.switchesOffOn(Intent.ACTION_DREAMING_STARTED));
        assertFalse(Dim.switchesOffOn(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
        assertFalse(Dim.switchesOffOn(null));
    }

    // ---- K12: boot count unknown ----

    @Test
    public void startzaehlerMinusEinsBleibtAusMitGrund() {
        Prefs prefs = new Prefs();
        assertEquals(DimStore.Result.BOOT_UNKNOWN, DimStore.saveOn(prefs, -1, 1000L));
        assertFalse(DimStore.isOn(prefs, -1, 1000L));
        assertEquals(DimStore.ERROR_BOOT_UNKNOWN, DimStore.error(prefs));
        // Nothing stored from before counts either.
        assertFalse(DimStore.isOn(prefs, 0, 1000L));
        // Boot 0 is a valid count: on, and the reason is cleared.
        assertEquals(DimStore.Result.OK, DimStore.saveOn(prefs, 0, 1000L));
        assertTrue(DimStore.isOn(prefs, 0, 1000L));
        assertNull(DimStore.error(prefs));
    }

    // ---- K16: "keep settings" ----

    @Test
    public void merkenIstVorgabeNichtAngehaktUndBleibtGespeichert() {
        Prefs prefs = new Prefs();
        assertFalse(DimStore.keep(prefs));
        DimStore.saveKeep(prefs, true, 5, 1000L);
        assertTrue(DimStore.keep(prefs));
        // A new read of the same store, as after standby or a restart.
        assertTrue(DimStore.keep(prefs));
        DimStore.saveKeep(prefs, false, 5, 1000L);
        assertFalse(DimStore.keep(prefs));
    }

    @Test
    public void nichtAngehaktStandbyUndNeustartSchaltenAus() {
        Prefs prefs = new Prefs();
        DimStore.saveValue(prefs, 37);
        DimStore.saveOn(prefs, 5, 1000L);
        assertTrue(DimStore.isOn(prefs, 5, 1000L));
        DimStore.afterScreenOff(prefs);
        assertFalse("standby", DimStore.isOn(prefs, 5, 1000L));
        DimStore.saveOn(prefs, 5, 1000L);
        assertFalse("restart", DimStore.isOn(prefs, 6, 2000L));
        assertEquals(37, DimStore.value(prefs));
    }

    @Test
    public void angehaktStandbyLaesstEinStehen() {
        Prefs prefs = new Prefs();
        DimStore.saveKeep(prefs, true, 5, 1000L);
        DimStore.saveValue(prefs, 37);
        DimStore.saveOn(prefs, 5, 1000L);
        DimStore.afterScreenOff(prefs);
        assertTrue(DimStore.isOn(prefs, 5, 1000L));
        assertEquals(Dim.Decision.SHOW, Dim.decide(DimStore.isOn(prefs, 5, 1000L), DimStore.value(prefs), true));
    }

    @Test
    public void angehaktNeustartLaesstEinStehen() {
        Prefs prefs = new Prefs();
        DimStore.saveKeep(prefs, true, 5, 1000L);
        DimStore.saveOn(prefs, 5, 1000L);
        assertTrue("another boot, another process", DimStore.isOn(prefs, 6, 2000L));
        // The same restart without the tick: off.
        Prefs plain = new Prefs();
        DimStore.saveOn(plain, 5, 1000L);
        assertFalse(DimStore.isOn(plain, 6, 2000L));
    }

    @Test
    public void angehaktNeustartOhneBerechtigungSchaltetAusMitGrund() {
        Prefs prefs = new Prefs();
        DimStore.saveKeep(prefs, true, 5, 1000L);
        DimStore.saveValue(prefs, 37);
        DimStore.saveOn(prefs, 5, 1000L);
        boolean on = DimStore.isOn(prefs, 6, 2000L);
        Dim.Decision decision = Dim.decide(on, DimStore.value(prefs), false);
        assertEquals(Dim.Decision.DENIED, decision);
        DimStore.applyDecision(prefs, decision);
        assertFalse(DimStore.isOn(prefs, 6, 2000L));
        assertEquals(DimStore.ERROR_PERMISSION, DimStore.error(prefs));
        assertEquals(37, DimStore.value(prefs));
        assertTrue("the tick stays", DimStore.keep(prefs));
    }

    @Test
    public void bootEmpfaengerStartetNurBeiAngehaktemMerkenUndEin() {
        Prefs both = new Prefs();
        DimStore.saveKeep(both, true, 5, 1000L);
        DimStore.saveOn(both, 5, 1000L);
        assertTrue(DimStore.shouldStartAtBoot(both));
        Prefs tickOnly = new Prefs();
        DimStore.saveKeep(tickOnly, true, 5, 1000L);
        assertFalse(DimStore.shouldStartAtBoot(tickOnly));
        Prefs onOnly = new Prefs();
        DimStore.saveOn(onOnly, 5, 1000L);
        assertFalse(DimStore.shouldStartAtBoot(onOnly));
        assertTrue(Dim.isBootCompleted(Intent.ACTION_BOOT_COMPLETED));
        assertFalse(Dim.isBootCompleted(Intent.ACTION_SCREEN_ON));
        assertFalse(Dim.isBootCompleted("android.intent.action.QUICKBOOT_POWERON"));
        assertFalse(Dim.isBootCompleted(null));
    }

    @Test
    public void haekchenSetzenLaesstEinStehen() {
        // An install that only knew the first version: the switch is stored by the markers alone, no dim_on.
        Prefs prefs = new Prefs();
        prefs.values.put(DimStore.ON_BOOT_KEY, 5);
        prefs.values.put(DimStore.ON_RUN_KEY, 1000L);
        assertTrue(DimStore.isOn(prefs, 5, 1000L));
        DimStore.saveKeep(prefs, true, 5, 1000L);
        assertTrue("still on after ticking", DimStore.isOn(prefs, 5, 1000L));
        assertTrue("and after a restart", DimStore.isOn(prefs, 6, 2000L));
    }

    @Test
    public void haekchenSetzenBeiAusLaesstAusAuchMitAltemDimOn() {
        Prefs prefs = new Prefs();
        DimStore.saveOn(prefs, 5, 1000L);   // dim_on is left over from before the restart
        assertFalse(DimStore.isOn(prefs, 6, 2000L));
        DimStore.saveKeep(prefs, true, 6, 2000L);
        assertFalse("ticking must not switch on", DimStore.isOn(prefs, 6, 2000L));
        assertFalse(DimStore.shouldStartAtBoot(prefs));
    }

    @Test
    public void abwaehlenDesHakensNachNeustartLaesstEinStehen() {
        Prefs prefs = new Prefs();
        DimStore.saveKeep(prefs, true, 5, 1000L);
        DimStore.saveOn(prefs, 5, 1000L);
        // After a restart the switch is on through the tick only; unticking must not turn it off.
        assertTrue(DimStore.isOn(prefs, 6, 2000L));
        assertEquals(DimStore.Result.OK, DimStore.saveKeep(prefs, false, 6, 2000L));
        assertTrue(DimStore.isOn(prefs, 6, 2000L));
        assertFalse("the next restart is a plain one again", DimStore.isOn(prefs, 7, 3000L));
    }

    @Test
    public void abwaehlenOhneStartzaehlerSchaltetAusMitGrund() {
        Prefs prefs = new Prefs();
        DimStore.saveKeep(prefs, true, 5, 1000L);
        DimStore.saveOn(prefs, 5, 1000L);
        assertEquals(DimStore.Result.BOOT_UNKNOWN, DimStore.saveKeep(prefs, false, -1, 2000L));
        assertFalse(DimStore.isOn(prefs, -1, 2000L));
        assertEquals(DimStore.ERROR_BOOT_UNKNOWN, DimStore.error(prefs));
    }

    @Test
    public void oeffnenStartetDenDienstNurWennMerkenEinUndNichtLaufend() {
        assertTrue(Dim.shouldRestartOnOpen(true, true, false));
        assertFalse(Dim.shouldRestartOnOpen(true, true, true));
        assertFalse(Dim.shouldRestartOnOpen(false, true, false));
        assertFalse(Dim.shouldRestartOnOpen(true, false, false));
    }

    @Test
    public void merkenSchluesselBleibenBeiThemeZuruecksetzen() {
        Prefs prefs = new Prefs();
        DimStore.saveKeep(prefs, true, 5, 1000L);
        DimStore.saveOn(prefs, 5, 1000L);
        Config.removeThemeChoices(prefs.edit()).apply();
        assertTrue(DimStore.keep(prefs));
        assertTrue(DimStore.shouldStartAtBoot(prefs));
        assertFalse(Config.hasOnTvChoices(prefs));
    }

    // ---- status line: a failure reason ----

    @Test
    public void bewusstAusDurchNutzerLoeschtDenFehlergrund() {
        Prefs prefs = new Prefs();
        DimStore.saveOff(prefs, DimStore.ERROR_SERVICE);
        assertEquals(DimStore.ERROR_SERVICE, DimStore.error(prefs));
        DimStore.saveOffByUser(prefs);
        assertNull(DimStore.error(prefs));
    }

    @Test
    public void fehlergrundBleibtStehenSolangeAusWegenDesFehlers() {
        Prefs prefs = new Prefs();
        DimStore.saveValue(prefs, 37);
        DimStore.saveOff(prefs, DimStore.ERROR_SERVICE);
        assertEquals(DimStore.ERROR_SERVICE, DimStore.error(prefs));
        DimStore.afterScreenOff(prefs);
        assertEquals("standby is not the user switching off", DimStore.ERROR_SERVICE, DimStore.error(prefs));
        assertEquals(37, DimStore.value(prefs));
    }

    @Test
    public void fehlergrundWirdNurBeiAusGezeigt() {
        Prefs prefs = new Prefs();
        DimStore.saveOff(prefs, DimStore.ERROR_SERVICE);
        assertEquals(DimStore.ERROR_SERVICE, DimStore.shownError(prefs, false));
        assertNull(DimStore.shownError(prefs, true));
    }

    // ---- neighbour: the theme code leaves dimming alone ----

    @Test
    public void themeZuruecksetzenLaesstDenAbdunkelWertStehen() {
        Prefs prefs = new Prefs();
        DimStore.saveValue(prefs, 37);
        DimStore.saveOn(prefs, 5, 1000L);
        Config.removeThemeChoices(prefs.edit()).apply();
        assertEquals(37, DimStore.value(prefs));
        assertTrue(DimStore.isOn(prefs, 5, 1000L));
    }

    @Test
    public void abdunkelnZaehltNichtAlsGeraetewahl() {
        Prefs prefs = new Prefs();
        DimStore.saveValue(prefs, 37);
        DimStore.saveOn(prefs, 5, 1000L);
        assertFalse(Config.hasOnTvChoices(prefs));
        prefs.edit().putInt(Config.DOCK_OPACITY_KEY, 70).apply();
        assertTrue("the check still sees a real on-TV choice", Config.hasOnTvChoices(prefs));
    }
}
