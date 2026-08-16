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
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.velofine.diagnostics.mc.ShaderExtractor;
import dev.velofine.diagnostics.model.DiagnosticReport;
import dev.velofine.diagnostics.model.PendingShaderKey;
import dev.velofine.diagnostics.model.ProgramLinkEntry;
import dev.velofine.diagnostics.model.RunOutcome;
import dev.velofine.diagnostics.model.ShaderCompileEntry;
import dev.velofine.diagnostics.model.ShaderInventoryEntry;
import dev.velofine.diagnostics.shader.DefineVariants;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Spawns the actual GL pipeline as a child process (via {@link SelfLauncher}) and supervises it
 * with a watchdog timeout, so a genuinely hung or crashed native GPU driver can never take the
 * whole tool down with it - a native segfault or a truly blocked native GL call can't be caught
 * from inside the same JVM (a Java {@code try/catch} can't unblock an in-progress native call, and
 * a segfault kills the watchdog thread right along with everything else). Process isolation is the
 * only architecture that actually delivers this.
 *
 * <p><b>Watchdog timeout is deliberately set well past Windows' own GPU-hang recovery</b>: WDDM's
 * Timeout Detection and Recovery (TDR) resets a hung driver after a 2-second default timeout
 * (<a href="https://learn.microsoft.com/en-us/windows-hardware/drivers/display/timeout-detection-and-recovery">Microsoft Learn</a>).
 * {@link #WATCHDOG_TIMEOUT} gives TDR the first chance to recover the driver itself - this
 * supervisor's own timeout is the backstop for cases TDR doesn't cleanly resolve (some old/buggy
 * drivers don't recover from a TDR reset), not a substitute for it.
 *
 * <p><b>No in-process fallback path exists on purpose.</b> An in-process fallback would only ever
 * run if {@code ProcessBuilder.start()} itself fails (rare: missing exec permission, AV
 * interference, disk full) - the least-exercised path in the whole tool, which is backwards for a
 * tool whose entire value proposition is trustworthiness. A spawn failure is reported as
 * {@link RunOutcome#SPAWN_FAILED} instead.
 */
public final class ChildProcessSupervisor {

    private static final Duration WATCHDOG_TIMEOUT = Duration.ofSeconds(15);
    private static final int STDERR_TAIL_LINES = 200;
    private static final Gson GSON = new Gson();

    private ChildProcessSupervisor() {
    }

    public static DiagnosticReport runAndAwait(PipelineRequest request, Consumer<String> logSink) {
        List<String> baseCommand;
        try {
            baseCommand = SelfLauncher.buildRelaunchCommand();
        } catch (Exception e) {
            return spawnFailedReport(request, "Could not determine how to relaunch self: " + e);
        }

        Path argsFile;
        try {
            argsFile = Files.createTempFile("velofine-pipeline-args-", ".json");
            Files.writeString(argsFile, GSON.toJson(WorkerArgsDto.from(request)), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return spawnFailedReport(request, "Could not write pipeline worker args file: " + e);
        }

        List<String> fullCommand = new ArrayList<>(baseCommand);
        fullCommand.add("--pipeline-worker");
        fullCommand.add(argsFile.toString());

        Process process;
        try {
            process = new ProcessBuilder(fullCommand).start();
        } catch (IOException e) {
            deleteQuietly(argsFile);
            return spawnFailedReport(request, "Failed to spawn pipeline worker process: " + e);
        }

        List<ShaderCompileEntry> shaders = Collections.synchronizedList(new ArrayList<>());
        List<ProgramLinkEntry> links = Collections.synchronizedList(new ArrayList<>());
        List<String> warnings = Collections.synchronizedList(new ArrayList<>());
        AtomicReference<DiagnosticReport> doneReport = new AtomicReference<>();
        AtomicLong lastActivityAt = new AtomicLong(System.currentTimeMillis());
        Deque<String> stderrTail = new ArrayDeque<>();

        Thread stdoutThread = new Thread(() -> readStdout(process, shaders, links, warnings, doneReport, lastActivityAt, logSink),
                "velofine-child-stdout");
        stdoutThread.setDaemon(true);
        stdoutThread.start();

        Thread stderrThread = new Thread(() -> readStderr(process, stderrTail), "velofine-child-stderr");
        stderrThread.setDaemon(true);
        stderrThread.start();

        AtomicBoolean timedOut = new AtomicBoolean(false);
        ScheduledExecutorService watchdog = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "velofine-watchdog");
            t.setDaemon(true);
            return t;
        });
        watchdog.scheduleWithFixedDelay(() -> {
            if (System.currentTimeMillis() - lastActivityAt.get() > WATCHDOG_TIMEOUT.toMillis() && process.isAlive()) {
                timedOut.set(true);
                logSink.accept("WATCHDOG: no activity for " + WATCHDOG_TIMEOUT.getSeconds() + "s - killing pipeline worker.");
                process.destroyForcibly();
            }
        }, WATCHDOG_TIMEOUT.toMillis(), 1000, TimeUnit.MILLISECONDS);

        int exitCode;
        try {
            exitCode = process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            exitCode = -1;
        } finally {
            watchdog.shutdownNow();
        }

        joinQuietly(stdoutThread);
        joinQuietly(stderrThread);
        deleteQuietly(argsFile);

        DiagnosticReport completed = doneReport.get();
        if (completed != null && exitCode == 0 && !timedOut.get()) {
            return completed;
        }

        RunOutcome outcome = timedOut.get() ? RunOutcome.TIMED_OUT : RunOutcome.PROCESS_CRASHED;
        return buildPartialReport(request, outcome, shaders, links, warnings, new ArrayList<>(stderrTail));
    }

    private static void readStdout(
            Process process, List<ShaderCompileEntry> shaders, List<ProgramLinkEntry> links, List<String> warnings,
            AtomicReference<DiagnosticReport> doneReport, AtomicLong lastActivityAt, Consumer<String> logSink) {
        try (BufferedReader reader = process.inputReader(StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                lastActivityAt.set(System.currentTimeMillis());
                handleLine(line, shaders, links, warnings, doneReport, logSink);
            }
        } catch (IOException ignored) {
            // Stream closed - typically because the process died; the caller's exit-code/report-null
            // check after waitFor() is what actually distinguishes that from a clean finish.
        }
    }

    private static void readStderr(Process process, Deque<String> stderrTail) {
        try (BufferedReader reader = process.errorReader(StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                synchronized (stderrTail) {
                    stderrTail.addLast(line);
                    if (stderrTail.size() > STDERR_TAIL_LINES) {
                        stderrTail.removeFirst();
                    }
                }
            }
        } catch (IOException ignored) {
            // Same reasoning as readStdout.
        }
    }

    private static void handleLine(
            String line, List<ShaderCompileEntry> shaders, List<ProgramLinkEntry> links, List<String> warnings,
            AtomicReference<DiagnosticReport> doneReport, Consumer<String> logSink) {
        if (line.isBlank()) {
            return;
        }
        JsonObject obj;
        try {
            obj = JsonParser.parseString(line).getAsJsonObject();
        } catch (RuntimeException e) {
            logSink.accept(line);
            return;
        }
        String type = obj.has("type") ? obj.get("type").getAsString() : "";
        switch (type) {
            case "step" -> logSink.accept(GSON.fromJson(obj.get("message"), String.class));
            case "warning" -> {
                String message = GSON.fromJson(obj.get("message"), String.class);
                warnings.add(message);
                logSink.accept("WARNING: " + message);
            }
            case "shaderResult" -> shaders.add(GSON.fromJson(obj.get("entry"), ShaderCompileEntry.class));
            case "linkResult" -> links.add(GSON.fromJson(obj.get("entry"), ProgramLinkEntry.class));
            case "done" -> doneReport.set(GSON.fromJson(obj.get("report"), DiagnosticReport.class));
            default -> logSink.accept(line);
        }
    }

    /**
     * Builds a report from whatever was streamed before failure, plus the real expected
     * shader×stage×variant matrix (computed here, parent-side, via the same GL-free
     * {@code ShaderExtractor}/{@code DefineVariants} the pipeline itself uses) diffed against what
     * was actually confirmed, so a crash/timeout still produces a saved, actionable report instead
     * of nothing.
     */
    private static DiagnosticReport buildPartialReport(
            PipelineRequest request, RunOutcome outcome, List<ShaderCompileEntry> shaders, List<ProgramLinkEntry> links,
            List<String> warnings, List<String> stderrTail) {

        List<PendingShaderKey> incomplete = computeIncompleteShaders(request, shaders, outcome);

        List<String> allWarnings = new ArrayList<>(warnings);
        allWarnings.add("Pipeline worker did not complete normally (" + outcome + ") - this report contains only "
                + "what was confirmed before failure. See incompleteShaders for what's missing.");
        if (!stderrTail.isEmpty()) {
            allWarnings.add("Pipeline worker stderr tail:\n" + String.join("\n", stderrTail));
        }

        return DiagnosticReport.builder()
                .mode(request.mode())
                .mcVersionId(request.mcVersion().versionId())
                .mcClientJarPath(request.mcVersion().jarPath().toString())
                .candidateShaderDir(request.candidateShaderDir() != null ? request.candidateShaderDir().toString() : null)
                .shaders(shaders)
                .programLinks(links)
                .toolWarnings(allWarnings)
                .runOutcome(outcome)
                .incompleteShaders(incomplete)
                .build();
    }

    private static DiagnosticReport spawnFailedReport(PipelineRequest request, String message) {
        return DiagnosticReport.builder()
                .mode(request.mode())
                .mcVersionId(request.mcVersion().versionId())
                .mcClientJarPath(request.mcVersion().jarPath().toString())
                .toolWarnings(List.of(message))
                .runOutcome(RunOutcome.SPAWN_FAILED)
                .build();
    }

    private static List<PendingShaderKey> computeIncompleteShaders(
            PipelineRequest request, List<ShaderCompileEntry> shaders, RunOutcome outcome) {
        List<PendingShaderKey> incomplete = new ArrayList<>();
        Set<String> confirmed = new HashSet<>();
        for (ShaderCompileEntry entry : shaders) {
            confirmed.add(entry.shaderName() + "|" + entry.stage() + "|" + entry.defineVariant());
        }

        try (ShaderExtractor extractor = ShaderExtractor.open(request.mcVersion().jarPath())) {
            for (ShaderInventoryEntry inv : extractor.discoverCoreShaders()) {
                for (String variant : DefineVariants.variantsFor(inv.name())) {
                    if (inv.hasVertex() && !confirmed.contains(inv.name() + "|vertex|" + variant)) {
                        incomplete.add(new PendingShaderKey(inv.name(), "vertex", variant, outcome));
                    }
                    if (inv.hasFragment() && !confirmed.contains(inv.name() + "|fragment|" + variant)) {
                        incomplete.add(new PendingShaderKey(inv.name(), "fragment", variant, outcome));
                    }
                }
            }
        } catch (Exception e) {
            // Best-effort bookkeeping only - if even re-reading the jar fails here, the partial
            // report still has everything that WAS confirmed; it just can't enumerate what wasn't.
        }
        return incomplete;
    }

    private static void joinQuietly(Thread thread) {
        try {
            thread.join(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Best-effort cleanup only.
        }
    }
}
