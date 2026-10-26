package com.silo.algo;

import com.silo.model.Donation;
import java.util.Comparator;

/** Priority rule: 1) higher perishability first, 2) earlier best-before, 3) shorter distance, 4) lower id (stable tie-break). */
public final class DonationPriority {
    private DonationPriority() { }

    public static final Comparator<Donation> ORDER = (a, b) -> {
        int c = Integer.compare(b.getPerishability(), a.getPerishability());
        if (c != 0) return c;
        c = a.getBestBefore().compareTo(b.getBestBefore());
        if (c != 0) return c;
        c = Double.compare(a.getDistanceKm(), b.getDistanceKm());
        if (c != 0) return c;
        return Integer.compare(a.getId(), b.getId());
    };
}
