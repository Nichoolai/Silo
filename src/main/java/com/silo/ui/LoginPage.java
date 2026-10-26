package com.silo.ui;

import com.silo.service.AuthService;
import com.fasterxml.jackson.databind.JsonNode;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.IOException;
import java.util.Arrays;
import java.util.function.Consumer;

public class LoginPage extends JFrame {

    private static final Color FOREST = Ui.FOREST;
    private static final Color BG = Ui.BG;
    private static final Color TEXT = Ui.TEXT;
    private static final Color MUTED = Ui.MUTED;
    private static final Color BORDER = Ui.BORDER;

    private final AuthService authService;
    private final Consumer<AuthService.Account> onLogin;
    private final com.silo.config.SupabaseAuth supabaseAuth;

    private JTextField emailField;
    private JPasswordField passwordField;

    public LoginPage(
            AuthService authService,
            Consumer<AuthService.Account> onLogin) {

        this.authService = authService;
        this.onLogin = onLogin;
        this.supabaseAuth = new com.silo.config.SupabaseAuth();

        setTitle("Silo | Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 680);
        setMinimumSize(new Dimension(900, 620));
        setLocationRelativeTo(null);

        buildUI();
    }

    private void buildUI() {
        JPanel root = new JPanel(new GridLayout(1, 2));
        root.setBackground(BG);
        root.add(buildBrandPanel());

        JPanel formSurface = new JPanel(new GridBagLayout());
        formSurface.setBackground(BG);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(BG);
        content.setBorder(new EmptyBorder(38, 44, 38, 44));
        content.setPreferredSize(new Dimension(460, 540));

        JLabel eyebrow = label("SECURE WORKSPACE");
        eyebrow.setForeground(Ui.PRIMARY);

        JLabel heading = new JLabel("Welcome back");
        heading.setFont(Ui.font(Font.BOLD, 29));
        heading.setForeground(TEXT);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel description = new JLabel(
                "<html><div style='width:350px'>"
                + "Sign in to coordinate donations, pickups, "
                + "and community distribution.</div></html>");
        description.setFont(Ui.font(Font.PLAIN, 13));
        description.setForeground(MUTED);
        description.setAlignmentX(Component.LEFT_ALIGNMENT);

        emailField = new JTextField();
        styleField(emailField);

        passwordField = new JPasswordField();
        styleField(passwordField);

        JButton loginButton = Ui.button("Sign in to Silo", true);
        loginButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        loginButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        loginButton.addActionListener(e -> login());

        JButton farmerButton = Ui.button("Create a farmer account", false);
        farmerButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        farmerButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        farmerButton.addActionListener(e -> showRegistrationDialog(false));

        JButton coordinatorButton = Ui.button(
                "Apply as a food-bank coordinator", false);
        coordinatorButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        coordinatorButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        coordinatorButton.addActionListener(e -> showRegistrationDialog(true));

        content.add(eyebrow);
        content.add(Box.createVerticalStrut(11));
        content.add(heading);
        content.add(Box.createVerticalStrut(8));
        content.add(description);
        content.add(Box.createVerticalStrut(28));
        content.add(label("Email address"));
        content.add(Box.createVerticalStrut(7));
        content.add(emailField);
        content.add(Box.createVerticalStrut(17));
        content.add(label("Password"));
        content.add(Box.createVerticalStrut(7));
        content.add(passwordWithToggle());
        content.add(Box.createVerticalStrut(22));
        content.add(loginButton);
        content.add(Box.createVerticalStrut(15));
        content.add(new JSeparator());
        content.add(Box.createVerticalStrut(12));
        content.add(farmerButton);
        content.add(Box.createVerticalStrut(7));
        content.add(coordinatorButton);

        formSurface.add(content);
        root.add(formSurface);
        setContentPane(root);
        getRootPane().setDefaultButton(loginButton);
    }

    private JPanel buildBrandPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(FOREST);
        panel.setBorder(new EmptyBorder(44, 46, 38, 44));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        top.setOpaque(false);

        JLabel mark = new JLabel("S");
        mark.setHorizontalAlignment(SwingConstants.CENTER);
        mark.setFont(Ui.font(Font.BOLD, 22));
        mark.setForeground(FOREST);
        mark.setOpaque(true);
        mark.setBackground(Ui.ACCENT);
        mark.setPreferredSize(new Dimension(44, 44));

        JLabel brand = new JLabel("silo");
        brand.setFont(Ui.font(Font.BOLD, 30));
        brand.setForeground(Color.WHITE);

        top.add(mark);
        top.add(brand);
        panel.add(top, BorderLayout.NORTH);

        JPanel story = new JPanel();
        story.setOpaque(false);
        story.setLayout(new BoxLayout(story, BoxLayout.Y_AXIS));

        JLabel overline = new JLabel("A BETTER ROUTE FOR SURPLUS");
        overline.setFont(Ui.font(Font.BOLD, 10));
        overline.setForeground(new Color(0xB7D0BE));

        JLabel statement = new JLabel(
                "<html><div style='width:370px'>"
                + "Good food deserves<br>a place at every table."
                + "</div></html>");
        statement.setFont(Ui.font(Font.BOLD, 32));
        statement.setForeground(Color.WHITE);

        JLabel detail = new JLabel(
                "<html><div style='width:350px;line-height:1.6'>"
                + "Silo brings farmers, volunteers, and community "
                + "partners together to move surplus food where "
                + "it can do the most good.</div></html>");
        detail.setFont(Ui.font(Font.PLAIN, 14));
        detail.setForeground(new Color(0xD2E1D5));

        story.add(overline);
        story.add(Box.createVerticalStrut(17));
        story.add(statement);
        story.add(Box.createVerticalStrut(16));
        story.add(detail);

        panel.add(story, BorderLayout.CENTER);

        JLabel footer = new JLabel("COMMUNITY FOOD RESCUE NETWORK");
        footer.setFont(Ui.font(Font.BOLD, 9));
        footer.setForeground(new Color(0xA5BDAE));
        panel.add(footer, BorderLayout.SOUTH);

        return panel;
    }

