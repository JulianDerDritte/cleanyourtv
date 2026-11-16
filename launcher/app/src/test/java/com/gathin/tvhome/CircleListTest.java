package com.gathin.tvhome;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * circle_list: where the rows sit on the arc, how many are seen, the step without ends, the turn and its timing, the rows
 * kept attached, and the switch. All in dp on a 1920 x 1080 screen at density 2.0 (960 x 540 dp), so the screen's middle
 * is at 270 dp.
 */
public class CircleListTest {
    private static final float MIDDLE = 270f;
    private static final float SCREEN_W = 960f;
    private static final float SCREEN_H = 540f;
    private static final int[] SIZES = {1, 2, 3, 4, 17};

    // ---- the switch ----

    @Test
    public void circleListIsActiveOnlyTogetherWithListMode() {
        assertFalse(ListMode.circleActive(false, false));
        assertFalse(ListMode.circleActive(true, false));   // list_mode alone: the straight list
        assertFalse(ListMode.circleActive(false, true));   // circle_list alone: the tiles, as today
        assertTrue(ListMode.circleActive(true, true));
    }

    // ---- the arc ----

    private static double distanceFromCentre(double d) {
        double dx = CircleList.centreX(d) - CircleList.CENTER_X_DP;
        double dy = CircleList.centreY(d, MIDDLE) - CircleList.arcCentreY(MIDDLE);
        return Math.sqrt(dx * dx + dy * dy);
    }

    @Test
    public void centresLieOnOneCircleForEveryListSize() {
        for (int n : SIZES) {
            for (int selected = 0; selected < n; selected++) {
                // The radius is the selected symbol's own distance, not a number written here: the radius may change.
                double radius = distanceFromCentre(CircleList.delta(selected, selected, n));
                for (int p = 0; p < n; p++) {
                    double d = CircleList.delta(p, selected, n);
                    assertEquals("n=" + n + " selected=" + selected + " row=" + p, radius, distanceFromCentre(d), 0.25);
                }
            }
        }
    }

    @Test
    public void circleCentreIsLeftOfTheScreen() {
        assertTrue(CircleList.CENTER_X_DP < 0);
        assertEquals(CircleList.CENTER_X_DP + CircleList.RADIUS_DP, CircleList.centreX(0), 0.001);
    }

    @Test
    public void selectedSitsRightmostAndNeighboursLeftOfIt() {
        for (int n : SIZES) {
            int selected = n / 2;
            double dSelected = CircleList.delta(selected, selected, n);
            // The chosen symbol sits at the circle's centre height, a little below the screen's middle (K11).
            assertEquals("n=" + n, CircleList.arcCentreY(MIDDLE), CircleList.centreY(dSelected, MIDDLE), 0.001);
            for (int p = 0; p < n; p++) {
                if (p == selected) continue;
                double d = CircleList.delta(p, selected, n);
                assertTrue("n=" + n + " row " + p + " is left of the selected one",
                        CircleList.centreX(d) < CircleList.centreX(dSelected));
            }
        }
    }

    @Test
    public void neighboursAreSymmetricAboveAndBelowForThreeOrMore() {
        for (int n : new int[]{3, 4, 17}) {
            int selected = 1;
            float above = CircleList.centreY(CircleList.delta(0, selected, n), MIDDLE);
            float below = CircleList.centreY(CircleList.delta(2, selected, n), MIDDLE);
            float arcY = CircleList.arcCentreY(MIDDLE);
            assertTrue("n=" + n + " the previous app is above", above < arcY);
            assertTrue("n=" + n + " the next app is below", below > arcY);
            assertEquals("n=" + n, arcY - above, below - arcY, 0.001);
            assertEquals("n=" + n + " same x on both sides",
                    CircleList.centreX(CircleList.delta(0, selected, n)),
                    CircleList.centreX(CircleList.delta(2, selected, n)), 0.001);
        }
    }

    // ---- where the arc sits on the screen (Nachbesserung 1) ----

