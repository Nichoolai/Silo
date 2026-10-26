package com.silo.ui;

import com.silo.model.Distribution;
import com.silo.model.Donation;
import com.silo.model.Donation.Status;
import com.silo.model.Pickup;
import com.silo.model.Volunteer;
import com.silo.service.SampleData;
import com.silo.service.SiloService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

class DashboardPage extends Page {
    private static final Color[] STATUS_COLORS = {
        new Color(0x1FA463),
        new Color(0xE5A321),
        new Color(0x4689C8),
        new Color(0x7157C8),
        new Color(0xC96B61)
    };

    private final JPanel content = new JPanel();
    private final JPanel metrics = new JPanel(new GridLayout(1, 4, 14, 0));
    private final Map<String, JLabel> metricValues = new java.util.LinkedHashMap<>();
    private final JTable expiring = Ui.table("Food", "Donor", "Remaining", "Best before");
    private final JTable upcoming = Ui.table("When", "Food", "Volunteer", "Status");
    private final JLabel expiringCount = Ui.muted(" ");
    private final JLabel upcomingCount = Ui.muted(" ");
    private final DistributionTrendChart distributionTrend = new DistributionTrendChart();
    private final StatusDonutChart statusDonut = new StatusDonutChart();
    private final JPanel activityList = new JPanel();
    private final JButton sampleData = Ui.button("Load sample records", false);

    DashboardPage(SiloApp app) {
        super(app);

        JButton addDonation = Ui.button("New donation", true);
        addDonation.addActionListener(e -> {
            if (new DonationsPage.DonationDialog(app, svc, null).open()) {
                app.refreshAll();
            }
        });
        JButton schedulePickup = Ui.button("Schedule pickup", false);
        schedulePickup.addActionListener(e -> {
            if (new PickupsPage.ScheduleDialog(app, svc, null).open()) {
                app.refreshAll();
            }
        });
        sampleData.addActionListener(e ->
            app.act("Sample data loaded", () -> SampleData.load(svc))
        );

        add(header(
            "Dashboard overview",
            "A clear view of surplus, collections, and food reaching the community.",
            sampleData,
            schedulePickup,
            addDonation
        ), BorderLayout.NORTH);

        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        metrics.setOpaque(false);
        metrics.add(metric("Donation records", "All recorded donations", Ui.PRIMARY, "Donations"));
        metrics.add(metric("Available surplus", "Ready to be scheduled", new Color(0x1FA463), "Donations"));
        metrics.add(metric("Scheduled pickups", "Awaiting collection", new Color(0xD29428), "Pickups"));
        metrics.add(metric("Food distributed", "Totals kept separate by unit", new Color(0x6486A8), "Reports"));
        content.add(metrics);
        content.add(Box.createVerticalStrut(16));

        JPanel charts = new JPanel(new GridLayout(1, 2, 14, 0));
        charts.setOpaque(false);
        charts.add(section(
            "Distribution activity",
            "Recorded deliveries per month · last six months",
            distributionTrend
        ));
        charts.add(section(
            "Donation status",
            "Current records by workflow status",
            statusDonut
        ));
        content.add(charts);
        content.add(Box.createVerticalStrut(16));

        JPanel operations = new JPanel(new GridLayout(1, 2, 14, 0));
        operations.setOpaque(false);
        JPanel expiringPanel = section(
            "Best-before attention",
            "Open donations nearing their best-before date",
            tableWithHint(expiring, "No available food is nearing its best-before date.")
        );
        JPanel urgentHeader = new JPanel(new BorderLayout());
        urgentHeader.setOpaque(false);
        urgentHeader.add(Ui.heading("Best-before attention"), BorderLayout.WEST);
        urgentHeader.add(expiringCount, BorderLayout.EAST);
        replaceSectionHeader(expiringPanel, urgentHeader);
        operations.add(expiringPanel);

        JPanel pickupPanel = section(
            "Upcoming pickups",
            "The next scheduled collections",
            tableWithHint(upcoming, "No pickups are scheduled.")
        );
        JPanel pickupHeader = new JPanel(new BorderLayout());
        pickupHeader.setOpaque(false);
        pickupHeader.add(Ui.heading("Upcoming pickups"), BorderLayout.WEST);
        pickupHeader.add(upcomingCount, BorderLayout.EAST);
        replaceSectionHeader(pickupPanel, pickupHeader);
        operations.add(pickupPanel);
        content.add(operations);
        content.add(Box.createVerticalStrut(16));

        JPanel activityPanel = section("Recent activity", "Latest saved operational events", activityList);
        content.add(activityPanel);

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        add(scroll, BorderLayout.CENTER);
    }

