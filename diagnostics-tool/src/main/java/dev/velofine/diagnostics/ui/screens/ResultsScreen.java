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

import dev.velofine.diagnostics.model.ComparisonResult;
import dev.velofine.diagnostics.model.DebugMessage;
import dev.velofine.diagnostics.model.DiagnosticReport;
import dev.velofine.diagnostics.model.ProgramLinkEntry;
import dev.velofine.diagnostics.model.ShaderCompileEntry;
import dev.velofine.diagnostics.model.VisualComparisonEntry;
import dev.velofine.diagnostics.report.ReferenceBaseline;
import dev.velofine.diagnostics.report.ReportComparator;
import dev.velofine.diagnostics.ui.DiagnosticApp;
import dev.velofine.diagnostics.ui.VelofineSwingTheme;
import dev.velofine.diagnostics.ui.VelofineSwingTheme.ButtonKind;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.GridLayout;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/** Fifth screen: pass/fail summary, saved report path, and a "run again" loop back to mode selection. */
public final class ResultsScreen extends JPanel {

    private final DiagnosticApp app;
    private final JPanel statsRow = new JPanel(new GridLayout(1, 6, 1, 0));
    private final JTextArea details = new JTextArea();
    private Path reportsDir;

    public ResultsScreen(DiagnosticApp app) {
        super(new BorderLayout());
        this.app = app;
        VelofineSwingTheme.stylePanel(this);

        statsRow.setBackground(VelofineSwingTheme.LINE);

        details.setEditable(false);
        details.setBackground(VelofineSwingTheme.BLACK);
        details.setForeground(VelofineSwingTheme.WHITE);
        details.setFont(VelofineSwingTheme.BODY);
        details.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(details);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(VelofineSwingTheme.BLACK);

        JPanel body = new JPanel(new BorderLayout(0, 16));
        VelofineSwingTheme.stylePanel(body);
        body.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        body.add(statsRow, BorderLayout.NORTH);
        body.add(VelofineSwingTheme.compartment("REPORT DETAIL", scrollPane), BorderLayout.CENTER);

        JButton openFolderButton = VelofineSwingTheme.button("Open reports folder", ButtonKind.SECONDARY);
        openFolderButton.addActionListener(e -> openReportsFolder());

        JButton compareButton = VelofineSwingTheme.button("Compare reports", ButtonKind.SECONDARY);
        compareButton.addActionListener(e -> app.showCompareReports());

        JPanel leftButtons = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 12, 0));
        leftButtons.setBackground(VelofineSwingTheme.BLACK);
        leftButtons.add(openFolderButton);
        leftButtons.add(compareButton);

        JButton runAgainButton = VelofineSwingTheme.button("Run again", ButtonKind.PRIMARY);
        runAgainButton.addActionListener(e -> app.showModeSelection());

        add(VelofineSwingTheme.topBar("STEP 5 / 5 - RESULTS"), BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);
        add(VelofineSwingTheme.footerBar(leftButtons, runAgainButton), BorderLayout.SOUTH);
    }

    public void display(DiagnosticReport report, Path savedReportPath, String failureMessage) {
        if (report == null) {
            statsRow.removeAll();
            statsRow.add(stat("RUN", "FAILED", VelofineSwingTheme.RED));
            details.setText(failureMessage != null ? failureMessage : "Unknown failure.");
            reportsDir = null;
            statsRow.revalidate();
            statsRow.repaint();
            return;
        }

        reportsDir = savedReportPath != null ? savedReportPath.getParent() : null;

        long compiledOk = report.shaders().stream().filter(s -> s.compile() != null && s.compile().success()).count();
        long compiledFail = report.shaders().stream().filter(s -> s.compile() != null && !s.compile().success()).count();
        long missing = report.shaders().stream().filter(ShaderCompileEntry::missingFromJar).count();
        long linkedOk = report.programLinks().stream().filter(ProgramLinkEntry::success).count();
        long linkedFail = report.programLinks().stream().filter(l -> !l.success()).count();
        long highSeverityErrors = report.debugMessages().stream().filter(ResultsScreen::isHighSeverity).count();

        Optional<DiagnosticReport> reference = ReferenceBaseline.load();
        ComparisonResult referenceDiff = reference.map(ref -> ReportComparator.compare(ref, report)).orElse(null);
        long visualRegressions = referenceDiff != null ? referenceDiff.visualRegressionCount() : 0;

        statsRow.removeAll();
        statsRow.add(stat("COMPILE OK", String.valueOf(compiledOk), VelofineSwingTheme.TERMINAL_GREEN));
        statsRow.add(stat("COMPILE FAIL", String.valueOf(compiledFail), compiledFail > 0 ? VelofineSwingTheme.RED : VelofineSwingTheme.DIM_WHITE));
        statsRow.add(stat("LINK FAIL", String.valueOf(linkedFail), linkedFail > 0 ? VelofineSwingTheme.RED : VelofineSwingTheme.DIM_WHITE));
        statsRow.add(stat("MISSING", String.valueOf(missing), missing > 0 ? VelofineSwingTheme.RED : VelofineSwingTheme.DIM_WHITE));
        // A shader can compile, link, and draw without a new GL error and still be a real problem -
        // this tile is what used to be silently buried inside a raw "DEBUG MESSAGES: N" count with
        // no severity distinction from benign performance notices sitting right next to it.
        statsRow.add(stat("GL ERRORS", String.valueOf(highSeverityErrors), highSeverityErrors > 0 ? VelofineSwingTheme.RED : VelofineSwingTheme.DIM_WHITE));
        // The tile that actually answers "does this look like the invisible-lava/black-block bug" -
        // "N/A" (not zero) when no bundled reference exists yet, so an empty reference directory
        // doesn't silently read as "confirmed fine."
        statsRow.add(stat("VS. REFERENCE", referenceDiff != null ? String.valueOf(visualRegressions) : "N/A",
                visualRegressions > 0 ? VelofineSwingTheme.RED : VelofineSwingTheme.DIM_WHITE));
        statsRow.revalidate();
        statsRow.repaint();
        // linkedOk isn't surfaced as its own tile (compile/link-fail/missing/GL-errors/vs-reference
        // are the tiles that matter for spotting a problem at a glance) but stays folded into the
        // detail text below.

        StringBuilder sb = new StringBuilder();
        sb.append("REPORT SAVED: ").append(savedReportPath != null ? savedReportPath : "(not saved - see warnings)").append('\n');
        sb.append("RUN OUTCOME: ").append(report.runOutcome())
                .append("   DEBUG MESSAGES: ").append(report.debugMessages().size())
                .append(" (").append(highSeverityErrors).append(" GL error(s))")
                .append("   LINK OK: ").append(linkedOk).append('\n');
        if (report.contextRungLabel() != null) {
            sb.append("CONTEXT RUNG (forced): ").append(report.contextRungLabel()).append('\n');
        }

        List<DebugMessage> highSeverity = report.debugMessages().stream()
                .filter(ResultsScreen::isHighSeverity).toList();
        if (!highSeverity.isEmpty()) {
            sb.append("\n[ GL ERRORS (").append(highSeverity.size()).append(") ]\n");
            highSeverity.forEach(m -> sb.append(String.format("  [%s] %-24s %s%s%n",
                    m.severity(), m.type(),
                    m.activeShaderContext() != null ? "(" + m.activeShaderContext() + ") " : "",
                    m.message())));
        }

        if (referenceDiff == null) {
            sb.append("\n[ VS. REFERENCE: no bundled known-good reference report found - see "
                    + "diagnostics-tool/src/main/resources/reference/README.md ]\n");
        } else if (!referenceDiff.sameMachine()) {
            sb.append("\n[ VS. REFERENCE: comparison skipped - ").append(referenceDiff.machineMismatchNote()).append(" ]\n");
        } else {
            List<VisualComparisonEntry> regressions = referenceDiff.visualEntries().stream()
                    .filter(e -> e.classification() == VisualComparisonEntry.Classification.VISUAL_REGRESSION)
                    .toList();
            if (!regressions.isEmpty()) {
                sb.append("\n[ VISUAL REGRESSIONS vs. known-good reference (").append(regressions.size()).append(") ]\n");
                regressions.forEach(e -> sb.append(String.format("  %-28s [%s]  reference=rgba%s  this run=rgba%s%n",
                        e.shaderName(), e.defineVariant(), toRgbaString(e.rgbaInA()), toRgbaString(e.rgbaInB()))));
            } else {
                sb.append("\n[ VS. REFERENCE: no visual regressions detected against the known-good baseline ]\n");
            }
        }

        if (!report.incompleteShaders().isEmpty()) {
            sb.append("\n[ INCOMPLETE SHADERS - run ended before these were confirmed (")
                    .append(report.incompleteShaders().get(0).reason()).append(") ]\n");
            report.incompleteShaders().forEach(k -> sb.append(String.format("  %-28s %-9s [%s]%n", k.shaderName(), k.stage(), k.defineVariant())));
        }

        if (!report.toolWarnings().isEmpty()) {
            sb.append("\n[ WARNINGS ]\n");
            report.toolWarnings().forEach(w -> sb.append("  - ").append(w).append('\n'));
        }

        if (!report.knownQuirkNotes().isEmpty()) {
            sb.append("\n[ KNOWN HARDWARE NOTES - informational only, not pass/fail signal ]\n");
            report.knownQuirkNotes().forEach(n -> sb.append("  - ").append(n).append('\n'));
        } else if (report.forceFullSuite()) {
            // This hardware didn't match any known signature at all, yet the full suite still ran
            // in full because the tester explicitly forced it - flagged so this doesn't read as
            // "this looks like verified/matched hardware" when it's actually unmatched-but-forced.
            sb.append("\n[ FORCE FULL SUITE: this hardware matched no known signature - every test "
                    + "still ran in full because \"Force full test suite\" was checked ]\n");
        }

        if (!report.lintFindings().isEmpty()) {
            sb.append("\n[ LINT FINDINGS (").append(report.lintFindings().size()).append(") ]\n");
            report.lintFindings().forEach(f -> sb.append(String.format("  [%s] %s.%s line %d: %s%n",
                    f.severity(), f.shaderName(), f.stage(), f.line(), f.message())));
        }

        if (!report.flakinessFindings().isEmpty()) {
            sb.append("\n[ FLAKINESS ACROSS REPEAT RUNS ]\n");
            report.flakinessFindings().forEach(f -> sb.append(String.format("  %-28s %-9s [%s]  outcomes=%s%n",
                    f.shaderName(), f.stage(), f.defineVariant(), f.outcomesPerRun())));
        }

        sb.append("\n[ ENVIRONMENT ]\n");
        sb.append("  CPU: ").append(report.cpuInfo() != null ? report.cpuInfo().name() : "unknown").append('\n');
        sb.append("  OS: ").append(report.osInfo() != null ? report.osInfo().caption() + " (build "
                + report.osInfo().buildNumber() + ")" : "unknown").append('\n');
        if (report.environmentInfo() != null) {
            sb.append(String.format("  JVM: %s (%s)   LWJGL: %s   Heap: %dMB%n",
                    report.environmentInfo().jvmVersion(), report.environmentInfo().jvmVendor(),
                    report.environmentInfo().lwjglVersion(), report.environmentInfo().maxHeapBytes() / 1_000_000));
        }
        for (int i = 0; i < report.gpuAdapters().size(); i++) {
            var gpu = report.gpuAdapters().get(i);
            String marker = report.boundAdapterIndex() != null && report.boundAdapterIndex() == i ? " <- GL context bound to this one" : "";
            sb.append(String.format("  GPU[%d]: %s (driver %s)%s%n", i, gpu.adapterName(), gpu.driverVersion(), marker));
        }

        sb.append("\n[ SHADER COMPILE RESULTS ]\n");
        for (ShaderCompileEntry entry : report.shaders()) {
            if (entry.missingFromJar()) {
                sb.append(String.format("  [MISSING] %-28s %-9s [%s]%n", entry.shaderName(), entry.stage(), entry.defineVariant()));
                continue;
            }
            String status = entry.compile().success() ? "OK    " : "FAILED";
            sb.append(String.format("  [%s] %-28s %-9s [%s] (%s)%n",
                    status, entry.shaderName(), entry.stage(), entry.defineVariant(), entry.sourceOrigin()));
            if (!entry.compile().success() && !entry.compile().infoLog().isBlank()) {
                sb.append("           ").append(entry.compile().infoLog().strip().replace("\n", "\n           ")).append('\n');
            }
        }

        sb.append("\n[ PROGRAM LINK RESULTS ]\n");
        for (ProgramLinkEntry entry : report.programLinks()) {
            String status = entry.success() ? "OK    " : "FAILED";
            sb.append(String.format("  [%s] %-28s [%s]%n", status, entry.shaderName(), entry.defineVariant()));
            if (!entry.success() && !entry.infoLog().isBlank()) {
                sb.append("           ").append(entry.infoLog().strip().replace("\n", "\n           ")).append('\n');
            }
        }

        details.setText(sb.toString());
        details.setCaretPosition(0);
    }

    private static boolean isHighSeverity(DebugMessage message) {
        return "HIGH".equals(message.severity());
    }

    private static String toRgbaString(int[] rgba) {
        if (rgba == null || rgba.length != 4) {
            return "(none)";
        }
        return "(" + rgba[0] + "," + rgba[1] + "," + rgba[2] + "," + rgba[3] + ")";
    }

    private JPanel stat(String label, String value, Color valueColor) {
        JPanel tile = new JPanel(new BorderLayout());
        tile.setBackground(VelofineSwingTheme.BLACK);
        tile.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        javax.swing.JLabel labelComponent = VelofineSwingTheme.sectionLabel(label);
        labelComponent.setForeground(VelofineSwingTheme.DIM_WHITE);

        javax.swing.JLabel valueComponent = new javax.swing.JLabel(value);
        valueComponent.setFont(VelofineSwingTheme.MACRO);
        valueComponent.setForeground(valueColor);

        tile.add(labelComponent, BorderLayout.NORTH);
        tile.add(valueComponent, BorderLayout.CENTER);
        return tile;
    }

    private void openReportsFolder() {
        if (reportsDir == null || !Desktop.isDesktopSupported()) {
            return;
        }
        try {
            Desktop.getDesktop().open(reportsDir.toFile());
        } catch (Exception ignored) {
            // Best-effort convenience only.
        }
    }
}