    @Test
    public void restSymbolsAreCentredBetweenTheClockAndTheBottomEdge() {
        for (int n : new int[]{3, 4, 17}) {
            float upperTop = CircleList.symbolRect(-1, MIDDLE)[1];
            float lowerBottom = CircleList.symbolRect(1, MIDDLE)[3];
            float gapAbove = upperTop - CircleList.CLOCK_BOTTOM_DP;   // clock digits to the upper neighbour's field
            float gapBelow = SCREEN_H - lowerBottom;                   // lower neighbour's field to the bottom edge
            // 4 px = 2 dp; plus 0.01 dp for float rounding.
            assertEquals("n=" + n + " gap above " + gapAbove + " dp, below " + gapBelow + " dp", gapAbove, gapBelow, 2.01);
            assertTrue("n=" + n + " both neighbours are seen at rest",
                    CircleList.alphaFor(CircleList.delta(0, 1, n), n) > 0 && CircleList.alphaFor(CircleList.delta(2, 1, n), n) > 0);
        }
    }

    @Test
    public void leftmostSeenSymbolIsFlushWithTheClockAndEverySeenFieldIsInTheImage() {
        for (int n : new int[]{3, 4, 17}) {
            double leftmost = Double.MAX_VALUE;
            for (int p = 0; p < n; p++) {
                double d = CircleList.delta(p, 1, n);
                if (CircleList.alphaFor(d, n) <= 0f) continue;
                float[] field = CircleList.symbolRect(d, MIDDLE);
                assertTrue("n=" + n + " row " + p + " field is inside the image: " + java.util.Arrays.toString(field),
                        field[0] >= 0 && field[1] >= 0 && field[2] <= SCREEN_W && field[3] <= SCREEN_H);
                leftmost = Math.min(leftmost, field[0]);
            }
            // The drawn glyph starts GLYPH_INSET_DP inside the field and must start on the clock digits' left edge (2 px = 1 dp).
            assertEquals("n=" + n, ListMode.ROOT_PADDING_DP, leftmost + CircleList.GLYPH_INSET_DP, 1.0);
        }
    }

    // ---- how many are seen ----

    @Test
    public void onlyThreeSymbolsAreVisibleAtRest() {
        assertEquals(0, CircleList.visibleAtRest(0, 0));
        assertEquals(1, CircleList.visibleAtRest(1, 0));
        assertEquals(2, CircleList.visibleAtRest(2, 0));
        assertEquals(3, CircleList.visibleAtRest(3, 0));
        assertEquals(3, CircleList.visibleAtRest(4, 2));
        for (int selected = 0; selected < 17; selected++) {
            assertEquals("selected=" + selected, 3, CircleList.visibleAtRest(17, selected));
            for (int p = 0; p < 17; p++) {
                double d = CircleList.delta(p, selected, 17);
                if (Math.abs(d) >= 2) assertEquals("row " + p + " is not seen", 0f, CircleList.alphaFor(d, 17), 0f);
            }
        }
    }

    @Test
    public void twoAppsAreBothVisibleAtRest() {
        for (int selected = 0; selected < 2; selected++) {
            int other = 1 - selected;
            assertEquals(1f, CircleList.alphaFor(CircleList.delta(selected, selected, 2), 2), 0.0001f);
            double dOther = CircleList.delta(other, selected, 2);
            assertEquals("the other app sits at +1", 1.0, dOther, 0.0001);
            assertEquals(0.6f, CircleList.alphaFor(dOther, 2), 0.0001f);
        }
    }

    @Test
    public void twoAppsSwapWithoutAJump() {
        double[] before = {CircleList.delta(0, 0, 2), CircleList.delta(1, 0, 2)};
        for (double phase = 0.05; phase <= 1.0001; phase += 0.05) {
            for (int p = 0; p < 2; p++) {
                double d = CircleList.delta(p, phase, 2);
                assertTrue("row " + p + " never runs through -1 (phase " + phase + ": " + d + ")", d >= -0.0001 && d <= 1.0001);
                assertTrue("row " + p + " moves at most 0.1 places in a 0.05 step (phase " + phase + ")",
                        Math.abs(d - before[p]) <= 0.1001);
                before[p] = d;
            }
        }
        assertEquals("the chosen app went to +1", 1.0, CircleList.delta(0, 1.0, 2), 0.0001);
        assertEquals("the other one came to 0", 0.0, CircleList.delta(1, 1.0, 2), 0.0001);
    }

