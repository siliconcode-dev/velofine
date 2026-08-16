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

import com.google.gson.Gson;
import dev.velofine.diagnostics.model.DiagnosticReport;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Headless entry point for the child process spawned by {@code ChildProcessSupervisor}
 * ({@code Main} dispatches here when {@code args[0].equals("--pipeline-worker")}). Deserializes a
 * {@link WorkerArgsDto} from the given args-file, runs the real pipeline (via
 * {@link DiagnosticPipeline} directly, or {@link RepeatRunner} when the request's repeat count is
 * more than 1), and streams NDJSON progress/results to stdout via {@link NdjsonProgressSink}.
 *
 * <p>Any exception reaching this method is caught and reported as a best-effort {@code warning}
 * line before exiting non-zero - a genuine native crash (segfault) obviously bypasses this
 * entirely, which is exactly the case {@code ChildProcessSupervisor}'s exit-code/watchdog path on
 * the parent side exists to catch.
 */
public final class PipelineWorkerMode {

    private static final Gson GSON = new Gson();

    private PipelineWorkerMode() {
    }

    public static void run(Path argsFile) {
        NdjsonProgressSink sink = new NdjsonProgressSink(System.out);
        try {
            String json = Files.readString(argsFile, StandardCharsets.UTF_8);
            WorkerArgsDto dto = GSON.fromJson(json, WorkerArgsDto.class);
            PipelineRequest request = dto.toRequest();

            DiagnosticReport report = request.repeatCount() > 1
                    ? RepeatRunner.runRepeated(request, request.repeatCount(), sink)
                    : DiagnosticPipeline.run(request, sink);

            sink.emitDone(report);
        } catch (IOException | RuntimeException e) {
            sink.onToolWarning("Pipeline worker failed before completing: " + e);
            System.exit(1);
        }
    }
}
