package com.silo.ui;

import com.silo.model.*;
import com.silo.service.SiloService;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

class PickupsPage extends Page {
    private final JTable table = Ui.table("ID", "Donation", "Food", "Volunteer", "Scheduled for", "Status");
    private final JLabel next = Ui.muted(" ");
    private final JButton reschedule = Ui.button("Reschedule", false), pickedUp = Ui.button("Mark picked up", false),
            delivered = Ui.button("Confirm delivered", false), cancel = Ui.button("Cancel pickup", false, true), processNext = Ui.button("Process next in queue", false);

    PickupsPage(SiloApp app) {
        super(app);
        JButton add = Ui.button("+ Schedule pickup", true);
        add.addActionListener(e -> { if (new ScheduleDialog(app, svc, null).open()) app.refreshAll(); });
        reschedule.addActionListener(e -> { int id = Ui.selectedId(table, 0); if (id > 0 && new ScheduleDialog(app, svc, svc.pickup(id)).open()) app.refreshAll(); });
        pickedUp.addActionListener(e -> { int id = Ui.selectedId(table, 0); if (id > 0) app.act("Pickup #" + id + " marked picked up", () -> svc.markPickedUp(id)); });
        delivered.addActionListener(e -> { int id = Ui.selectedId(table, 0); if (id > 0) app.act("Pickup #" + id + " delivered", () -> svc.confirmDelivered(id)); });
        cancel.addActionListener(e -> {
            int id = Ui.selectedId(table, 0);
            if (id > 0 && confirm("Cancel pickup #" + id + "? The donation becomes available again.")) app.act("Pickup #" + id + " cancelled", () -> svc.cancelPickup(id));
        });
        processNext.addActionListener(e -> app.act(null, () -> { Pickup p = svc.processNextPickup(); app.success("Pickup #" + p.getId() + " (earliest in the queue) marked picked up"); }));

        add(header("Pickups", "Scheduled pickups are processed in time order (queue)", add), BorderLayout.NORTH);
        JPanel center = new JPanel(new BorderLayout(0, 10)); center.setOpaque(false);
        JPanel top = new JPanel(new BorderLayout(0, 6)); top.setOpaque(false);
        top.add(toolbar(processNext, reschedule, pickedUp, delivered, cancel), BorderLayout.NORTH);
        top.add(next, BorderLayout.SOUTH);
        center.add(top, BorderLayout.NORTH);
        center.add(tableWithHint(table, "No pickups yet. Schedule one for an available donation and an active volunteer."), BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);
        table.getSelectionModel().addListSelectionListener(e -> updateButtons());
    }

    private void updateButtons() {
        int id = Ui.selectedId(table, 0);
        Pickup p = id > 0 ? svc.pickup(id) : null;
        boolean sched = p != null && p.getStatus() == Pickup.Status.SCHEDULED;
        reschedule.setEnabled(sched); pickedUp.setEnabled(sched); cancel.setEnabled(sched);
        delivered.setEnabled(p != null && p.getStatus() == Pickup.Status.PICKED_UP);
        processNext.setEnabled(!svc.queueOrder().isEmpty());
    }

    @Override void refresh() {
        List<Object[]> rows = new ArrayList<>();
        for (Pickup p : svc.pickupsInDisplayOrder()) {
            String st = SiloService.label(p.getStatus());
            rows.add(new Object[]{p.getId(), "#" + p.getDonationId(), svc.donation(p.getDonationId()).getFoodName(), svc.volunteer(p.getVolunteerId()).getName(),
                    p.getScheduledAt().toString().replace('T', ' '), svc.isOverdue(p) ? "Overdue (scheduled)" : st});
        }
        Ui.fill(table, rows);
        Pickup n = svc.queueOrder().isEmpty() ? null : svc.queueOrder().get(0);
        next.setText(n == null ? "Queue is empty." : "Next in queue: pickup #" + n.getId() + " - " + svc.donation(n.getDonationId()).getFoodName() + " at " + n.getScheduledAt().toString().replace('T', ' ')
                + "  |  " + svc.queueOrder().size() + " waiting");
        updateButtons();
    }

    /** Schedules a new pickup (existing == null) or changes the volunteer/time of an existing one. */
    static class ScheduleDialog extends FormDialog {
        private final SiloService svc; private final Pickup existing;
        private final JComboBox<Ui.Item> donation = new JComboBox<>(), volunteer = new JComboBox<>();
        private final JSpinner when;

        ScheduleDialog(SiloApp app, SiloService svc, Pickup p) {
            super(app, p == null ? "Schedule pickup" : "Reschedule pickup #" + p.getId(), p == null ? "Schedule" : "Save changes");
            this.svc = svc; this.existing = p;
            if (p == null) {
                for (Donation d : svc.prioritized(true)) donation.addItem(new Ui.Item(d.getId(), "#" + d.getId() + "  " + d.getFoodName() + " (" + SiloService.fmt(d.getRemaining()) + " " + d.getUnit() + ", " + d.getLocation() + ")"));
                addRow("Donation (priority order) *", donation);
            } else addRow("Donation", new JLabel("#" + p.getDonationId() + " " + svc.donation(p.getDonationId()).getFoodName()));
            for (Volunteer v : svc.volunteers()) if (v.isActive()) volunteer.addItem(new Ui.Item(v.getId(), v.getName() + " - " + v.getServiceArea() + " (" + v.getAvailability() + ")"));
            if (p != null) for (int i = 0; i < volunteer.getItemCount(); i++) if (volunteer.getItemAt(i).id() == p.getVolunteerId()) volunteer.setSelectedIndex(i);
            addRow("Volunteer *", volunteer);
            when = Ui.dateTimeSpinner(p == null ? svc.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0) : p.getScheduledAt());
            addRow("Date and time *", when);
            if (p == null && donation.getItemCount() == 0) setError("There are no available donations to schedule.");
            else if (volunteer.getItemCount() == 0) setError("There are no active volunteers. Add or reactivate one first.");
        }

        @Override protected void onSave() {
            Ui.Item v = (Ui.Item) volunteer.getSelectedItem();
            if (v == null) throw new com.silo.service.ValidationException("Choose a volunteer.");
            if (existing == null) {
                Ui.Item d = (Ui.Item) donation.getSelectedItem();
                if (d == null) throw new com.silo.service.ValidationException("Choose a donation.");
                svc.schedulePickup(d.id(), v.id(), Ui.dateTimeOf(when));
            } else svc.reschedulePickup(existing.getId(), v.id(), Ui.dateTimeOf(when));
            ((SiloApp) getOwner()).success(existing == null ? "Pickup scheduled" : "Pickup updated");
        }
    }
}
