package com.silo.ui;

import com.silo.model.Volunteer;
import com.silo.service.SiloService;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

class VolunteersPage extends Page {
    private final JTable table = Ui.table("ID", "Name", "Phone", "Service area", "Availability", "Status", "Active tasks", "Completed");
    private final JTextField search = searchField("Search name, area, phone or availability", this::refresh);
    private final JButton edit = Ui.button("Edit", false), toggle = Ui.button("Deactivate", false, true);

    VolunteersPage(SiloApp app) {
        super(app);
        JButton add = Ui.button("+ Add volunteer", true);
        add.addActionListener(e -> { if (new VolunteerDialog(app, svc, null).open()) app.refreshAll(); });
        edit.addActionListener(e -> { int id = Ui.selectedId(table, 0); if (id > 0 && new VolunteerDialog(app, svc, svc.volunteer(id)).open()) app.refreshAll(); });
        toggle.addActionListener(e -> {
            int id = Ui.selectedId(table, 0); if (id < 0) return;
            Volunteer v = svc.volunteer(id);
            if (v.isActive()) { if (confirm("Deactivate " + v.getName() + "? They can't be assigned to new pickups until reactivated.")) app.act(v.getName() + " deactivated", () -> svc.setVolunteerActive(id, false)); }
            else app.act(v.getName() + " reactivated", () -> svc.setVolunteerActive(id, true));
        });
        add(header("Volunteers", "People who collect and deliver donations", add), BorderLayout.NORTH);
        JPanel center = new JPanel(new BorderLayout(0, 10)); center.setOpaque(false);
        center.add(toolbar(search, edit, toggle), BorderLayout.NORTH);
        center.add(tableWithHint(table, "No volunteers found. Add a volunteer so pickups can be assigned."), BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);
        table.getSelectionModel().addListSelectionListener(e -> updateButtons());
    }

    private void updateButtons() {
        int id = Ui.selectedId(table, 0);
        edit.setEnabled(id > 0); toggle.setEnabled(id > 0);
        if (id > 0) toggle.setText(svc.volunteer(id).isActive() ? "Deactivate" : "Reactivate");
    }

    @Override void refresh() {
        List<Object[]> rows = new ArrayList<>();
        for (Volunteer v : svc.searchVolunteers(search.getText()))
            rows.add(new Object[]{v.getId(), v.getName(), v.getPhone(), v.getServiceArea(), v.getAvailability(), v.isActive() ? "Active" : "Inactive", svc.activePickupsOf(v.getId()), svc.completedPickupsOf(v.getId())});
        Ui.fill(table, rows);
        updateButtons();
    }

    static class VolunteerDialog extends FormDialog {
        private final SiloService svc; private final Volunteer existing;
        private final JTextField name = Ui.field(20), phone = Ui.field(20), area = Ui.field(20), avail = Ui.field(20);

        VolunteerDialog(SiloApp app, SiloService svc, Volunteer v) {
            super(app, v == null ? "Add volunteer" : "Edit volunteer", v == null ? "Add volunteer" : "Save changes");
            this.svc = svc; this.existing = v;
            if (v != null) { name.setText(v.getName()); phone.setText(v.getPhone()); area.setText(v.getServiceArea()); avail.setText(v.getAvailability()); }
            addRow("Full name *", name); addRow("Phone *", phone); addRow("Service area *", area); addRow("Availability", avail);
        }
        @Override protected void onSave() {
            if (existing == null) svc.addVolunteer(name.getText(), phone.getText(), area.getText(), avail.getText());
            else svc.updateVolunteer(existing.getId(), name.getText(), phone.getText(), area.getText(), avail.getText());
            ((SiloApp) getOwner()).success(existing == null ? "Volunteer added" : "Volunteer updated");
        }
    }
}
