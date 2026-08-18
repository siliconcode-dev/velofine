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

import dev.velofine.diagnostics.model.DiagnosticReport;
import dev.velofine.diagnostics.model.HumanVisualCheck;
import dev.velofine.diagnostics.report.ReportPaths;
import dev.velofine.diagnostics.report.ReportWriter;
import dev.velofine.diagnostics.ui.DiagnosticApp;
import dev.velofine.diagnostics.ui.VelofineSwingTheme;
import dev.velofine.diagnostics.ui.VelofineSwingTheme.ButtonKind;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.nio.file.Path;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * Inserted between the run finishing and the Results screen - a lightweight, entirely skippable
 * prompt for the tester's own in-game observation (see {@link HumanVisualCheck}'s javadoc for why
 * this exists: Build_plan_v1.5's own design treats a human's eyes as the real final visual-
 * correctness check, this just gives that observation a structured home in the report).
 *
 * <p>The report is written to disk from here, not from {@code RunProgressScreen} - {@link
 * DiagnosticApp} defers the actual {@code ReportWriter.write} call until this screen's
 * Continue/Skip action fires, since {@code SwingWorker#doInBackground()} runs off the Event
 * Dispatch Thread and can't itself pause for interactive Swing input; routing through a real screen
 * in the normal {@code CardLayout} flow avoids that without needing to re-write an already-saved
 * file afterward.
 */
public final class HumanVisualCheckScreen extends JPanel {

    private static final String[] TRI_STATE_OPTIONS = {"Not checked", "Yes", "No"};

    private final DiagnosticApp app;
    private final JComboBox<String> lavaWaterPortalsCombo = new JComboBox<>(TRI_STATE_OPTIONS);
    private final JComboBox<String> blackBlocksCombo = new JComboBox<>(TRI_STATE_OPTIONS);
    private final JTextArea notesArea = new JTextArea(4, 40);
    private final JLabel statusLabel = new JLabel(" ");

    private DiagnosticReport pendingReport;
    private String runTimestamp;

    public HumanVisualCheckScreen(DiagnosticApp app) {
        super(new BorderLayout());
        this.app = app;
        VelofineSwingTheme.stylePanel(this);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        VelofineSwingTheme.stylePanel(body);
        body.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JLabel intro = VelofineSwingTheme.hint(
                "Optional: if you also launched the real Velofine build in-game on this hardware, "
                        + "what did you actually see? Entirely skippable - the diagnostic report is saved either way.");
        intro.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));
        body.add(intro);

        JPanel questions = new JPanel(new GridLayout(2, 2, 12, 10));
        VelofineSwingTheme.stylePanel(questions);
        questions.add(questionLabel("Lava/water/portals render visible (not invisible/x-ray)?"));
        questions.add(lavaWaterPortalsCombo);
        questions.add(questionLabel("Previously-black blocks render with correct color?"));
        questions.add(blackBlocksCombo);
        questions.add(new JLabel());
        questions.add(new JLabel());
        body.add(questions);
        body.add(Box.createVerticalStrut(16));

        body.add(VelofineSwingTheme.sectionLabel("NOTES (optional)"));
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        notesArea.setBackground(VelofineSwingTheme.BLACK);
        notesArea.setForeground(VelofineSwingTheme.WHITE);
        notesArea.setFont(VelofineSwingTheme.BODY);
        JScrollPane notesScroll = new JScrollPane(notesArea);
        body.add(notesScroll);
        body.add(Box.createVerticalStrut(10));

        statusLabel.setForeground(VelofineSwingTheme.RED);
        body.add(statusLabel);

        JButton skipButton = VelofineSwingTheme.button("Skip", ButtonKind.SECONDARY);
        skipButton.addActionListener(e -> saveAndContinue(false));

        JButton continueButton = VelofineSwingTheme.button("Continue", ButtonKind.PRIMARY);
        continueButton.addActionListener(e -> saveAndContinue(true));

        JPanel leftButtons = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 12, 0));
        leftButtons.setBackground(VelofineSwingTheme.BLACK);
        leftButtons.add(skipButton);

        add(VelofineSwingTheme.topBar("BEFORE RESULTS - YOUR OBSERVATION"), BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);
        add(VelofineSwingTheme.footerBar(leftButtons, continueButton), BorderLayout.SOUTH);
    }

    private JLabel questionLabel(String text) {
        JLabel label = new JLabel("<html>" + text + "</html>");
        label.setForeground(VelofineSwingTheme.WHITE);
        label.setFont(VelofineSwingTheme.BODY);
        return label;
    }

    /** Called by {@link DiagnosticApp} right before showing this card - resets fields for a fresh run. */
    public void reset(DiagnosticReport report, String runTimestamp) {
        this.pendingReport = report;
        this.runTimestamp = runTimestamp;
        lavaWaterPortalsCombo.setSelectedIndex(0);
        blackBlocksCombo.setSelectedIndex(0);
        notesArea.setText("");
        statusLabel.setText(" ");
    }

    private void saveAndContinue(boolean recordAnswers) {
        HumanVisualCheck check = recordAnswers ? buildCheckIfAnyAnswered() : null;
        DiagnosticReport finalReport = check != null ? pendingReport.withHumanVisualCheck(check) : pendingReport;

        try {
            Path saved = ReportWriter.write(finalReport, ReportPaths.reportsDirectory(), runTimestamp);
            app.showResults(finalReport, saved, null);
        } catch (Exception e) {
            statusLabel.setText("Failed to write report: " + e);
        }
    }

    /** Returns {@code null} if every field is still "Not checked"/blank - no point attaching an empty check. */
    private HumanVisualCheck buildCheckIfAnyAnswered() {
        Boolean lavaWaterPortals = triStateValue(lavaWaterPortalsCombo);
        Boolean blackBlocks = triStateValue(blackBlocksCombo);
        String notes = notesArea.getText() != null ? notesArea.getText().strip() : "";

        if (lavaWaterPortals == null && blackBlocks == null && notes.isEmpty()) {
            return null;
        }
        return new HumanVisualCheck(lavaWaterPortals, blackBlocks, notes.isEmpty() ? null : notes);
    }

    private Boolean triStateValue(JComboBox<String> combo) {
        return switch (combo.getSelectedIndex()) {
            case 1 -> Boolean.TRUE;
            case 2 -> Boolean.FALSE;
            default -> null;
        };
    }
}
