package com.silo.service;

import com.silo.algo.*;
import com.silo.model.*;
import com.silo.model.Donation.Status;
import com.silo.storage.FileStore;

import java.time.*;
import java.util.*;
import java.util.function.Function;

/** All business rules. The UI only calls this class; it never touches files or sorts data itself. */
public class SiloService {
    public static final int PICKUP_SLOT_MINUTES = 60;

    private final FileStore store;
    private final Clock clock;
    private final List<Donation> donations = new ArrayList<>();
    private final List<Volunteer> volunteers = new ArrayList<>();
    private final List<Recipient> recipients = new ArrayList<>();
    private final List<Pickup> pickups = new ArrayList<>();
    private final List<Distribution> distributions = new ArrayList<>();
    private final List<String[]> activity = new ArrayList<>(); // {timestamp, text}
    private final PickupQueue pickupQueue = new PickupQueue();
    private final ActionHistory history = new ActionHistory();

    public SiloService(FileStore store) { this(store, Clock.systemDefaultZone()); }

    public SiloService(FileStore store, Clock clock) {
        this.store = store; this.clock = clock;
        load();
    }

    public LocalDate today() { return LocalDate.now(clock); }
    public LocalDateTime now() { return LocalDateTime.now(clock); }

    // ---------- Donations ----------
    public Donation addDonation(String donor, String food, String qty, String unit, String location,
                                String distance, int perishability, LocalDate harvest, LocalDate bestBefore) {
        Donation d = new Donation(nextId(donations.stream().mapToInt(Donation::getId)), donor.trim(), food.trim(),
                parsePositive(qty, "Quantity"), unit.trim(), location.trim(), parseNonNegative(distance, "Distance"),
                perishability, harvest, bestBefore);
        validateDonation(d.getDonor(), d.getFoodName(), d.getUnit(), d.getLocation(), perishability, harvest, bestBefore, true);
        donations.add(d);
        log("Donation #" + d.getId() + " added: " + d.getFoodName() + " from " + d.getDonor());
        history.push(undo("Added donation #" + d.getId(), () -> {
            if (d.getStatus() != Status.AVAILABLE) throw new ValidationException("Donation #" + d.getId() + " has moved on and can't be undone.");
            donations.remove(d);
        }));
        save();
        return d;
    }

    public void updateDonation(int id, String donor, String food, String qty, String unit, String location,
                               String distance, int perishability, LocalDate harvest, LocalDate bestBefore) {
        Donation d = donation(id);
        if (d.getStatus() == Status.DELIVERED || d.getStatus() == Status.CANCELLED)
            throw new ValidationException("A " + d.getStatus().name().toLowerCase() + " donation can't be edited.");
        double q = parsePositive(qty, "Quantity"), dist = parseNonNegative(distance, "Distance");
        validateDonation(donor.trim(), food.trim(), unit.trim(), location.trim(), perishability, harvest, bestBefore, false);
        double used = d.getQuantity() - d.getRemaining();
        if (q < used) throw new ValidationException("Quantity can't be lower than the " + fmt(used) + " already distributed.");
        d.update(donor.trim(), food.trim(), q, unit.trim(), location.trim(), dist, perishability, harvest, bestBefore);
        log("Donation #" + id + " edited");
        save();
    }

    public void cancelDonation(int id) {
        Donation d = donation(id);
        if (d.getStatus() != Status.AVAILABLE && d.getStatus() != Status.SCHEDULED)
            throw new ValidationException("Only available or scheduled donations can be cancelled (this one is " + label(d.getStatus()) + ").");
        for (Pickup p : pickups) if (p.getDonationId() == id && p.getStatus() == Pickup.Status.SCHEDULED) {
            p.setStatus(Pickup.Status.CANCELLED); pickupQueue.remove(p);
        }
        d.setStatus(Status.CANCELLED);
        log("Donation #" + id + " cancelled");
        save();
    }

