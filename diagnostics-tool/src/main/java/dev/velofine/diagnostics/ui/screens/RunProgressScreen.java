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

import dev.velofine.diagnostics.gl.DiagnosticGlContext;
import dev.velofine.diagnostics.mc.McVersionEntry;
import dev.velofine.diagnostics.mc.ShaderExtractor;
import dev.velofine.diagnostics.model.DiagnosticReport;
import dev.velofine.diagnostics.model.Mode;
import dev.velofine.diagnostics.pipeline.ChildProcessSupervisor;
import dev.velofine.diagnostics.pipeline.DiagnosticPipeline;
import dev.velofine.diagnostics.pipeline.PipelineRequest;
import dev.velofine.diagnostics.report.ReportPaths;
import dev.velofine.diagnostics.report.ReportWriter;
import dev.velofine.diagnostics.shader.DefineVariants;
import dev.velofine.diagnostics.ui.DiagnosticApp;
import dev.velofine.diagnostics.ui.VelofineSwingTheme;
import java.awt.BorderLayout;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;

/**
 * Fourth screen: runs the entire diagnostic pipeline on a dedicated background thread and shows a
 * scrolling progress log while it works.
 *
 * <p>The actual GL pipeline no longer runs in this process at all - {@link ChildProcessSupervisor}
 * spawns it as a supervised child process (see that class's javadoc for why: a native driver
 * crash or genuine hang can't be caught from inside the same JVM, so real crash/hang resilience
 * needs process isolation). This screen's job is building {@link PipelineRequest}(s) per
 * {@link DiagnosticApp}'s repeat-count/context-rung-sweep settings, handing each to the
 * supervisor, and adapting its log callback onto the Swing log - it has no direct knowledge of
 * {@link DiagnosticPipeline} or GL at all anymore. A context-rung sweep's non-final rungs are
 * written to disk immediately (nothing further to confirm about them); the run/rung actually shown
 * to the tester is left unsaved and handed to {@link HumanVisualCheckScreen} instead, which performs
 * the actual write once the tester has answered (or skipped) its prompt.
 *
 * <p>Iterates {@link ShaderExtractor#discoverCoreShaders()}'s <b>full</b> inventory - every real
 * core shader in the client jar, not a hardcoded subset - compiling and linking each one, so a
 * baseline run captures everything relevant to vanilla 26.2, not just the handful of shaders the
 * spec docs happened to name (several of which don't exist as separate files - see
 * {@code DefineVariants}'s javadoc).
 */
public final class RunProgressScreen extends JPanel {

    private final DiagnosticApp app;
    private final JTextArea log = new JTextArea();

    public RunProgressScreen(DiagnosticApp app) {
        super(new BorderLayout());
        this.app = app;
        VelofineSwingTheme.stylePanel(this);

        log.setEditable(false);
        log.setBackground(VelofineSwingTheme.BLACK);
        log.setForeground(VelofineSwingTheme.WHITE);
        log.setFont(VelofineSwingTheme.BODY);
        log.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(log);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(VelofineSwingTheme.BLACK);

        JPanel body = new JPanel(new BorderLayout());
        VelofineSwingTheme.stylePanel(body);
        body.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        body.add(VelofineSwingTheme.compartment("LIVE LOG", scrollPane), BorderLayout.CENTER);

        add(VelofineSwingTheme.topBar("STEP 4 / 5 - RUNNING"), BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);
    }

    private volatile boolean runInProgress = false;

    /**
     * Starts a run - but only if one isn't already active. Real, confirmed bug this guards
     * against: with no such guard, a rapid double-click on "Run diagnostic" (or any other
     * accidental double-invocation) before the wizard visually transitions away from
     * {@code ModeSelectionScreen} spawns a <i>second</i>, fully independent
     * {@link PipelineWorker} concurrently with the first - both write to this same shared
     * {@code log} component, and the second worker's output (its own "Probing GPU adapter(s)..."
     * opening line) appears to interleave in right as the first one's results screen shows,
     * looking exactly like the whole run restarted from zero after "finishing." See
     * {@link ModeSelectionScreen}'s run button, which also disables itself on click as the other
     * half of this fix.
     */
    public void startRun() {
        if (runInProgress) {
            return;
        }
        runInProgress = true;
        log.setText("");
        new PipelineWorker().execute();
    }

