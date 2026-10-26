package com.silo.ui;

import com.silo.model.Donation;
import com.silo.service.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;

class ReportsPage extends Page {
    private final JTable totals = Ui.table("Unit", "Donated", "Picked up", "Delivered");
    private final JTable statuses = Ui.table("Status", "Donations");
    private final JTable expiring = Ui.table("ID", "Food", "Donor", "Remaining", "Best before", "Days left", "Status");
    private final JTable dist = Ui.table("Delivered on", "Donation", "Food", "Donor", "Recipient", "Quantity", "Unit");
    private final JSpinner days = new JSpinner(new SpinnerNumberModel(7, 1, 90, 1));
    private final JTextField from = Ui.field(9), to = Ui.field(9), food = Ui.field(10), donor = Ui.field(10);
    private final JLabel donationCount = new JLabel("0");
    private final JLabel distributionCount = new JLabel("0");
    private final JLabel deliveredQuantity = new JLabel("0");
    private final JLabel attentionCount = new JLabel("0");

    ReportsPage(SiloApp app) {
        super(app);
        add(header("Reports", "Everything below is calculated from saved records"), BorderLayout.NORTH);
        CardLayout tabCards = new CardLayout();
        JPanel tabs = new JPanel(tabCards); tabs.setOpaque(false);
        JPanel tabBar = toolbar();

        JPanel summary = new JPanel(new BorderLayout(0, 14)); summary.setOpaque(false);
        JPanel overview = new JPanel(new GridLayout(1, 4, 12, 0)); overview.setOpaque(false);
        overview.add(metric("Donation records", donationCount, Ui.PRIMARY));
        overview.add(metric("Distributions", distributionCount, new Color(0x6486A8)));
        overview.add(metric("Food delivered", deliveredQuantity, new Color(0x1FA463)));
        overview.add(metric("Near best-before", attentionCount, new Color(0xD29428)));
        JPanel two = new JPanel(new GridLayout(1, 2, 12, 0)); two.setOpaque(false);
        two.add(titled("Quantities by unit", "Donated, collected, and delivered totals", Ui.scroll(totals)));
        two.add(titled("Donation status", "Saved donation records by workflow stage", Ui.scroll(statuses)));
        JButton exportAll = Ui.button("Export all donations (CSV)", false);
        exportAll.addActionListener(e -> export("donations", SiloService.DONATION_HEADER, svc.donationRows()));
        JPanel exportRow = new JPanel(new BorderLayout(12, 0));
        exportRow.setOpaque(false);
        JLabel unitNote = Ui.muted("Quantities stay separated by unit; kg and pieces are never combined.");
        exportRow.add(unitNote, BorderLayout.WEST);
        exportRow.add(exportAll, BorderLayout.EAST);
        summary.add(overview, BorderLayout.NORTH);
        JPanel summaryDetails = new JPanel(new BorderLayout(0, 12));
        summaryDetails.setOpaque(false);
        summaryDetails.add(exportRow, BorderLayout.NORTH);
        summaryDetails.add(two, BorderLayout.CENTER);
        summary.add(summaryDetails, BorderLayout.CENTER);
        addTab(tabs, tabBar, tabCards, "Summary", summary);

        JPanel near = new JPanel(new BorderLayout(0, 10)); near.setOpaque(false);
        days.addChangeListener(e -> refresh());
        JPanel expiryFilter = Ui.card(new BorderLayout(12, 0));
        expiryFilter.add(Ui.muted("Show remaining food with a best-before date within"), BorderLayout.WEST);
        expiryFilter.add(days, BorderLayout.CENTER);
        JLabel daySuffix = Ui.muted("days · earliest first");
        expiryFilter.add(daySuffix, BorderLayout.EAST);
        near.add(expiryFilter, BorderLayout.NORTH);
        near.add(titled("Food nearing its best-before date", "Prioritize open donations that need attention",
            tableWithHint(expiring, "Nothing is approaching its best-before date in this window.")), BorderLayout.CENTER);
        addTab(tabs, tabBar, tabCards, "Near best-before", near);

        JPanel dp = new JPanel(new BorderLayout(0, 10)); dp.setOpaque(false);
        JButton apply = Ui.button("Apply filters", false), export = Ui.button("Export distributions (CSV)", true);
        apply.addActionListener(e -> refresh());
        export.addActionListener(e -> { var rows = filteredDistributions(); if (rows != null) export("distributions", SiloService.DISTRIBUTION_HEADER, rows); });
        from.setToolTipText("yyyy-mm-dd, blank = no limit"); to.setToolTipText("yyyy-mm-dd, blank = no limit");
        JPanel filters = Ui.card(new BorderLayout(12, 12));
        JPanel fields = new JPanel(new GridLayout(1, 4, 10, 0));
        fields.setOpaque(false);
        fields.add(labeledField("From date", from));
        fields.add(labeledField("To date", to));
        fields.add(labeledField("Food contains", food));
        fields.add(labeledField("Donor contains", donor));
        JPanel filterActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        filterActions.setOpaque(false);
        filterActions.add(apply);
        filterActions.add(export);
        filters.add(fields, BorderLayout.CENTER);
        filters.add(filterActions, BorderLayout.SOUTH);
        dp.add(filters, BorderLayout.NORTH);
        dp.add(titled("Distribution records", "Filter by delivery date, food, or donor",
            tableWithHint(dist, "No distributions match. Dates use yyyy-mm-dd.")), BorderLayout.CENTER);
        addTab(tabs, tabBar, tabCards, "Distributions", dp);
        ((JToggleButton) tabBar.getComponent(0)).setSelected(true);
        JPanel body = new JPanel(new BorderLayout(0, 12)); body.setOpaque(false);
        body.add(tabBar, BorderLayout.NORTH); body.add(tabs, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);
    }

