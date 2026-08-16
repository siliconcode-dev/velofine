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
import dev.velofine.diagnostics.model.ShaderCompileEntry;
import dev.velofine.diagnostics.model.ShaderComparisonEntry;
import dev.velofine.diagnostics.model.ShaderComparisonEntry.Classification;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Diffs two {@link DiagnosticReport}s' shader compile outcomes - GUI-independent, used by both
 * {@code ui.screens.CompareReportsScreen} and, eventually, Phase 4's own before/after fix-iteration
 * workflow. Fully unit-testable: pure data in, pure data out, no file/GL/GUI dependency.
 */
public final class ReportComparator {

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

        return new ComparisonResult(sameMachine, mismatchNote, entries);
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