    public List<Donation> donations() { return List.copyOf(donations); }
    public Donation donation(int id) {
        for (Donation d : donations) if (d.getId() == id) return d;
        throw new ValidationException("Donation #" + id + " not found.");
    }
    /** Donations in priority order (Merge Sort). */
    public List<Donation> prioritized(boolean onlyAvailable) {
        List<Donation> base = new ArrayList<>();
        for (Donation d : donations) if (!onlyAvailable || d.getStatus() == Status.AVAILABLE) base.add(d);
        return DonationMergeSort.sort(base, DonationPriority.ORDER);
    }
    public List<Donation> searchDonations(String query, Status status) {
        List<Function<Donation, String>> f = List.of(Donation::getDonor, Donation::getFoodName, Donation::getLocation, d -> d.getStatus().name());
        List<Donation> hits = Search.filter(prioritized(false), query, f);
        if (status != null) hits.removeIf(d -> d.getStatus() != status);
        // Available donations (the actionable ones) first; each group keeps its Merge Sort priority order.
        List<Donation> available = new ArrayList<>(), others = new ArrayList<>();
        for (Donation d : hits) (d.getStatus() == Status.AVAILABLE ? available : others).add(d);
        available.addAll(others);
        return available;
    }

    // ---------- Volunteers ----------
    public Volunteer addVolunteer(String name, String phone, String area, String availability) {
        requireText(name, "Name"); requireText(area, "Service area"); validatePhone(phone);
        Volunteer v = new Volunteer(nextId(volunteers.stream().mapToInt(Volunteer::getId)), name.trim(), phone.trim(), area.trim(), availability.trim());
        volunteers.add(v);
        log("Volunteer added: " + v.getName());
        history.push(undo("Added volunteer " + v.getName(), () -> {
            if (pickups.stream().anyMatch(p -> p.getVolunteerId() == v.getId())) throw new ValidationException("Volunteer already has pickups and can't be undone.");
            volunteers.remove(v);
        }));
        save();
        return v;
    }
    public void updateVolunteer(int id, String name, String phone, String area, String availability) {
        requireText(name, "Name"); requireText(area, "Service area"); validatePhone(phone);
        volunteer(id).update(name.trim(), phone.trim(), area.trim(), availability.trim());
        log("Volunteer #" + id + " edited"); save();
    }
    public void setVolunteerActive(int id, boolean active) {
        Volunteer v = volunteer(id);
        if (!active && activePickupsOf(id) > 0) throw new ValidationException(v.getName() + " still has active pickups. Reassign or complete them first.");
        v.setActive(active);
        log("Volunteer " + v.getName() + (active ? " reactivated" : " deactivated")); save();
    }
    public List<Volunteer> volunteers() { return List.copyOf(volunteers); }
    public Volunteer volunteer(int id) {
        for (Volunteer v : volunteers) if (v.getId() == id) return v;
        throw new ValidationException("Volunteer #" + id + " not found.");
    }
    public List<Volunteer> searchVolunteers(String q) {
        return Search.filter(volunteers(), q, List.of(Volunteer::getName, Volunteer::getServiceArea, Volunteer::getPhone, Volunteer::getAvailability));
    }
    public int activePickupsOf(int volunteerId) {
        int n = 0;
        for (Pickup p : pickups) if (p.getVolunteerId() == volunteerId && isActive(p)) n++;
        return n;
    }
    public int completedPickupsOf(int volunteerId) {
        int n = 0;
        for (Pickup p : pickups) if (p.getVolunteerId() == volunteerId && p.getStatus() == Pickup.Status.DELIVERED) n++;
        return n;
    }

    // ---------- Recipients ----------
    public Recipient addRecipient(String name, String contact, String address, String type) {
        requireText(name, "Name"); requireText(address, "Address");
        Recipient r = new Recipient(nextId(recipients.stream().mapToInt(Recipient::getId)), name.trim(), contact.trim(), address.trim(), type.trim());
        recipients.add(r);
        log("Recipient added: " + r.getName());
        history.push(undo("Added recipient " + r.getName(), () -> {
            if (distributions.stream().anyMatch(x -> x.recipientId() == r.getId())) throw new ValidationException("Recipient already has distributions and can't be undone.");
            recipients.remove(r);
        }));
        save();
        return r;
    }
    public void updateRecipient(int id, String name, String contact, String address, String type) {
        requireText(name, "Name"); requireText(address, "Address");
        recipient(id).update(name.trim(), contact.trim(), address.trim(), type.trim());
        log("Recipient #" + id + " edited"); save();
    }
    public List<Recipient> recipients() { return List.copyOf(recipients); }
    public Recipient recipient(int id) {
        for (Recipient r : recipients) if (r.getId() == id) return r;
        throw new ValidationException("Recipient #" + id + " not found.");
    }
    public List<Recipient> searchRecipients(String q) {
        return Search.filter(recipients(), q, List.of(Recipient::getName, Recipient::getAddress, Recipient::getType, Recipient::getContact));
    }