    private final class PipelineWorker extends SwingWorker<PendingReport, String> {

        @Override
        protected PendingReport doInBackground() {
            McVersionEntry version = app.getSelectedVersion();
            Mode mode = app.getMode();
            Path candidateDir = app.getCandidateShaderDir();
            int repeatCount = app.getRepeatCount();
            boolean rungSweep = app.isContextRungSweep();
            boolean forceFullSuite = app.isForceFullSuite();

            if (!rungSweep) {
                PipelineRequest request = new PipelineRequest(
                        version, mode, candidateDir, repeatCount, null, ReportWriter.newTimestamp(), forceFullSuite);
                return runWithoutSaving(request);
            }

            // Context-rung sweep: one supervised run (or repeat-set) PER fallback-ladder rung. Every
            // rung except the last is written immediately (its own independent, already-final
            // report - nothing further to confirm about it); only the LAST rung's report is
            // deferred to the human visual-check step before saving, matching how only the last
            // rung's outcome is shown in ResultsScreen too.
            List<String> savedPaths = new ArrayList<>();
            List<String> rungLabels = DiagnosticGlContext.hintLabels();
            for (int rungIndex = 0; rungIndex < rungLabels.size() - 1; rungIndex++) {
                publish("##### Context rung " + (rungIndex + 1) + "/" + rungLabels.size()
                        + ": " + rungLabels.get(rungIndex) + " #####");
                PipelineRequest request = new PipelineRequest(
                        version, mode, candidateDir, repeatCount, rungIndex, ReportWriter.newTimestamp(), forceFullSuite);
                Path saved = runAndSave(request);
                if (saved != null) {
                    savedPaths.add(saved.toString());
                }
            }

            int lastRungIndex = rungLabels.size() - 1;
            publish("##### Context rung " + rungLabels.size() + "/" + rungLabels.size()
                    + ": " + rungLabels.get(lastRungIndex) + " #####");
            PipelineRequest lastRequest = new PipelineRequest(
                    version, mode, candidateDir, repeatCount, lastRungIndex, ReportWriter.newTimestamp(), forceFullSuite);
            PendingReport pending = runWithoutSaving(lastRequest);
            publish("Context rung sweep complete. Earlier-rung reports already saved: " + savedPaths);
            return pending;
        }

        /** Used for every sweep rung except the last - no further confirmation needed, write immediately. */
        private Path runAndSave(PipelineRequest request) {
            DiagnosticReport report = ChildProcessSupervisor.runAndAwait(request, this::publish);

            publish("Writing report...");
            try {
                Path saved = ReportWriter.write(report, ReportPaths.reportsDirectory(), request.runTimestamp());
                publish("Saved: " + saved);
                return saved;
            } catch (Exception e) {
                publish("Failed to write report: " + e);
                return null;
            }
        }

        /** Used for the run/rung actually shown to the tester - saving is deferred to {@link HumanVisualCheckScreen}. */
        private PendingReport runWithoutSaving(PipelineRequest request) {
            DiagnosticReport report = ChildProcessSupervisor.runAndAwait(request, this::publish);
            return new PendingReport(report, request.runTimestamp());
        }

        private void publish(String line) {
            super.publish(line);
        }

        @Override
        protected void process(List<String> chunks) {
            for (String line : chunks) {
                log.append(line);
                log.append("\n");
            }
            log.setCaretPosition(log.getDocument().getLength());
        }

        @Override
        protected void done() {
            runInProgress = false;
            try {
                PendingReport pending = get();
                app.showHumanVisualCheck(pending.report(), pending.runTimestamp());
            } catch (Exception e) {
                app.showResults(null, null, "Diagnostic run failed: " + e);
            }
        }
    }

    private record PendingReport(DiagnosticReport report, String runTimestamp) {
    }
}
