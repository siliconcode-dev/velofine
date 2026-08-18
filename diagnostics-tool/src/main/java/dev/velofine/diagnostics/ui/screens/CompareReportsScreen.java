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

import com.google.gson.Gson;
import dev.velofine.diagnostics.model.ComparisonResult;
import dev.velofine.diagnostics.model.DiagnosticReport;
import dev.velofine.diagnostics.model.ShaderComparisonEntry;
import dev.velofine.diagnostics.model.ShaderComparisonEntry.Classification;
import dev.velofine.diagnostics.report.ReportComparator;
import dev.velofine.diagnostics.ui.DiagnosticApp;
import dev.velofine.diagnostics.ui.VelofineSwingTheme;
import dev.velofine.diagnostics.ui.VelofineSwingTheme.ButtonKind;
import java.awt.BorderLayout;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Sixth screen (reachable from {@link ResultsScreen}, not part of the main linear wizard flow):
 * loads two saved {@code velofine-diagnosis-*.json} reports and diffs their shader compile
 * outcomes via {@link ReportComparator} - Phase 4's actual before/after fix-iteration use case,
 * built into the GUI directly instead of eyeballing two JSON files by hand.
 */
public final class CompareReportsScreen extends JPanel {

    private static final Gson GSON = new Gson();

    private final DiagnosticApp app;
    private final JLabel pathALabel = VelofineSwingTheme.hint("no file chosen");
    private final JLabel pathBLabel = VelofineSwingTheme.hint("no file chosen");
    private final JTextArea details = new JTextArea();
    private Path reportAPath;
    private Path reportBPath;

    public CompareReportsScreen(DiagnosticApp app) {
        super(new BorderLayout());
        this.app = app;
        VelofineSwingTheme.stylePanel(this);

        JPanel body = new JPanel(new BorderLayout(0, 16));
        VelofineSwingTheme.stylePanel(body);
        body.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JPanel pickers = new JPanel();
        pickers.setLayout(new BoxLayout(pickers, BoxLayout.Y_AXIS));
        VelofineSwingTheme.stylePanel(pickers);

        JButton pickAButton = VelofineSwingTheme.button("Choose report A (before)", ButtonKind.SECONDARY);
        pickAButton.addActionListener(e -> {
            Path chosen = chooseReportFile();
            if (chosen != null) {
                reportAPath = chosen;
                pathALabel.setText("> " + chosen);
            }
        });
        JButton pickBButton = VelofineSwingTheme.button("Choose report B (after)", ButtonKind.SECONDARY);
        pickBButton.addActionListener(e -> {
            Path chosen = chooseReportFile();
            if (chosen != null) {
                reportBPath = chosen;
                pathBLabel.setText("> " + chosen);
            }
        });

        pickers.add(VelofineSwingTheme.sectionLabel("Report A (before)"));
        pickers.add(Box.createVerticalStrut(4));
        pickers.add(pathALabel);
        pickers.add(Box.createVerticalStrut(4));
        pickers.add(pickAButton);
        pickers.add(Box.createVerticalStrut(16));
        pickers.add(VelofineSwingTheme.sectionLabel("Report B (after)"));
        pickers.add(Box.createVerticalStrut(4));
        pickers.add(pathBLabel);
        pickers.add(Box.createVerticalStrut(4));
        pickers.add(pickBButton);
        pickers.add(Box.createVerticalStrut(16));

        JButton compareButton = VelofineSwingTheme.button("Compare", ButtonKind.PRIMARY);
        compareButton.addActionListener(e -> runComparison());
        pickers.add(compareButton);

        details.setEditable(false);
        details.setBackground(VelofineSwingTheme.BLACK);
        details.setForeground(VelofineSwingTheme.WHITE);
        details.setFont(VelofineSwingTheme.BODY);
        details.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JScrollPane scrollPane = new JScrollPane(details);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(VelofineSwingTheme.BLACK);

        body.add(pickers, BorderLayout.NORTH);
        body.add(VelofineSwingTheme.compartment("COMPARISON", scrollPane), BorderLayout.CENTER);

        JButton backButton = VelofineSwingTheme.button("Back to results", ButtonKind.BACK);
        backButton.addActionListener(e -> app.showModeSelection());

        add(VelofineSwingTheme.topBar("REPORT COMPARISON"), BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);
        add(VelofineSwingTheme.footerBar(backButton, null), BorderLayout.SOUTH);
    }