    @Test
    public void emptyListNeverThrows() {
        for (int n : new int[]{0, -1}) {
            assertEquals(0.0, CircleList.delta(0, 0, n), 0);
            assertEquals(0f, CircleList.alphaFor(0, n), 0f);
            assertEquals(0, CircleList.visibleAtRest(n, 0));
            assertEquals(0, CircleList.attached(0, 0, n).length);
            assertEquals(0, CircleList.order(n, 0).length);
            assertEquals(0, CircleList.step(0, n, true));
            assertEquals(0, CircleList.step(0, n, false));
            assertEquals(0, ListMode.afterHeaderDown(3, n));
        }
    }

    // ---- no ends ----

    @Test
    public void stepWrapsAroundInBothDirections() {
        assertEquals(0, CircleList.step(0, 1, false));
        assertEquals(0, CircleList.step(0, 1, true));
        assertEquals(1, CircleList.step(0, 2, false));
        assertEquals(0, CircleList.step(1, 2, false));
        assertEquals(1, CircleList.step(0, 2, true));
        assertEquals(0, CircleList.step(2, 3, false));
        assertEquals(2, CircleList.step(0, 3, true));
        assertEquals(0, CircleList.step(16, 17, false));
        assertEquals(16, CircleList.step(0, 17, true));
        assertEquals(6, CircleList.step(5, 17, false));
        assertEquals(4, CircleList.step(5, 17, true));
    }

    @Test
    public void stepNeverLeavesForTheTopBarOrTheEnd() {
        for (int n = 1; n <= 17; n++) {
            for (int position = 0; position < n; position++) {
                for (boolean up : new boolean[]{true, false}) {
                    int next = CircleList.step(position, n, up);
                    assertTrue("n=" + n + " position=" + position + " up=" + up + " gave " + next, next >= 0 && next < n);
                }
            }
        }
    }

    // ---- the turn ----

    @Test
    public void phaseStartsAtTheStart() {
        assertEquals(2.0, CircleList.phaseAt(2, 5, 0, false), 0);
        assertEquals(2.0, CircleList.phaseAt(2, 5, 0, true), 0);
    }

    @Test
    public void phaseEndsOnTheTarget() {
        assertEquals(5.0, CircleList.phaseAt(2, 5, 1, false), 0);
        assertEquals(5.0, CircleList.phaseAt(2, 5, 1, true), 0);
    }

    @Test
    public void phaseIsBetweenInTheMiddle() {
        double mid = CircleList.phaseAt(2, 5, 0.5, false);
        assertTrue("middle " + mid, mid > 2 && mid < 5);
        assertEquals("DecelerateInterpolator(1.5) at half time", 2 + 3 * (1 - 0.125), mid, 1e-9);
        double reduced = CircleList.phaseAt(2, 5, 0.5, true);
        assertEquals("DecelerateInterpolator() at half time", 2 + 3 * (1 - 0.25), reduced, 1e-9);
    }

    @Test
    public void interruptedTurnContinuesFromTheCurrentPhase() {
        CircleList.Turner turner = new CircleList.Turner(CircleList.DURATION_MS);
        assertTrue(turner.press(1));
        for (int i = 0; i < 60; i++) turner.tick(1);
        double atTheTurn = turner.shown();
        assertTrue("it is on its way: " + atTheTurn, atTheTurn > 0.3 && atTheTurn < 1);
        assertTrue(turner.press(-1));
        assertEquals("a press changes nothing by itself", atTheTurn, turner.shown(), 0);
        double last = atTheTurn;
        for (int i = 0; i < 400 && turner.running(); i++) {
            turner.tick(1);
            assertTrue("it never runs away from the target (" + last + " -> " + turner.shown() + ")", turner.shown() <= last + 1e-9);
            last = turner.shown();
        }
        assertEquals(0.0, turner.shown(), 0);
        assertEquals(0, turner.target());
    }

    private static int msToStandStill(CircleList.Turner turner, int limit) {
        int ms = 0;
        while (turner.running() && ms < limit) {
            turner.tick(1);
            ms++;
        }
        return ms;
    }

