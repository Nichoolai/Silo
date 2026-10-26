package com.silo.service;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Optional demo records, created through the normal service API so every rule still applies. Only loaded when the user clicks "Load sample data". */
public final class SampleData {
    private SampleData() { }

    public static void load(SiloService s) {
        LocalDate t = s.today();
        var tomato = s.addDonation("Dela Cruz Farm", "Tomatoes", "120", "kg", "Antipolo, Rizal", "18.5", 4, t.minusDays(1), t.plusDays(4));
        s.addDonation("Santos Orchard", "Ripe bananas", "300", "pcs", "Quezon City", "6", 5, t.minusDays(1), t.plusDays(2));
        s.addDonation("Reyes Rice Co-op", "Milled rice", "500", "kg", "San Mateo, Rizal", "22", 1, t.minusDays(10), t.plusDays(120));
        s.addDonation("Garcia Vegetable Farm", "Leafy greens", "45", "kg", "Marikina", "9", 5, t.minusDays(1), t.plusDays(3));
        s.addDonation("Lopez Poultry", "Eggs", "200", "pcs", "Cainta, Rizal", "14", 3, t.minusDays(2), t.plusDays(14));
        var v1 = s.addVolunteer("Mia Ramos", "0917 555 0101", "Quezon City", "Weekday mornings");
        s.addVolunteer("Jon Bautista", "0918 555 0102", "Marikina / Rizal", "Weekends");
        s.addVolunteer("Ana Villanueva", "0920 555 0103", "Quezon City", "Weekday afternoons");
        s.addRecipient("Barangay Commonwealth Kitchen", "0919 555 0201", "Commonwealth Ave, Quezon City", "Community kitchen");
        s.addRecipient("Hope Shelter", "0921 555 0202", "Marikina City", "Shelter");
        s.addRecipient("Bagong Silangan Food Bank", "0922 555 0203", "Bagong Silangan, Quezon City", "Food bank");
        s.schedulePickup(tomato.getId(), v1.getId(), LocalDateTime.of(t.plusDays(1), java.time.LocalTime.of(9, 0)));
    }
}
