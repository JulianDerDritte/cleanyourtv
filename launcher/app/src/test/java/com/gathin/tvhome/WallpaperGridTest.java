package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** list_mode's Wallpaper window: tile and cell sizes, preview shrinking, the cache, the keys and where focus starts. */
public class WallpaperGridTest {

    // ---- sizes ----

    @Test
    public void gridIs1248Wide() {
        assertEquals(1248, (WallpaperGrid.WINDOW_WIDTH_DP - 2 * WallpaperGrid.PADDING_DP) * 2);
    }

    @Test
    public void tileIs392Wide() {
        assertEquals(392, WallpaperGrid.tileWidthPx(1248, 3, 24));
    }

    @Test
    public void tileIs221High() {
        assertEquals(221, WallpaperGrid.tileHeightPx(392));
    }

    @Test
    public void cellIs416Wide() {
        assertEquals(416, WallpaperGrid.cellWidthPx(392, 24));
    }

    @Test
    public void cellOfAWiderGapIs418() {
        assertEquals(418, WallpaperGrid.cellWidthPx(392, 26));
    }

    @Test
    public void rowsAre245Apart() {
        assertEquals(245, WallpaperGrid.cellWidthPx(WallpaperGrid.tileHeightPx(392), 24));
    }

    @Test
    public void windowIsAtTheDraftsPlace() {
        assertEquals(300, WallpaperGrid.WINDOW_X_DP * 2);
        assertEquals(1620, (WallpaperGrid.WINDOW_X_DP + WallpaperGrid.WINDOW_WIDTH_DP) * 2);
    }

    @Test
    public void firstTileOfARowStartsAt348() {
        assertEquals(348, WallpaperGrid.WINDOW_X_DP * 2 + WallpaperGrid.PADDING_DP * 2 + WallpaperGrid.GAP_DP);
    }

    // ---- previews ----

    @Test
    public void fullHdIsShrunkByFour() {
        assertEquals(4, WallpaperGrid.sampleSize(1920, 1080, 392, 221));
    }

    @Test
    public void exactlyDoubleTheTileIsShrunkByTwo() {
        assertEquals(2, WallpaperGrid.sampleSize(784, 442, 392, 221));
    }

    @Test
    public void hdReadyIsShrunkByTwo() {
        assertEquals(2, WallpaperGrid.sampleSize(1280, 720, 392, 221));
    }

    @Test
    public void aSmallPictureIsNotShrunk() {
        assertEquals(1, WallpaperGrid.sampleSize(390, 220, 392, 221));
    }

    @Test
    public void aPreviewTakes259200Bytes() {
        assertEquals(259_200, WallpaperGrid.bitmapBytes(480, 270));
    }

    @Test
    public void cacheHoldsTwentyFourPreviews() {
        assertTrue(24 * 259_200 <= WallpaperGrid.CACHE_BYTES);
    }

    @Test
    public void cacheDoesNotHoldTwentyFivePreviews() {
        assertTrue(25 * 259_200 > WallpaperGrid.CACHE_BYTES);
    }

    // ---- keys: one tile per key, the focus stays at every edge ----

    @Test
    public void upMovesOneRow() {
        assertEquals(1, WallpaperGrid.step(4, 33, 3, WallpaperGrid.UP));
    }

    @Test
    public void upInTheFirstRowLeavesForTheTick() {
        assertEquals(WallpaperGrid.TOGGLE, WallpaperGrid.step(1, 33, 3, WallpaperGrid.UP));
    }

    @Test
    public void upFromTheLastTileOfTheFirstRowLeavesForTheTick() {
        assertEquals(WallpaperGrid.TOGGLE, WallpaperGrid.step(2, 17, 3, WallpaperGrid.UP));
    }

    @Test
    public void upFromTheSecondRowDoesNotReachTheTick() {
        assertEquals(0, WallpaperGrid.step(3, 17, 3, WallpaperGrid.UP));
    }

    @Test
    public void downFromTheTickGoesToTheSameColumn() {
        assertEquals(2, WallpaperGrid.afterToggle(2, 17));
    }

    @Test
    public void downFromTheTickStaysInsideAShortList() {
        assertEquals(1, WallpaperGrid.afterToggle(2, 2));
    }

    @Test
    public void downMovesOneRow() {
        assertEquals(7, WallpaperGrid.step(4, 33, 3, WallpaperGrid.DOWN));
    }

    @Test
    public void downStaysInTheLastRow() {
        assertEquals(30, WallpaperGrid.step(30, 33, 3, WallpaperGrid.DOWN));
    }

    @Test
    public void downFromTheRowAboveAShortLastRowLandsOnTheLastTile() {
        assertEquals(31, WallpaperGrid.step(29, 32, 3, WallpaperGrid.DOWN));
    }

    @Test
    public void leftMovesOneTile() {
        assertEquals(3, WallpaperGrid.step(4, 33, 3, WallpaperGrid.LEFT));
    }

    @Test
    public void leftStaysAtTheRowStart() {
        assertEquals(3, WallpaperGrid.step(3, 33, 3, WallpaperGrid.LEFT));
    }

    @Test
    public void rightMovesOneTile() {
        assertEquals(5, WallpaperGrid.step(4, 33, 3, WallpaperGrid.RIGHT));
    }

    @Test
    public void rightStaysAtTheRowEnd() {
        assertEquals(5, WallpaperGrid.step(5, 33, 3, WallpaperGrid.RIGHT));
    }

    @Test
    public void rightStaysOnTheLastTileOfAShortRow() {
        assertEquals(31, WallpaperGrid.step(31, 32, 3, WallpaperGrid.RIGHT));
    }

    // ---- where focus starts ----

    private static List<String> names(int n) {
        List<String> names = new ArrayList<>();
        for (int i = 0; i < n; i++) names.add("picture-" + i + ".jpg");
        return names;
    }

    @Test
    public void focusStartsOnTheChosenPicture() {
        assertEquals(5, WallpaperGrid.startIndex(names(33), "picture-5.jpg"));
    }

    @Test
    public void focusStartsOnTheFirstTileWithoutAChoice() {
        assertEquals(0, WallpaperGrid.startIndex(names(33), null));
    }

    @Test
    public void focusStartsOnTheFirstTileForNone() {
        assertEquals(0, WallpaperGrid.startIndex(names(33), "none"));
    }

    @Test
    public void focusStartsOnTheFirstTileWhenTheChosenFileIsGone() {
        assertEquals(0, WallpaperGrid.startIndex(Arrays.asList("a.jpg", "b.jpg"), "c.jpg"));
    }
}
