package com.gathin.tvhome;

import java.util.ArrayList;
import java.util.List;

/** The rows of the tile menu (hold OK on a tile) and their order; a JVM test pins the rule, MainActivity builds the views. */
final class TileMenu {
    private TileMenu() { }

    /** Replace the app in the dock, or add it (dock_row only). */
    static final int DOCK = 1;
    /** Move the app (the order of Home). */
    static final int MOVE = 2;
    /** Take the app off Home (app library). */
    static final int REMOVE = 3;
    /** The system's App info page. */
    static final int INFO = 4;

    /**
     * The rows, first to last: the dock row when the tile is movable and there is a dock, Move when the tile is
     * movable (not the "Default home" tile), Remove when the app library is on, App info when the firmware has the page.
     */
    static int[] rows(boolean dockRow, boolean movable, boolean libraryOn, boolean infoAvailable) {
        List<Integer> rows = new ArrayList<>();
        if (dockRow && movable) rows.add(DOCK);
        if (movable) rows.add(MOVE);
        if (libraryOn) rows.add(REMOVE);
        if (infoAvailable) rows.add(INFO);
        int[] out = new int[rows.size()];
        for (int i = 0; i < out.length; i++) out[i] = rows.get(i);
        return out;
    }
}
