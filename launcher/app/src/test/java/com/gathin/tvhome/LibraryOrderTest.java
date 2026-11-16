package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The app library's order in list_mode: the apps not on Home first (saved order, then by name), the apps on
 * Home last in Home's order; moving inside the first block; what choosing and un-choosing do to the saved order; the
 * saved text. Package names in the cases are made up ("pkg.a" and so on).
 */
public class LibraryOrderTest {
    private static List<String> l(String... names) {
        return Arrays.asList(names);
    }

    private static Set<String> set(String... names) {
        return new HashSet<>(Arrays.asList(names));
    }

    // ---- K5: the order of the library ----

    @Test
    public void nothingSavedGivesNameOrderAndTheChosenLast() {
        List<String> byName = l("pkg.a", "pkg.b", "pkg.c", "pkg.d");
        assertEquals(l("pkg.a", "pkg.c", "pkg.b", "pkg.d"),
                LibraryOrder.arrange(byName, set("pkg.b", "pkg.d"), Collections.<String>emptyList(), l("pkg.b", "pkg.d")));
    }

    @Test
    public void savedOrderComesFirstThenTheRestByNameThenTheChosen() {
        List<String> byName = l("pkg.a", "pkg.b", "pkg.c", "pkg.d", "pkg.e");
        // saved: c before a; b was never saved (new); d is chosen and e is gone from the saved text's point of view
        List<String> saved = l("pkg.c", "pkg.gone", "pkg.d", "pkg.a");
        assertEquals(l("pkg.c", "pkg.a", "pkg.b", "pkg.e", "pkg.d"),
                LibraryOrder.arrange(byName, set("pkg.d"), saved, l("pkg.d")));
    }

    @Test
    public void chosenGoesWhereTheHomeListPutsIt() {
        List<String> byName = l("pkg.a", "pkg.b", "pkg.c", "pkg.x");
        Set<String> chosen = set("pkg.a", "pkg.b", "pkg.c", "pkg.x");
        // x is a dock app: Home shows it first; by name it would come last
        assertEquals(l("pkg.x", "pkg.a", "pkg.b", "pkg.c"),
                LibraryOrder.arrange(byName, chosen, Collections.<String>emptyList(), l("pkg.x", "pkg.a", "pkg.b", "pkg.c")));
        // a Home list in name order gives name order
        assertEquals(l("pkg.a", "pkg.b", "pkg.c", "pkg.x"),
                LibraryOrder.arrange(byName, chosen, Collections.<String>emptyList(), l("pkg.a", "pkg.b", "pkg.c", "pkg.x")));
    }

    @Test
    public void aChosenAppOutsideTheHomeListGoesLastByName() {
        List<String> byName = l("pkg.a", "pkg.b", "pkg.c");
        assertEquals(l("pkg.b", "pkg.c", "pkg.a"),
                LibraryOrder.arrange(byName, set("pkg.a", "pkg.c"), Collections.<String>emptyList(), l("pkg.c")));
    }

    @Test
    public void homeOrderListsEachPackageOnce() {
        assertEquals(l("pkg.a", "pkg.b"), LibraryOrder.distinct(l("pkg.a", "pkg.b", "pkg.a")));
    }

    @Test
    public void listOrderOnlyInListModeOutsideTheSelectionMode() {
        assertTrue(LibraryOrder.useListOrder(true, false));
        assertFalse(LibraryOrder.useListOrder(true, true));
        assertFalse(LibraryOrder.useListOrder(false, false));
        assertFalse(LibraryOrder.useListOrder(false, true));
    }

    // ---- K6: moving ----

    @Test
    public void moveNeverCrossesIntoTheChosen() {
        List<String> all = l("A", "B", "C", "X", "Y");
        assertEquals(l("A", "C", "B", "X", "Y"), LibraryOrder.moveBy(all, 3, 1, 1));    // just allowed
        assertEquals(all, LibraryOrder.moveBy(all, 3, 2, 1));                           // just refused: onto X
        assertEquals(all, LibraryOrder.moveBy(all, 3, 0, -1));                          // before the first place
        assertEquals(all, LibraryOrder.moveBy(all, 3, 3, -1));                          // X is chosen: not movable
        assertEquals(l("A", "C", "B", "X", "Y"), LibraryOrder.moveBy(all, 3, 2, -1));   // the neighbour case still moves
    }

    @Test
    public void moveSessionNeverCrossesIntoTheChosen() {
        // the real way of the dialog: X and Y are chosen and stand behind the block of three
        LibraryOrder.MoveSession session = new LibraryOrder.MoveSession(l("A", "B", "C", "X", "Y"));
        assertEquals(2, session.move("C", 3, 1));                       // just refused: C stays last of the block
        assertEquals(l("A", "B", "C", "X", "Y"), session.current());
        assertEquals(2, session.move("B", 3, 1));                       // just allowed: B passes C
        assertEquals(l("A", "C", "B", "X", "Y"), session.current());
        assertEquals(2, session.move("B", 3, 1));                       // and now it is the last of the block
        assertEquals(l("A", "C", "B", "X", "Y"), session.current());
    }

