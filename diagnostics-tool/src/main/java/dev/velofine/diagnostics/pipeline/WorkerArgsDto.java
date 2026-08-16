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
import java.nio.file.Path;

/**
 * A plain-strings-and-primitives mirror of {@link PipelineRequest}, used only for the
 * {@code --pipeline-worker} args-file. Gson has no built-in adapter for {@link Path} (or a
 * sensible one for {@link McVersionEntry}, which wraps one) - rather than registering a custom
 * {@code TypeAdapter}, this DTO sidesteps the problem entirely by only ever containing types Gson
 * handles natively.
 */
public record WorkerArgsDto(
        String mcVersionId,
        String mcJarPath,
        String mode,
        String candidateShaderDirPath,
        int repeatCount,
        Integer forcedContextRungIndex,
        String runTimestamp) {

    public static WorkerArgsDto from(PipelineRequest request) {
        return new WorkerArgsDto(
                request.mcVersion().versionId(),
                request.mcVersion().jarPath().toString(),
                request.mode().name(),
                request.candidateShaderDir() != null ? request.candidateShaderDir().toString() : null,
                request.repeatCount(),
                request.forcedContextRungIndex(),
                request.runTimestamp());
    }

    public PipelineRequest toRequest() {
        McVersionEntry version = new McVersionEntry(mcVersionId, Path.of(mcJarPath));
        Path candidateDir = candidateShaderDirPath != null ? Path.of(candidateShaderDirPath) : null;
        return new PipelineRequest(version, Mode.valueOf(mode), candidateDir, repeatCount, forcedContextRungIndex, runTimestamp);
    }
}
