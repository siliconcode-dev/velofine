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

package dev.velofine.diagnostics.report;

import dev.velofine.diagnostics.model.ComparisonResult;
import dev.velofine.diagnostics.model.DiagnosticReport;
import dev.velofine.diagnostics.model.DrawTestResult;
import dev.velofine.diagnostics.model.ProgramLinkEntry;
import dev.velofine.diagnostics.model.ShaderCompileEntry;
import dev.velofine.diagnostics.model.ShaderComparisonEntry;
import dev.velofine.diagnostics.model.ShaderComparisonEntry.Classification;
import dev.velofine.diagnostics.model.VisualComparisonEntry;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Diffs two {@link DiagnosticReport}s' shader compile outcomes and draw-test pixel results -
 * GUI-independent, used by both {@code ui.screens.CompareReportsScreen} and, eventually, Phase 4's
 * own before/after fix-iteration workflow. Fully unit-testable: pure data in, pure data out, no
 * file/GL/GUI dependency.
 */
public final class ReportComparator {

    /**
     * Per-channel (0-255) tolerance for the visual-regression pixel diff, averaged across R/G/B/A -
     * a plain numeric-delta threshold, not a perceptual (SSIM-style) metric, matching how real GPU
     * conformance suites (Khronos VK-GL-CTS/dEQP) compare against a reference image for this kind of
     * data. Loose enough to absorb driver-level rounding/dithering noise between two different real
     * GPUs, tight enough that "visible vs. fully transparent/black" (the actual bug this tool exists
     * to catch) always exceeds it.
     */
    private static final double VISUAL_TOLERANCE = 24.0;

    private ReportComparator() {
    }

    public static ComparisonResult compare(DiagnosticReport a, DiagnosticReport b) {
        String mismatchNote = machineMismatchNote(a, b);
        boolean sameMachine = mismatchNote == null;

        Map<String, ShaderCompileEntry> byKeyA = index(a);
        Map<String, ShaderCompileEntry> byKeyB = index(b);

        List<String> allKeys = new ArrayList<>(byKeyA.keySet());
        for (String key : byKeyB.keySet()) {
            if (!allKeys.contains(key)) {
                allKeys.add(key);
            }
        }

        List<ShaderComparisonEntry> entries = new ArrayList<>();
        for (String key : allKeys) {
            String[] parts = key.split("\\|", 3);
            ShaderCompileEntry entryA = byKeyA.get(key);
            ShaderCompileEntry entryB = byKeyB.get(key);

            Boolean successA = successOf(entryA);
            Boolean successB = successOf(entryB);
            Classification classification = classify(entryA, entryB, successA, successB);

            entries.add(new ShaderComparisonEntry(parts[0], parts[1], parts[2], classification, successA, successB));
        }

        List<VisualComparisonEntry> visualEntries = compareVisuals(a, b);

        return new ComparisonResult(sameMachine, mismatchNote, entries, visualEntries);
    }

    private static List<VisualComparisonEntry> compareVisuals(DiagnosticReport a, DiagnosticReport b) {
        Map<String, ProgramLinkEntry> byKeyA = indexPrograms(a);
        Map<String, ProgramLinkEntry> byKeyB = indexPrograms(b);

        List<String> allKeys = new ArrayList<>(byKeyA.keySet());
        for (String key : byKeyB.keySet()) {
            if (!allKeys.contains(key)) {
                allKeys.add(key);
            }
        }

        List<VisualComparisonEntry> visualEntries = new ArrayList<>();
        for (String key : allKeys) {
            String[] parts = key.split("\\|", 2);
            DrawTestResult drawA = drawTestOf(byKeyA.get(key));
            DrawTestResult drawB = drawTestOf(byKeyB.get(key));

            if (drawA == null && drawB == null) {
                continue; // neither side linked successfully - nothing to say about visual output
            }
            if (drawA == null) {
                visualEntries.add(new VisualComparisonEntry(parts[0], parts[1],
                        VisualComparisonEntry.Classification.ONLY_IN_B, null, drawB.sampledRgba(), false, drawB.anyPixelRendered()));
                continue;
            }
            if (drawB == null) {
                visualEntries.add(new VisualComparisonEntry(parts[0], parts[1],
                        VisualComparisonEntry.Classification.ONLY_IN_A, drawA.sampledRgba(), null, drawA.anyPixelRendered(), false));
                continue;
            }

            VisualComparisonEntry.Classification classification;
            if (!drawA.anyPixelRendered() && !drawB.anyPixelRendered()) {
                classification = VisualComparisonEntry.Classification.NOTHING_RENDERED_IN_EITHER;
            } else if (visuallyDiffers(drawA.sampledRgba(), drawB.sampledRgba())) {
                classification = VisualComparisonEntry.Classification.VISUAL_REGRESSION;
            } else {
                classification = VisualComparisonEntry.Classification.UNCHANGED;
            }

            visualEntries.add(new VisualComparisonEntry(parts[0], parts[1], classification,
                    drawA.sampledRgba(), drawB.sampledRgba(), drawA.anyPixelRendered(), drawB.anyPixelRendered()));
        }
        return visualEntries;
    }