    private final ButtonGroup tabGroup = new ButtonGroup();
    private void addTab(JPanel cards, JPanel bar, CardLayout cl, String name, JComponent content) {
        JToggleButton b = Ui.tabButton(name);
        b.addActionListener(e -> cl.show(cards, name));
        tabGroup.add(b); bar.add(b); cards.add(content, name);
    }
    private JPanel metric(String title, JLabel value, Color accent) {
        JPanel card = Ui.card(new BorderLayout(12, 0));
        card.setBorder(new EmptyBorder(14, 15, 14, 15));
        JPanel stripe = new JPanel();
        stripe.setBackground(accent);
        stripe.setPreferredSize(new Dimension(4, 0));
        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        JLabel label = Ui.muted(title);
        label.setFont(Ui.font(Font.PLAIN, 11));
        value.setFont(Ui.font(Font.BOLD, 20));
        value.setForeground(Ui.TEXT);
        copy.add(label);
        copy.add(Box.createVerticalStrut(8));
        copy.add(value);
        card.add(stripe, BorderLayout.WEST);
        card.add(copy, BorderLayout.CENTER);
        return card;
    }
    private JPanel labeledField(String label, JTextField field) {
        JPanel group = new JPanel(new BorderLayout(0, 5));
        group.setOpaque(false);
        JLabel caption = Ui.muted(label);
        caption.setFont(Ui.font(Font.BOLD, 11));
        group.add(caption, BorderLayout.NORTH);
        group.add(field, BorderLayout.CENTER);
        return group;
    }
    private JPanel titled(String title, String subtitle, JComponent component) {
        JPanel panel = Ui.card(new BorderLayout(0, 11));
        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        heading.add(Ui.heading(title));
        heading.add(Box.createVerticalStrut(3));
        JLabel caption = Ui.muted(subtitle);
        caption.setFont(Ui.font(Font.PLAIN, 11));
        heading.add(caption);
        panel.add(heading, BorderLayout.NORTH);
        panel.add(component, BorderLayout.CENTER);
        return panel;
    }

    /** Returns null (and shows a message) when a date filter is malformed. */
    private List<String[]> filteredDistributions() {
        try {
            LocalDate f = from.getText().isBlank() ? null : LocalDate.parse(from.getText().trim());
            LocalDate t = to.getText().isBlank() ? null : LocalDate.parse(to.getText().trim());
            if (f != null && t != null && t.isBefore(f)) { app.error("'To' date is before the 'From' date."); return null; }
            return svc.distributionRows(f, t, food.getText(), donor.getText());
        } catch (DateTimeParseException e) { app.error("Dates must look like 2026-10-31."); return null; }
    }

    private void export(String name, String[] header, List<String[]> rows) {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new java.io.File("silo-" + name + "-" + svc.today() + ".csv"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path p = fc.getSelectedFile().toPath();
        app.act("Exported " + rows.size() + " rows to " + p.getFileName(), () -> CsvExporter.write(p, header, rows));
    }

    @Override void refresh() {
        List<Donation> donations = svc.donations();
        donationCount.setText(String.valueOf(donations.size()));
        distributionCount.setText(String.valueOf(svc.distributions().size()));
        deliveredQuantity.setText(formatUnits(svc.deliveredByUnit()));
        attentionCount.setText(String.valueOf(svc.nearingBestBefore(7).size()));

        Set<String> units = new TreeSet<>();
        units.addAll(svc.donatedByUnit().keySet()); units.addAll(svc.deliveredByUnit().keySet());
        List<Object[]> rows = new ArrayList<>();
        for (String u : units) rows.add(new Object[]{u, SiloService.fmt(svc.donatedByUnit().getOrDefault(u, 0.0)), SiloService.fmt(svc.pickedUpByUnit().getOrDefault(u, 0.0)), SiloService.fmt(svc.deliveredByUnit().getOrDefault(u, 0.0))});
        Ui.fill(totals, rows);
        List<Object[]> statusRows = new ArrayList<>();
        svc.statusCounts().forEach((s, n) -> statusRows.add(new Object[]{SiloService.label(s), n}));
        Ui.fill(statuses, statusRows);

        rows = new ArrayList<>();
        for (Donation d : svc.nearingBestBefore((Integer) days.getValue()))
            rows.add(new Object[]{d.getId(), d.getFoodName(), d.getDonor(), SiloService.fmt(d.getRemaining()) + " " + d.getUnit(), d.getBestBefore(),
                    ChronoUnit.DAYS.between(svc.today(), d.getBestBefore()), SiloService.label(d.getStatus())});
        Ui.fill(expiring, rows);

        List<String[]> dr = filteredDistributions();
        if (dr != null) { rows = new ArrayList<>(); for (String[] r : dr) rows.add(r); Ui.fill(dist, rows); }
    }

    private static String formatUnits(Map<String, Double> values) {
        if (values.isEmpty()) return "0";
        return values.entrySet().stream()
            .map(entry -> SiloService.fmt(entry.getValue()) + " " + entry.getKey())
            .collect(java.util.stream.Collectors.joining(" · "));
    }
}