    @Test
    public void moveInTheMiddleGoesBothWays() {
        List<String> all = l("A", "B", "C", "D", "X");
        assertEquals(l("A", "C", "B", "D", "X"), LibraryOrder.moveBy(all, 4, 1, 1));
        assertEquals(l("B", "A", "C", "D", "X"), LibraryOrder.moveBy(all, 4, 1, -1));
    }

    @Test
    public void rtlSwapsTheMoveKeys() {
        assertEquals(-1, LibraryOrder.deltaForKey(true, false));
        assertEquals(1, LibraryOrder.deltaForKey(false, false));
        assertEquals(1, LibraryOrder.deltaForKey(true, true));
        assertEquals(-1, LibraryOrder.deltaForKey(false, true));
    }

    @Test
    public void focusFollowsTheMovedApp() {
        LibraryOrder.MoveSession session = new LibraryOrder.MoveSession(l("A", "B", "C"));
        assertEquals(1, LibraryOrder.indexOf(session.current(), "B"));
        assertEquals(0, session.move("B", 3, -1));
        assertEquals(0, LibraryOrder.indexOf(session.current(), "B"));
        assertEquals(1, LibraryOrder.indexOf(session.cancel(), "B"));
        assertEquals(0, session.move("A", 3, -1));   // the edge: A stays at 0
    }

    // ---- K7: wechseln und zurueckkommen ----

    @Test
    public void cancelRestoresTheOrderBeforeMoving() {
        LibraryOrder.MoveSession session = new LibraryOrder.MoveSession(l("A", "B", "C"));
        session.move("A", 3, 1);
        session.move("A", 3, 1);
        assertEquals(l("B", "C", "A"), session.current());
        assertEquals(l("A", "B", "C"), session.cancel());
        // the other way: OK keeps the new order
        LibraryOrder.MoveSession other = new LibraryOrder.MoveSession(l("A", "B", "C"));
        other.move("A", 3, 1);
        assertEquals(l("B", "A", "C"), other.confirm());
    }

    @Test
    public void cancelAfterConfirmKeepsTheNewOrder() {
        LibraryOrder.MoveSession session = new LibraryOrder.MoveSession(l("A", "B", "C"));
        session.move("A", 3, 1);
        session.confirm();
        assertEquals(l("B", "A", "C"), session.cancel());   // the window closes after OK and calls cancel too
        assertEquals(l("B", "A", "C"), session.current());
    }

    @Test
    public void afterRemovedPutsThePackageAtTheEnd() {
        assertEquals(l("a.a", "c.c", "b.b"), LibraryOrder.afterRemoved(l("a.a", "c.c"), "b.b"));
        assertEquals(l("a.a", "c.c", "b.b"), LibraryOrder.afterRemoved(l("b.b", "a.a", "c.c"), "b.b"));
    }

    @Test
    public void afterChosenForgetsThePackage() {
        assertEquals(l("a.a", "c.c"), LibraryOrder.afterChosen(l("a.a", "b.b", "c.c"), "b.b"));
        assertEquals(l("a.a"), LibraryOrder.afterChosen(l("a.a"), "b.b"));
    }

    // ---- the saved text ----

    @Test
    public void orderRoundTrips() {
        List<String> order = l("a.b", "c.d", "e.f");
        assertEquals("a.b\nc.d\ne.f\n", LibraryOrder.encode(order));
        assertEquals(order, LibraryOrder.decode(LibraryOrder.encode(order)));
        assertEquals(Collections.<String>emptyList(), LibraryOrder.decode(""));
        assertEquals(Collections.<String>emptyList(), LibraryOrder.decode(null));
    }

    @Test
    public void encodeSkipsInvalidNames() {
        assertEquals("a.b\nc.d\n", LibraryOrder.encode(l("a.b", "x y", "c.d")));
        assertEquals("a.b\n", LibraryOrder.encode(l("a.b", "a.b")));
    }

    @Test
    public void decodeSkipsInvalidNamesAndNeverThrows() {
        assertEquals(l("a.b", "c.d"), LibraryOrder.decode("a.b\n\nx y\n..\nc.d"));
    }

    @Test
    public void encodeKeepsTheLastAndDropsFromTheFront() {
        List<String> order = new ArrayList<>();
        for (int i = 0; i < 127; i++) order.add("p.a" + i);
        order.add("p.last");                       // 128: nothing falls away
        assertEquals(128, LibraryOrder.decode(LibraryOrder.encode(order)).size());
        assertEquals("p.a0", LibraryOrder.decode(LibraryOrder.encode(order)).get(0));
        order.add("p.newest");                     // 129: the first falls away, the last stays
        List<String> kept = LibraryOrder.decode(LibraryOrder.encode(order));
        assertEquals(128, kept.size());
        assertEquals("p.a1", kept.get(0));
        assertEquals("p.newest", kept.get(127));
    }
}
