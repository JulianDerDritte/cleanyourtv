package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** list_mode: which row Up and Down reach, where focus returns to, the shadow values and the values the mode forces. */
public class ListModeTest {

    // ---- navigation ----

    @Test
    public void upMovesExactlyOneRow() {
        assertEquals(4, ListMode.step(5, 10, true));
        assertEquals(0, ListMode.step(1, 10, true));
    }

    @Test
    public void downStopsAtTheEndInsteadOfLeavingTheList() {
        assertEquals(ListMode.END, ListMode.step(9, 10, false));
        assertEquals(ListMode.END, ListMode.step(0, 1, false));
        assertEquals(9, ListMode.step(8, 10, false));
        assertEquals(4, ListMode.step(3, 10, false));
    }

    @Test
    public void upFromTheFirstRowLeavesForTheTopBar() {
        assertEquals(ListMode.TOP_BAR, ListMode.step(0, 10, true));
        assertEquals(0, ListMode.step(1, 10, true));
    }

    // ---- where focus returns to ----

    @Test
    public void returnFocusGoesToTheAppThatWasOpened() {
        assertEquals(2, ListMode.returnTo(2, 5, 8));
        assertEquals(2, ListMode.returnTo(2, 2, 8));   // position and slot agree: nothing changes
    }

    @Test
    public void returnFocusKeepsASlotInsideTheListWhenTheAppIsGone() {
        assertEquals(4, ListMode.returnTo(-1, 9, 5));
        assertEquals(3, ListMode.returnTo(-1, 3, 5));
        assertEquals(0, ListMode.returnTo(-1, -2, 5));
    }

    @Test
    public void returnFocusInAnEmptyListIsZero() {
        assertEquals(0, ListMode.returnTo(-1, 3, 0));
    }

    // ---- the shadow ----

    @Test
    public void shadowMarginCoversBlurDropAndTwoPixels() {
        assertEquals(10, ListMode.shadowMarginPx(6, 2));
    }

    @Test
    public void shadowMarginOfTheShippedValuesIs10() {
        assertEquals(10, ListMode.shadowMarginPx(ListMode.SHADOW_RADIUS_PX, ListMode.SHADOW_DY_PX));
    }

    @Test
    public void shadowBitmapOfASquareSymbolTakes33856Bytes() {
        assertEquals(33_856L, ListMode.shadowBitmapBytes(72, 72, 10));
    }

    @Test
    public void shadowBitmapOfThePrimeSymbolTakes20880Bytes() {
        assertEquals(20_880L, ListMode.shadowBitmapBytes(96, 25, 10));
    }

    @Test
    public void shadowsOfTheFourSymbolsAndTheTwoButtonSignsTake155216Bytes() {
        int m = ListMode.shadowMarginPx(ListMode.SHADOW_RADIUS_PX, ListMode.SHADOW_DY_PX);
        long total = 3 * ListMode.shadowBitmapBytes(72, 72, m) + ListMode.shadowBitmapBytes(96, 25, m)
                + 2 * ListMode.shadowBitmapBytes(44, 44, m);
        assertEquals(155_216L, total);
    }

    @Test
    public void shadowIsBlackAtAbout80Percent() {
        assertEquals(0xCC000000, ListMode.SHADOW_COLOR);
    }

    @Test
    public void shadowDropsBelowTheShape() {
        assertTrue(ListMode.SHADOW_DY_PX > 0);
        assertEquals(6, ListMode.SHADOW_RADIUS_PX);
    }

    @Test
    public void aNameWithASymbolEndsAtX640() {
        assertEquals(640, (ListMode.ROOT_PADDING_DP + ListMode.NAME_START_DP + ListMode.nameMaxWidthDp(true)) * 2);
    }

    @Test
    public void aNameWithoutASymbolEndsAtX640() {
        assertEquals(640, (ListMode.ROOT_PADDING_DP + ListMode.nameMaxWidthDp(false)) * 2);
    }

    // ---- the values the mode forces ----