    // ---------- Pickups ----------
    public Pickup schedulePickup(int donationId, int volunteerId, LocalDateTime when) {
        Donation d = donation(donationId);
        Volunteer v = volunteer(volunteerId);
        if (when == null) throw new ValidationException("Choose a pickup date and time.");
        if (d.getStatus() != Status.AVAILABLE) throw new ValidationException("Donation #" + donationId + " is " + label(d.getStatus()) + " and can't be scheduled.");
        if (!v.isActive()) throw new ValidationException(v.getName() + " is deactivated.");
        if (when.isBefore(now())) throw new ValidationException("The pickup time is in the past.");
        if (when.toLocalDate().isAfter(d.getBestBefore())) throw new ValidationException("Pickup would be after the best-before date (" + d.getBestBefore() + ").");
        checkConflict(volunteerId, when, -1);
        Pickup p = new Pickup(nextId(pickups.stream().mapToInt(Pickup::getId)), donationId, volunteerId, when);
        pickups.add(p); pickupQueue.enqueue(p);
        d.setStatus(Status.SCHEDULED);
        log("Pickup #" + p.getId() + " scheduled: donation #" + donationId + " with " + v.getName() + " at " + when.toString().replace('T', ' '));
        history.push(undo("Scheduled pickup #" + p.getId(), () -> {
            if (p.getStatus() != Pickup.Status.SCHEDULED) throw new ValidationException("Pickup #" + p.getId() + " already progressed and can't be undone.");
            pickups.remove(p); pickupQueue.remove(p); d.setStatus(Status.AVAILABLE);
        }));
        save();
        return p;
    }

    public void reschedulePickup(int pickupId, int volunteerId, LocalDateTime when) {
        Pickup p = pickup(pickupId);
        Volunteer v = volunteer(volunteerId);
        if (p.getStatus() != Pickup.Status.SCHEDULED) throw new ValidationException("Only scheduled pickups can be changed.");
        if (!v.isActive()) throw new ValidationException(v.getName() + " is deactivated.");
        if (when == null || when.isBefore(now())) throw new ValidationException("Choose a pickup time in the future.");
        checkConflict(volunteerId, when, pickupId);
        pickupQueue.remove(p); p.reschedule(volunteerId, when); pickupQueue.enqueue(p);
        log("Pickup #" + pickupId + " rescheduled to " + when.toString().replace('T', ' ') + " with " + v.getName());
        save();
    }

    /** Takes the next pickup from the queue (earliest scheduled time) and marks it picked up. */
    public Pickup processNextPickup() {
        Pickup p = pickupQueue.peek();
        if (p == null) throw new ValidationException("No scheduled pickups in the queue.");
        markPickedUp(p.getId());
        return p;
    }

    public void markPickedUp(int pickupId) {
        Pickup p = pickup(pickupId);
        if (p.getStatus() != Pickup.Status.SCHEDULED) throw new ValidationException("Pickup #" + pickupId + " is " + p.getStatus().name().toLowerCase().replace('_', ' ') + ", not scheduled.");
        p.setStatus(Pickup.Status.PICKED_UP); pickupQueue.remove(p);
        donation(p.getDonationId()).setStatus(Status.PICKED_UP);
        log("Pickup #" + pickupId + " picked up"); save();
    }

    public void cancelPickup(int pickupId) {
        Pickup p = pickup(pickupId);
        if (p.getStatus() != Pickup.Status.SCHEDULED) throw new ValidationException("Only scheduled pickups can be cancelled.");
        p.setStatus(Pickup.Status.CANCELLED); pickupQueue.remove(p);
        donation(p.getDonationId()).setStatus(Status.AVAILABLE);
        log("Pickup #" + pickupId + " cancelled; donation #" + p.getDonationId() + " available again"); save();
    }

    /** Confirms delivery when at least one distribution is recorded; any leftover quantity stays recorded as undistributed. */
    public void confirmDelivered(int pickupId) {
        Pickup p = pickup(pickupId);
        if (p.getStatus() != Pickup.Status.PICKED_UP) throw new ValidationException("Only picked-up pickups can be marked delivered.");
        if (distributions.stream().noneMatch(x -> x.donationId() == p.getDonationId()))
            throw new ValidationException("Record at least one distribution to a recipient first.");
        finishDelivery(p); save();
    }

