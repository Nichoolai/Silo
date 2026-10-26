package com.silo.ui;

import com.silo.model.Donation;
import com.silo.model.Donation.Status;
import com.silo.service.SiloService;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.*;
import java.util.List;

class DonationsPage extends Page {
    private final JTable table = Ui.table("Rank", "ID", "Food", "Donor", "Remaining / total", "Location", "Km", "Perishability", "Best before", "Status");
    private final JTextField search = searchField("Search food, donor, location or status", this::refresh);
    private final JComboBox<String> statusBox = new JComboBox<>();
    private final JButton edit = Ui.button("Edit", false), cancel = Ui.button("Cancel donation", false, true);

    DonationsPage(SiloApp app) {
        super(app);
        JButton add = Ui.button("+ Add donation", true);
        add.addActionListener(e -> { if (new DonationDialog(app, svc, null).open()) app.refreshAll(); });
        edit.addActionListener(e -> { int id = Ui.selectedId(table, 1); if (id > 0 && new DonationDialog(app, svc, svc.donation(id)).open()) app.refreshAll(); });
        cancel.addActionListener(e -> {
            int id = Ui.selectedId(table, 1);
            if (id > 0 && confirm("Cancel donation #" + id + "? Any scheduled pickup for it will be cancelled too.")) app.act("Donation #" + id + " cancelled", () -> svc.cancelDonation(id));
        });
        statusBox.addItem("All statuses");
        for (Status s : Status.values()) statusBox.addItem(SiloService.label(s));
        statusBox.addActionListener(e -> refresh());
        statusBox.setFont(Ui.font(Font.PLAIN, 13));

        add(header("Donations", "Available donations are listed first, ranked by Merge Sort priority", add), BorderLayout.NORTH);
        JPanel center = new JPanel(new BorderLayout(0, 10)); center.setOpaque(false);
        center.add(toolbar(search, statusBox, edit, cancel), BorderLayout.NORTH);
        center.add(tableWithHint(table, "No donations match. Add one with the button above, or clear the search and filter."), BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);
        table.getSelectionModel().addListSelectionListener(e -> updateButtons());
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) { if (e.getClickCount() == 2) edit.doClick(); }
        });
    }

    static String perishLabel(int p) { return p + " - " + new String[]{"", "Very low", "Low", "Medium", "High", "Very high"}[p]; }

    private void updateButtons() {
        int id = Ui.selectedId(table, 1);
        Donation d = id > 0 ? svc.donation(id) : null;
        edit.setEnabled(d != null && d.getStatus() != Status.DELIVERED && d.getStatus() != Status.CANCELLED);
        cancel.setEnabled(d != null && (d.getStatus() == Status.AVAILABLE || d.getStatus() == Status.SCHEDULED));
    }

    @Override void refresh() {
        int sel = statusBox.getSelectedIndex();
        List<Donation> list = svc.searchDonations(search.getText(), sel <= 0 ? null : Status.values()[sel - 1]);
        List<Object[]> rows = new ArrayList<>();
        int rank = 0;
        for (Donation d : list) {
            boolean avail = d.getStatus() == Status.AVAILABLE;
            rows.add(new Object[]{avail ? String.valueOf(++rank) : "-", d.getId(), d.getFoodName(), d.getDonor(),
                    SiloService.fmt(d.getRemaining()) + " / " + SiloService.fmt(d.getQuantity()) + " " + d.getUnit(), d.getLocation(),
                    SiloService.fmt(d.getDistanceKm()), perishLabel(d.getPerishability()), d.getBestBefore(), SiloService.label(d.getStatus())});
        }
        Ui.fill(table, rows);
        updateButtons();
    }

    static class DonationDialog extends FormDialog {
        private final SiloService svc; private final Donation existing;
        private final JTextField donor = Ui.field(20), food = Ui.field(20), qty = Ui.field(20), location = Ui.field(20), distance = Ui.field(20);
        private final JComboBox<String> unit = new JComboBox<>(new String[]{"kg", "pcs", "sacks", "crates", "liters"});
        private final JComboBox<String> perish = new JComboBox<>();
        private final JSpinner harvest, best;

        DonationDialog(SiloApp app, SiloService svc, Donation d) {
            super(app, d == null ? "Add donation" : "Edit donation #" + d.getId(), d == null ? "Add donation" : "Save changes");
            this.svc = svc; this.existing = d;
            unit.setEditable(true);
            for (int i = 1; i <= 5; i++) perish.addItem(perishLabel(i));
            LocalDate today = svc.today();
            harvest = Ui.dateSpinner(d == null ? today : d.getHarvestDate());
            best = Ui.dateSpinner(d == null ? today.plusDays(3) : d.getBestBefore());
            if (d != null) {
                donor.setText(d.getDonor()); food.setText(d.getFoodName()); qty.setText(SiloService.fmt(d.getQuantity())); unit.setSelectedItem(d.getUnit());
                location.setText(d.getLocation()); distance.setText(SiloService.fmt(d.getDistanceKm())); perish.setSelectedIndex(d.getPerishability() - 1);
            } else { perish.setSelectedIndex(2); distance.setText("0"); }
            addRow("Donor / farm *", donor); addRow("Food *", food); addRow("Quantity *", qty); addRow("Unit *", unit);
            addRow("Pickup location *", location); addRow("Distance to pickup (km) *", distance); addRow("Perishability *", perish);
            addRow("Harvest date *", harvest); addRow("Best before *", best);
        }

        @Override protected void onSave() {
            String u = String.valueOf(unit.getSelectedItem());
            if (existing == null) svc.addDonation(donor.getText(), food.getText(), qty.getText(), u, location.getText(), distance.getText(), perish.getSelectedIndex() + 1, Ui.dateOf(harvest), Ui.dateOf(best));
            else svc.updateDonation(existing.getId(), donor.getText(), food.getText(), qty.getText(), u, location.getText(), distance.getText(), perish.getSelectedIndex() + 1, Ui.dateOf(harvest), Ui.dateOf(best));
            ((SiloApp) getOwner()).success(existing == null ? "Donation added" : "Donation updated");
        }
    }
}
