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
import dev.velofine.diagnostics.model.CpuInfo;
import dev.velofine.diagnostics.model.DiagnosticReport;
import dev.velofine.diagnostics.model.GlContextInfo;
import dev.velofine.diagnostics.model.Mode;
import dev.velofine.diagnostics.model.ShaderCompileEntry;
import dev.velofine.diagnostics.model.ShaderCompileResult;
import dev.velofine.diagnostics.model.ShaderComparisonEntry.Classification;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ReportComparatorTest {

    private GlContextInfo sameRenderer() {
        return new GlContextInfo("Intel", "Intel(R) HD Graphics 4000", "3.3.0", "3.30", List.of(), true);
    }

    private ShaderCompileEntry entry(String name, String stage, boolean success) {
        return new ShaderCompileEntry(name, stage, "default", "baseline-jar", false, List.of(), false,
                new ShaderCompileResult(success, "", 0), null, null);
    }

    private DiagnosticReport reportWith(List<ShaderCompileEntry> shaders, GlContextInfo glContext, CpuInfo cpuInfo) {
        return DiagnosticReport.builder()
                .mode(Mode.BASELINE)
                .mcVersionId("26.2")
                .glContext(glContext)
                .cpuInfo(cpuInfo)
                .shaders(shaders)
                .build();
    }

    @Test
    void sameMachineWhenRendererAndCpuMatch() {
        CpuInfo cpu = new CpuInfo("Intel i3-3110M", 2, 4);
        DiagnosticReport a = reportWith(List.of(entry("terrain", "fragment", false)), sameRenderer(), cpu);
        DiagnosticReport b = reportWith(List.of(entry("terrain", "fragment", true)), sameRenderer(), cpu);

        ComparisonResult result = ReportComparator.compare(a, b);

        assertTrue(result.sameMachine());
    }

    @Test
    void flagsDifferentMachinesByRenderer() {
        GlContextInfo rendererA = sameRenderer();
        GlContextInfo rendererB = new GlContextInfo("NVIDIA", "GeForce RTX 4080", "4.6", "4.60", List.of(), true);

        DiagnosticReport a = reportWith(List.of(), rendererA, null);
        DiagnosticReport b = reportWith(List.of(), rendererB, null);

        ComparisonResult result = ReportComparator.compare(a, b);

        assertFalse(result.sameMachine());
        assertTrue(result.machineMismatchNote().contains("GL_RENDERER differs"));
    }

    @Test
    void classifiesFixedRegressedAndUnchanged() {
        List<ShaderCompileEntry> a = List.of(
                entry("terrain", "fragment", false),   // will be fixed in B
                entry("rendertype_end_portal", "fragment", true), // will regress in B
                entry("entity", "vertex", true));       // unchanged

        List<ShaderCompileEntry> b = List.of(
                entry("terrain", "fragment", true),
                entry("rendertype_end_portal", "fragment", false),
                entry("entity", "vertex", true));

        ComparisonResult result = ReportComparator.compare(reportWith(a, sameRenderer(), null), reportWith(b, sameRenderer(), null));

        assertEquals(1, result.fixedCount());
        assertEquals(1, result.regressedCount());
        assertTrue(result.entries().stream().anyMatch(e -> e.shaderName().equals("terrain") && e.classification() == Classification.FIXED));
        assertTrue(result.entries().stream()
                .anyMatch(e -> e.shaderName().equals("rendertype_end_portal") && e.classification() == Classification.REGRESSED));
        assertTrue(result.entries().stream().anyMatch(e -> e.shaderName().equals("entity") && e.classification() == Classification.UNCHANGED));
    }

    @Test
    void classifiesOnlyInAAndOnlyInB() {
        List<ShaderCompileEntry> a = List.of(entry("removed_shader", "fragment", true));
        List<ShaderCompileEntry> b = List.of(entry("new_shader", "fragment", true));

        ComparisonResult result = ReportComparator.compare(reportWith(a, sameRenderer(), null), reportWith(b, sameRenderer(), null));

        assertEquals(2, result.entries().size());
        assertTrue(result.entries().stream().anyMatch(e -> e.shaderName().equals("removed_shader") && e.classification() == Classification.ONLY_IN_A));
        assertTrue(result.entries().stream().anyMatch(e -> e.shaderName().equals("new_shader") && e.classification() == Classification.ONLY_IN_B));
    }
}
