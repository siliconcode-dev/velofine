/*
 * This file is part of Velofine.
 *
 * Velofine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Velofine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Velofine. If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2026 siliconcode-dev
 */

package dev.velofine.diagnostics.ui.screens;

import dev.velofine.diagnostics.mc.McInstallationScanner;
import dev.velofine.diagnostics.mc.McVersionEntry;
import dev.velofine.diagnostics.ui.DiagnosticApp;
import dev.velofine.diagnostics.ui.VelofineSwingTheme;
import dev.velofine.diagnostics.ui.VelofineSwingTheme.ButtonKind;
import java.awt.BorderLayout;
import java.awt.Component;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/** Second screen: lists installed Minecraft versions found under {@code <mcDir>/versions/}. */
public final class VersionPickerScreen extends JPanel {

    private final DiagnosticApp app;
    private final DefaultListModel<McVersionEntry> listModel = new DefaultListModel<>();
    private final JList<McVersionEntry> list = new JList<>(listModel);
    private final JLabel statusLabel = VelofineSwingTheme.hint("26.2 is preferred and pre-selected when present");
    private final JButton backButton = VelofineSwingTheme.button("Back", ButtonKind.BACK);
    private final JButton nextButton = VelofineSwingTheme.button("Next", ButtonKind.PRIMARY);

    public VersionPickerScreen(DiagnosticApp app) {
        super(new BorderLayout());
        this.app = app;
        VelofineSwingTheme.stylePanel(this);

        list.setBackground(VelofineSwingTheme.BLACK);
        list.setForeground(VelofineSwingTheme.WHITE);
        list.setFont(VelofineSwingTheme.BODY);
        list.setSelectionBackground(VelofineSwingTheme.RED);
        list.setSelectionForeground(VelofineSwingTheme.WHITE);
        list.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        list.setCellRenderer(new VersionCellRenderer());

        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(VelofineSwingTheme.BLACK);

        JPanel body = new JPanel(new BorderLayout());
        VelofineSwingTheme.stylePanel(body);
        body.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        VelofineSwingTheme.stylePanel(header);
        header.add(VelofineSwingTheme.sectionLabel("Installed Minecraft Versions"));
        header.add(Box.createVerticalStrut(6));
        header.add(statusLabel);
        header.add(Box.createVerticalStrut(14));

        body.add(header, BorderLayout.NORTH);
        body.add(VelofineSwingTheme.compartment("VERSIONS", scrollPane), BorderLayout.CENTER);

        backButton.addActionListener(e -> app.showDirectoryPicker());
        nextButton.addActionListener(e -> {
            McVersionEntry selected = list.getSelectedValue();
            if (selected == null) {
                return;
            }
            app.setSelectedVersion(selected);
            app.showModeSelection();
        });

        add(VelofineSwingTheme.topBar("STEP 2 / 5"), BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);
        add(VelofineSwingTheme.footerBar(backButton, nextButton), BorderLayout.SOUTH);
    }

    /** Re-scans {@code app.getMcDir()} every time this screen becomes visible. */
    public void refresh() {
        listModel.clear();
        try {
            List<McVersionEntry> versions = McInstallationScanner.scan(app.getMcDir());
            versions.forEach(listModel::addElement);
            if (versions.isEmpty()) {
                statusLabel.setText("> NO VERSIONS FOUND in " + app.getMcDir().resolve("versions") + " - go back and re-check your directory");
                statusLabel.setForeground(VelofineSwingTheme.RED);
            } else {
                list.setSelectedIndex(0);
                statusLabel.setText("> " + versions.size() + " version(s) found - 26.2 is preferred and pre-selected when present");
                statusLabel.setForeground(VelofineSwingTheme.DIM_WHITE);
            }
        } catch (Exception e) {
            statusLabel.setText("> FAILED TO SCAN versions/: " + e.getMessage());
            statusLabel.setForeground(VelofineSwingTheme.RED);
        }
    }

    private static final class VersionCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
                JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            McVersionEntry entry = (McVersionEntry) value;
            boolean preferred = entry.versionId().equals(McInstallationScanner.PREFERRED_VERSION_ID);
            label.setText((preferred ? "[ " + entry.versionId() + " ]" : "  " + entry.versionId()));
            label.setFont(preferred ? VelofineSwingTheme.MICRO_BOLD : VelofineSwingTheme.BODY);
            label.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
            label.setOpaque(true);
            label.setBackground(isSelected ? VelofineSwingTheme.RED : VelofineSwingTheme.BLACK);
            label.setForeground(VelofineSwingTheme.WHITE);
            return label;
        }
    }
}
