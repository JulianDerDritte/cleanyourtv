package com.gathin.tvhome;

import java.util.List;

/**
 * The wallpaper window of list_mode (WallpaperTiles): everything that can be decided without a TV, so a JVM test can
 * pin it. Sizes in px are those of a 1920 x 1080 screen at density 2.0; the window converts its dp values.
 */
final class WallpaperGrid {
    private WallpaperGrid() { }

    static final int COLUMNS = 3;

    /** The window: x 300 to 1620, y 60 to 1020 px, 36 px padding left and right, so the grid is 1248 px wide. */
    static final int WINDOW_X_DP = 150;
    static final int WINDOW_Y_DP = 30;
    static final int WINDOW_WIDTH_DP = 660;
    static final int WINDOW_HEIGHT_DP = 480;
    static final int PADDING_DP = 18;
    /** Air between two tiles; each cell holds its tile with half of it on every side, room for the focus ring. */
    static final int GAP_DP = 12;
    /** Padding above and below the grid, so the focus ring is not cut off (the ring reaches 8 px past the tile). */
    static final int GRID_PADDING_DP = 6;

    /** Directions for {@link #step}. */
    static final int UP = 0, DOWN = 1, LEFT = 2, RIGHT = 3;
    /** What {@link #step} answers for Up in the first row: the focus leaves for the tick "Random abspielen" above. */
    static final int TOGGLE = -1;

    /** The decoded previews are RGB_565 (2 bytes a pixel); the cache holds at most this many bytes of them. */
    static final int BYTES_PER_PIXEL = 2;
    static final int CACHE_BYTES = 6 * 1024 * 1024;

    /** A cell is its tile plus the air between tiles: 392 + 24 = 416 px. */
    static int cellWidthPx(int tileWidthPx, int gapPx) {
        return tileWidthPx + gapPx;
    }

    /** The tile's width when {@code columns} cells share the grid: 1248 px, 3 columns, 24 px gap give 392 px. */
    static int tileWidthPx(int gridWidthPx, int columns, int gapPx) {
        return (gridWidthPx - columns * gapPx) / columns;
    }

    /** 16:9, rounded: 392 px wide gives 221 px (220.5). */
    static int tileHeightPx(int tileWidthPx) {
        return Math.round(tileWidthPx * 9f / 16f);
    }

    /**
     * BitmapFactory's inSampleSize for a picture of this size shown in a tile of that size: the largest power of two
     * that keeps both sides at or above the tile's, never below 1. 1920 x 1080 into 392 x 221 gives 4.
     */
    static int sampleSize(int srcWidth, int srcHeight, int dstWidth, int dstHeight) {
        int sample = 1;
        while (srcWidth / (sample * 2) >= dstWidth && srcHeight / (sample * 2) >= dstHeight) sample *= 2;
        return sample;
    }

    /** Bytes a decoded preview of this size takes: 480 x 270 gives 259,200. */
    static int bitmapBytes(int width, int height) {
        return width * height * BYTES_PER_PIXEL;
    }

    /**
     * The tile after a direction key: one tile, or the same one at an edge. Up in the first row is {@link #TOGGLE};
     * Down from the row above a short last row lands on its last tile; Left and Right never leave their row.
     */
    static int step(int position, int count, int columns, int direction) {
        switch (direction) {
            case UP:
                if (position - columns < 0) return TOGGLE;
                return position - columns;
            case DOWN:
                if (position / columns >= (count - 1) / columns) return position;
                return Math.min(position + columns, count - 1);
            case LEFT:
                if (position % columns == 0) return position;
                return position - 1;
            case RIGHT:
                if (position % columns == columns - 1) return position;
                if (position >= count - 1) return position;
                return position + 1;
            default:
                return position;
        }
    }

    /** The tile Down from the tick lands on: the one in the first row, in the column Up left from. */
    static int afterToggle(int column, int count) {
        return Math.min(column, count - 1);
    }

    /** The tile that has focus when the window opens: the chosen picture, else (no choice, "none", a file gone) the first. */
    static int startIndex(List<String> names, String choice) {
        int at = names.indexOf(choice);
        return at < 0 ? 0 : at;
    }
}
