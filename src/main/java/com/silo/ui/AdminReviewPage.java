package com.silo.ui;

import com.silo.service.AuthService;

import javax.swing.*;
import java.awt.*;
import java.util.List;

class AdminReviewPage extends Page {
    private final AuthService authService;
    private final JTable table;
    private final javax.swing.table.DefaultTableModel model;

    AdminReviewPage(SiloApp app) {
        super(app);
        this.authService = app.authService();
        this.table = Ui.table("Applicant", "Email address");
        this.model = Ui.model(table);
        buildUI();
        refresh();
    }

    private void buildUI() {
        JButton refreshButton = Ui.button("Refresh", false);
        JButton approveButton = Ui.button("Approve", true);
        JButton rejectButton = Ui.button("Reject", false, true);

        refreshButton.addActionListener(e -> refresh());
        approveButton.addActionListener(e -> approveSelected());
        rejectButton.addActionListener(e -> rejectSelected());

        JComponent top = header(
                "Coordinator applications",
                "Review pending applicants before they can access coordinator features.",
                refreshButton
        );

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(approveButton);
        actions.add(rejectButton);

        JPanel workspace = new JPanel(new BorderLayout(0, 12));
        workspace.setOpaque(false);
        workspace.add(actions, BorderLayout.NORTH);
        workspace.add(tableWithHint(table, "No coordinator applications are pending."), BorderLayout.CENTER);

        table.getSelectionModel().addListSelectionListener(e -> {
            boolean selected = table.getSelectedRow() >= 0;
            approveButton.setEnabled(selected);
            rejectButton.setEnabled(selected);
        });
        approveButton.setEnabled(false);
        rejectButton.setEnabled(false);
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    reviewSelected();
                }
            }
        });

        add(top, BorderLayout.NORTH);
        add(workspace, BorderLayout.CENTER);
    }

    private void approveSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            app.error("Select an applicant first.");
            return;
        }

        String email = (String) model.getValueAt(table.convertRowIndexToModel(row), 1);
        try {
            authService.approveCoordinator(email);
            app.success("Approved coordinator: " + email);
            refresh();
        } catch (IllegalArgumentException ex) {
            app.error(ex.getMessage());
        }
    }

    private void rejectSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            app.error("Select an applicant first.");
            return;
        }

        String email = (String) model.getValueAt(table.convertRowIndexToModel(row), 1);
        if (!confirm("Reject the coordinator application from " + email + "?")) {
            return;
        }
        try {
            authService.rejectCoordinator(email);
            app.success("Rejected coordinator: " + email);
            refresh();
        } catch (IllegalArgumentException ex) {
            app.error(ex.getMessage());
        }
    }

    private void reviewSelected() {
        int row = table.getSelectedRow();
        if (row >= 0) {
            int modelRow = table.convertRowIndexToModel(row);
            JOptionPane.showMessageDialog(
                this,
                "Applicant: " + model.getValueAt(modelRow, 0)
                    + "\nEmail: " + model.getValueAt(modelRow, 1)
                    + "\nStatus: Pending review",
                "Review coordinator application",
                JOptionPane.INFORMATION_MESSAGE
            );
        }
    }

    @Override
    void refresh() {
        model.setRowCount(0);
        List<AuthService.Account> applications = authService.getCoordinatorApplications();
        for (AuthService.Account account : applications) {
            model.addRow(new Object[] { account.getName(), account.getEmail() });
        }
    }
}
