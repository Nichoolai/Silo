package com.silo;

import com.silo.algo.*;
import com.silo.model.*;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class AlgorithmTest {
    private static Donation d(int id, int perish, String best, double km) {
        return new Donation(id, "Donor", "Food" + id, 10, "kg", "Place", km, perish, LocalDate.of(2026, 1, 1), LocalDate.parse(best));
    }

    @Test void emptyAndSingleListsAreHandled() {
        assertTrue(DonationMergeSort.sort(new ArrayList<Donation>(), DonationPriority.ORDER).isEmpty());
        assertEquals(1, DonationMergeSort.sort(List.of(d(1, 3, "2026-12-01", 1)), DonationPriority.ORDER).size());
    }

    @Test void priorityRulesApplyInDocumentedOrder() {
        Donation a = d(1, 2, "2026-11-01", 1);   // low perishability
        Donation b = d(2, 5, "2026-11-10", 9);   // highest perishability, later date
        Donation c = d(3, 5, "2026-11-05", 20);  // highest perishability, earlier date -> before b
        Donation e = d(4, 5, "2026-11-05", 3);   // same date as c, shorter distance -> before c
        List<Donation> sorted = DonationMergeSort.sort(List.of(a, b, c, e), DonationPriority.ORDER);
        assertEquals(List.of(4, 3, 2, 1), sorted.stream().map(Donation::getId).toList());
    }

    @Test void sortDoesNotModifyInputAndIsStable() {
        List<Integer> in = new ArrayList<>(List.of(3, 1, 2));
        DonationMergeSort.sort(in, Comparator.naturalOrder());
        assertEquals(List.of(3, 1, 2), in);
        // stability: equal keys keep original order
        List<String> words = List.of("bb", "a", "cc", "d", "ee");
        assertEquals(List.of("a", "d", "bb", "cc", "ee"), DonationMergeSort.sort(words, Comparator.comparingInt(String::length)));
    }

    @Test void matchesReferenceSortOnRandomData() {
        Random rnd = new Random(42);
        for (int n : new int[]{2, 3, 10, 257, 1000}) {
            List<Integer> data = new ArrayList<>();
            for (int i = 0; i < n; i++) data.add(rnd.nextInt(50));
            List<Integer> expected = new ArrayList<>(data); Collections.sort(expected);
            assertEquals(expected, DonationMergeSort.sort(data, Comparator.naturalOrder()));
        }
    }

    @Test void queueProcessesEarliestFirstAndFifoOnTies() {
        PickupQueue q = new PickupQueue();
        LocalDateTime t = LocalDateTime.of(2026, 11, 1, 9, 0);
        Pickup late = new Pickup(1, 1, 1, t.plusHours(5)), early = new Pickup(2, 2, 1, t),
               tie1 = new Pickup(3, 3, 1, t.plusHours(2)), tie2 = new Pickup(4, 4, 1, t.plusHours(2));
        q.enqueue(late); q.enqueue(tie1); q.enqueue(early); q.enqueue(tie2);
        assertEquals(2, q.dequeue().getId());
        assertEquals(3, q.dequeue().getId());
        assertEquals(4, q.dequeue().getId());
        assertEquals(1, q.dequeue().getId());
        assertNull(q.dequeue());
        assertTrue(q.isEmpty());
    }

    @Test void stackUndoesInReverseOrder() {
        ActionHistory h = new ActionHistory();
        List<String> undone = new ArrayList<>();
        for (String s : List.of("one", "two", "three")) h.push(new ActionHistory.Undoable() {
            public String describe() { return s; }
            public void undo() { undone.add(s); }
        });
        assertEquals("three", h.peekDescription());
        h.undoLast(); h.undoLast();
        assertEquals(List.of("three", "two"), undone);
        h.undoLast();
        assertNull(h.undoLast());
        assertTrue(h.isEmpty());
    }

    @Test void searchIsCaseInsensitiveAcrossFieldsAndBlankReturnsAll() {
        List<Donation> items = List.of(d(1, 1, "2026-12-01", 1), d(2, 1, "2026-12-01", 1));
        assertEquals(2, Search.filter(items, "  ", List.of(Donation::getFoodName)).size());
        assertEquals(1, Search.filter(items, "FOOD2", List.of(Donation::getFoodName)).size());
        assertEquals(0, Search.filter(items, "zzz", List.of(Donation::getFoodName, Donation::getDonor)).size());
        assertEquals(2, Search.filter(items, "donor", List.of(Donation::getFoodName, Donation::getDonor)).size());
    }
}
