package com.silo;

import com.silo.model.*;
import com.silo.model.Donation.Status;
import com.silo.service.*;
import com.silo.storage.FileStore;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ServiceTest {
    @TempDir Path dir;
    SiloService s;
    // Fixed clock: 2026-10-26 08:00
    static final Clock CLOCK = Clock.fixed(LocalDateTime.of(2026, 10, 26, 8, 0).atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
    static final LocalDate TODAY = LocalDate.of(2026, 10, 26);
    static final LocalDateTime TOMORROW_9 = LocalDateTime.of(2026, 10, 27, 9, 0);

    @BeforeEach void setUp() { s = new SiloService(new FileStore(dir), CLOCK); }

    Donation donation(String food, int perish, int bestInDays) {
        return s.addDonation("Farm A", food, "50", "kg", "Quezon City", "4.5", perish, TODAY.minusDays(1), TODAY.plusDays(bestInDays));
    }

    @Test void validDonationIsSavedAndReloadedAfterRestart() {
        Donation d = donation("Tomatoes", 4, 5);
        s.addVolunteer("Ana", "0917 123 4567", "QC", "Weekends");
        SiloService again = new SiloService(new FileStore(dir), CLOCK);
        assertEquals(1, again.donations().size());
        assertEquals("Tomatoes", again.donation(d.getId()).getFoodName());
        assertEquals(50.0, again.donation(d.getId()).getQuantity());
        assertEquals(1, again.volunteers().size());
    }

    @Test void invalidDonationInputsGiveClearMessages() {
        assertTrue(assertThrows(ValidationException.class, () -> s.addDonation("", "x", "5", "kg", "p", "1", 3, TODAY, TODAY)).getMessage().contains("Donor"));
        assertTrue(assertThrows(ValidationException.class, () -> s.addDonation("a", "x", "abc", "kg", "p", "1", 3, TODAY, TODAY)).getMessage().contains("number"));
        assertTrue(assertThrows(ValidationException.class, () -> s.addDonation("a", "x", "-3", "kg", "p", "1", 3, TODAY, TODAY)).getMessage().contains("greater than 0"));
        assertTrue(assertThrows(ValidationException.class, () -> s.addDonation("a", "x", "3", "kg", "p", "-1", 3, TODAY, TODAY)).getMessage().contains("negative"));
        assertTrue(assertThrows(ValidationException.class, () -> s.addDonation("a", "x", "3", "kg", "p", "1", 9, TODAY, TODAY)).getMessage().contains("1 and 5"));
        assertTrue(assertThrows(ValidationException.class, () -> s.addDonation("a", "x", "3", "kg", "p", "1", 3, TODAY, TODAY.minusDays(2))).getMessage().contains("earlier"));
        assertTrue(assertThrows(ValidationException.class, () -> s.addDonation("a", "x", "3", "kg", "p", "1", 3, TODAY.minusDays(9), TODAY.minusDays(1))).getMessage().contains("past"));
        assertTrue(s.donations().isEmpty());
    }

    @Test void prioritizedListUsesMergeSortRules() {
        donation("Rice", 1, 90); Donation fish = donation("Fish", 5, 1); donation("Banana", 4, 2);
        assertEquals(fish.getId(), s.prioritized(true).get(0).getId());
        assertEquals("Rice", s.prioritized(true).get(2).getFoodName());
    }

    @Test void searchAndStatusFilterWork() {
        donation("Tomatoes", 4, 5); Donation b = donation("Bananas", 3, 5);
        assertEquals(1, s.searchDonations("banan", null).size());
        s.cancelDonation(b.getId());
        assertEquals(1, s.searchDonations("", Status.CANCELLED).size());
        assertEquals(2, s.searchDonations("", null).size());
    }

    @Test void fullWorkflowScheduleCollectDistributeDeliver() {
        Donation d = donation("Corn", 3, 5);
        Volunteer v = s.addVolunteer("Ben", "09171234567", "QC", "Weekdays");
        Recipient r = s.addRecipient("Barangay Hall", "", "Commonwealth", "Barangay");
        Pickup p = s.schedulePickup(d.getId(), v.getId(), TOMORROW_9);
        assertEquals(Status.SCHEDULED, d.getStatus());
        s.markPickedUp(p.getId());
        assertEquals(Status.PICKED_UP, d.getStatus());
        s.recordDistribution(d.getId(), r.getId(), "20", TODAY);
        assertEquals(30.0, d.getRemaining(), 1e-9);
        assertEquals(Status.PICKED_UP, d.getStatus());
        s.recordDistribution(d.getId(), r.getId(), "30", TODAY);
        assertEquals(Status.DELIVERED, d.getStatus());
        assertEquals(Pickup.Status.DELIVERED, p.getStatus());
        assertEquals(1, s.completedPickupsOf(v.getId()));
        assertEquals(50.0, s.deliveredByUnit().get("kg"), 1e-9);
    }

    @Test void cannotDistributeMoreThanRemainingOrBeforePickup() {
        Donation d = donation("Corn", 3, 5);
        Volunteer v = s.addVolunteer("Ben", "09171234567", "QC", "Any");
        Recipient r = s.addRecipient("Shelter", "", "Addr", "Shelter");
        assertThrows(ValidationException.class, () -> s.recordDistribution(d.getId(), r.getId(), "5", TODAY)); // not picked up
        Pickup p = s.schedulePickup(d.getId(), v.getId(), TOMORROW_9);
        s.markPickedUp(p.getId());
        ValidationException e = assertThrows(ValidationException.class, () -> s.recordDistribution(d.getId(), r.getId(), "50.5", TODAY));
        assertTrue(e.getMessage().contains("Only 50"));
        assertThrows(ValidationException.class, () -> s.recordDistribution(d.getId(), r.getId(), "0", TODAY));
        assertThrows(ValidationException.class, () -> s.recordDistribution(d.getId(), r.getId(), "5", TODAY.plusDays(1)));
        assertEquals(50.0, d.getRemaining(), 1e-9);
    }

    @Test void volunteerConflictsAndInvalidAssignmentsAreRejected() {
        Donation a = donation("A", 3, 5), b = donation("B", 3, 5), c = donation("C", 3, 5);
        Volunteer v = s.addVolunteer("Ben", "09171234567", "QC", "Any");
        s.schedulePickup(a.getId(), v.getId(), TOMORROW_9);
        assertThrows(ValidationException.class, () -> s.schedulePickup(b.getId(), v.getId(), TOMORROW_9.plusMinutes(30))); // conflict
        s.schedulePickup(b.getId(), v.getId(), TOMORROW_9.plusMinutes(60));                                              // exactly one slot later is fine
        assertThrows(ValidationException.class, () -> s.schedulePickup(a.getId(), v.getId(), TOMORROW_9.plusHours(5)));  // donation already scheduled
        assertThrows(ValidationException.class, () -> s.schedulePickup(c.getId(), v.getId(), TOMORROW_9.minusDays(5)));  // past
        assertThrows(ValidationException.class, () -> s.schedulePickup(c.getId(), v.getId(), TODAY.plusDays(30).atTime(9, 0))); // after best-before
        assertThrows(ValidationException.class, () -> s.setVolunteerActive(v.getId(), false)); // has active pickups
        assertThrows(ValidationException.class, () -> s.schedulePickup(c.getId(), 999, TOMORROW_9.plusHours(8)));
    }

    @Test void deactivatedVolunteerCannotBeAssigned() {
        Donation a = donation("A", 3, 5);
        Volunteer v = s.addVolunteer("Ben", "09171234567", "QC", "Any");
        s.setVolunteerActive(v.getId(), false);
        assertThrows(ValidationException.class, () -> s.schedulePickup(a.getId(), v.getId(), TOMORROW_9));
    }

    @Test void queueProcessesEarliestPickupFirst() {
        Donation a = donation("A", 3, 5), b = donation("B", 3, 5);
        Volunteer v = s.addVolunteer("Ben", "09171234567", "QC", "Any");
        Pickup late = s.schedulePickup(a.getId(), v.getId(), TOMORROW_9.plusHours(4));
        Pickup early = s.schedulePickup(b.getId(), v.getId(), TOMORROW_9);
        assertEquals(early.getId(), s.queueOrder().get(0).getId());
        assertEquals(early.getId(), s.processNextPickup().getId());
        assertEquals(Pickup.Status.PICKED_UP, early.getStatus());
        assertEquals(late.getId(), s.queueOrder().get(0).getId());
        // queue is rebuilt after a restart
        assertEquals(1, new SiloService(new FileStore(dir), CLOCK).queueOrder().size());
        s.processNextPickup();
        assertThrows(ValidationException.class, () -> s.processNextPickup());
    }

    @Test void cancellingPickupFreesDonationAndCancellingDonationCancelsPickup() {
        Donation a = donation("A", 3, 5), b = donation("B", 3, 5);
        Volunteer v = s.addVolunteer("Ben", "09171234567", "QC", "Any");
        Pickup pa = s.schedulePickup(a.getId(), v.getId(), TOMORROW_9);
        Pickup pb = s.schedulePickup(b.getId(), v.getId(), TOMORROW_9.plusHours(3));
        s.cancelPickup(pa.getId());
        assertEquals(Status.AVAILABLE, a.getStatus());
        s.cancelDonation(b.getId());
        assertEquals(Pickup.Status.CANCELLED, pb.getStatus());
        assertTrue(s.queueOrder().isEmpty());
        assertThrows(ValidationException.class, () -> s.cancelDonation(b.getId())); // already cancelled
    }

    @Test void undoReversesLastActionsInOrder() {
        Donation a = donation("A", 3, 5);
        Volunteer v = s.addVolunteer("Ben", "09171234567", "QC", "Any");
        s.schedulePickup(a.getId(), v.getId(), TOMORROW_9);
        s.undoLast();
        assertEquals(Status.AVAILABLE, a.getStatus());
        assertTrue(s.pickups().isEmpty());
        s.undoLast();
        assertTrue(s.volunteers().isEmpty());
        s.undoLast();
        assertTrue(s.donations().isEmpty());
        assertFalse(s.canUndo());
        assertThrows(ValidationException.class, () -> s.undoLast());
    }

    @Test void reportsReflectSavedRecordsAndKeepUnitsSeparate() {
        donation("Corn", 3, 2);
        s.addDonation("Farm B", "Eggs", "100", "pcs", "QC", "2", 4, TODAY, TODAY.plusDays(20));
        assertEquals(50.0, s.donatedByUnit().get("kg"));
        assertEquals(100.0, s.donatedByUnit().get("pcs"));
        assertEquals(1, s.nearingBestBefore(3).size());
        assertEquals("Corn", s.nearingBestBefore(3).get(0).getFoodName());
        assertEquals(2, s.statusCounts().get(Status.AVAILABLE));
        assertTrue(s.deliveredByUnit().isEmpty());
    }

    @Test void editingDonationKeepsDistributedQuantityConsistent() {
        Donation d = donation("Corn", 3, 5);
        Volunteer v = s.addVolunteer("Ben", "09171234567", "QC", "Any");
        Recipient r = s.addRecipient("Shelter", "", "Addr", "Shelter");
        s.markPickedUp(s.schedulePickup(d.getId(), v.getId(), TOMORROW_9).getId());
        s.recordDistribution(d.getId(), r.getId(), "40", TODAY);
        assertThrows(ValidationException.class, () -> s.updateDonation(d.getId(), "Farm A", "Corn", "30", "kg", "QC", "4", 3, TODAY.minusDays(1), TODAY.plusDays(5)));
        s.updateDonation(d.getId(), "Farm A", "Corn", "60", "kg", "QC", "4", 3, TODAY.minusDays(1), TODAY.plusDays(5));
        assertEquals(20.0, d.getRemaining(), 1e-9);
    }

    @Test void volunteerPhoneAndRecipientValidation() {
        assertThrows(ValidationException.class, () -> s.addVolunteer("Ana", "abc", "QC", "Any"));
        assertThrows(ValidationException.class, () -> s.addVolunteer(" ", "09171234567", "QC", "Any"));
        assertThrows(ValidationException.class, () -> s.addRecipient("Shelter", "", " ", "Shelter"));
        assertEquals(0, s.searchVolunteers("").size());
    }

    @Test void textWithTabsAndNewlinesSurvivesSaveAndLoad() {
        s.addDonation("Farm\tA", "Mango\nRipe", "5", "kg", "Back\\slash", "1", 2, TODAY, TODAY.plusDays(3));
        Donation back = new SiloService(new FileStore(dir), CLOCK).donations().get(0);
        assertEquals("Farm\tA", back.getDonor());
        assertEquals("Mango\nRipe", back.getFoodName());
        assertEquals("Back\\slash", back.getLocation());
    }
}
