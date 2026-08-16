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
import dev.velofine.diagnostics.ui.DiagnosticApp;
import dev.velofine.diagnostics.ui.LastDirStore;
import dev.velofine.diagnostics.ui.VelofineSwingTheme;
import dev.velofine.diagnostics.ui.VelofineSwingTheme.ButtonKind;
import java.awt.BorderLayout;
import java.awt.Color;
import java.nio.file.Path;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;

/** First screen: asks for the user's real Minecraft installation directory (not Velofine's). */
public final class DirectoryPickerScreen extends JPanel {

    private static final int STATIC_ROW_COUNT = 3;

    private final DiagnosticApp app;
    private final JPanel body = new JPanel();
    private final JButton chooseButton = VelofineSwingTheme.button("Choose directory", ButtonKind.SECONDARY);
    private final JButton nextButton = VelofineSwingTheme.button("Next", ButtonKind.PRIMARY);

    private Path selectedDir;
    private String statusText = "NOT SET";
    private Color statusColor = VelofineSwingTheme.DIM_WHITE;

    public DirectoryPickerScreen(DiagnosticApp app) {
        super(new BorderLayout());
        this.app = app;
        VelofineSwingTheme.stylePanel(this);

        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        VelofineSwingTheme.stylePanel(body);
        body.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        body.add(VelofineSwingTheme.sectionLabel("Target Minecraft Directory"));
        body.add(Box.createVerticalStrut(6));
        body.add(VelofineSwingTheme.hint("folder containing versions/ - not Velofine's own folder"));

        chooseButton.addActionListener(e -> chooseDirectory());
        nextButton.addActionListener(e -> {
            if (app.getMcDir() == null) {
                setStatus("NOT SET", VelofineSwingTheme.RED);
                return;
            }
            LastDirStore.save(app.getMcDir());
            app.showVersionPicker();
        });

        add(VelofineSwingTheme.topBar("STEP 1 / 5"), BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);
        add(VelofineSwingTheme.footerBar(chooseButton, nextButton), BorderLayout.SOUTH);

        Path remembered = LastDirStore.load();
        if (remembered != null && McInstallationScanner.isValidInstallation(remembered)) {
            applySelection(remembered);
        } else {
            rebuildDataRows();
        }
    }

    private void chooseDirectory() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Select your Minecraft installation directory");
        if (app.getMcDir() != null) {
            chooser.setCurrentDirectory(app.getMcDir().toFile());
        }
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            applySelection(chooser.getSelectedFile().toPath());
        }
    }

    private void applySelection(Path dir) {
        selectedDir = dir;
        if (!McInstallationScanner.isValidInstallation(dir)) {
            app.setMcDir(null);
            setStatus("INVALID - no versions/ folder found", VelofineSwingTheme.RED);
            return;
        }
        app.setMcDir(dir);
        setStatus("OK", VelofineSwingTheme.TERMINAL_GREEN);
    }

    private void setStatus(String text, Color color) {
        statusText = text;
        statusColor = color;
        rebuildDataRows();
    }

    private void rebuildDataRows() {
        // Rebuilt (not mutated in place) each time - dataRow() is a small, cheap throwaway panel,
        // and rebuilding keeps this screen's state->view mapping a single, reliably-correct path
        // rather than juggling live label references.
        while (body.getComponentCount() > STATIC_ROW_COUNT) {
            body.remove(body.getComponentCount() - 1);
        }
        body.add(Box.createVerticalStrut(20));
        String pathText = selectedDir != null ? selectedDir.toString() : "(none selected)";
        body.add(VelofineSwingTheme.dataRow("Selected path", pathText, VelofineSwingTheme.WHITE));
        body.add(VelofineSwingTheme.dataRow("Status", statusText, statusColor));
        body.add(Box.createVerticalGlue());
        body.revalidate();
        body.repaint();
    }
}
