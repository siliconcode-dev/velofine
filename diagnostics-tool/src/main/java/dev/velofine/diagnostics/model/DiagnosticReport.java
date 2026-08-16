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

package dev.velofine.diagnostics.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Root of one diagnostic run's JSON report. BASELINE and CANDIDATE runs share this exact schema
 * ({@link #mode} is the only thing that differs in shape) so Phase 4's fix-iteration loop can diff
 * two report files directly.
 *
 * <p>Nullable fields ({@link #glContext}, {@link #glCapabilities}) are genuinely nullable: if GL
 * context creation fails outright (see {@link ContextCreationAttempt}), the report is still written
 * with whatever was captured pre-context (GPU probe, every failed creation attempt) rather than
 * aborting with nothing - a total failure on reference hardware is itself the finding.
 *
 * <p>Stays a flat record (not grouped into nested sub-records) so the written JSON reads as one
 * flat, diffable document rather than nested wrapper objects - {@link Builder} exists purely for
 * construction-site readability as the field count has grown across the 24-item extension, without
 * changing the JSON shape at all.
 */
public record DiagnosticReport(
        String toolVersion,
        String generatedAtIso,
        Mode mode,
        String mcVersionId,
        String mcClientJarPath,
        String candidateShaderDir,
        List<GpuInfo> gpuAdapters,
        CpuInfo cpuInfo,
        OsInfo osInfo,
        EnvironmentInfo environmentInfo,
        Integer boundAdapterIndex,
        List<ContextCreationAttempt> contextCreationAttempts,
        String contextRungLabel,
        GlContextInfo glContext,
        GlCapabilities glCapabilities,
        List<ShaderInventoryEntry> shaderInventory,
        InventoryBaselineCheck inventoryBaseline,
        List<ShaderCompileEntry> shaders,
        List<ProgramLinkEntry> programLinks,
        List<LintFinding> lintFindings,
        List<ShaderPrecisionEntry> shaderPrecision,
        List<DebugMessage> debugMessages,
        List<String> toolWarnings,
        List<String> knownQuirkNotes,
        List<FlakinessFinding> flakinessFindings,
        RunOutcome runOutcome,
        List<PendingShaderKey> incompleteShaders) {

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns a copy with {@link #flakinessFindings} replaced - used by {@code pipeline.RepeatRunner}
     * to attach cross-run flakiness results to the last run's otherwise-complete report, rather than
     * threading an extra builder field through every other call site that never needs it.
     */
    public DiagnosticReport withFlakinessFindings(List<FlakinessFinding> findings) {
        return new DiagnosticReport(
                toolVersion, generatedAtIso, mode, mcVersionId, mcClientJarPath, candidateShaderDir,
                gpuAdapters, cpuInfo, osInfo, environmentInfo, boundAdapterIndex, contextCreationAttempts,
                contextRungLabel, glContext, glCapabilities, shaderInventory, inventoryBaseline, shaders,
                programLinks, lintFindings, shaderPrecision, debugMessages, toolWarnings, knownQuirkNotes, findings,
                runOutcome, incompleteShaders);
    }

    /** Mutable, named-setter construction helper - see the class javadoc for why this exists. */
    public static final class Builder {
        private String toolVersion;
        private String generatedAtIso;
        private Mode mode;
        private String mcVersionId;
        private String mcClientJarPath;
        private String candidateShaderDir;
        private List<GpuInfo> gpuAdapters = new ArrayList<>();
        private CpuInfo cpuInfo = CpuInfo.unknown();
        private OsInfo osInfo = OsInfo.unknown();
        private EnvironmentInfo environmentInfo;
        private Integer boundAdapterIndex;
        private List<ContextCreationAttempt> contextCreationAttempts = new ArrayList<>();
        private String contextRungLabel;
        private GlContextInfo glContext;
        private GlCapabilities glCapabilities;
        private List<ShaderInventoryEntry> shaderInventory = new ArrayList<>();
        private InventoryBaselineCheck inventoryBaseline = InventoryBaselineCheck.unavailable();
        private List<ShaderCompileEntry> shaders = new ArrayList<>();
        private List<ProgramLinkEntry> programLinks = new ArrayList<>();
        private List<LintFinding> lintFindings = new ArrayList<>();
        private List<ShaderPrecisionEntry> shaderPrecision = new ArrayList<>();
        private List<DebugMessage> debugMessages = new ArrayList<>();
        private List<String> toolWarnings = new ArrayList<>();
        private List<String> knownQuirkNotes = new ArrayList<>();
        private RunOutcome runOutcome = RunOutcome.COMPLETED;
        private List<PendingShaderKey> incompleteShaders = new ArrayList<>();

        private Builder() {
        }

        public Builder toolVersion(String v) {
            this.toolVersion = v;
            return this;
        }

        public Builder generatedAtIso(String v) {
            this.generatedAtIso = v;
            return this;
        }

        public Builder mode(Mode v) {
            this.mode = v;
            return this;
        }

        public Builder mcVersionId(String v) {
            this.mcVersionId = v;
            return this;
        }

        public Builder mcClientJarPath(String v) {
            this.mcClientJarPath = v;
            return this;
        }

        public Builder candidateShaderDir(String v) {
            this.candidateShaderDir = v;
            return this;
        }

        public Builder gpuAdapters(List<GpuInfo> v) {
            this.gpuAdapters = v;
            return this;
        }

        public Builder cpuInfo(CpuInfo v) {
            this.cpuInfo = v;
            return this;
        }

        public Builder osInfo(OsInfo v) {
            this.osInfo = v;
            return this;
        }

        public Builder environmentInfo(EnvironmentInfo v) {
            this.environmentInfo = v;
            return this;
        }

        public Builder boundAdapterIndex(Integer v) {
            this.boundAdapterIndex = v;
            return this;
        }

        public Builder contextCreationAttempts(List<ContextCreationAttempt> v) {
            this.contextCreationAttempts = v;
            return this;
        }

        public Builder contextRungLabel(String v) {
            this.contextRungLabel = v;
            return this;
        }

        public Builder glContext(GlContextInfo v) {
            this.glContext = v;
            return this;
        }

        public Builder glCapabilities(GlCapabilities v) {
            this.glCapabilities = v;
            return this;
        }

        public Builder shaderInventory(List<ShaderInventoryEntry> v) {
            this.shaderInventory = v;
            return this;
        }

        public Builder inventoryBaseline(InventoryBaselineCheck v) {
            this.inventoryBaseline = v;
            return this;
        }

        public Builder shaders(List<ShaderCompileEntry> v) {
            this.shaders = v;
            return this;
        }

        public Builder programLinks(List<ProgramLinkEntry> v) {
            this.programLinks = v;
            return this;
        }

        public Builder lintFindings(List<LintFinding> v) {
            this.lintFindings = v;
            return this;
        }

        public Builder shaderPrecision(List<ShaderPrecisionEntry> v) {
            this.shaderPrecision = v;
            return this;
        }

        public Builder debugMessages(List<DebugMessage> v) {
            this.debugMessages = v;
            return this;
        }

        public Builder toolWarnings(List<String> v) {
            this.toolWarnings = v;
            return this;
        }

        public Builder knownQuirkNotes(List<String> v) {
            this.knownQuirkNotes = v;
            return this;
        }

        public Builder runOutcome(RunOutcome v) {
            this.runOutcome = v;
            return this;
        }

        public Builder incompleteShaders(List<PendingShaderKey> v) {
            this.incompleteShaders = v;
            return this;
        }

        public DiagnosticReport build() {
            return new DiagnosticReport(
                    toolVersion, generatedAtIso, mode, mcVersionId, mcClientJarPath, candidateShaderDir,
                    gpuAdapters, cpuInfo, osInfo, environmentInfo, boundAdapterIndex, contextCreationAttempts,
                    contextRungLabel, glContext, glCapabilities, shaderInventory, inventoryBaseline, shaders,
                    programLinks, lintFindings, shaderPrecision, debugMessages, toolWarnings, knownQuirkNotes,
                    new ArrayList<>(), runOutcome, incompleteShaders);
        }
    }
}