    private Path chooseReportFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Velofine diagnosis report (*.json)", "json"));
        if (reportAPath != null) {
            chooser.setCurrentDirectory(reportAPath.getParent().toFile());
        }
        int result = chooser.showOpenDialog(this);
        return result == JFileChooser.APPROVE_OPTION ? chooser.getSelectedFile().toPath() : null;
    }

    private void runComparison() {
        if (reportAPath == null || reportBPath == null) {
            details.setText("Choose both reports first.");
            return;
        }
        try {
            DiagnosticReport reportA = GSON.fromJson(Files.readString(reportAPath, StandardCharsets.UTF_8), DiagnosticReport.class);
            DiagnosticReport reportB = GSON.fromJson(Files.readString(reportBPath, StandardCharsets.UTF_8), DiagnosticReport.class);
            ComparisonResult result = ReportComparator.compare(reportA, reportB);
            render(result);
        } catch (Exception e) {
            details.setText("Failed to load/compare reports: " + e);
        }
    }

    private void render(ComparisonResult result) {
        StringBuilder sb = new StringBuilder();
        if (!result.sameMachine()) {
            sb.append("[ WARNING ] ").append(result.machineMismatchNote()).append("\n\n");
        }
        sb.append("REGRESSED: ").append(result.regressedCount()).append("   FIXED: ").append(result.fixedCount())
                .append("   VISUAL REGRESSIONS: ").append(result.visualRegressionCount()).append("\n\n");

        for (Classification c : new Classification[] {
                Classification.REGRESSED, Classification.FIXED, Classification.ONLY_IN_A, Classification.ONLY_IN_B, Classification.UNCHANGED}) {
            java.util.List<ShaderComparisonEntry> group = result.entries().stream().filter(e -> e.classification() == c).toList();
            if (group.isEmpty()) {
                continue;
            }
            sb.append("[ ").append(c).append(" ] (").append(group.size()).append(")\n");
            for (ShaderComparisonEntry entry : group) {
                sb.append(String.format("  %-28s %-9s [%s]  A=%s B=%s%n",
                        entry.shaderName(), entry.stage(), entry.defineVariant(), entry.successInA(), entry.successInB()));
            }
            sb.append('\n');
        }

        for (dev.velofine.diagnostics.model.VisualComparisonEntry.Classification c : new dev.velofine.diagnostics.model.VisualComparisonEntry.Classification[] {
                dev.velofine.diagnostics.model.VisualComparisonEntry.Classification.VISUAL_REGRESSION,
                dev.velofine.diagnostics.model.VisualComparisonEntry.Classification.NOTHING_RENDERED_IN_EITHER,
                dev.velofine.diagnostics.model.VisualComparisonEntry.Classification.ONLY_IN_A,
                dev.velofine.diagnostics.model.VisualComparisonEntry.Classification.ONLY_IN_B}) {
            java.util.List<dev.velofine.diagnostics.model.VisualComparisonEntry> group =
                    result.visualEntries().stream().filter(e -> e.classification() == c).toList();
            if (group.isEmpty()) {
                continue;
            }
            sb.append("[ DRAW TEST ").append(c).append(" ] (").append(group.size()).append(")\n");
            for (dev.velofine.diagnostics.model.VisualComparisonEntry entry : group) {
                sb.append(String.format("  %-28s [%s]  A=%s B=%s%n",
                        entry.shaderName(), entry.defineVariant(),
                        java.util.Arrays.toString(entry.rgbaInA()), java.util.Arrays.toString(entry.rgbaInB())));
            }
            sb.append('\n');
        }

        details.setText(sb.toString());
        details.setCaretPosition(0);
    }
}
