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

package dev.velofine.diagnostics;

import dev.velofine.diagnostics.pipeline.PipelineWorkerMode;
import dev.velofine.diagnostics.ui.DiagnosticApp;
import java.nio.file.Path;

/**
 * Entry point for the standalone {@code diagnostic.exe} tool (see Masterdoc_v1.5.md S5,
 * Build_plan_v1.5.md Phase 1). Fully decoupled from the rest of Velofine - this must run correctly
 * on a machine that has never installed Velofine at all.
 *
 * <p>Dispatches to the headless {@code --pipeline-worker <args-file>} mode (see
 * {@link PipelineWorkerMode}) before falling through to the GUI - this is the child process
 * {@code pipeline.ChildProcessSupervisor} spawns for crash/hang-resilient pipeline runs (items
 * 17-18 of the v1.5 extension). Real testers never pass this flag themselves.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        if (args.length >= 2 && "--pipeline-worker".equals(args[0])) {
            PipelineWorkerMode.run(Path.of(args[1]));
            return;
        }
        new DiagnosticApp().start();
    }
}