    @Test
    public void turnLastsAtMost250msAndReducedMotionAtMost90ms() {
        assertTrue(CircleList.DURATION_MS <= 250);
        assertEquals(CircleList.DURATION_MS, CircleList.durationMs(true, false));
        assertTrue(CircleList.durationMs(true, true) <= Motion.REDUCED_MS);
        CircleList.Turner normal = new CircleList.Turner(CircleList.durationMs(true, false));
        normal.press(1);
        int ms = msToStandStill(normal, 1000);
        assertTrue("a single press stood still after " + ms + " ms", ms <= 250);
        CircleList.Turner reduced = new CircleList.Turner(CircleList.durationMs(true, true));
        reduced.press(-1);
        ms = msToStandStill(reduced, 1000);
        assertTrue("a single reduced press stood still after " + ms + " ms", ms <= Motion.REDUCED_MS);
    }

    @Test
    public void noAnimationsMeansInstant() {
        assertEquals(0, CircleList.durationMs(false, false));
        assertEquals(0, CircleList.durationMs(false, true));
        CircleList.Turner turner = new CircleList.Turner(CircleList.durationMs(false, false));
        assertTrue(turner.press(1));
        assertFalse(turner.running());
        assertEquals(1.0, turner.shown(), 0);
    }

    @Test
    public void heldKeyNeverJumps() {
        for (int spacing : new int[]{20, 30, 50}) {
            CircleList.Turner turner = new CircleList.Turner(CircleList.DURATION_MS);
            double previous = 0;
            double biggest = 0;
            int taken = 0;
            for (int ms = 0; ms < 2000; ms++) {
                if (ms % spacing == 0 && turner.press(1)) taken++;
                turner.tick(1);
                if (ms % 16 == 0) {
                    biggest = Math.max(biggest, Math.abs(turner.shown() - previous));
                    previous = turner.shown();
                }
            }
            assertTrue("spacing " + spacing + " ms: the shown phase moved " + biggest + " places between two frames", biggest <= 0.6);
            assertTrue("spacing " + spacing + " ms: " + taken + " presses taken", taken > 0);
            int stop = msToStandStill(turner, 5000);
            assertTrue("spacing " + spacing + " ms: stood still " + stop + " ms after the last press", stop <= 250);
            assertEquals("the target is the number of presses taken", taken, turner.target());
        }
    }

    @Test
    public void noRowJumpsWhileVisible() {
        for (int n : new int[]{3, 4, 5, 17}) {
            for (int p = 0; p < n; p++) {
                double before = CircleList.delta(p, 0, n);
                float alphaBefore = CircleList.alphaFor(before, n);
                for (double phase = 0.05; phase <= n + 0.0001; phase += 0.05) {
                    double d = CircleList.delta(p, phase, n);
                    float alpha = CircleList.alphaFor(d, n);
                    if (Math.abs(d - before) > 0.1001) {
                        assertTrue("n=" + n + " row " + p + " jumped from " + before + " to " + d + " at phase " + phase
                                + " with alpha " + alphaBefore + " / " + alpha, alphaBefore <= 0.05f && alpha <= 0.05f);
                    }
                    before = d;
                    alphaBefore = alpha;
                }
            }
        }
    }

    // ---- inside the screen ----

    private static float[] inside(float[] rect) {
        float w = rect[2] - rect[0], h = rect[3] - rect[1];
        float iw = Math.max(0, Math.min(rect[2], SCREEN_W) - Math.max(rect[0], 0));
        float ih = Math.max(0, Math.min(rect[3], SCREEN_H) - Math.max(rect[1], 0));
        return new float[]{iw / w, ih / h};
    }

    @Test
    public void selectedSymbolAndNameStayInsideTheScreen() {
        float[] symbol = CircleList.symbolRect(0, MIDDLE);
        float[] name = CircleList.nameRect(0, MIDDLE);
        for (float[] rect : new float[][]{symbol, name}) {
            assertTrue(rect[0] >= 0 && rect[1] >= 0 && rect[2] <= SCREEN_W && rect[3] <= SCREEN_H);
        }
        assertTrue("the name starts behind the symbol", name[0] >= symbol[2]);
        for (double d : new double[]{-1, 1}) {
            float[] fraction = inside(CircleList.symbolRect(d, MIDDLE));
            assertTrue("a neighbour is at least half inside", fraction[0] * fraction[1] >= 0.5f);
        }
        assertTrue("symbols are as big as ListMode's", symbol[2] - symbol[0] >= ListMode.ICON_W_DP
                && symbol[3] - symbol[1] >= ListMode.ICON_H_DP);
    }

