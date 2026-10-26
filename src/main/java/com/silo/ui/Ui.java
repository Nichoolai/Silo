
package com.silo.ui;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.time.*;
import java.util.Date;
import java.util.List;

/** Shared visual system for Silo. */
public final class Ui {
    private Ui() { }

    // Silo's visual identity
    public static final Color FOREST = new Color(0x153B31);
    public static final Color PRIMARY = new Color(0x1F6049);
    public static final Color ACCENT = new Color(0xA5CE82);
    public static final Color BG = new Color(0xF3F6F3);
    public static final Color SAGE = new Color(0xE7F1E9);
    public static final Color WHITE = new Color(0xFFFFFF);
    public static final Color TEXT = new Color(0x1C2D26);
    public static final Color MUTED = new Color(0x75847C);
    public static final Color BORDER = new Color(0xE1E9E3);
    public static final Color SIDEBAR = new Color(0x112D25);
    public static final Color SIDEBAR_TEXT = new Color(0xE4EEE8);
    public static final Color SIDEBAR_MUTED = new Color(0x8FA69A);
    public static final Color SIDEBAR_ICON = new Color(0xA9BDB2);
    public static final Color SIDEBAR_HOVER = new Color(0x1A3B30);
    public static final Color SIDEBAR_ACTIVE = new Color(0x28634A);
    public static final Color DANGER = new Color(0xC95F55);
    public static final Color SUCCESS = new Color(0x286447);
    public static final Color WARN = new Color(0xD99A35);
    public static final Color INFO = new Color(0x6486A8);

    public static final String FONT = "Segoe UI";
    private static final int SURFACE_RADIUS = 14;

    public static Font font(int style, float size) {
        return new Font(FONT, style, 1).deriveFont(style, size);
    }

    public static JLabel title(String t) {
        JLabel l = new JLabel(t);
        l.setFont(font(Font.BOLD, 26));
        l.setForeground(TEXT);
        return l;
    }

    public static JLabel heading(String t) {
        JLabel l = new JLabel(t);
        l.setFont(font(Font.BOLD, 15));
        l.setForeground(TEXT);
        return l;
    }

    public static JLabel muted(String t) {
        JLabel l = new JLabel(t);
        l.setFont(font(Font.PLAIN, 13));
        l.setForeground(MUTED);
        return l;
    }

    /** Reusable rounded buttons. */
    public static JButton button(String text, boolean primary) {
        return button(text, primary, false);
    }

    public static JButton button(String text, boolean primary, boolean danger) {
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
                );

                boolean hover = getModel().isRollover() && isEnabled();
                Color fill;

                if (!isEnabled()) {
                    fill = primary
                        ? new Color(0xA8B9AF)
                        : new Color(0xF0F2ED);
                } else if (primary) {
                    fill = hover
                        ? new Color(0x2D604A)
                        : FOREST;
                } else {
                    fill = hover ? new Color(0xEAF1E8) : WHITE;
                }

                g2.setColor(fill);
                g2.fillRoundRect(
                    0, 0, getWidth() - 1, getHeight() - 1, 15, 15
                );

                if (!primary) {
                    g2.setColor(danger ? new Color(0xE8C7C7) : BORDER);
                    g2.drawRoundRect(
                        0, 0, getWidth() - 1, getHeight() - 1, 15, 15
                    );
                }