    private JPanel metric(String title, String subtitle, Color color, String destination) {
        JPanel card = Ui.card(new BorderLayout(12, 0));
        card.setBorder(new EmptyBorder(16, 17, 16, 17));

        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        JLabel label = Ui.muted(title);
        label.setFont(Ui.font(Font.PLAIN, 12));
        JLabel indicator = new JLabel(" ");
        indicator.setOpaque(true);
        indicator.setBackground(color);
        indicator.setPreferredSize(new Dimension(8, 8));
        heading.add(label, BorderLayout.WEST);
        heading.add(indicator, BorderLayout.EAST);

        JLabel value = new JLabel("0");
        value.setFont(Ui.font(Font.BOLD, 27));
        value.setForeground(Ui.TEXT);
        JLabel caption = Ui.muted(subtitle);
        caption.setFont(Ui.font(Font.PLAIN, 10));
        JPanel details = new JPanel();
        details.setOpaque(false);
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        value.setAlignmentX(Component.LEFT_ALIGNMENT);
        caption.setAlignmentX(Component.LEFT_ALIGNMENT);
        details.add(value);
        details.add(Box.createVerticalStrut(4));
        details.add(caption);
        JPanel content = new JPanel(new BorderLayout(0, 9));
        content.setOpaque(false);
        content.add(heading, BorderLayout.NORTH);
        content.add(details, BorderLayout.CENTER);
        JPanel accent = new JPanel();
        accent.setBackground(color);
        accent.setPreferredSize(new Dimension(4, 0));
        card.add(accent, BorderLayout.WEST);
        card.add(content, BorderLayout.CENTER);
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.setToolTipText("Open " + destination.toLowerCase(Locale.ROOT));
        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                app.show(destination);
            }
        });
        metricValues.put(title, value);
        return card;
    }

    private JPanel section(String title, String subtitle, JComponent component) {
        JPanel card = Ui.card(new BorderLayout(0, 12));
        card.setBorder(new EmptyBorder(16, 17, 16, 17));
        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        JLabel titleLabel = Ui.heading(title);
        JLabel subtitleLabel = Ui.muted(subtitle);
        subtitleLabel.setFont(Ui.font(Font.PLAIN, 11));
        heading.add(titleLabel);
        heading.add(Box.createVerticalStrut(3));
        heading.add(subtitleLabel);
        card.add(heading, BorderLayout.NORTH);
        card.add(component, BorderLayout.CENTER);
        return card;
    }

    private void replaceSectionHeader(JPanel card, JComponent header) {
        Component currentHeader = ((BorderLayout) card.getLayout())
            .getLayoutComponent(BorderLayout.NORTH);
        card.remove(currentHeader);
        card.add(header, BorderLayout.NORTH);
    }

    @Override
    void refresh() {
        List<Donation> donations = svc.donations();
        long availableCount = donations.stream()
            .filter(d -> d.getStatus() == Status.AVAILABLE).count();
        long scheduledCount = svc.queueOrder().size();
        metricValues.get("Donation records").setText(String.valueOf(donations.size()));
        metricValues.get("Available surplus").setText(String.valueOf(availableCount));
        metricValues.get("Scheduled pickups").setText(String.valueOf(scheduledCount));
        metricValues.get("Food distributed").setText(formatUnits(svc.deliveredByUnit()));

        List<Object[]> expiringRows = new ArrayList<>();
        for (Donation d : svc.nearingBestBefore(7)) {
            if (d.getStatus() == Status.AVAILABLE || d.getStatus() == Status.SCHEDULED) {
                expiringRows.add(new Object[] {
                    d.getFoodName(),
                    d.getDonor(),
                    SiloService.fmt(d.getRemaining()) + " " + d.getUnit(),
                    d.getBestBefore()
                });
            }
        }
        Ui.fill(expiring, expiringRows);
        expiringCount.setText(expiringRows.size() + " items");

        List<Object[]> pickupRows = new ArrayList<>();
        List<Pickup> pickups = svc.upcomingPickups(6);
        for (Pickup pickup : pickups) {
            Donation donation = svc.donation(pickup.getDonationId());
            Volunteer volunteer = svc.volunteer(pickup.getVolunteerId());
            pickupRows.add(new Object[] {
                pickup.getScheduledAt().toString().replace('T', ' '),
                donation == null ? "Unknown donation" : donation.getFoodName(),
                volunteer == null ? "Unassigned" : volunteer.getName(),
                svc.isOverdue(pickup) ? "Overdue" : "Scheduled"
            });
        }
        Ui.fill(upcoming, pickupRows);
        upcomingCount.setText(scheduledCount + " queued");

        distributionTrend.updateData(monthlyDistributionCounts());
        statusDonut.updateData(svc.statusCounts());
        updateActivity();
        sampleData.setVisible(donations.isEmpty()
            && svc.volunteers().isEmpty()
            && svc.recipients().isEmpty());
    }

    private int[] monthlyDistributionCounts() {
        int[] counts = new int[6];
        YearMonth currentMonth = YearMonth.from(svc.today());
        for (Distribution distribution : svc.distributions()) {
            long offset = java.time.temporal.ChronoUnit.MONTHS.between(
                YearMonth.from(distribution.deliveredOn()), currentMonth);
            if (offset >= 0 && offset < counts.length) {
                counts[counts.length - 1 - (int) offset]++;
            }
        }
        return counts;
    }

    private void updateActivity() {
        activityList.removeAll();
        activityList.setOpaque(false);
        activityList.setLayout(new BoxLayout(activityList, BoxLayout.Y_AXIS));
        List<String[]> recent = svc.activity(5);
        if (recent.isEmpty()) {
            activityList.add(Ui.muted("No activity has been recorded yet."));
            return;
        }
        for (int i = 0; i < recent.size(); i++) {
            String[] item = recent.get(i);
            JPanel row = new JPanel(new BorderLayout(14, 0));
            row.setOpaque(false);
            row.setBorder(new EmptyBorder(8, 2, 8, 2));
            JLabel dot = new JLabel(" ");
            dot.setOpaque(true);
            dot.setBackground(Ui.ACCENT);
            dot.setPreferredSize(new Dimension(7, 7));
            JLabel message = new JLabel(item[1]);
            message.setFont(Ui.font(Font.PLAIN, 12));
            message.setForeground(Ui.TEXT);
            JLabel date = Ui.muted(item[0].replace('T', ' '));
            date.setFont(Ui.font(Font.PLAIN, 10));
            row.add(dot, BorderLayout.WEST);
            row.add(message, BorderLayout.CENTER);
            row.add(date, BorderLayout.EAST);
            activityList.add(row);
            if (i < recent.size() - 1) {
                JSeparator line = new JSeparator();
                line.setForeground(Ui.BORDER);
                activityList.add(line);
            }
        }
    }

    private static String formatUnits(Map<String, Double> values) {
        if (values.isEmpty()) {
            return "0";
        }
        return values.entrySet().stream()
            .map(entry -> SiloService.fmt(entry.getValue()) + " " + entry.getKey())
            .collect(java.util.stream.Collectors.joining(" · "));
    }

    private final class DistributionTrendChart extends JPanel {
        private int[] values = new int[6];

        DistributionTrendChart() {
            setOpaque(false);
            setPreferredSize(new Dimension(350, 230));
            setMinimumSize(new Dimension(250, 170));
        }

        void updateData(int[] counts) {
            values = counts.clone();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int left = 38, right = 16, top = 15, bottom = 30;
            int width = Math.max(1, getWidth() - left - right);
            int height = Math.max(1, getHeight() - top - bottom);
            int max = Math.max(4, java.util.Arrays.stream(values).max().orElse(0));
            max = ((max + 3) / 4) * 4;
            g.setColor(new Color(0xBFC8C0));
            g.drawLine(left, top, left, top + height);
            g.drawLine(left, top + height, left + width, top + height);

            g.setFont(Ui.font(Font.PLAIN, 10));
            for (int line = 0; line <= 4; line++) {
                int y = top + height - height * line / 4;
                g.setColor(new Color(0xE9EDE8));
                g.drawLine(left, y, left + width, y);
                g.setColor(Ui.MUTED);
                String label = String.valueOf(max * line / 4);
                g.drawString(label, left - g.getFontMetrics().stringWidth(label) - 7, y + 4);
            }

            Path2D path = new Path2D.Double();
            Path2D area = new Path2D.Double();
            for (int i = 0; i < values.length; i++) {
                int x = left + (values.length == 1 ? width / 2 : width * i / (values.length - 1));
                int y = top + height - (int) Math.round((double) values[i] / max * height);
                if (i == 0) {
                    path.moveTo(x, y);
                    area.moveTo(x, top + height);
                    area.lineTo(x, y);
                } else {
                    path.lineTo(x, y);
                    area.lineTo(x, y);
                }
            }
            area.lineTo(left + width, top + height);
            area.closePath();
            g.setPaint(new GradientPaint(
                0, top, new Color(111, 175, 133, 75),
                0, top + height, new Color(234, 243, 236, 0)
            ));
            g.fill(area);
            g.setColor(Ui.PRIMARY);
            g.setStroke(new BasicStroke(2.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(path);

            YearMonth current = YearMonth.from(svc.today());
            for (int i = 0; i < values.length; i++) {
                int x = left + (values.length == 1 ? width / 2 : width * i / (values.length - 1));
                int y = top + height - (int) Math.round((double) values[i] / max * height);
                g.setColor(Color.WHITE);
                g.fillOval(x - 4, y - 4, 8, 8);
                g.setColor(Ui.PRIMARY);
                g.setStroke(new BasicStroke(2));
                g.drawOval(x - 4, y - 4, 8, 8);
                YearMonth month = current.minusMonths(values.length - 1L - i);
                String monthName = month.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
                g.setColor(Ui.MUTED);
                int labelWidth = g.getFontMetrics().stringWidth(monthName);
                g.drawString(monthName, x - labelWidth / 2, top + height + 18);
            }
            g.dispose();
        }
    }

    private static final class StatusDonutChart extends JPanel {
        private Map<Status, Integer> values = new EnumMap<>(Status.class);

        StatusDonutChart() {
            setOpaque(false);
            setPreferredSize(new Dimension(350, 230));
            setMinimumSize(new Dimension(250, 170));
        }

        void updateData(Map<Status, Integer> counts) {
            values = new EnumMap<>(counts);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int total = values.values().stream().mapToInt(Integer::intValue).sum();
            int diameter = Math.min(142, Math.max(90, getHeight() - 35));
            int x = Math.max(8, (getWidth() - diameter) / 2);
            int y = 4;
            int angle = 90;
            int drawn = 0;
            int cumulative = 0;
            Status[] statuses = Status.values();
            for (int i = 0; i < statuses.length; i++) {
                int value = values.getOrDefault(statuses[i], 0);
                cumulative += value;
                int extent = total == 0 ? 0
                    : (int) Math.round(360.0 * cumulative / total) - drawn;
                if (extent > 0) {
                    g.setColor(STATUS_COLORS[i]);
                    g.fill(new Arc2D.Double(x, y, diameter, diameter, angle, -extent, Arc2D.PIE));
                    angle -= extent;
                    drawn += extent;
                }
            }
            int inner = (int) (diameter * 0.58);
            g.setColor(Ui.WHITE);
            g.fillOval(x + (diameter - inner) / 2, y + (diameter - inner) / 2, inner, inner);
            g.setFont(Ui.font(Font.BOLD, 20));
            g.setColor(Ui.TEXT);
            String totalLabel = String.valueOf(total);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(totalLabel, x + (diameter - fm.stringWidth(totalLabel)) / 2,
                y + diameter / 2 + fm.getAscent() / 3);
            g.setFont(Ui.font(Font.PLAIN, 10));
            g.setColor(Ui.MUTED);
            String records = "records";
            g.drawString(records, x + (diameter - g.getFontMetrics().stringWidth(records)) / 2,
                y + diameter / 2 + 18);

            int legendY = y + diameter + 18;
            int colWidth = Math.max(130, getWidth() / 2);
            for (int i = 0; i < statuses.length; i++) {
                int col = i % 2;
                int row = i / 2;
                int legendX = Math.max(0, (getWidth() - colWidth * 2) / 2) + col * colWidth;
                int itemY = legendY + row * 19;
                g.setColor(STATUS_COLORS[i]);
                g.fillOval(legendX, itemY - 7, 8, 8);
                g.setFont(Ui.font(Font.PLAIN, 10));
                g.setColor(Ui.TEXT);
                g.drawString(SiloService.label(statuses[i]) + "  "
                    + values.getOrDefault(statuses[i], 0), legendX + 13, itemY);
            }
            g.dispose();
        }
    }
}
