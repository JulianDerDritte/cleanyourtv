package com.gathin.tvhome;

import java.util.ArrayList;
import java.util.List;

/**
 * circle_list: the app list of list_mode as part of a circle. The centre lies left of the screen, the symbols sit on an
 * arc, the selected one is on the right of the arc a little below the screen's middle with its name, the one before
 * and the one after sit above and below it, further left. The list has no ends. Everything that can be decided without
 * a TV lives here (places, transparency, the step, the turn and its timing, which rows are attached), so a JVM test can
 * pin it; {@link CircleLayoutManager} only applies these values to the views. All sizes are in dp (the numbers in the
 * comments are px at density 2.0 on a 1920 x 1080 screen).
 *
 * <p>A row's distance {@code d} from the selection is a signed number of places: 0 is the selected row, +1 the next
 * app, -1 the previous one. It is continuous while the list turns, so a turn is only a number running from the old
 * phase to the new one.
 */
final class CircleList {
    private CircleList() { }

    // ---- the circle (dp) ----------------------------------------------------------------------------------

    /**
     * The circle's centre, left of the screen (x = -226 px). Chosen so that the symbol furthest to the left of the three
     * seen (its drawn glyph starts GLYPH_INSET_DP inside its field) stands flush with the left edge of the clock digits,
     * ListMode.ROOT_PADDING_DP = 56 dp (112 px): field left 50 dp.
     */
    static final float CENTER_X_DP = -113f;
    /**
     * The circle's centre lies this far below the screen's middle (78 px), so that the space between the clock digits and
     * the symbol above the chosen one equals the space between the symbol below it and the bottom edge (235 px each).
     */
    static final float CENTER_Y_OFFSET_DP = 39f;
    /** The bottom edge of the clock digits (156 px), measured on a photo of the TV. */
    static final float CLOCK_BOTTOM_DP = 78f;
    /** A symbol's drawn glyph starts this far inside its 48 dp field (12 px, measured on a photo of the TV). */
    static final float GLYPH_INSET_DP = 6f;
    /** x of the selected symbol's centre is CENTER_X_DP + RADIUS_DP = 160 dp (320 px). */
    static final float RADIUS_DP = 210f;
    /** The angle between two neighbours: 190.7 px apart in height at the radius above (a row of the list is 128 px). */
    static final float STEP_DEGREES = 27f;

    /** The symbol field is ListMode's (96 x 72 px); a row is the field plus the name: 384 dp (768 px) wide. */
    static final float ROW_WIDTH_DP = 384f;
    /** A row's symbol centre sits this far from the row's left edge (half the 48 dp field). */
    static final float SYMBOL_HALF_DP = ListMode.ICON_W_DP / 2f;
    /** The name starts this far behind the symbol's centre: 64 dp from the row's edge minus the half field. */
    static final float NAME_START_FROM_CENTRE_DP = ListMode.NAME_START_DP - SYMBOL_HALF_DP;
    /** A name ends at most this far from where it starts (640 px, 22 sp at 2.0 holds "ARD Mediathek" in 290 px). */
    static final float NAME_MAX_DP = 320f;
    /** The row below the header: nothing a user can see may reach above this (260 px; measured at the TV by the owner). */
    static final float HEADER_MAX_BOTTOM_DP = 130f;

    /** The place of row {@code d} on the arc: x of the symbol's centre in dp. */
    static float centreX(double d) {
        return (float) (CENTER_X_DP + RADIUS_DP * Math.cos(Math.toRadians(d * STEP_DEGREES)));
    }

    /** The circle's centre height in dp for a screen middle at {@code middleY}. */
    static float arcCentreY(float middleY) {
        return middleY + CENTER_Y_OFFSET_DP;
    }

    /** The place of row {@code d} on the arc: y of the symbol's centre in dp, for a screen middle at {@code middleY}. */
    static float centreY(double d, float middleY) {
        return (float) (arcCentreY(middleY) + RADIUS_DP * Math.sin(Math.toRadians(d * STEP_DEGREES)));
    }

    /** The symbol's rectangle (left, top, right, bottom) in dp. */
    static float[] symbolRect(double d, float middleY) {
        float cx = centreX(d), cy = centreY(d, middleY);
        return new float[]{cx - ListMode.ICON_W_DP / 2f, cy - ListMode.ICON_H_DP / 2f,
                cx + ListMode.ICON_W_DP / 2f, cy + ListMode.ICON_H_DP / 2f};
    }