    @Test
    public void nothingOpaqueReachesTheHeader() {
        int n = 17;
        for (int step = 0; step <= n * 20; step++) {
            boolean atRest = step % 20 == 0;
            // Exact at whole places, so "at rest" is not blurred by rounding dust.
            double phase = atRest ? step / 20 : step * 0.05;
            float limit = atRest ? 0f : 0.5f;
            for (int p = 0; p < n; p++) {
                double d = CircleList.delta(p, phase, n);
                if (CircleList.alphaFor(d, n) > limit) {
                    assertTrue("symbol of row " + p + " at phase " + phase + " reaches " + CircleList.symbolRect(d, MIDDLE)[1],
                            CircleList.symbolRect(d, MIDDLE)[1] >= CircleList.HEADER_MAX_BOTTOM_DP);
                }
                if (CircleList.nameAlphaFor(d) > limit) {
                    assertTrue("name of row " + p + " at phase " + phase + " reaches " + CircleList.nameRect(d, MIDDLE)[1],
                            CircleList.nameRect(d, MIDDLE)[1] >= CircleList.HEADER_MAX_BOTTOM_DP);
                }
            }
        }
    }

    @Test
    public void nameShowsOnlyAtTheSelection() {
        assertEquals(1f, CircleList.nameAlphaFor(0), 0f);
        for (double d : new double[]{1, -1, 1.5, 2, -3, 8}) {
            assertEquals("distance " + d, 0f, CircleList.nameAlphaFor(d), 0f);
        }
        float last = 1f;
        for (double d = 0.1; d < 1; d += 0.1) {
            float alpha = CircleList.nameAlphaFor(d);
            assertTrue("falls between 0 and 1: " + alpha, alpha < last && alpha > 0);
            last = alpha;
        }
    }

    // ---- which rows ----

    @Test
    public void windowAroundTargetAlwaysContainsTarget() {
        int[] far = CircleList.attached(9.0, 0, 17);
        assertTrue("a jump from 9 to 0", contains(far, 0));
        assertTrue(contains(CircleList.attached(0.0, 16, 17), 16));
        assertTrue(contains(CircleList.attached(8.0, 16, 17), 15));
        assertTrue(contains(CircleList.attached(8.0, 16, 17), 0));   // the neighbour after the last one is the first
        for (int target = 0; target < 17; target++) {
            for (double phase = 0; phase < 17; phase += 0.5) {
                int[] rows = CircleList.attached(phase, target, 17);
                assertTrue(contains(rows, target));
                for (int p = 0; p < 17; p++) {
                    if (Math.abs(CircleList.delta(p, phase, 17)) <= 2) assertTrue("seen row " + p + " is attached", contains(rows, p));
                }
            }
        }
        assertEquals("all rows of a short list", 5, CircleList.attached(2.5, 2, 5).length);
    }

    private static boolean contains(int[] rows, int value) {
        for (int row : rows) if (row == value) return true;
        return false;
    }

    @Test
    public void circleOrderIsTheListOrder() {
        List<String> apps = new ArrayList<>();
        for (int i = 1; i <= 17; i++) apps.add("App " + i);
        for (int selected = 0; selected < 17; selected++) {
            int[] order = CircleList.order(17, selected);
            assertEquals(5, order.length);
            for (int i = 0; i < 5; i++) {
                assertEquals("selected " + selected + " place " + i, apps.get(Math.floorMod(selected - 2 + i, 17)), apps.get(order[i]));
            }
        }
        assertEquals(1, CircleList.order(1, 0).length);
        assertEquals(2, CircleList.order(2, 0).length);
    }

    // ---- back from the top buttons ----

    @Test
    public void headerDownReturnsToTheLastApp() {
        assertEquals(3, ListMode.afterHeaderDown(3, 5));
        assertEquals(0, ListMode.afterHeaderDown(0, 5));
        assertEquals(4, ListMode.afterHeaderDown(9, 5));
        assertEquals(0, ListMode.afterHeaderDown(3, 0));
    }

