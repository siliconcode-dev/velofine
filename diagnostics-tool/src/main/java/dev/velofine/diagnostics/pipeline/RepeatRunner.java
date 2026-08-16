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

package dev.velofine.diagnostics.pipeline;

import dev.velofine.diagnostics.model.DiagnosticReport;
import dev.velofine.diagnostics.model.FlakinessFinding;
import dev.velofine.diagnostics.model.ShaderCompileEntry;
import dev.velofine.diagnostics.report.ReportWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs {@link DiagnosticPipeline#run} {@code times} times and diffs each shader's compile-success
 * outcome across the runs - flags flaky/non-deterministic driver behavior (a real possibility on
 * old, buggy drivers) that a single run would silently hide.
 *
 * <p>Returns the <b>last</b> run's report (the one that gets saved) with
 * {@link DiagnosticReport#flakinessFindings()} attached - earlier runs' full reports are discarded
 * after their compile outcomes are recorded, not persisted separately, since this feature is about
 * cross-run consistency, not keeping N redundant report files.
 */
public final class RepeatRunner {

    private RepeatRunner() {
    }

    public static DiagnosticReport runRepeated(PipelineRequest baseRequest, int times, DiagnosticPipeline.ProgressSink sink) {
        Map<String, List<Boolean>> outcomesByKey = new LinkedHashMap<>();
        DiagnosticReport lastReport = null;

        for (int run = 0; run < times; run++) {
            sink.onStep("=== Repeat run " + (run + 1) + " / " + times + " ===");
            // Each repeat gets its own timestamp so per-run shader-archive folders don't collide -
            // only the base request's (the one actually reused for the saved report/folder naming
            // by the caller) is passed through unchanged on the first iteration.
            PipelineRequest request = run == 0 ? baseRequest : new PipelineRequest(
                    baseRequest.mcVersion(), baseRequest.mode(), baseRequest.candidateShaderDir(),
                    baseRequest.repeatCount(), baseRequest.forcedContextRungIndex(), ReportWriter.newTimestamp());

            lastReport = DiagnosticPipeline.run(request, sink);
            for (ShaderCompileEntry entry : lastReport.shaders()) {
                if (entry.compile() == null) {
                    continue;
                }
                String key = entry.shaderName() + "|" + entry.stage() + "|" + entry.defineVariant();
                outcomesByKey.computeIfAbsent(key, k -> new ArrayList<>()).add(entry.compile().success());
            }
        }

        List<FlakinessFinding> findings = new ArrayList<>();
        for (Map.Entry<String, List<Boolean>> e : outcomesByKey.entrySet()) {
            String[] parts = e.getKey().split("\\|", 3);
            FlakinessFinding finding = new FlakinessFinding(parts[0], parts[1], parts[2], e.getValue());
            if (!finding.consistent()) {
                findings.add(finding);
            }
        }

        return lastReport.withFlakinessFindings(findings);
    }
}