    /** The name's rectangle in dp when it is as long as it may be. */
    static float[] nameRect(double d, float middleY) {
        float cx = centreX(d), cy = centreY(d, middleY);
        float left = cx + NAME_START_FROM_CENTRE_DP;
        return new float[]{left, cy - ListMode.ICON_H_DP / 2f, left + NAME_MAX_DP, cy + ListMode.ICON_H_DP / 2f};
    }

    // ---- which row where -----------------------------------------------------------------------------------

    /**
     * Row {@code p}'s distance from the selection when the list is at {@code phase}, for {@code n} rows. From three rows
     * on it is the signed distance round the ring, in [-n/2, n/2). With two rows the two swap between 0 and +1 along
     * the same arc (a triangle wave), so neither ever runs through -1. 0 for fewer than two rows.
     */
    static double delta(int p, double phase, int n) {
        if (n < 2) return 0;
        double x = p - phase;
        if (n == 2) {
            double e = x - 2 * Math.floor(x / 2);
            return e <= 1 ? e : 2 - e;
        }
        return x - n * Math.floor(x / n + 0.5);
    }

    /**
     * How opaque a symbol at distance {@code d} is: 1 at the selection, 0.6 for the neighbours, fading to 0 at two places,
     * and 0 from there on, so only three symbols are seen. From three rows on the factor is also 0 where the ring wraps
     * round (|d| = n/2), so a row that goes round behind the back never jumps while visible. With two rows the other
     * row stays at 0.6.
     */
    static float alphaFor(double d, int n) {
        if (n < 1) return 0f;
        double a = Math.abs(d);
        if (n == 1) return 1f;
        if (n == 2) return (float) (1 - 0.4 * a);
        double base = a <= 1 ? 1 - 0.4 * a : Math.max(0, 0.6 * (2 - a));
        double wrap = Math.min(1, Math.max(0, (n / 2.0 - a) * 2));
        return (float) (base * wrap);
    }

    /** The name is seen at the selection only: 1 at d = 0, falling to 0 at |d| = 1 and staying there. */
    static float nameAlphaFor(double d) {
        return (float) Math.max(0, 1 - Math.abs(d));
    }

    /** The rows seen at rest when row {@code selected} is chosen: those with some opacity. */
    static int visibleAtRest(int n, int selected) {
        int seen = 0;
        for (int p = 0; p < n; p++) if (alphaFor(delta(p, selected, n), n) > 0f) seen++;
        return seen;
    }

    /**
     * The rows kept attached to the list: every row within three places of the shown phase (the ones beyond two are
     * not seen yet, so they come in unseen) and the row of the target with its two neighbours, so focus can always go
     * to it, even after a jump. All rows when there are five or fewer.
     */
    static int[] attached(double shown, int target, int n) {
        if (n <= 0) return new int[0];
        boolean[] keep = new boolean[n];
        for (int p = 0; p < n; p++) {
            if (n <= 5 || Math.abs(delta(p, shown, n)) <= 3.0) keep[p] = true;
        }
        for (int k = -1; k <= 1; k++) keep[Math.floorMod(target + k, n)] = true;
        int count = 0;
        for (boolean b : keep) if (b) count++;
        int[] out = new int[count];
        int i = 0;
        for (int p = 0; p < n; p++) if (keep[p]) out[i++] = p;
        return out;
    }

    /** The rows that count for the order of the circle: the selected one and two either side, round the ring. */
    static int[] order(int n, int selected) {
        if (n <= 0) return new int[0];
        int[] out = new int[Math.min(5, n)];
        int base = n >= 5 ? -2 : -(out.length / 2);
        for (int i = 0; i < out.length; i++) out[i] = Math.floorMod(selected + base + i, n);
        return out;
    }

    // ---- the step: no ends --------------------------------------------------------------------------------

    /** The row after Up or Down from {@code position}: the neighbour round the ring; it stays for an empty list or one row. */
    static int step(int position, int count, boolean up) {
        if (count <= 1) return 0;
        return Math.floorMod(up ? position - 1 : position + 1, count);
    }

