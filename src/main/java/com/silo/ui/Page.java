package com.silo.ui;

import com.silo.service.SiloService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Base class for every screen. refresh() re-reads data from the service. */
abstract class Page extends JPanel {
    protected final SiloApp app;
    protected final SiloService svc;

    Page(SiloApp app) {
        this.app = app; this.svc = app.service();
        setLayout(new BorderLayout(0, 20));
        setBackground(Ui.BG);
        setBorder(new EmptyBorder(26, 28, 24, 28));
    }

    abstract void refresh();

    /** Title row with an optional subtitle on the left and action components on the right. */
    protected JComponent header(String title, String subtitle, JComponent... actions) {
        JPanel p = new JPanel(new BorderLayout(12, 0)); p.setOpaque(false);
        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        copy.add(Ui.title(title));
        copy.add(Box.createVerticalStrut(3));
        copy.add(Ui.muted(subtitle));
        JPanel accent = new JPanel();
        accent.setBackground(Ui.ACCENT);
        accent.setPreferredSize(new Dimension(4, 48));
        JPanel left = new JPanel(new BorderLayout(12, 0));
        left.setOpaque(false);
        left.add(accent, BorderLayout.WEST);
        left.add(copy, BorderLayout.CENTER);
        p.add(left, BorderLayout.WEST);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0)); right.setOpaque(false);
        for (JComponent a : actions) right.add(a);
        p.add(right, BorderLayout.EAST);
        return p;
    }

    protected JPanel toolbar(JComponent... items) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); p.setOpaque(false);
        for (JComponent i : items) p.add(i);
        return p;
    }

    protected JTextField searchField(String placeholder, Runnable onChange) {
        JTextField f = Ui.field(26, placeholder);
        f.setToolTipText(placeholder);
        f.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { onChange.run(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { onChange.run(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { onChange.run(); }
        });
        return f;
    }

    protected boolean confirm(String message) {
        return JOptionPane.showConfirmDialog(this, message, "Please confirm", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }

    /** Table in a scroll pane; swaps to a centred hint when the table has no rows. */
    protected JComponent tableWithHint(JTable table, String emptyText) {
        CardLayout cl = new CardLayout();
        JPanel wrap = new JPanel(cl); wrap.setOpaque(false);
        JPanel hint = Ui.card(new GridBagLayout());
        hint.add(Ui.muted("<html><div style='text-align:center;width:360px'>" + emptyText + "</div></html>"));
        wrap.add(Ui.scroll(table), "table"); wrap.add(hint, "hint");
        Runnable sync = () -> cl.show(wrap, table.getModel().getRowCount() == 0 ? "hint" : "table");
        table.getModel().addTableModelListener(e -> sync.run());
        sync.run();
        return wrap;
    }
}