    public Pickup pickup(int id) {
        for (Pickup p : pickups) if (p.getId() == id) return p;
        throw new ValidationException("Pickup #" + id + " not found.");
    }
    public List<Pickup> pickups() { return List.copyOf(pickups); }
    public List<Pickup> queueOrder() { return pickupQueue.toList(); }
    /** Scheduled (queue order), then picked up, delivered, cancelled; earliest first within each group. */
    public List<Pickup> pickupsInDisplayOrder() {
        return DonationMergeSort.sort(pickups, Comparator.<Pickup>comparingInt(p -> p.getStatus().ordinal()).thenComparing(Pickup::getScheduledAt));
    }
    public boolean isOverdue(Pickup p) { return p.getStatus() == Pickup.Status.SCHEDULED && p.getScheduledAt().isBefore(now()); }
    public List<Pickup> upcomingPickups(int limit) {
        List<Pickup> q = pickupQueue.toList();
        return q.size() > limit ? q.subList(0, limit) : q;
    }

    // ---------- Distributions ----------
    public Distribution recordDistribution(int donationId, int recipientId, String qty, LocalDate date) {
        Donation d = donation(donationId); recipient(recipientId);
        if (d.getStatus() != Status.PICKED_UP) throw new ValidationException("Donation #" + donationId + " must be picked up before it can be distributed (currently " + label(d.getStatus()) + ").");
        double q = parsePositive(qty, "Quantity");
        if (q > d.getRemaining() + 1e-9) throw new ValidationException("Only " + fmt(d.getRemaining()) + " " + d.getUnit() + " left to distribute.");
        if (date == null || date.isAfter(today())) throw new ValidationException("Delivery date can't be in the future.");
        Distribution x = new Distribution(nextId(distributions.stream().mapToInt(Distribution::id)), donationId, recipientId, q, date);
        distributions.add(x);
        d.setRemaining(Math.max(0, d.getRemaining() - q));
        log("Distributed " + fmt(q) + " " + d.getUnit() + " of donation #" + donationId + " to " + recipient(recipientId).getName());
        if (d.getRemaining() <= 1e-9)
            for (Pickup p : pickups) if (p.getDonationId() == donationId && p.getStatus() == Pickup.Status.PICKED_UP) finishDelivery(p);
        save();
        return x;
    }
    public List<Distribution> distributions() { return List.copyOf(distributions); }

    // ---------- Reports ----------
    public Map<Status, Integer> statusCounts() {
        Map<Status, Integer> m = new EnumMap<>(Status.class);
        for (Status s : Status.values()) m.put(s, 0);
        for (Donation d : donations) m.merge(d.getStatus(), 1, Integer::sum);
        return m;
    }
    /** Quantities are totalled per unit because kg and pieces can't be added together. */
    public Map<String, Double> donatedByUnit() {
        Map<String, Double> m = new TreeMap<>();
        for (Donation d : donations) if (d.getStatus() != Status.CANCELLED) m.merge(d.getUnit(), d.getQuantity(), Double::sum);
        return m;
    }
    public Map<String, Double> pickedUpByUnit() {
        Map<String, Double> m = new TreeMap<>();
        for (Donation d : donations) if (d.getStatus() == Status.PICKED_UP || d.getStatus() == Status.DELIVERED) m.merge(d.getUnit(), d.getQuantity(), Double::sum);
        return m;
    }
    public Map<String, Double> deliveredByUnit() {
        Map<String, Double> m = new TreeMap<>();
        for (Distribution x : distributions) m.merge(donation(x.donationId()).getUnit(), x.quantity(), Double::sum);
        return m;
    }
    public List<Donation> nearingBestBefore(int days) {
        List<Donation> out = new ArrayList<>();
        LocalDate limit = today().plusDays(days);
        for (Donation d : donations)
            if ((d.getStatus() == Status.AVAILABLE || d.getStatus() == Status.SCHEDULED || d.getStatus() == Status.PICKED_UP)
                    && d.getRemaining() > 0 && !d.getBestBefore().isAfter(limit)) out.add(d);
        return DonationMergeSort.sort(out, Comparator.comparing(Donation::getBestBefore));
    }
    /** rows of {date, donation id, food, donor, recipient, quantity, unit}, optionally filtered. */
    public List<String[]> distributionRows(LocalDate from, LocalDate to, String foodContains, String donorContains) {
        List<String[]> rows = new ArrayList<>();
        for (Distribution x : distributions) {
            Donation d = donation(x.donationId());
            if (from != null && x.deliveredOn().isBefore(from)) continue;
            if (to != null && x.deliveredOn().isAfter(to)) continue;
            if (foodContains != null && !foodContains.isBlank() && !d.getFoodName().toLowerCase().contains(foodContains.trim().toLowerCase())) continue;
            if (donorContains != null && !donorContains.isBlank() && !d.getDonor().toLowerCase().contains(donorContains.trim().toLowerCase())) continue;
            rows.add(new String[]{x.deliveredOn().toString(), String.valueOf(d.getId()), d.getFoodName(), d.getDonor(),
                    recipient(x.recipientId()).getName(), fmt(x.quantity()), d.getUnit()});
        }
        return rows;
    }
    public static final String[] DONATION_HEADER = {"ID", "Donor", "Food", "Quantity", "Remaining", "Unit", "Location", "Distance km", "Perishability", "Harvest date", "Best before", "Status"};
    public List<String[]> donationRows() {
        List<String[]> rows = new ArrayList<>();
        for (Donation d : prioritized(false)) rows.add(new String[]{"" + d.getId(), d.getDonor(), d.getFoodName(), fmt(d.getQuantity()), fmt(d.getRemaining()),
                d.getUnit(), d.getLocation(), fmt(d.getDistanceKm()), "" + d.getPerishability(), "" + d.getHarvestDate(), "" + d.getBestBefore(), d.getStatus().name()});
        return rows;
    }
    public static final String[] DISTRIBUTION_HEADER = {"Delivered on", "Donation ID", "Food", "Donor", "Recipient", "Quantity", "Unit"};