    private static ListMode.Layout allDifferent(boolean animations) {
        return new ListMode.Layout(5, 2, true, true, true, true, true, true, true, true, true, true, animations);
    }

    @Test
    public void switchOffLeavesEveryLayoutValueAsGiven() {
        ListMode.Layout in = allDifferent(true);
        ListMode.Layout out = ListMode.apply(false, in);
        assertSame(in, out);
        assertEquals(5, out.columns);
        assertEquals(2, out.rowsBelowDock);
        assertTrue(out.dockRow && out.dockTitle && out.singleRow && out.ambientGlow && out.appGlow && out.cinematic
                && out.uniformTiles && out.fadeRows && out.alwaysShowNames && out.advancedEffects && out.animations);
    }

    @Test
    public void switchOnForcesTheTwelveValues() {
        ListMode.Layout out = ListMode.apply(true, allDifferent(true));
        assertEquals(1, out.columns);
        assertEquals(0, out.rowsBelowDock);
        assertFalse(out.dockRow);
        assertFalse(out.dockTitle);
        assertFalse(out.singleRow);
        assertFalse(out.ambientGlow);
        assertFalse(out.appGlow);
        assertFalse(out.cinematic);
        assertFalse(out.uniformTiles);
        assertFalse(out.fadeRows);
        assertFalse(out.alwaysShowNames);
        assertFalse(out.advancedEffects);
    }

    @Test
    public void switchOnLeavesTheThirteenthFieldAlone() {
        assertTrue(ListMode.apply(true, allDifferent(true)).animations);
        assertFalse(ListMode.apply(true, allDifferent(false)).animations);
    }

    // ---- the white symbols and the name ----

    @Test
    public void symbolForNetflix() {
        assertEquals(R.drawable.ic_list_netflix, ListMode.symbolFor("com.netflix.ninja"));
    }

    @Test
    public void symbolForYouTube() {
        assertEquals(R.drawable.ic_list_youtube, ListMode.symbolFor("com.google.android.youtube.tv"));
    }

    @Test
    public void tagesschauHasNoSymbolSoItGetsAMonogram() {
        assertEquals(0, ListMode.symbolFor("de.tagesschau"));
        assertEquals("T", ListMode.monogramLetter("Tagesschau", "de.tagesschau"));
    }

    @Test
    public void symbolForPrimeVideo() {
        assertEquals(R.drawable.ic_list_primevideo, ListMode.symbolFor("com.amazon.amazonvideo.livingroom"));
    }

    @Test
    public void symbolForUnknownPackageIsZero() {
        assertEquals(0, ListMode.symbolFor("org.example.unknown"));
    }

    @Test
    public void symbolForNoPackageIsZero() {
        assertEquals(0, ListMode.symbolFor(null));
    }

    // ---- the 15 logos and the monogram for every other app ----

    private static final String[] FIFTEEN = {
            "com.netflix.ninja", "com.google.android.youtube.tv", "com.amazon.amazonvideo.livingroom",
            "com.crunchyroll.crunchyroid", "com.spotify.tv.android", "com.zdf.android.mediathek",
            "com.valvesoftware.steamlink", "com.ted.android.tv", "com.android.vending", "de.telekom.magentatv.atv",
            "de.swr.avp.ard.tv", "tv.arte.plus7", "com.disney.disneyplus", "de.sky.online",
            "de.prosiebensat1digital.seventv"};

    @Test
    public void everyPackageOfTHasASymbol() {
        assertEquals(15, FIFTEEN.length);
        for (String pkg : FIFTEEN) assertTrue(pkg, ListMode.symbolFor(pkg) != 0);
    }

    @Test
    public void theFifteenSymbolsAreAllDifferent() {
        java.util.Set<Integer> ids = new java.util.HashSet<>();
        for (String pkg : FIFTEEN) assertTrue(pkg, ids.add(ListMode.symbolFor(pkg)));
    }

