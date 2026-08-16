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

package dev.velofine.diagnostics.ui;

import dev.velofine.diagnostics.ToolVersion;
import dev.velofine.diagnostics.mc.McVersionEntry;
import dev.velofine.diagnostics.model.DiagnosticReport;
import dev.velofine.diagnostics.model.Mode;
import dev.velofine.diagnostics.ui.screens.CompareReportsScreen;
import dev.velofine.diagnostics.ui.screens.DirectoryPickerScreen;
import dev.velofine.diagnostics.ui.screens.ModeSelectionScreen;
import dev.velofine.diagnostics.ui.screens.ResultsScreen;
import dev.velofine.diagnostics.ui.screens.RunProgressScreen;
import dev.velofine.diagnostics.ui.screens.VersionPickerScreen;
import java.awt.CardLayout;
import java.nio.file.Path;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Owns the wizard shell (a {@link CardLayout} swapping between five screens in sequence) and the
 * session's mutable state (Minecraft dir, chosen version, mode, candidate shader dir), passed
 * screen-to-screen via plain getters/setters - a lightweight linear wizard doesn't need a heavier
 * state-management pattern.
 *
 * <p>Flow: directory picker -&gt; version picker -&gt; mode selection -&gt; run (background thread,
 * see {@link RunProgressScreen}) -&gt; results, with "run again" looping back to mode selection.
 */
public final class DiagnosticApp {

    private static final String CARD_DIRECTORY = "directory";
    private static final String CARD_VERSION = "version";
    private static final String CARD_MODE = "mode";
    private static final String CARD_RUN = "run";
    private static final String CARD_RESULTS = "results";
    private static final String CARD_COMPARE = "compare";

    private final JFrame frame = new JFrame("Velofine Diagnostic " + ToolVersion.version());
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);

    private final DirectoryPickerScreen directoryPickerScreen;
    private final VersionPickerScreen versionPickerScreen;
    private final ModeSelectionScreen modeSelectionScreen;
    private final RunProgressScreen runProgressScreen;
    private final ResultsScreen resultsScreen;
    private final CompareReportsScreen compareReportsScreen;

    private Path mcDir;
    private McVersionEntry selectedVersion;
    private Mode mode = Mode.BASELINE;
    private Path candidateShaderDir;
    private int repeatCount = 1;
    private boolean contextRungSweep = false;

    public DiagnosticApp() {
        directoryPickerScreen = new DirectoryPickerScreen(this);
        versionPickerScreen = new VersionPickerScreen(this);
        modeSelectionScreen = new ModeSelectionScreen(this);
        runProgressScreen = new RunProgressScreen(this);
        resultsScreen = new ResultsScreen(this);
        compareReportsScreen = new CompareReportsScreen(this);

        cards.add(directoryPickerScreen, CARD_DIRECTORY);
        cards.add(versionPickerScreen, CARD_VERSION);
        cards.add(modeSelectionScreen, CARD_MODE);
        cards.add(runProgressScreen, CARD_RUN);
        cards.add(resultsScreen, CARD_RESULTS);
        cards.add(compareReportsScreen, CARD_COMPARE);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(cards);
        frame.setSize(880, 640);
        frame.setMinimumSize(new java.awt.Dimension(700, 500));
        frame.setLocationRelativeTo(null);
        VelofineSwingTheme.styleFrame(frame);
    }

    public void start() {
        SwingUtilities.invokeLater(() -> {
            frame.setVisible(true);
            showDirectoryPicker();
        });
    }

    public void showDirectoryPicker() {
        cardLayout.show(cards, CARD_DIRECTORY);
    }

    public void showVersionPicker() {
        versionPickerScreen.refresh();
        cardLayout.show(cards, CARD_VERSION);
    }

    public void showModeSelection() {
        modeSelectionScreen.onShown();
        cardLayout.show(cards, CARD_MODE);
    }

    public void startRun() {
        cardLayout.show(cards, CARD_RUN);
        runProgressScreen.startRun();
    }

    public void showResults(DiagnosticReport report, Path savedReportPath, String failureMessage) {
        resultsScreen.display(report, savedReportPath, failureMessage);
        cardLayout.show(cards, CARD_RESULTS);
    }

    public void showCompareReports() {
        cardLayout.show(cards, CARD_COMPARE);
    }

    // --- session state ---

    public JFrame getFrame() {
        return frame;
    }

    public void setMcDir(Path mcDir) {
        this.mcDir = mcDir;
    }

    public Path getMcDir() {
        return mcDir;
    }

    public void setSelectedVersion(McVersionEntry selectedVersion) {
        this.selectedVersion = selectedVersion;
    }

    public McVersionEntry getSelectedVersion() {
        return selectedVersion;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public Mode getMode() {
        return mode;
    }

    public void setCandidateShaderDir(Path candidateShaderDir) {
        this.candidateShaderDir = candidateShaderDir;
    }

    public Path getCandidateShaderDir() {
        return candidateShaderDir;
    }

    public void setRepeatCount(int repeatCount) {
        this.repeatCount = repeatCount;
    }

    public int getRepeatCount() {
        return repeatCount;
    }

    public void setContextRungSweep(boolean contextRungSweep) {
        this.contextRungSweep = contextRungSweep;
    }

    public boolean isContextRungSweep() {
        return contextRungSweep;
    }
}