    // ---- moving an app in the order (Nachbesserung 2) ---------------------------------------------------------

    /**
     * Where the carried app goes on Up (-1) or Down (+1) among {@code movable} apps: one place, and nowhere past either
     * end (the order does not wrap, unlike the turn). {@code from} when nothing moves.
     */
    static int moveTarget(int from, int movable, int direction) {
        if (movable < 2 || from < 0 || from >= movable) return from;
        return Math.max(0, Math.min(movable - 1, from + direction));
    }

    /**
     * The moves (from, to), in this order, that bring {@code current} back to {@code before}: Back cancels a move. The
     * lists hold the same keys.
     */
    static List<int[]> restoreMoves(List<String> current, List<String> before) {
        List<String> work = new ArrayList<>(current);
        List<int[]> moves = new ArrayList<>();
        for (int i = 0; i < before.size(); i++) {
            int at = work.indexOf(before.get(i));
            if (at >= 0 && at != i) {
                moves.add(new int[]{at, i});
                work.add(i, work.remove(at));
            }
        }
        return moves;
    }

    // ---- the turn -------------------------------------------------------------------------------------------

    /** A turn takes this long (ListMode's focus animation of 220 ms). */
    static final int DURATION_MS = 220;
    /** The shown phase never moves faster than this: 0.5 places in a frame of 16 ms. */
    static final float MAX_STEP_PER_MS = 0.03125f;
    /** A press is dropped while the target is already more than this many places ahead of the shown phase. */
    static final float MAX_LEAD = 4f;

    /** The time a turn takes: 0 without animations (it is instant), the reduced-motion limit, else 220 ms. */
    static int durationMs(boolean animations, boolean reducedMotion) {
        if (!animations) return 0;
        return reducedMotion ? Motion.REDUCED_MS : DURATION_MS;
    }

    /** The turn's path from the start phase to the target for {@code fraction} of its time (0 start, 1 end). */
    static double phaseAt(double start, double target, double fraction, boolean reduced) {
        if (fraction <= 0) return start;
        if (fraction >= 1) return target;
        // DecelerateInterpolator(1.5) is 1 - (1 - x)^3; the reduced one (factor 1) is 1 - (1 - x)^2.
        double left = 1 - fraction;
        double eased = reduced ? 1 - left * left : 1 - left * left * left;
        return start + (target - start) * eased;
    }

    /**
     * The turn of the list over time: a target (a whole number, not wrapped) and the phase shown. Every press starts
     * a new run at the phase shown, so nothing jumps back; the shown phase follows the run at most
     * {@link #MAX_STEP_PER_MS} a millisecond; a press is dropped while the target leads the shown phase by more than
     * {@link #MAX_LEAD} places, which keeps the end of a held key near the turn's own length.
     */
    static final class Turner {
        private final int durationMs;
        private final boolean reduced;
        private int target;
        private double shown;
        private double runStart;
        private double elapsed;

        Turner(int durationMs) {
            this.durationMs = durationMs;
            this.reduced = durationMs <= Motion.REDUCED_MS;
        }

        int target() { return target; }

        double shown() { return shown; }

        boolean running() { return shown != target; }

        /** The selection takes this place at once, with no turn. */
        void jump(int to) {
            target = to;
            shown = to;
            runStart = to;
            elapsed = 0;
        }

        /** One place up (-1) or down (+1). False when the press was dropped. */
        boolean press(int direction) {
            double lead = (target + direction) - shown;
            if (Math.abs(lead) > MAX_LEAD && lead * direction > 0) return false;
            target += direction;
            if (durationMs <= 0) {
                shown = target;
                return true;
            }
            runStart = shown;
            elapsed = 0;
            return true;
        }

        /** Moves time on by {@code dtMs}. */
        void tick(float dtMs) {
            if (shown == target) return;
            elapsed += dtMs;
            double wanted = durationMs <= 0 ? target
                    : phaseAt(runStart, target, elapsed / durationMs, reduced);
            double most = MAX_STEP_PER_MS * dtMs;
            double move = wanted - shown;
            if (Math.abs(move) > most) move = Math.signum(move) * most;
            shown += move;
            if (Math.abs(target - shown) < 1e-9) shown = target;
        }
    }
}