    // ---- changing the order (Nachbesserung 2) ----

    private static List<String> apps(int n) {
        List<String> apps = new ArrayList<>();
        for (int i = 1; i <= n; i++) apps.add("App " + i);
        return apps;
    }

    @Test
    public void carriedAppMovesOnePlaceAndNeverPastEitherEnd() {
        for (int n : new int[]{2, 3, 17}) {
            assertEquals("n=" + n + " Up at the start", 0, CircleList.moveTarget(0, n, -1));
            assertEquals("n=" + n + " Down at the end", n - 1, CircleList.moveTarget(n - 1, n, 1));
            assertEquals("n=" + n + " Down at the start", 1, CircleList.moveTarget(0, n, 1));
            assertEquals("n=" + n + " Up at the end", n - 2, CircleList.moveTarget(n - 1, n, -1));
        }
        assertEquals(4, CircleList.moveTarget(5, 17, -1));
        assertEquals(6, CircleList.moveTarget(5, 17, 1));
        assertEquals("one app: nothing to move", 0, CircleList.moveTarget(0, 1, 1));
        assertEquals("not a movable place", 9, CircleList.moveTarget(9, 9, 1));
    }

    @Test
    public void movingOneAppLeavesTheOthersInTheirOrder() {
        List<String> list = apps(17);
        int to = CircleList.moveTarget(5, 17, 1);
        list.add(to, list.remove(5));
        List<String> expected = apps(17);
        expected.remove("App 6");
        expected.add(6, "App 6");
        assertEquals(expected, list);
        assertEquals("the carried app is where the move said", "App 6", list.get(to));
    }

    @Test
    public void cancelPutsTheOrderBackAsItWas() {
        List<String> before = apps(17);
        List<String> list = new ArrayList<>(before);
        int at = 8;
        int[] presses = {-1, -1, -1, 1, -1, -1, -1, -1, -1, -1};   // carried up towards the start, one press went back
        for (int press : presses) {
            int to = CircleList.moveTarget(at, 17, press);
            list.add(to, list.remove(at));
            at = to;
        }
        assertFalse("the carried app has moved", before.equals(list));
        for (int[] move : CircleList.restoreMoves(list, before)) list.add(move[1], list.remove(move[0]));
        assertEquals(before, list);
        assertTrue("nothing to undo when nothing moved", CircleList.restoreMoves(before, before).isEmpty());
    }

    @Test
    public void afterMovingTheAppStaysChosenWhenComingBackFromTheTopButtons() {
        int to = CircleList.moveTarget(5, 17, -1);
        assertEquals(4, to);
        assertEquals("Down from the top buttons goes to the moved app", to, ListMode.afterHeaderDown(to, 17));
    }

    @Test
    public void circleListAlwaysOpensTheTileMenuButTheStraightListKeepsItsSetting() {
        assertTrue("circle list, tile_menu off: the menu opens", ListMode.circleTileMenu(false, true, true));
        assertFalse("straight list, tile_menu off: App info as before", ListMode.circleTileMenu(false, true, false));
        assertFalse("circle_list without list_mode changes nothing", ListMode.circleTileMenu(false, false, true));
        assertTrue("tile_menu on stays on", ListMode.circleTileMenu(true, false, false));
        assertTrue(ListMode.circleTileMenu(true, true, false));
    }

    @Test
    public void tileMenuListsMoveFirstWhenTheAppCanMove() {
        assertArrayEquals(new int[]{TileMenu.MOVE, TileMenu.REMOVE, TileMenu.INFO}, TileMenu.rows(false, true, true, true));
        assertArrayEquals("the Default home tile cannot move", new int[]{TileMenu.REMOVE, TileMenu.INFO},
                TileMenu.rows(false, false, true, true));
        assertArrayEquals("with a dock the dock row comes first", new int[]{TileMenu.DOCK, TileMenu.MOVE, TileMenu.INFO},
                TileMenu.rows(true, true, false, true));
        assertArrayEquals("no App info page on this firmware", new int[]{TileMenu.MOVE},
                TileMenu.rows(false, true, false, false));
    }
}