    public List<String[]> activity(int limit) {
        List<String[]> out = new ArrayList<>();
        for (int i = activity.size() - 1; i >= 0 && out.size() < limit; i--) out.add(activity.get(i));
        return out;
    }

    // ---------- Undo ----------
    public boolean canUndo() { return !history.isEmpty(); }
    public String nextUndoDescription() { return history.peekDescription(); }
    public String undoLast() {
        String d = history.undoLast();
        if (d == null) throw new ValidationException("Nothing to undo.");
        log("Undid: " + d); save();
        return d;
    }

    // ---------- Helpers ----------
    public static String label(Object e) {
        String s = e.toString().toLowerCase().replace('_', ' ');
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
    public static String fmt(double v) { return java.math.BigDecimal.valueOf(v).setScale(2, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString(); }

    private void finishDelivery(Pickup p) {
        p.setStatus(Pickup.Status.DELIVERED);
        donation(p.getDonationId()).setStatus(Status.DELIVERED);
        log("Pickup #" + p.getId() + " delivered");
    }
    private boolean isActive(Pickup p) { return p.getStatus() == Pickup.Status.SCHEDULED || p.getStatus() == Pickup.Status.PICKED_UP; }
    private void checkConflict(int volunteerId, LocalDateTime when, int ignorePickupId) {
        for (Pickup o : pickups) {
            if (o.getId() == ignorePickupId || o.getVolunteerId() != volunteerId || o.getStatus() != Pickup.Status.SCHEDULED) continue;
            if (Math.abs(Duration.between(o.getScheduledAt(), when).toMinutes()) < PICKUP_SLOT_MINUTES)
                throw new ValidationException(volunteer(volunteerId).getName() + " already has pickup #" + o.getId() + " at " + o.getScheduledAt().toLocalTime() + " (pickups need " + PICKUP_SLOT_MINUTES + " minutes apart).");
        }
    }
    private void validateDonation(String donor, String food, String unit, String location, int perish, LocalDate harvest, LocalDate best, boolean isNew) {
        requireText(donor, "Donor"); requireText(food, "Food name"); requireText(unit, "Unit"); requireText(location, "Location");
        if (perish < 1 || perish > 5) throw new ValidationException("Perishability must be between 1 and 5.");
        if (harvest == null || best == null) throw new ValidationException("Harvest date and best-before date are required.");
        if (harvest.isAfter(today())) throw new ValidationException("Harvest date can't be in the future.");
        if (best.isBefore(harvest)) throw new ValidationException("Best-before date can't be earlier than the harvest date.");
        if (isNew && best.isBefore(today())) throw new ValidationException("This food is already past its best-before date.");
    }
    private static void requireText(String s, String field) { if (s == null || s.isBlank()) throw new ValidationException(field + " is required."); }
    private static void validatePhone(String phone) {
        if (phone == null || !phone.trim().matches("[0-9+()\\- ]{7,20}")) throw new ValidationException("Phone must be 7-20 characters of digits, spaces, +, - or ().");
    }
    private static double parsePositive(String s, String field) {
        double v = parseNumber(s, field);
        if (v <= 0) throw new ValidationException(field + " must be greater than 0.");
        return v;
    }
    private static double parseNonNegative(String s, String field) {
        double v = parseNumber(s, field);
        if (v < 0) throw new ValidationException(field + " can't be negative.");
        return v;
    }
    private static double parseNumber(String s, String field) {
        if (s == null || s.isBlank()) throw new ValidationException(field + " is required.");
        try {
            double v = Double.parseDouble(s.trim());
            if (Double.isNaN(v) || Double.isInfinite(v)) throw new NumberFormatException();
            return v;
        } catch (NumberFormatException e) { throw new ValidationException(field + " must be a number."); }
    }
    private static int nextId(java.util.stream.IntStream ids) { return ids.max().orElse(0) + 1; }
    private ActionHistory.Undoable undo(String text, Runnable r) {
        return new ActionHistory.Undoable() {
            public String describe() { return text; }
            public void undo() { r.run(); }
        };
    }
    private void log(String text) { activity.add(new String[]{now().withNano(0).toString(), text}); }

    // ---------- Persistence ----------
    private void save() {
        List<String[]> r = new ArrayList<>();
        for (Donation d : donations) r.add(new String[]{"" + d.getId(), d.getDonor(), d.getFoodName(), "" + d.getQuantity(), "" + d.getRemaining(), d.getUnit(),
                d.getLocation(), "" + d.getDistanceKm(), "" + d.getPerishability(), "" + d.getHarvestDate(), "" + d.getBestBefore(), d.getStatus().name()});
        store.write("donations.tsv", r);
        r = new ArrayList<>();
        for (Volunteer v : volunteers) r.add(new String[]{"" + v.getId(), v.getName(), v.getPhone(), v.getServiceArea(), v.getAvailability(), "" + v.isActive()});
        store.write("volunteers.tsv", r);
        r = new ArrayList<>();
        for (Recipient x : recipients) r.add(new String[]{"" + x.getId(), x.getName(), x.getContact(), x.getAddress(), x.getType()});
        store.write("recipients.tsv", r);
        r = new ArrayList<>();
        for (Pickup p : pickups) r.add(new String[]{"" + p.getId(), "" + p.getDonationId(), "" + p.getVolunteerId(), "" + p.getScheduledAt(), p.getStatus().name()});
        store.write("pickups.tsv", r);
        r = new ArrayList<>();
        for (Distribution x : distributions) r.add(new String[]{"" + x.id(), "" + x.donationId(), "" + x.recipientId(), "" + x.quantity(), "" + x.deliveredOn()});
        store.write("distributions.tsv", r);
        store.write("activity.tsv", activity);
    }

    private void load() {
        for (String[] c : store.read("donations.tsv")) {
            Donation d = new Donation(Integer.parseInt(c[0]), c[1], c[2], Double.parseDouble(c[3]), c[5], c[6], Double.parseDouble(c[7]),
                    Integer.parseInt(c[8]), LocalDate.parse(c[9]), LocalDate.parse(c[10]));
            d.setRemaining(Double.parseDouble(c[4])); d.setStatus(Status.valueOf(c[11]));
            donations.add(d);
        }
        for (String[] c : store.read("volunteers.tsv")) {
            Volunteer v = new Volunteer(Integer.parseInt(c[0]), c[1], c[2], c[3], c[4]);
            v.setActive(Boolean.parseBoolean(c[5])); volunteers.add(v);
        }
        for (String[] c : store.read("recipients.tsv")) recipients.add(new Recipient(Integer.parseInt(c[0]), c[1], c[2], c[3], c[4]));
        for (String[] c : store.read("pickups.tsv")) {
            Pickup p = new Pickup(Integer.parseInt(c[0]), Integer.parseInt(c[1]), Integer.parseInt(c[2]), LocalDateTime.parse(c[3]));
            p.setStatus(Pickup.Status.valueOf(c[4])); pickups.add(p);
            if (p.getStatus() == Pickup.Status.SCHEDULED) pickupQueue.enqueue(p);
        }
        for (String[] c : store.read("distributions.tsv"))
            distributions.add(new Distribution(Integer.parseInt(c[0]), Integer.parseInt(c[1]), Integer.parseInt(c[2]), Double.parseDouble(c[3]), LocalDate.parse(c[4])));
        activity.addAll(store.read("activity.tsv"));
    }
}
