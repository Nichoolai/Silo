package com.silo.ui;

import com.silo.service.SiloService;
import com.silo.service.ValidationException;
import com.silo.storage.FileStore;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import com.silo.service.AuthService;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class SiloApp extends JFrame {
    private final SiloService service;
    private final AuthService authService;
    private final AuthService.Account currentAccount;
    private final CardLayout cards = new CardLayout();
    private final JPanel pageArea = new JPanel(cards);
    private final Map<String, Page> pages = new LinkedHashMap<>();
    private final Map<String, JButton> navButtons = new LinkedHashMap<>();

    private final JLabel feedback = new JLabel(" ");
    private final JLabel pendingIndicator = new JLabel();
    private final JLabel pageTitle = new JLabel("Dashboard");
    private final JButton undo = Ui.sidebarButton("Undo last action");
    private final javax.swing.Timer clearTimer;

    private String current = "Dashboard";

    public SiloApp(SiloService service) {
        this(service, null, null);
    }

    public SiloApp(SiloService service, AuthService authService, AuthService.Account currentAccount) {
        super("Silo | Community Food Rescue Network");
        this.service = service;
        this.authService = authService;
        this.currentAccount = currentAccount;

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1080, 680));
        setSize(1400, 900);
        setLocationRelativeTo(null);

        pageArea.setBackground(Ui.BG);
        pageArea.setBorder(new EmptyBorder(0, 0, 0, 0));

        pages.put("Dashboard", new DashboardPage(this));
        pages.put("Donations", new DonationsPage(this));
        pages.put("Pickups", new PickupsPage(this));
        pages.put("Volunteers", new VolunteersPage(this));
        pages.put("Recipients", new RecipientsPage(this));
        pages.put("Reports", new ReportsPage(this));
        if (currentAccount != null && currentAccount.getRole() == AuthService.Role.ADMIN) {
            pages.put("Applications", new AdminReviewPage(this));
        }

        pages.forEach((name, page) -> pageArea.add(page, name));

        feedback.setFont(Ui.font(Font.PLAIN, 13));
        feedback.setBorder(new EmptyBorder(12, 26, 12, 26));
        feedback.setOpaque(true);
        feedback.setBackground(Ui.BG);
        feedback.setForeground(Ui.MUTED);

        clearTimer = new javax.swing.Timer(
            6000, e -> setFeedback(" ", Ui.BG, Ui.MUTED)
        );
        clearTimer.setRepeats(false);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Ui.BG);
        root.add(buildSidebar(), BorderLayout.WEST);

        JPanel right = new JPanel(new BorderLayout());
        right.setBackground(Ui.BG);
        right.add(buildTopbar(), BorderLayout.NORTH);
        right.add(pageArea, BorderLayout.CENTER);
        right.add(feedback, BorderLayout.SOUTH);

        root.add(right, BorderLayout.CENTER);
        setContentPane(root);

        show("Dashboard");
    }

    public SiloService service() {
        return service;
    }

    public AuthService authService() {
        return authService;
    }

    public AuthService.Account currentAccount() {
        return currentAccount;
    }

    private JComponent buildSidebar() {
        JPanel side = new JPanel();
        side.setBackground(Ui.SIDEBAR);
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(0x274238)),
            new EmptyBorder(24, 14, 18, 14)
        ));
        side.setPreferredSize(new Dimension(244, 0));

        JPanel brandPanel = new JPanel();
        brandPanel.setOpaque(false);
        brandPanel.setLayout(new BorderLayout(10, 0));
        brandPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel mark = new JLabel("S");
        mark.setHorizontalAlignment(SwingConstants.CENTER);
        mark.setFont(Ui.font(Font.BOLD, 20));
        mark.setForeground(Ui.FOREST);
        mark.setOpaque(true);
        mark.setBackground(Ui.ACCENT);
        mark.setPreferredSize(new Dimension(42, 42));
        JLabel brand = new JLabel("silo");
        brand.setFont(Ui.font(Font.BOLD, 27));
        brand.setForeground(Ui.WHITE);
        JLabel tagline = Ui.muted("FOOD RESCUE NETWORK");
        tagline.setFont(Ui.font(Font.BOLD, 9));
        tagline.setForeground(Ui.SIDEBAR_MUTED);
        JPanel brandText = new JPanel();
        brandText.setOpaque(false);
        brandText.setLayout(new BoxLayout(brandText, BoxLayout.Y_AXIS));
        brandText.add(brand);
        brandText.add(tagline);
        brandPanel.add(mark, BorderLayout.WEST);
        brandPanel.add(brandText, BorderLayout.CENTER);

        side.add(brandPanel);
        side.add(Box.createVerticalStrut(30));

        addNavGroup(side, "OVERVIEW", "Dashboard");
        addNavGroup(side, "FOOD OPERATIONS", "Donations", "Pickups");
        addNavGroup(side, "COMMUNITY", "Volunteers", "Recipients");
        addNavGroup(side, "INSIGHTS", "Reports");
        if (pages.containsKey("Applications")) {
            addNavGroup(side, "ADMINISTRATION", "Applications");
        }

        side.add(Box.createVerticalStrut(8));
        JLabel historyLabel = new JLabel("RECENT ACTIONS");
        historyLabel.setFont(Ui.font(Font.BOLD, 10));
        historyLabel.setForeground(Ui.SIDEBAR_MUTED);
        historyLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        historyLabel.setBorder(new EmptyBorder(0, 12, 8, 0));
        side.add(historyLabel);

        undo.setAlignmentX(Component.LEFT_ALIGNMENT);
        undo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        undo.setPreferredSize(new Dimension(210, 42));
        undo.setHorizontalAlignment(SwingConstants.LEFT);
        undo.addActionListener(e -> act(
            null,
            () -> {
                String description = service.undoLast();
                setFeedback("Undone: " + description, Ui.SAGE, Ui.SUCCESS);
            }
        ));
        side.add(undo);

        side.add(Box.createVerticalGlue());

        JSeparator separator = new JSeparator();
        separator.setForeground(new Color(0x2B473B));
        separator.setBackground(new Color(0x2B473B));
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        side.add(separator);
        side.add(Box.createVerticalStrut(14));

        if (currentAccount != null) {
            JPanel profile = new JPanel(new BorderLayout(10, 0));
            profile.setOpaque(false);
            profile.setBorder(new EmptyBorder(2, 4, 12, 4));
            JLabel avatar = new JLabel(initials(currentAccount.getName()));
            avatar.setHorizontalAlignment(SwingConstants.CENTER);
            avatar.setFont(Ui.font(Font.BOLD, 12));
            avatar.setForeground(Ui.PRIMARY);
            avatar.setOpaque(true);
            avatar.setBackground(Ui.SAGE);
            avatar.setPreferredSize(new Dimension(36, 36));
            JLabel identity = new JLabel("<html><b>" + escapeHtml(currentAccount.getName())
                + "</b><br><span style='color:#9BB0A4;font-size:10px'>"
                + currentAccount.getRole().name() + "</span></html>");
            identity.setFont(Ui.font(Font.PLAIN, 12));
            profile.add(avatar, BorderLayout.WEST);
            profile.add(identity, BorderLayout.CENTER);
            side.add(profile);
        }

        if (authService != null && currentAccount != null) {
            JButton logout = Ui.sidebarButton("Sign out");
            logout.setAlignmentX(Component.LEFT_ALIGNMENT);
            logout.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            logout.addActionListener(e -> signOut());
            side.add(logout);
        }
        return side;
    }

    private JComponent buildTopbar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Ui.WHITE);
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, Ui.BORDER),
            new EmptyBorder(13, 26, 13, 24)
        ));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 0));
        left.setOpaque(false);
        pageTitle.setFont(Ui.font(Font.BOLD, 16));
        pageTitle.setForeground(Ui.TEXT);
        left.add(pageTitle);
        JTextField quickNavigation = Ui.field(24, "Jump to a page...");
        quickNavigation.setPreferredSize(new Dimension(260, 40));
        quickNavigation.addActionListener(e -> navigateQuickly(quickNavigation.getText()));
        left.add(quickNavigation);
        bar.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);
        if (currentAccount != null && currentAccount.getRole() == AuthService.Role.ADMIN) {
            pendingIndicator.setFont(Ui.font(Font.BOLD, 12));
            pendingIndicator.setForeground(Ui.WARN);
            pendingIndicator.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            pendingIndicator.setToolTipText("Open coordinator applications");
            pendingIndicator.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (pages.containsKey("Applications")) {
                        show("Applications");
                    }
                }
            });
            right.add(pendingIndicator);
        }
        if (currentAccount != null) {
            JLabel avatar = new JLabel(initials(currentAccount.getName()));
            avatar.setHorizontalAlignment(SwingConstants.CENTER);
            avatar.setFont(Ui.font(Font.BOLD, 11));
            avatar.setForeground(Ui.PRIMARY);
            avatar.setOpaque(true);
            avatar.setBackground(Ui.SAGE);
            avatar.setPreferredSize(new Dimension(30, 30));
            right.add(avatar);
            JLabel identity = new JLabel("<html><b>" + escapeHtml(currentAccount.getName())
                + "</b><br><span style='color:#718078;font-size:10px'>"
                + currentAccount.getRole().name() + "</span></html>");
            identity.setFont(Ui.font(Font.PLAIN, 11));
            right.add(identity);
        } else {
            right.add(Ui.muted("Local workspace"));
        }
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private void addNavGroup(JPanel sidebar, String title, String... names) {
        JLabel heading = new JLabel(title);
        heading.setFont(Ui.font(Font.BOLD, 9));
        heading.setForeground(Ui.SIDEBAR_MUTED);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        heading.setBorder(new EmptyBorder(0, 12, 7, 0));
        sidebar.add(heading);
        for (String name : names) {
            if (!pages.containsKey(name)) {
                continue;
            }
            JButton button = createNavButton(name);
            navButtons.put(name, button);
            sidebar.add(button);
            sidebar.add(Box.createVerticalStrut(3));
        }
        sidebar.add(Box.createVerticalStrut(14));
    }

    private JButton createNavButton(String name) {
        JButton b = new JButton(name) {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (isEnabled() && !getBackground().equals(Ui.SIDEBAR)) {
                    g.setColor(getBackground());
                    g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 13, 13);
                }
                g.dispose();
                super.paintComponent(graphics);
            }
        };
        b.setIcon(new NavIcon(name, false));
        b.setIconTextGap(12);
        b.setFont(Ui.font(Font.PLAIN, 13));
        b.setForeground(Ui.SIDEBAR_TEXT);
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setBackground(Ui.SIDEBAR);
        b.setBorder(new EmptyBorder(10, 13, 10, 10));
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> show(name));
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (!name.equals(current)) {
                    b.setBackground(Ui.SIDEBAR_HOVER);
                }
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                updateNavStyles();
            }
        });
        return b;
    }

    private void updateNavStyles() {
        navButtons.forEach((name, button) -> {
            boolean active = name.equals(current);

            button.setBackground(
                active
                    ? Ui.SIDEBAR_ACTIVE
                    : Ui.SIDEBAR
            );

            button.setForeground(
                active
                    ? Ui.WHITE
                    : Ui.SIDEBAR_TEXT
            );
            button.setIcon(new NavIcon(name, active));

            button.setFont(
                Ui.font(active ? Font.BOLD : Font.PLAIN, 13)
            );
        });
    }

    public void show(String name) {
        if (!pages.containsKey(name)) {
            return;
        }

        current = name;
        pageTitle.setText(name);
        cards.show(pageArea, name);
        updateNavStyles();
        refreshAll();
    }

    private void navigateQuickly(String query) {
        if (query == null || query.isBlank()) {
            return;
        }
        String target = pages.keySet().stream()
            .filter(name -> name.toLowerCase(Locale.ROOT)
                .contains(query.trim().toLowerCase(Locale.ROOT)))
            .findFirst()
            .orElse(null);
        if (target == null) {
            error("No workspace page matches \"" + query.trim() + "\".");
        } else {
            show(target);
        }
    }

    /** Refresh the visible page and action-history controls. */
    public void refreshAll() {
        Page page = pages.get(current);

        if (page != null) {
            page.refresh();
        }

        undo.setEnabled(service.canUndo());
        undo.setToolTipText(
            service.canUndo()
                ? "Undo: " + service.nextUndoDescription()
                : "Nothing to undo"
        );
        if (authService != null && currentAccount != null
                && currentAccount.getRole() == AuthService.Role.ADMIN) {
            int pending = authService.getCoordinatorApplications().size();
            pendingIndicator.setText(pending == 0
                ? "No pending applications"
                : pending + " pending application" + (pending == 1 ? "" : "s"));
        }
    }

    /** Run an action and display success or validation feedback. */
    public boolean act(String success, Runnable r) {
        try {
            r.run();

            if (success != null) {
                success(success);
            }

            refreshAll();
            return true;

        } catch (ValidationException e) {
            error(e.getMessage());

        } catch (RuntimeException e) {
            error("Unexpected problem: " + e.getMessage());
        }

        refreshAll();
        return false;
    }

    public void success(String message) {
        setFeedback(
            message,
            new Color(0xE6F1E8),
            Ui.SUCCESS
        );
    }

    public void error(String message) {
        setFeedback(
            message,
            new Color(0xFBEAE8),
            Ui.DANGER
        );
    }

    private void signOut() {
        dispose();
        LoginPage loginPage = new LoginPage(authService, account -> {
            SiloApp app = new SiloApp(service, authService, account);
            app.setVisible(true);
        });
        loginPage.setVisible(true);
    }

    private static String initials(String name) {
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, 1).toUpperCase(Locale.ROOT);
        }
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1))
            .toUpperCase(Locale.ROOT);
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;")
            .replace(">", "&gt;");
    }

    private static final class NavIcon implements Icon {
        private final String page;
        private final boolean active;

        NavIcon(String page, boolean active) {
            this.page = page;
            this.active = active;
        }

        @Override
        public int getIconWidth() { return 18; }

        @Override
        public int getIconHeight() { return 18; }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.translate(x + 1, y + 1);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(active ? Color.WHITE : Ui.SIDEBAR_ICON);
            g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            switch (page) {
                case "Dashboard" -> {
                    g.drawRoundRect(1, 1, 6, 6, 2, 2);
                    g.drawRoundRect(9, 1, 6, 6, 2, 2);
                    g.drawRoundRect(1, 9, 6, 6, 2, 2);
                    g.drawRoundRect(9, 9, 6, 6, 2, 2);
                }
                case "Donations" -> {
                    g.drawRoundRect(2, 3, 12, 12, 2, 2);
                    g.drawLine(2, 6, 8, 9);
                    g.drawLine(14, 6, 8, 9);
                    g.drawLine(8, 9, 8, 15);
                }
                case "Pickups" -> {
                    g.drawRoundRect(1, 4, 9, 8, 2, 2);
                    g.drawLine(10, 7, 13, 7);
                    g.drawLine(13, 7, 16, 10);
                    g.drawLine(16, 10, 16, 12);
                    g.drawLine(10, 12, 16, 12);
                    g.drawOval(3, 11, 3, 3);
                    g.drawOval(12, 11, 3, 3);
                }
                case "Volunteers" -> {
                    g.drawOval(5, 1, 6, 6);
                    g.drawArc(2, 8, 12, 9, 0, 180);
                }
                case "Recipients" -> {
                    g.drawLine(2, 6, 8, 1);
                    g.drawLine(8, 1, 14, 6);
                    g.drawRect(3, 6, 10, 10);
                    g.drawRect(6, 9, 2, 2);
                    g.drawRect(10, 9, 2, 2);
                }
                case "Reports" -> {
                    g.drawLine(2, 15, 2, 2);
                    g.drawLine(2, 15, 15, 15);
                    g.drawLine(5, 12, 5, 9);
                    g.drawLine(9, 12, 9, 5);
                    g.drawLine(13, 12, 13, 2);
                }
                case "Applications" -> {
                    g.drawRoundRect(3, 1, 10, 15, 2, 2);
                    g.drawLine(5, 5, 11, 5);
                    g.drawLine(5, 8, 9, 8);
                    g.drawLine(5, 12, 7, 14);
                    g.drawLine(7, 14, 11, 10);
                }
                default -> g.drawOval(2, 2, 13, 13);
            }
            g.dispose();
        }
    }

    private void setFeedback(String text, Color bg, Color fg) {
        feedback.setText(text);
        feedback.setBackground(bg);
        feedback.setForeground(fg);
        clearTimer.restart();
    }

    private static AuthService.Account createInitialAdmin(AuthService auth) {
        JTextField nameField = new JTextField(22);
        JTextField emailField = new JTextField(22);
        JPasswordField passwordField = new JPasswordField(22);
        JPasswordField confirmField = new JPasswordField(22);

        JPanel panel = new JPanel(new GridLayout(0, 1, 6, 6));
        panel.add(new JLabel("Administrator full name"));
        panel.add(nameField);
        panel.add(new JLabel("Administrator email"));
        panel.add(emailField);
        panel.add(new JLabel("Password (at least 8 characters)"));
        panel.add(passwordField);
        panel.add(new JLabel("Confirm password"));
        panel.add(confirmField);

        while (true) {
            int result = JOptionPane.showConfirmDialog(
                null,
                panel,
                "Create the first administrator",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
            );

            if (result != JOptionPane.OK_OPTION) {
                return null;
            }

            String name = nameField.getText();
            String email = emailField.getText();
            char[] password = passwordField.getPassword();
            char[] confirmation = confirmField.getPassword();

            try {
                if (!java.util.Arrays.equals(password, confirmation)) {
                    throw new IllegalArgumentException("The passwords do not match.");
                }
                return auth.ensureAdminAccount(name, email, password);
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(
                    null,
                    ex.getMessage(),
                    "Administrator setup",
                    JOptionPane.WARNING_MESSAGE
                );
            } finally {
                java.util.Arrays.fill(password, '\0');
                java.util.Arrays.fill(confirmation, '\0');
                passwordField.setText("");
                confirmField.setText("");
            }
        }
    }

    public static void main(String[] args) {
    String dir = System.getProperty("silo.data", "data");

    SwingUtilities.invokeLater(() -> {
        Ui.installTheme();

        SiloService svc;
        AuthService auth;

        try {
            Path dataPath = Path.of(dir);

            svc = new SiloService(new FileStore(dataPath));
            auth = new AuthService(dataPath);

        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(
                null,
                "Silo could not open its data folder (" + dir + "):\n"
                    + e.getMessage(),
                "Startup problem",
                JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        if (!auth.hasAdminAccount()) {
            AuthService.Account admin = createInitialAdmin(auth);
            if (admin == null) {
                return;
            }
        }

        AuthService finalAuth = auth;
        SiloService finalService = svc;

        LoginPage loginPage = new LoginPage(
            finalAuth,
            account -> {
                SiloApp app = new SiloApp(finalService, finalAuth, account);
                app.setVisible(true);
            }
        );

        loginPage.setVisible(true);
    });
    }
}