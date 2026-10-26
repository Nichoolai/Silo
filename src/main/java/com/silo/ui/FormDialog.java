package com.silo.ui;

import com.silo.service.ValidationException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Modal form with an inline error line. Save keeps the dialog open and shows the message if validation fails. */
public abstract class FormDialog extends JDialog {
    private final JPanel form = new JPanel(new GridBagLayout());
    private final JLabel error = new JLabel(" ");
    private int row = 0;
    protected boolean saved = false;

    protected FormDialog(Window owner, String title, String saveText) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(Ui.BG); root.setBorder(new EmptyBorder(24, 28, 20, 28));
        JLabel h = Ui.title(title); h.setFont(Ui.font(Font.BOLD, 20));
        root.add(h, BorderLayout.NORTH);
        form.setBackground(Ui.WHITE);
        form.setBorder(BorderFactory.createCompoundBorder(
            Ui.roundedBorder(Ui.BORDER, 14),
            new EmptyBorder(12, 18, 12, 18)
        ));
        root.add(form, BorderLayout.CENTER);

        error.setForeground(Ui.DANGER); error.setFont(Ui.font(Font.BOLD, 12));
        JButton save = Ui.button(saveText, true), cancel = Ui.button("Cancel", false);
        cancel.addActionListener(e -> dispose());
        save.addActionListener(e -> {
            try { onSave(); saved = true; dispose(); }
            catch (ValidationException ex) { setError(ex.getMessage()); }
            catch (RuntimeException ex) { setError("Unexpected problem: " + ex.getMessage()); }
        });
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0)); buttons.setOpaque(false);
        buttons.add(cancel); buttons.add(save);
        JPanel south = new JPanel(new BorderLayout(0, 8)); south.setOpaque(false);
        south.add(error, BorderLayout.NORTH); south.add(buttons, BorderLayout.SOUTH);
        root.add(south, BorderLayout.SOUTH);
        setContentPane(root);
        setMinimumSize(new Dimension(500, 0));
        getRootPane().setDefaultButton(save);
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    protected void addRow(String label, JComponent c) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridy = row++; g.insets = new Insets(8, 0, 8, 18); g.anchor = GridBagConstraints.WEST;
        JLabel l = new JLabel(label); l.setFont(Ui.font(Font.PLAIN, 13)); l.setForeground(Ui.MUTED);
        form.add(l, g);
        g = new GridBagConstraints();
        g.gridy = row - 1; g.gridx = 1; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL; g.insets = new Insets(8, 0, 8, 0);
        form.add(c, g);
    }
    protected void setError(String m) { error.setText("<html>" + m.replace("<", "&lt;") + "</html>"); pack(); }

    protected abstract void onSave();

    /** Shows the dialog; returns true if it was saved. */
    public boolean open() {
        pack(); setMinimumSize(new Dimension(520, getHeight()));
        setLocationRelativeTo(getOwner()); setVisible(true);
        return saved;
    }
}