    @Test
    public void aSonyAppHasNoLogoSoItGetsAMonogram() {
        assertEquals(0, ListMode.symbolFor("com.sony.dtv.timers"));
        assertEquals(0, ListMode.symbolFor("com.disney.disneyplus.fake"));
    }

    @Test
    public void everyListRowHasASymbolWhetherLogoOrMonogram() {
        assertTrue(ListMode.listRowHasSymbol(0));
        assertTrue(ListMode.listRowHasSymbol(ListMode.symbolFor("com.netflix.ninja")));
    }

    @Test
    public void aMonogramRowShowsItsNameOnlyWithFocus() {
        assertFalse(ListMode.nameVisible(ListMode.listRowHasSymbol(0), false));
        assertTrue(ListMode.nameVisible(ListMode.listRowHasSymbol(0), true));
    }

    @Test
    public void theMonogramLetterIsTheFirstLetterOrDigitOfTheName() {
        assertEquals("A", ListMode.monogramLetter("ARD Mediathek", "de.example.a"));
        assertEquals("7", ListMode.monogramLetter("7TV", "de.example.b"));
        assertEquals("Ü", ListMode.monogramLetter("über", "de.example.c"));
        assertEquals("Z", ListMode.monogramLetter("zeta", "de.example.d"));
    }

    @Test
    public void aNameWithoutLetterOrDigitTakesTheFirstLetterOfThePackage() {
        assertEquals("O", ListMode.monogramLetter("+ -", "org.example.x"));
        assertEquals("X", ListMode.monogramLetter("", "_x.y"));
        assertEquals("B", ListMode.monogramLetter("!!", "_1.b"));
    }

    @Test
    public void aPackageWithoutAnyLetterShowsTheFallbackMark() {
        assertEquals(ListMode.MONOGRAM_FALLBACK, ListMode.monogramLetter("", "_1._2"));
    }

    @Test
    public void nameHidesWithoutFocusWhenThereIsASymbol() {
        assertFalse(ListMode.nameVisible(true, false));
    }

    @Test
    public void nameShowsWithFocusWhenThereIsASymbol() {
        assertTrue(ListMode.nameVisible(true, true));
    }

    @Test
    public void nameShowsWithoutFocusWhenThereIsNoSymbol() {
        assertTrue(ListMode.nameVisible(false, false));
    }

    @Test
    public void nameStartsAtTwoHundredFortyPixels() {
        assertEquals(240, (ListMode.ROOT_PADDING_DP + ListMode.NAME_START_DP) * 2);
    }

    @Test
    public void nameWithoutSymbolStartsAtTheClockEdge() {
        assertEquals(112, ListMode.ROOT_PADDING_DP * 2);
    }

    @Test
    public void rowPitchIs128Pixels() {
        assertEquals(128, (ListMode.CARD_DP + 2 * ListMode.ROW_PAD_DP) * 2);
    }

    @Test
    public void symbolFieldIs96Wide() {
        assertEquals(96, ListMode.ICON_W_DP * 2);
    }

    @Test
    public void symbolFieldIs72High() {
        assertEquals(72, ListMode.ICON_H_DP * 2);
    }

    @Test
    public void symbolFieldFillsTheCard() {
        assertEquals(ListMode.CARD_DP, ListMode.ICON_H_DP);
    }

    @Test
    public void nameSizeIs44Pixels() {
        assertEquals(44, ListMode.NAME_SP * 2);
    }

    @Test
    public void lastSymbolEdgeIs996WithTheShippedPadding() {
        assertEquals(996.0, ListMode.smileBottomPx(ListMode.BOTTOM_PAD_DP), 0.01);
    }

    @Test
    public void lastSymbolEdgeMatchesTheClockDistance() {
        assertTrue(ListMode.bottomEdgeMatchesClock(ListMode.BOTTOM_PAD_DP));
    }

    @Test
    public void paddingOf17IsStillAllowed() {
        assertTrue(ListMode.bottomEdgeMatchesClock(17));   // 992: 4 px off
    }

