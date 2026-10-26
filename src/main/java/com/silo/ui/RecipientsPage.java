package com.silo.ui;

import com.silo.model.*;
import com.silo.model.Donation.Status;
import com.silo.service.SiloService;
import com.silo.service.ValidationException;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

class RecipientsPage extends Page {
    private final JTable recipients = Ui.table("ID", "Name", "Type", "Contact", "Address", "Received");
    private final JTable history = Ui.table("Delivered on", "Donation", "Food", "Recipient", "Quantity");
    private final JTextField search = searchField("Search name, type, contact or address", this::refresh);
    private final JButton edit = Ui.button("Edit", false);

    RecipientsPage(SiloApp app) {
        super(app);
        JButton add = Ui.button("+ Add recipient", true), dist = Ui.button("Record distribution", false);
        add.addActionListener(e -> { if (new RecipientDialog(app, svc, null).open()) app.refreshAll(); });
        edit.addActionListener(e -> { int id = Ui.selectedId(recipients, 0); if (id > 0 && new RecipientDialog(app, svc, svc.recipient(id)).open()) app.refreshAll(); });
        dist.addActionListener(e -> { if (new DistributionDialog(app, svc, Ui.selectedId(recipients, 0)).open()) app.refreshAll(); });
        add(header("Recipients & distribution", "Partner organisations and what they received", add, dist), BorderLayout.NORTH);

        JPanel top = new JPanel(new BorderLayout(0, 10)); top.setOpaque(false);
        top.add(toolbar(search, edit), BorderLayout.NORTH);
        top.add(tableWithHint(recipients, "No recipients yet. Add the organisations that receive food."), BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout(0, 8)); bottom.setOpaque(false);
        bottom.add(Ui.heading("Distribution history"), BorderLayout.NORTH);
        bottom.add(tableWithHint(history, "No distributions recorded yet. Once a donation is picked up you can record what each recipient received."), BorderLayout.CENTER);
        JPanel both = new JPanel(new GridLayout(2, 1, 0, 14)); both.setOpaque(false);
        both.add(top); both.add(bottom);
        add(both, BorderLayout.CENTER);
        recipients.getSelectionModel().addListSelectionListener(e -> edit.setEnabled(Ui.selectedId(recipients, 0) > 0));
    }

    @Override void refresh() {
        Map<Integer, Double> received = new HashMap<>();
        for (Distribution x : svc.distributions()) received.merge(x.recipientId(), 1.0, Double::sum);
        List<Object[]> rows = new ArrayList<>();
        for (Recipient r : svc.searchRecipients(search.getText()))
            rows.add(new Object[]{r.getId(), r.getName(), r.getType(), r.getContact(), r.getAddress(), received.getOrDefault(r.getId(), 0.0).intValue() + " deliveries"});
        Ui.fill(recipients, rows);
        rows = new ArrayList<>();
        List<Distribution> all = new ArrayList<>(svc.distributions());
        Collections.reverse(all);
        for (Distribution x : all) {
            Donation d = svc.donation(x.donationId());
            rows.add(new Object[]{x.deliveredOn(), "#" + d.getId(), d.getFoodName(), svc.recipient(x.recipientId()).getName(), SiloService.fmt(x.quantity()) + " " + d.getUnit()});
        }
        Ui.fill(history, rows);
        edit.setEnabled(Ui.selectedId(recipients, 0) > 0);
    }

    static class RecipientDialog extends FormDialog {
        private final SiloService svc; private final Recipient existing;
        private final JTextField name = Ui.field(20), contact = Ui.field(20), address = Ui.field(20);
        private final JComboBox<String> type = new JComboBox<>(new String[]{"Community kitchen", "Shelter", "Food bank", "Barangay", "School", "Other"});

        RecipientDialog(SiloApp app, SiloService svc, Recipient r) {
            super(app, r == null ? "Add recipient" : "Edit recipient", r == null ? "Add recipient" : "Save changes");
            this.svc = svc; this.existing = r; type.setEditable(true);
            if (r != null) { name.setText(r.getName()); contact.setText(r.getContact()); address.setText(r.getAddress()); type.setSelectedItem(r.getType()); }
            addRow("Name *", name); addRow("Type", type); addRow("Contact", contact); addRow("Address *", address);
        }
        @Override protected void onSave() {
            String t = String.valueOf(type.getSelectedItem());
            if (existing == null) svc.addRecipient(name.getText(), contact.getText(), address.getText(), t);
            else svc.updateRecipient(existing.getId(), name.getText(), contact.getText(), address.getText(), t);
            ((SiloApp) getOwner()).success(existing == null ? "Recipient added" : "Recipient updated");
        }
    }

    static class DistributionDialog extends FormDialog {
        private final SiloService svc;
        private final JComboBox<Ui.Item> donation = new JComboBox<>(), recipient = new JComboBox<>();
        private final JTextField qty = Ui.field(20);
        private final JSpinner date;

        DistributionDialog(SiloApp app, SiloService svc, int preselectRecipient) {
            super(app, "Record distribution", "Record");
            this.svc = svc;
            for (Donation d : svc.prioritized(false)) if (d.getStatus() == Status.PICKED_UP && d.getRemaining() > 0)
                donation.addItem(new Ui.Item(d.getId(), "#" + d.getId() + "  " + d.getFoodName() + " (" + SiloService.fmt(d.getRemaining()) + " " + d.getUnit() + " left)"));
            for (Recipient r : svc.recipients()) {
                recipient.addItem(new Ui.Item(r.getId(), r.getName()));
                if (r.getId() == preselectRecipient) recipient.setSelectedIndex(recipient.getItemCount() - 1);
            }
            date = Ui.dateSpinner(svc.today());
            addRow("Donation (picked up) *", donation); addRow("Recipient *", recipient); addRow("Quantity delivered *", qty); addRow("Delivery date *", date);
            if (donation.getItemCount() == 0) setError("No picked-up donations with food left. Mark a pickup as picked up first.");
            else if (recipient.getItemCount() == 0) setError("Add a recipient first.");
        }
        @Override protected void onSave() {
            Ui.Item d = (Ui.Item) donation.getSelectedItem(), r = (Ui.Item) recipient.getSelectedItem();
            if (d == null || r == null) throw new ValidationException("Choose a donation and a recipient.");
            svc.recordDistribution(d.id(), r.id(), qty.getText(), Ui.dateOf(date));
            ((SiloApp) getOwner()).success("Distribution recorded");
        }
    }
}
