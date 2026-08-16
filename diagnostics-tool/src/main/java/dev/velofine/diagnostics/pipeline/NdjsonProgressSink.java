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
import dev.velofine.diagnostics.model.ProgramLinkEntry;
import dev.velofine.diagnostics.model.ShaderCompileEntry;
import java.io.PrintStream;

/**
 * Serializes {@link DiagnosticPipeline.ProgressSink} callbacks as NDJSON (one JSON object per line)
 * on the given stream - the wire protocol {@code ChildProcessSupervisor} reads on the parent side.
 *
 * <p>Only the four {@link DiagnosticPipeline.ProgressSink} callbacks are streamed - {@code step},
 * {@code shaderResult}, {@code linkResult}, {@code warning} - plus a final {@code done} line
 * carrying the complete {@link DiagnosticReport}. GPU/CPU/OS/environment probing and the shader
 * inventory itself are <b>not</b> streamed incrementally (they happen once, in well under a
 * second, at the very start of a run) - what matters for crash/timeout resilience is the shader
 * compile/link/draw loop, which takes the overwhelming majority of a run's wall time and is where
 * a hang or crash is actually likely to happen. This is a deliberate scope choice, not an
 * oversight: streaming every sub-probe would need threading callbacks through several more classes
 * for marginal robustness benefit over what's already covered.
 */
public final class NdjsonProgressSink implements DiagnosticPipeline.ProgressSink {

    private static final Gson GSON = new Gson();

    private record StepMessage(String type, String message) {
    }

    private record ShaderResultMessage(String type, ShaderCompileEntry entry) {
    }

    private record LinkResultMessage(String type, ProgramLinkEntry entry) {
    }

    private record WarningMessage(String type, String message) {
    }

    private record DoneMessage(String type, DiagnosticReport report) {
    }

    private final PrintStream out;

    public NdjsonProgressSink(PrintStream out) {
        this.out = out;
    }

    @Override
    public void onStep(String message) {
        emit(new StepMessage("step", message));
    }

    @Override
    public void onShaderCompiled(ShaderCompileEntry entry) {
        emit(new ShaderResultMessage("shaderResult", entry));
    }

    @Override
    public void onProgramLinked(ProgramLinkEntry entry) {
        emit(new LinkResultMessage("linkResult", entry));
    }

    @Override
    public void onToolWarning(String message) {
        emit(new WarningMessage("warning", message));
    }

    public void emitDone(DiagnosticReport report) {
        emit(new DoneMessage("done", report));
    }

    private void emit(Object message) {
        out.println(GSON.toJson(message));
        out.flush();
    }
}