    private static boolean visuallyDiffers(int[] rgbaA, int[] rgbaB) {
        if (rgbaA == null || rgbaB == null || rgbaA.length != 4 || rgbaB.length != 4) {
            return false;
        }
        double sum = 0;
        for (int i = 0; i < 4; i++) {
            sum += Math.abs(rgbaA[i] - rgbaB[i]);
        }
        return (sum / 4.0) > VISUAL_TOLERANCE;
    }

    private static DrawTestResult drawTestOf(ProgramLinkEntry entry) {
        return entry != null ? entry.drawTest() : null;
    }

    private static Map<String, ProgramLinkEntry> indexPrograms(DiagnosticReport report) {
        Map<String, ProgramLinkEntry> byKey = new LinkedHashMap<>();
        for (ProgramLinkEntry entry : report.programLinks()) {
            byKey.put(entry.shaderName() + "|" + entry.defineVariant(), entry);
        }
        return byKey;
    }

    private static Classification classify(ShaderCompileEntry entryA, ShaderCompileEntry entryB, Boolean successA, Boolean successB) {
        if (entryA == null) {
            return Classification.ONLY_IN_B;
        }
        if (entryB == null) {
            return Classification.ONLY_IN_A;
        }
        if (Objects.equals(successA, successB)) {
            return Classification.UNCHANGED;
        }
        return Boolean.TRUE.equals(successB) ? Classification.FIXED : Classification.REGRESSED;
    }

    private static Boolean successOf(ShaderCompileEntry entry) {
        if (entry == null || entry.compile() == null) {
            return null;
        }
        return entry.compile().success();
    }

    private static Map<String, ShaderCompileEntry> index(DiagnosticReport report) {
        Map<String, ShaderCompileEntry> byKey = new LinkedHashMap<>();
        for (ShaderCompileEntry entry : report.shaders()) {
            byKey.put(entry.shaderName() + "|" + entry.stage() + "|" + entry.defineVariant(), entry);
        }
        return byKey;
    }

    /** Returns a human-readable mismatch note, or {@code null} if the two reports look like the same machine. */
    private static String machineMismatchNote(DiagnosticReport a, DiagnosticReport b) {
        String rendererA = a.glContext() != null ? a.glContext().renderer() : null;
        String rendererB = b.glContext() != null ? b.glContext().renderer() : null;
        if (!Objects.equals(rendererA, rendererB)) {
            return "GL_RENDERER differs (\"" + rendererA + "\" vs \"" + rendererB + "\") - comparing shader "
                    + "results across two different machines is not a meaningful diff.";
        }
        String cpuA = a.cpuInfo() != null ? a.cpuInfo().name() : null;
        String cpuB = b.cpuInfo() != null ? b.cpuInfo().name() : null;
        if (!Objects.equals(cpuA, cpuB)) {
            return "CPU differs (\"" + cpuA + "\" vs \"" + cpuB + "\") - likely two different machines.";
        }
        return null;
    }
}