                g2.dispose();
                super.paintComponent(g);
            }
        };

        b.setFont(font(Font.BOLD, 13));
        b.setForeground(primary ? WHITE : danger ? DANGER : FOREST);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setBorder(new EmptyBorder(10, 17, 10, 17));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setRolloverEnabled(true);

        return b;
    }

    /** Quiet action button designed for the dark navigation rail. */
    public static JButton sidebarButton(String text) {
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color fill = !isEnabled()
                    ? new Color(0x1A352C)
                    : getModel().isRollover() ? new Color(0x29483B) : new Color(0x1D3A30);
                g.setColor(fill);
                g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 13, 13);
                g.setColor(new Color(0x365347));
                g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 13, 13);
                g.dispose();
                super.paintComponent(graphics);
            }
        };
        b.setFont(font(Font.BOLD, 12));
        b.setForeground(SIDEBAR_TEXT);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setBorder(new EmptyBorder(10, 12, 10, 10));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setRolloverEnabled(true);
        return b;
    }

    /** Segmented navigation buttons. */
    public static JToggleButton tabButton(String text) {
        JToggleButton b = new JToggleButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
                );

                g2.setColor(
                    isSelected()
                        ? FOREST
                        : getModel().isRollover()
                            ? new Color(0xEDF4EE)
                            : WHITE
                );

                g2.fillRoundRect(
                    0, 0, getWidth() - 1, getHeight() - 1, 14, 14
                );

                if (!isSelected()) {
                    g2.setColor(BORDER);
                    g2.drawRoundRect(
                        0, 0, getWidth() - 1, getHeight() - 1, 14, 14
                    );
                }

                g2.dispose();
                setForeground(isSelected() ? WHITE : FOREST);
                super.paintComponent(g);
            }
        };

        b.setFont(font(Font.BOLD, 13));
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setBorder(new EmptyBorder(9, 18, 9, 18));
        b.setRolloverEnabled(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        return b;
    }

    /** Consistent white cards with subtle borders. */
    public static JPanel card(LayoutManager lm) {
        JPanel p = new SurfacePanel(lm);
        p.setBackground(WHITE);
        p.setBorder(new EmptyBorder(18, 19, 18, 19));
        return p;
    }

    public static Border roundedBorder(Color color, int radius) {
        return new RoundedBorder(color, radius);
    }

    /** Install the shared Swing theme before creating windows. */
    public static void installTheme() {
        System.setProperty("swing.boldMetal", "false");

        try {
            UIManager.setLookAndFeel(
                UIManager.getCrossPlatformLookAndFeelClassName()
            );
        } catch (Exception ignored) { }

        Font f = font(Font.PLAIN, 13);

        for (String k : new String[] {
            "Label.font",
            "Button.font",
            "ToggleButton.font",
            "ComboBox.font",
            "TextField.font",
            "TextArea.font",
            "Spinner.font",
            "OptionPane.messageFont",
            "OptionPane.buttonFont",
            "ToolTip.font",
            "CheckBox.font",
            "FormattedTextField.font",
            "Table.font",
            "TableHeader.font",
            "List.font"
        }) {
            UIManager.put(k, f);
        }

        UIManager.put("Label.foreground", TEXT);
        UIManager.put("Panel.background", BG);

        UIManager.put("Button.background", WHITE);
        UIManager.put("Button.foreground", TEXT);

        UIManager.put("ComboBox.background", WHITE);
        UIManager.put("ComboBox.foreground", TEXT);
        UIManager.put("ComboBox.selectionBackground", SAGE);
        UIManager.put("ComboBox.selectionForeground", TEXT);

        UIManager.put("TextField.background", WHITE);
        UIManager.put("TextField.foreground", TEXT);
        UIManager.put("TextField.selectionBackground", SAGE);
        UIManager.put("TextField.selectionForeground", TEXT);

        UIManager.put("TextArea.background", WHITE);
        UIManager.put("TextArea.foreground", TEXT);
        UIManager.put("TextArea.selectionBackground", SAGE);
        UIManager.put("TextArea.selectionForeground", TEXT);

        UIManager.put("OptionPane.background", WHITE);
        UIManager.put("OptionPane.messageForeground", TEXT);
        UIManager.put("OptionPane.buttonBackground", WHITE);
        UIManager.put("OptionPane.buttonForeground", TEXT);
        UIManager.put("ToolTip.background", FOREST);
        UIManager.put("ToolTip.foreground", WHITE);

        UIManager.put("ScrollBar.thumb", new Color(0xCCD9CC));
        UIManager.put("ScrollBar.thumbShadow", new Color(0xCCD9CC));
        UIManager.put("ScrollBar.thumbHighlight", new Color(0xCCD9CC));
        UIManager.put("ScrollBar.track", BG);

        UIManager.put("Table.background", WHITE);
        UIManager.put("Table.foreground", TEXT);
        UIManager.put("Table.selectionBackground", SAGE);
        UIManager.put("Table.selectionForeground", TEXT);
        UIManager.put("Table.gridColor", BORDER);
        UIManager.put("TableHeader.background", new Color(0xF2F6F2));
        UIManager.put("TableHeader.foreground", MUTED);
        UIManager.put("TableHeader.font", font(Font.BOLD, 12));
        UIManager.put("ComboBox.border", roundedBorder(BORDER, 10));
        UIManager.put("Spinner.border", roundedBorder(BORDER, 10));
    }

    /** Text fields with optional placeholder text. */
    public static JTextField field(int cols) {
        return field(cols, null);
    }

    public static JTextField field(int cols, String placeholder) {
        JTextField f = new JTextField(cols) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                if (placeholder != null && getText().isEmpty()
                        && !hasFocus()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(
                        RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON
                    );

                    g2.setColor(new Color(0x929D95));
                    g2.setFont(getFont());

                    g2.drawString(
                        placeholder,
                        getInsets().left,
                        getHeight() / 2
                            + g2.getFontMetrics().getAscent() / 2 - 1
                    );

                    g2.dispose();
                }
            }
        };

        f.setFont(font(Font.PLAIN, 13));
        f.setForeground(TEXT);
        f.setBackground(WHITE);
        f.setCaretColor(FOREST);
        f.setPreferredSize(new Dimension(180, 40));
        f.setBorder(new CompoundBorder(
            roundedBorder(BORDER, 11),
            new EmptyBorder(6, 10, 6, 10)
        ));

        return f;
    }

    /** Read-only data tables with consistent spacing and styling. */
    public static JTable table(String... columns) {
        DefaultTableModel m = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        JTable t = new JTable(m);
        t.setRowHeight(42);
        t.setFont(font(Font.PLAIN, 13));
        t.setForeground(TEXT);
        t.setBackground(WHITE);
        t.setShowGrid(false);
        t.setShowHorizontalLines(true);
        t.setGridColor(new Color(0xEEF0EA));
        t.setSelectionBackground(SAGE);
        t.setSelectionForeground(TEXT);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setFillsViewportHeight(true);
        t.setAutoCreateRowSorter(true);
        t.setIntercellSpacing(new Dimension(0, 1));
        t.setBorder(null);

        JTableHeader h = t.getTableHeader();
        h.setReorderingAllowed(false);
        h.setPreferredSize(new Dimension(0, 42));
        h.setBackground(new Color(0xF2F6F2));
        h.setForeground(MUTED);
        h.setFont(font(Font.BOLD, 12));

        h.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable tb, Object v, boolean s, boolean f,
                    int r, int c) {

                JLabel l = (JLabel) super.getTableCellRendererComponent(
                    tb, v, false, false, r, c
                );

                l.setOpaque(true);
                l.setBackground(new Color(0xF2F6F2));
                l.setForeground(MUTED);
                l.setFont(font(Font.BOLD, 12));
                l.setBorder(new CompoundBorder(
                    new MatteBorder(0, 0, 1, 0, BORDER),
                    new EmptyBorder(0, 9, 0, 9)
                ));

                return l;
            }
        });

        t.setDefaultRenderer(Object.class, new StatusAwareRenderer());

        return t;
    }

    private static final class StatusAwareRenderer implements TableCellRenderer {
        private final DefaultTableCellRenderer textRenderer = new DefaultTableCellRenderer();
        private final JPanel cell = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        private final StatusBadge badge = new StatusBadge();

        StatusAwareRenderer() {
            cell.setBorder(new EmptyBorder(0, 1, 0, 1));
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean selected, boolean focused, int row, int column) {
            String label = value == null ? "" : value.toString();
            BadgeColors colors = badgeColors(label);
            if (colors == null) {
                JLabel rendered = (JLabel) textRenderer.getTableCellRendererComponent(
                    table, value, selected, focused, row, column
                );
                rendered.setOpaque(true);
                rendered.setBorder(new EmptyBorder(0, 9, 0, 9));
                rendered.setToolTipText(label.isEmpty() ? null : label);
                rendered.setBackground(selected ? table.getSelectionBackground()
                    : row % 2 == 0 ? WHITE : new Color(0xFCFCF9));
                rendered.setForeground(TEXT);
                return rendered;
            }

            cell.removeAll();
            cell.setOpaque(true);
            cell.setBackground(selected ? table.getSelectionBackground()
                : row % 2 == 0 ? WHITE : new Color(0xFCFCF9));
            cell.setToolTipText(label.isEmpty() ? null : label);
            badge.setText(label);
            badge.setColors(colors.background(), colors.foreground());
            badge.setFont(font(Font.BOLD, 11));
            cell.add(badge);
            return cell;
        }
    }

    private record BadgeColors(Color background, Color foreground) { }

    private static BadgeColors badgeColors(String value) {
        return switch (value.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "available", "active", "delivered", "approved" ->
                new BadgeColors(new Color(0xE4F3E9), new Color(0x23734B));
            case "scheduled", "pending", "overdue (scheduled)" ->
                new BadgeColors(new Color(0xFFF2D9), new Color(0x96630E));
            case "picked up" ->
                new BadgeColors(new Color(0xE6F0FA), new Color(0x416E9C));
            case "cancelled", "rejected", "inactive", "overdue" ->
                new BadgeColors(new Color(0xFBE9E7), new Color(0xA94740));
            default -> null;
        };
    }

    private static final class StatusBadge extends JLabel {
        private Color backgroundColor = SAGE;

        StatusBadge() {
            setOpaque(false);
            setBorder(new EmptyBorder(4, 9, 4, 9));
            setHorizontalAlignment(SwingConstants.CENTER);
        }

        void setColors(Color background, Color foreground) {
            backgroundColor = background;
            setForeground(foreground);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(backgroundColor);
            g.fillRoundRect(0, 1, getWidth() - 1, getHeight() - 2, 12, 12);
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    /** Scroll panes that match the rest of the interface. */
    public static JScrollPane scroll(JComponent c) {
        if (c instanceof JTable table) {
            table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        }
        JScrollPane sp = new JScrollPane(c);
        if (c instanceof JTable) {
            sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        }
        sp.setBorder(roundedBorder(BORDER, 12));
        sp.getViewport().setBackground(WHITE);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        sp.getHorizontalScrollBar().setUnitIncrement(16);
        return sp;
    }

    public static DefaultTableModel model(JTable t) {
        return (DefaultTableModel) t.getModel();
    }

    public static void fill(JTable t, List<Object[]> rows) {
        DefaultTableModel m = model(t);
        m.setRowCount(0);

        for (Object[] r : rows) {
            m.addRow(r);
        }

        autoWidth(t);
    }

    /** Size columns according to their content, with a reasonable cap. */
    public static void autoWidth(JTable t) {
        TableColumnModel cm = t.getColumnModel();

        for (int c = 0; c < cm.getColumnCount(); c++) {
            TableColumn col = cm.getColumn(c);

            Component h = t.getTableHeader()
                .getDefaultRenderer()
                .getTableCellRendererComponent(
                    t, col.getHeaderValue(), false, false, -1, c
                );

            int w = h.getPreferredSize().width + 8;

            for (int r = 0; r < Math.min(t.getRowCount(), 200); r++) {
                w = Math.max(
                    w,
                    t.prepareRenderer(t.getCellRenderer(r, c), r, c)
                        .getPreferredSize().width + 8
                );
            }

            col.setPreferredWidth(Math.min(w, 320));
        }
    }

    /** ID from the selected row, or -1 if nothing is selected. */
    public static int selectedId(JTable t, int col) {
        int r = t.getSelectedRow();
        return r < 0
            ? -1
            : Integer.parseInt(String.valueOf(t.getValueAt(r, col)));
    }

    /** Helper text for empty table areas. */
    public static JPanel emptyHint(String text) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        p.add(muted(
            "<html><div style='text-align:center'>" + text + "</div></html>"
        ));
        return p;
    }

    private static final class RoundedBorder extends AbstractBorder {
        private final Color color;
        private final int radius;

        RoundedBorder(Color color, int radius) {
            this.color = color;
            this.radius = radius;
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.set(1, 1, 1, 1);
            return insets;
        }

        @Override
        public void paintBorder(
                Component c, Graphics graphics, int x, int y, int width, int height) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(color);
            g.draw(new RoundRectangle2D.Float(
                x + 0.5f, y + 0.5f, width - 1, height - 1, radius, radius
            ));
            g.dispose();
        }
    }

    private static final class SurfacePanel extends JPanel {
        private boolean hovered;

        SurfacePanel(LayoutManager layout) {
            super(layout);
            setOpaque(false);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    setHovered(true);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    updateHoverFrom(e);
                }
            });
        }

        @Override
        protected void addImpl(Component component, Object constraints, int index) {
            super.addImpl(component, constraints, index);
            trackHover(component);
        }

        private void trackHover(Component component) {
            component.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    setHovered(true);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    updateHoverFrom(e);
                }
            });
            if (component instanceof Container container) {
                for (Component child : container.getComponents()) {
                    trackHover(child);
                }
            }
        }

        private void updateHoverFrom(MouseEvent e) {
            Point point = SwingUtilities.convertPoint(
                e.getComponent(), e.getPoint(), this
            );
            setHovered(contains(point));
        }

        private void setHovered(boolean value) {
            if (hovered != value) {
                hovered = value;
                repaint();
            }
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color fill = hovered ? new Color(0xF9FCFA) : getBackground();
            g.setColor(fill);
            g.fill(new RoundRectangle2D.Float(
                0, 0, getWidth() - 1, getHeight() - 1,
                SURFACE_RADIUS, SURFACE_RADIUS
            ));
            g.setColor(hovered ? new Color(0xC8D9CD) : BORDER);
            g.draw(new RoundRectangle2D.Float(
                0.5f, 0.5f, getWidth() - 2, getHeight() - 2,
                SURFACE_RADIUS, SURFACE_RADIUS
            ));
            g.dispose();
        }
    }

    public static JSpinner dateSpinner(LocalDate d) {
        JSpinner s = new JSpinner(new SpinnerDateModel(
            Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant()),
            null, null, java.util.Calendar.DAY_OF_MONTH
        ));

        s.setEditor(new JSpinner.DateEditor(s, "yyyy-MM-dd"));
        styleSpinner(s);
        return s;
    }

    public static JSpinner dateTimeSpinner(LocalDateTime d) {
        JSpinner s = new JSpinner(new SpinnerDateModel(
            Date.from(d.atZone(ZoneId.systemDefault()).toInstant()),
            null, null, java.util.Calendar.MINUTE
        ));

        s.setEditor(new JSpinner.DateEditor(s, "yyyy-MM-dd HH:mm"));
        styleSpinner(s);
        return s;
    }

    private static void styleSpinner(JSpinner s) {
        s.setFont(font(Font.PLAIN, 13));
        s.setPreferredSize(new Dimension(190, 36));
        s.setBorder(roundedBorder(BORDER, 10));
        s.setBackground(WHITE);
    }

    public static LocalDate dateOf(JSpinner s) {
        return ((Date) s.getValue()).toInstant()
            .atZone(ZoneId.systemDefault()).toLocalDate();
    }

    public static LocalDateTime dateTimeOf(JSpinner s) {
        return ((Date) s.getValue()).toInstant()
            .atZone(ZoneId.systemDefault()).toLocalDateTime()
            .withSecond(0).withNano(0);
    }

    /** Dropdown item carrying a database ID. */
    public record Item(int id, String label) {
        @Override
        public String toString() {
            return label;
        }
    }
}
