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

import dev.velofine.diagnostics.mc.McVersionEntry;
import dev.velofine.diagnostics.model.Mode;
import dev.velofine.diagnostics.report.ReportWriter;
import java.nio.file.Path;

/**
 * Everything one {@link DiagnosticPipeline#run} call needs. {@code repeatCount} is metadata for
 * the caller (see {@code RepeatRunner} - {@link DiagnosticPipeline#run} itself always executes
 * exactly once; repetition is the caller's loop, not this class's concern).
 * {@code forcedContextRungIndex}, when non-null, makes the pipeline call
 * {@code DiagnosticGlContext#createSpecific} for exactly that fallback-ladder rung instead of the
 * normal try-until-one-succeeds walk - used by the opt-in context-rung sweep (item 24).
 *
 * <p>{@code runTimestamp} is computed by the caller (via {@link ReportWriter#newTimestamp()}), not
 * internally by the pipeline - the pipeline uses it to name its per-run shader-archive folder, and
 * the same value must also name the report JSON file the caller writes afterward, so the two stay
 * consistent. Keeping this a plain input rather than a pipeline-internal {@code now()} call also
 * keeps {@link DiagnosticPipeline#run} free of hidden time-dependent side effects.
 */
public record PipelineRequest(
        McVersionEntry mcVersion,
        Mode mode,
        Path candidateShaderDir,
        int repeatCount,
        Integer forcedContextRungIndex,
        String runTimestamp) {

    public static PipelineRequest singleRun(McVersionEntry mcVersion, Mode mode, Path candidateShaderDir) {
        return new PipelineRequest(mcVersion, mode, candidateShaderDir, 1, null, ReportWriter.newTimestamp());
    }
}