    @Test
    public void paddingOf18IsRefused() {
        assertFalse(ListMode.bottomEdgeMatchesClock(18));   // 990: 6 px off
    }

    @Test
    public void paddingOf20IsRefused() {
        assertFalse(ListMode.bottomEdgeMatchesClock(20));   // 986: 10 px off
    }

    @Test
    public void gapBetweenTheTwoButtonsIs40Pixels() {
        assertEquals(40, ListMode.HEADER_GAP_DP * 2);
    }

    // ---- the top bar the mode forces ----

    private static ListMode.Header headerAllDifferent() {
        return new ListMode.Header(true, true, true, true, true, true, false, false, true,
                java.util.Arrays.asList("com.example.one", "com.example.two"));
    }

    @Test
    public void headerSwitchOffLeavesEveryValueAsGiven() {
        ListMode.Header in = headerAllDifferent();
        assertSame(in, ListMode.applyHeader(false, in));
    }

    @Test
    public void headerSwitchOffKeepsTheTopMenu() {
        assertTrue(ListMode.applyHeader(false, headerAllDifferent()).topMenu);
    }

    @Test
    public void headerSwitchOnHidesTheInputsPill() {
        assertFalse(ListMode.applyHeader(true, headerAllDifferent()).showInputsPill);
    }

    @Test
    public void headerSwitchOnHidesTheNetworkPill() {
        assertFalse(ListMode.applyHeader(true, headerAllDifferent()).showNetworkPill);
    }

    @Test
    public void headerSwitchOnTurnsTheTopMenuOff() {
        assertFalse(ListMode.applyHeader(true, headerAllDifferent()).topMenu);
    }

    @Test
    public void headerSwitchOnLetsTheGearOpenTheTvSettings() {
        assertFalse(ListMode.applyHeader(true, headerAllDifferent()).launcherSettingsButton);
    }

    @Test
    public void headerSwitchOnHidesTheReorderButton() {
        assertFalse(ListMode.applyHeader(true, headerAllDifferent()).reorderButton);
    }

    @Test
    public void headerSwitchOnHidesTheVpnPill() {
        assertFalse(ListMode.applyHeader(true, headerAllDifferent()).vpnPill);
    }

    @Test
    public void headerSwitchOnMakesTheButtonsRound() {
        assertTrue(ListMode.applyHeader(true, headerAllDifferent()).iconOnlyButtons);
    }

    @Test
    public void headerSwitchOnShowsTheLibraryButton() {
        assertTrue(ListMode.applyHeader(true, headerAllDifferent()).libraryButton);
    }

    @Test
    public void headerSwitchOnDropsTheLibraryRowAfterTheList() {
        assertFalse(ListMode.applyHeader(true, headerAllDifferent()).showLibraryPill);
    }

    @Test
    public void headerSwitchOnEmptiesTheTopBarApps() {
        assertTrue(ListMode.applyHeader(true, headerAllDifferent()).topBarApps.isEmpty());
    }

    // ---- back to the gear ----

    private static final int GEAR = 11, LIBRARY = 12, ROW = 13;

    @Test
    public void returnGoesBackToTheTopButtonThatHadFocus() {
        assertEquals(ListMode.HEADER, ListMode.returnTo(GEAR, 2, 2, 5));
    }

    @Test
    public void returnWithoutATopButtonGoesToTheRow() {
        assertEquals(2, ListMode.returnTo(0, 2, 2, 5));
    }

    @Test
    public void focusOnTheGearIsRemembered() {
        assertEquals(GEAR, ListMode.headerFocusAfter(GEAR, GEAR, LIBRARY));
    }

    @Test
    public void focusOnTheLibraryButtonIsRemembered() {
        assertEquals(LIBRARY, ListMode.headerFocusAfter(LIBRARY, GEAR, LIBRARY));
    }

    @Test
    public void focusOnARowIsNotRemembered() {
        assertEquals(0, ListMode.headerFocusAfter(ROW, GEAR, LIBRARY));
    }
}