    private JComponent passwordWithToggle() {
        return passwordToggle(passwordField);
    }

    private JComponent passwordToggle(JPasswordField field) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);

        field.setBorder(BorderFactory.createCompoundBorder(
                Ui.roundedBorder(BORDER, 11),
                new EmptyBorder(10, 12, 10, 12)));

        JButton toggle = new JButton("Show");
        toggle.setFont(Ui.font(Font.BOLD, 11));
        toggle.setForeground(Ui.PRIMARY);
        toggle.setBackground(Color.WHITE);
        toggle.setFocusPainted(false);
        toggle.setBorder(BorderFactory.createCompoundBorder(
                Ui.roundedBorder(BORDER, 11),
                new EmptyBorder(0, 10, 0, 10)));

        char echo = field.getEchoChar();

        toggle.addActionListener(e -> {
            boolean show = field.getEchoChar() != 0;
            field.setEchoChar(show ? (char) 0 : echo);
            toggle.setText(show ? "Hide" : "Show");
        });

        wrapper.add(field, BorderLayout.CENTER);
        wrapper.add(toggle, BorderLayout.EAST);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        wrapper.setPreferredSize(new Dimension(370, 44));
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);

        return wrapper;
    }

    private JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 12));
        label.setForeground(TEXT);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private void styleField(JTextField field) {
        field.setFont(Ui.font(Font.PLAIN, 14));
        field.setForeground(TEXT);
        field.setBackground(Color.WHITE);
        field.setCaretColor(FOREST);
        field.setBorder(BorderFactory.createCompoundBorder(
                Ui.roundedBorder(BORDER, 11),
                new EmptyBorder(10, 12, 10, 12)));

        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                        Ui.roundedBorder(Ui.PRIMARY, 11),
                        new EmptyBorder(10, 12, 10, 12)));
            }

            @Override
            public void focusLost(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                        Ui.roundedBorder(BORDER, 11),
                        new EmptyBorder(10, 12, 10, 12)));
            }
        });

        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        field.setPreferredSize(new Dimension(370, 44));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    private void login() {
        char[] password = passwordField.getPassword();

        try {
            JsonNode result = supabaseAuth.signIn(
                    emailField.getText(), password);

            String accessToken = result.path("access_token").asText();

            {
                com.silo.storage.SupabaseStore store =
                        new com.silo.storage.SupabaseStore();

                JsonNode profile = store.getMyProfile(accessToken);

                AuthService.Account account =
                        AuthService.Account.fromSupabaseProfile(
                                profile.path("full_name").asText(),
                                profile.path("email").asText(),
                                profile.path("role").asText(),
                                profile.path("status").asText());

                if (account.getStatus() != AuthService.Status.ACTIVE) {
                    JOptionPane.showMessageDialog(
                            this,
                            "Your account is not active. "
                            + "Please contact an administrator.",
                            "Access denied",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }

                dispose();
                onLogin.accept(account);
            }

        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    ex.getMessage(),
                    "Unable to log in",
                    JOptionPane.WARNING_MESSAGE);

        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }

            JOptionPane.showMessageDialog(
                    this,
                    "Silo could not connect to Supabase or retrieve "
                    + "your profile. Please check your connection "
                    + "and account profile.",
                    "Login error",
                    JOptionPane.ERROR_MESSAGE);

        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Silo could not complete the login. "
                    + "Please check the configuration.",
                    "Login error",
                    JOptionPane.ERROR_MESSAGE);

        } finally {
            Arrays.fill(password, '\0');
            passwordField.setText("");
        }
    }

    private void showRegistrationDialog(boolean coordinator) {
        JTextField nameField = new JTextField(22);
        JTextField emailInput = new JTextField(22);
        JPasswordField passwordInput = new JPasswordField(22);
        JPasswordField confirmInput = new JPasswordField(22);

        styleField(nameField);
        styleField(emailInput);
        styleField(passwordInput);
        styleField(confirmInput);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(12, 18, 8, 18));
        panel.setBackground(BG);

        JLabel note = new JLabel(coordinator
                ? "<html><div style='width:380px'>"
                + "Coordinator access begins after an administrator "
                + "reviews and approves your application.</div></html>"
                : "<html><div style='width:380px'>"
                + "Create an account to manage surplus donations "
                + "and connect with the rescue network.</div></html>");

        note.setForeground(MUTED);
        note.setFont(Ui.font(Font.PLAIN, 12));

        panel.add(note);
        panel.add(Box.createVerticalStrut(14));
        panel.add(new JLabel("Full name"));
        panel.add(Box.createVerticalStrut(4));
        panel.add(nameField);
        panel.add(Box.createVerticalStrut(8));
        panel.add(new JLabel("Email address"));
        panel.add(Box.createVerticalStrut(4));
        panel.add(emailInput);
        panel.add(Box.createVerticalStrut(8));
        panel.add(new JLabel("Password (at least 8 characters)"));
        panel.add(Box.createVerticalStrut(4));
        panel.add(passwordToggle(passwordInput));
        panel.add(Box.createVerticalStrut(8));
        panel.add(new JLabel("Confirm password"));
        panel.add(Box.createVerticalStrut(4));
        panel.add(passwordToggle(confirmInput));
        panel.setPreferredSize(new Dimension(450, 370));

        String title = coordinator
                ? "Apply as Food-Bank Coordinator"
                : "Create a Farmer Account";

        int result = JOptionPane.showConfirmDialog(
                this, panel, title,
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (result != JOptionPane.OK_OPTION) {
            clearPasswords(passwordInput, confirmInput);
            return;
        }

        char[] password = passwordInput.getPassword();
        char[] confirmation = confirmInput.getPassword();

        try {
            if (!Arrays.equals(password, confirmation)) {
                throw new IllegalArgumentException(
                        "The passwords do not match.");
            }

            AuthService.Account account;

            if (coordinator) {
                account = authService.applyAsCoordinator(
                        nameField.getText(),
                        emailInput.getText(),
                        password);
            } else {
                account = authService.registerFarmer(
                        nameField.getText(),
                        emailInput.getText(),
                        password);
            }

            String message = coordinator
                    ? "Your coordinator application has been submitted.\n"
                    + "You cannot log in until an authorized person approves it."
                    : "Your farmer account has been created.\n"
                    + "You can now log in.";

            JOptionPane.showMessageDialog(
                    this, message,
                    coordinator ? "Application submitted"
                            : "Registration successful",
                    JOptionPane.INFORMATION_MESSAGE);

            emailField.setText(account.getEmail());
            passwordField.setText("");

        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    ex.getMessage(),
                    "Registration failed",
                    JOptionPane.WARNING_MESSAGE);

        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "The account could not be saved. "
                    + "Please check the data folder.",
                    "Registration error",
                    JOptionPane.ERROR_MESSAGE);

        } finally {
            clearPasswords(passwordInput, confirmInput);
            Arrays.fill(password, '\0');
            Arrays.fill(confirmation, '\0');
        }
    }

    private void clearPasswords(
            JPasswordField first,
            JPasswordField second) {
        first.setText("");
        second.setText("");
    }
}
