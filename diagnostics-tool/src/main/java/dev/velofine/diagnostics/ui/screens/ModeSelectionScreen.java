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

import dev.velofine.diagnostics.model.Mode;
import dev.velofine.diagnostics.ui.DiagnosticApp;
import dev.velofine.diagnostics.ui.VelofineSwingTheme;
import dev.velofine.diagnostics.ui.VelofineSwingTheme.ButtonKind;
import java.awt.BorderLayout;
import java.awt.Color;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

/**
 * Third screen: BASELINE (test the shipped shaders as-is) or CANDIDATE (test a tester-supplied
 * folder of fixed shader files, used starting Phase 4's fix-iteration loop).
 */
public final class ModeSelectionScreen extends JPanel {

    private static final int STATIC_ROW_COUNT = 14;

    private final DiagnosticApp app;
    private final JPanel body = new JPanel();
    private final JRadioButton baselineOption = new JRadioButton("[ BASELINE ]  test the shipped shaders", true);
    private final JRadioButton candidateOption = new JRadioButton("[ CANDIDATE ]  test a folder of replacement shaders");
    private final JButton pickCandidateButton = VelofineSwingTheme.button("Choose candidate folder", ButtonKind.SECONDARY);
    private final JSpinner repeatCountSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 20, 1));
    private final JCheckBox contextRungSweepCheckbox =
            new JCheckBox("Test every context rung (Core/Compat/legacy - slower, one report per rung)");
    private final JButton runButton = VelofineSwingTheme.button("Run diagnostic", ButtonKind.PRIMARY);

    public ModeSelectionScreen(DiagnosticApp app) {
        super(new BorderLayout());
        this.app = app;
        VelofineSwingTheme.stylePanel(this);

        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        VelofineSwingTheme.stylePanel(body);
        body.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        body.add(VelofineSwingTheme.sectionLabel("Diagnostic Mode"));
        body.add(Box.createVerticalStrut(16));

        styleRadio(baselineOption);
        styleRadio(candidateOption);
        ButtonGroup group = new ButtonGroup();
        group.add(baselineOption);
        group.add(candidateOption);
        body.add(baselineOption);
        body.add(Box.createVerticalStrut(10));
        body.add(candidateOption);
        body.add(Box.createVerticalStrut(10));
        body.add(pickCandidateButton);
        body.add(Box.createVerticalStrut(14));

        body.add(VelofineSwingTheme.sectionLabel("Advanced"));
        body.add(Box.createVerticalStrut(10));

        JPanel repeatRow = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0));
        repeatRow.setBackground(VelofineSwingTheme.BLACK);
        JLabel repeatLabel = new JLabel("Repeat count (flakiness detection): ");
        repeatLabel.setForeground(VelofineSwingTheme.WHITE);
        repeatLabel.setFont(VelofineSwingTheme.BODY);
        repeatCountSpinner.setFont(VelofineSwingTheme.BODY);
        repeatCountSpinner.setMaximumSize(new java.awt.Dimension(70, 28));
        repeatRow.add(repeatLabel);
        repeatRow.add(repeatCountSpinner);
        body.add(repeatRow);
        body.add(Box.createVerticalStrut(10));

        contextRungSweepCheckbox.setBackground(VelofineSwingTheme.BLACK);
        contextRungSweepCheckbox.setForeground(VelofineSwingTheme.WHITE);
        contextRungSweepCheckbox.setFont(VelofineSwingTheme.BODY);
        contextRungSweepCheckbox.setFocusPainted(false);
        body.add(contextRungSweepCheckbox);
        body.add(Box.createVerticalStrut(14));

        pickCandidateButton.addActionListener(e -> chooseCandidateDir());
        candidateOption.addActionListener(e -> {
            if (app.getCandidateShaderDir() == null) {
                chooseCandidateDir();
            }
        });

        JButton backButton = VelofineSwingTheme.button("Back", ButtonKind.BACK);
        backButton.addActionListener(e -> app.showVersionPicker());

        // Disabled immediately on click, not just relying on RunProgressScreen's own runInProgress
        // guard - real, confirmed bug this fixes: a rapid double-click (or any other accidental
        // double-invocation) before the wizard visually transitions away from this screen could
        // fire this listener twice, spawning two independent full pipeline runs concurrently, both
        // writing into the same shared log component. Re-enabled in onShown() whenever this screen
        // becomes visible again (this instance is reused across "run again" cycles, not recreated).
        runButton.addActionListener(e -> {
            if (candidateOption.isSelected() && app.getCandidateShaderDir() == null) {
                return;
            }
            runButton.setEnabled(false);
            app.setMode(candidateOption.isSelected() ? Mode.CANDIDATE : Mode.BASELINE);
            app.setRepeatCount((Integer) repeatCountSpinner.getValue());
            app.setContextRungSweep(contextRungSweepCheckbox.isSelected());
            app.startRun();
        });

        add(VelofineSwingTheme.topBar("STEP 3 / 5"), BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);
        add(VelofineSwingTheme.footerBar(backButton, runButton), BorderLayout.SOUTH);
    }

    /**
     * Called by {@code DiagnosticApp.showModeSelection()} every time this screen becomes visible -
     * re-enables the run button, which the click handler disables to prevent a double-invocation
     * (see that listener's comment).
     */
    public void onShown() {
        runButton.setEnabled(true);
    }

    private void styleRadio(JRadioButton button) {
        button.setBackground(VelofineSwingTheme.BLACK);
        button.setForeground(VelofineSwingTheme.WHITE);
        button.setFont(VelofineSwingTheme.BODY);
        button.setFocusPainted(false);
    }

    private void chooseCandidateDir() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Select the folder of candidate fixed shader files");
        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path dir = chooser.getSelectedFile().toPath();
        app.setCandidateShaderDir(dir);
        candidateOption.setSelected(true);
        rebuildDataRows(dir, countShaderFiles(dir));
    }

    private void rebuildDataRows(Path dir, long shaderFileCount) {
        while (body.getComponentCount() > STATIC_ROW_COUNT) {
            body.remove(body.getComponentCount() - 1);
        }
        Color valueColor = shaderFileCount == 0 ? VelofineSwingTheme.RED : VelofineSwingTheme.WHITE;
        body.add(VelofineSwingTheme.dataRow("Candidate folder", dir.toString(), VelofineSwingTheme.WHITE));
        body.add(VelofineSwingTheme.dataRow("Shader files found", String.valueOf(shaderFileCount), valueColor));
        body.add(Box.createVerticalGlue());
        body.revalidate();
        body.repaint();
    }

    private long countShaderFiles(Path dir) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            long count = 0;
            for (Path p : stream) {
                String name = p.getFileName().toString();
                if (name.endsWith(".fsh") || name.endsWith(".vsh")) {
                    count++;
                }
            }
            return count;
        } catch (IOException e) {
            return 0;
        }
    }
}
