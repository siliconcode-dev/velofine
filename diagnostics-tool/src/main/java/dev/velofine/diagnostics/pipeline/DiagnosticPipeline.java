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

import dev.velofine.diagnostics.ToolVersion;
import dev.velofine.diagnostics.env.EnvironmentInfoCollector;
import dev.velofine.diagnostics.gl.DiagnosticGlContext;
import dev.velofine.diagnostics.gl.DrawCallTester;
import dev.velofine.diagnostics.gl.GlCapabilityProbe;
import dev.velofine.diagnostics.gl.GlErrorChecker;
import dev.velofine.diagnostics.gl.KhrDebugCapture;
import dev.velofine.diagnostics.gl.ProgramIntrospector;
import dev.velofine.diagnostics.gl.ProgramLinker;
import dev.velofine.diagnostics.gl.ShaderCompiler;
import dev.velofine.diagnostics.gl.ShaderPrecisionProbe;
import dev.velofine.diagnostics.gl.UboRoundTripTester;
import dev.velofine.diagnostics.gl.VertexFormatSynthesizer;
import dev.velofine.diagnostics.gpu.AdapterMatcher;
import dev.velofine.diagnostics.gpu.CpuProbe;
import dev.velofine.diagnostics.gpu.DriverQuirkMatcher;
import dev.velofine.diagnostics.gpu.GpuProbe;
import dev.velofine.diagnostics.gpu.GpuSelectionAdvisor;
import dev.velofine.diagnostics.gpu.OsProbe;
import dev.velofine.diagnostics.mc.ExpectedInventoryBaseline;
import dev.velofine.diagnostics.mc.McVersionEntry;
import dev.velofine.diagnostics.mc.ShaderExtractor;
import dev.velofine.diagnostics.model.ContextCreationAttempt;
import dev.velofine.diagnostics.model.DebugMessage;
import dev.velofine.diagnostics.model.DiagnosticReport;
import dev.velofine.diagnostics.model.DrawTestResult;
import dev.velofine.diagnostics.model.GlCapabilities;
import dev.velofine.diagnostics.model.GlContextInfo;
import dev.velofine.diagnostics.model.GpuInfo;
import dev.velofine.diagnostics.model.InventoryBaselineCheck;
import dev.velofine.diagnostics.model.LintFinding;
import dev.velofine.diagnostics.model.Mode;
import dev.velofine.diagnostics.model.ProgramIntrospection;
import dev.velofine.diagnostics.model.ProgramLinkEntry;
import dev.velofine.diagnostics.model.ShaderCompileEntry;
import dev.velofine.diagnostics.model.ShaderInventoryEntry;
import dev.velofine.diagnostics.model.ShaderPrecisionEntry;
import dev.velofine.diagnostics.model.UboRoundTripResult;
import dev.velofine.diagnostics.report.ReportPaths;
import dev.velofine.diagnostics.report.ShaderSourceArchiver;
import dev.velofine.diagnostics.shader.DefineVariants;
import dev.velofine.diagnostics.shader.GlslLinter;
import dev.velofine.diagnostics.shader.MojImportResolver;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.lwjgl.opengl.GL20;

/**
 * The entire diagnostic pipeline body - GPU/CPU/OS/environment probes, GL context creation, KHR
 * debug install, the full shader compile/link/introspect/draw/UBO matrix, report assembly -
 * extracted out of {@code ui.screens.RunProgressScreen} so both the repeat-mode loop
 * ({@code RepeatRunner}) and the subprocess worker ({@code PipelineWorkerMode}) can call the exact
 * same code, rather than each reimplementing it. GUI-independent: reports progress via
 * {@link ProgressSink} instead of a Swing-specific callback, so it has zero dependency on
 * {@code javax.swing}.
 */
public final class DiagnosticPipeline {

    private DiagnosticPipeline() {
    }

    /** Streamed progress/results as the pipeline runs - see {@code pipeline.NdjsonProgressSink} for the wire-format consumer. */
    public interface ProgressSink {
        void onStep(String message);

        void onShaderCompiled(ShaderCompileEntry entry);

        void onProgramLinked(ProgramLinkEntry entry);

        void onToolWarning(String message);
    }

    public static DiagnosticReport run(PipelineRequest request, ProgressSink sink) {
        McVersionEntry version = request.mcVersion();
        Mode mode = request.mode();
        Path candidateDir = request.candidateShaderDir();

        List<GpuInfo> gpuAdapters;
        List<ContextCreationAttempt> contextAttempts = new ArrayList<>();
        List<DebugMessage> debugMessages = new ArrayList<>();
        List<String> toolWarnings = new ArrayList<>();
        List<ShaderInventoryEntry> inventory = new ArrayList<>();
        List<ShaderCompileEntry> compileEntries = new ArrayList<>();
        List<ProgramLinkEntry> linkEntries = new ArrayList<>();
        List<LintFinding> lintFindings = new ArrayList<>();
        List<ShaderPrecisionEntry> shaderPrecisionEntries = new ArrayList<>();
        InventoryBaselineCheck inventoryBaseline = InventoryBaselineCheck.unavailable();
        GlContextInfo glContextInfo = null;
        GlCapabilities glCapabilities = null;
        Integer boundAdapterIndex = null;
        String contextRungLabel = request.forcedContextRungIndex() != null
                ? DiagnosticGlContext.hintLabels().get(request.forcedContextRungIndex()) : null;

        Path shaderArchiveDir =
                ReportPaths.reportsDirectory().resolve("velofine-diagnosis-" + request.runTimestamp() + "-shaders");

        sink.onStep("Probing GPU adapter(s) via WMI...");
        gpuAdapters = GpuProbe.queryAdapters();
        sink.onStep("Probing CPU/OS via WMI...");
        var cpuInfo = CpuProbe.query();
        var osInfo = OsProbe.query();
        var environmentInfo = EnvironmentInfoCollector.collect();
        List<String> knownQuirkNotes = DriverQuirkMatcher.match(cpuInfo, gpuAdapters);

        try (ShaderExtractor extractor = ShaderExtractor.open(version.jarPath())) {
            sink.onStep("Discovering core shaders in " + version.jarPath().getFileName() + "...");
            inventory = extractor.discoverCoreShaders();
            sink.onStep("Found " + inventory.size() + " core shader(s).");

            inventoryBaseline = ExpectedInventoryBaseline.check(version.versionId(), inventory);
            if (inventoryBaseline.baselineAvailable()
                    && (!inventoryBaseline.unexpectedShaders().isEmpty() || !inventoryBaseline.missingExpectedShaders().isEmpty())) {
                warn(sink, toolWarnings, "Discovered shader inventory doesn't match the expected 26.2 baseline - "
                        + "unexpected: " + inventoryBaseline.unexpectedShaders()
                        + ", missing: " + inventoryBaseline.missingExpectedShaders()
                        + ". This jar may be corrupted, wrong-version, or resource-pack-modified.");
            }

            sink.onStep("Creating OpenGL context" + (contextRungLabel != null
                    ? " (forced rung: " + contextRungLabel + ")..." : " (matching vanilla's exact request first)..."));
            Optional<DiagnosticGlContext> contextOpt = request.forcedContextRungIndex() != null
                    ? DiagnosticGlContext.createSpecific(request.forcedContextRungIndex(), contextAttempts)
                    : DiagnosticGlContext.create(contextAttempts);
            for (ContextCreationAttempt attempt : contextAttempts) {
                sink.onStep((attempt.succeeded() ? "  OK   " : "  FAIL ") + attempt.hints()
                        + (attempt.errorMessage() != null ? " -> " + attempt.errorMessage() : ""));
            }

            if (contextOpt.isPresent()) {
                try (DiagnosticGlContext context = contextOpt.get()) {
                    GlErrorChecker.checkAndRecord("context creation", debugMessages);

                    sink.onStep("Installing KHR_debug capture...");
                    try (KhrDebugCapture khrDebug = KhrDebugCapture.install(debugMessages)) {
                        if (!khrDebug.isSupported()) {
                            warn(sink, toolWarnings, "KHR_debug is not supported on this driver - relying on glGetError() polling only.");
                        }

                        glContextInfo = context.describe();
                        GlErrorChecker.checkAndRecord("describe context", debugMessages);
                        sink.onStep("GL_VENDOR: " + glContextInfo.vendor());
                        sink.onStep("GL_RENDERER: " + glContextInfo.renderer());
                        sink.onStep("GL_VERSION: " + glContextInfo.glVersion());

                        boundAdapterIndex = AdapterMatcher.matchRendererToAdapter(glContextInfo.renderer(), gpuAdapters).orElse(null);
                        GpuSelectionAdvisor.adviseIfNeeded(gpuAdapters, boundAdapterIndex, glContextInfo.renderer())
                                .ifPresent(advice -> warn(sink, toolWarnings, advice));

                        glCapabilities = GlCapabilityProbe.probe();
                        GlErrorChecker.checkAndRecord("capability probe", debugMessages);

                        sink.onStep("Probing shader precision format...");
                        shaderPrecisionEntries = ShaderPrecisionProbe.probe();
                        GlErrorChecker.checkAndRecord("shader precision probe", debugMessages);

                        runShaderMatrix(extractor, inventory, mode, candidateDir, shaderArchiveDir,
                                compileEntries, linkEntries, debugMessages, lintFindings, sink);
                    }
                }
            } else {
                // Deliberately falls through to the common report-assembly path below rather than
                // returning early - a total context-creation failure on reference hardware is itself
                // the finding, and must still produce a saved, actionable report, not nothing.
                warn(sink, toolWarnings, "GL context creation failed for every hint set tried - see contextCreationAttempts. "
                        + "This is itself a major finding on old-Intel-iGPU hardware, not just a tool error.");
            }
        } catch (Exception e) {
            warn(sink, toolWarnings, "Pipeline failed with an unexpected exception: " + e);
            sink.onStep("ERROR: " + e);
        }

        return DiagnosticReport.builder()
                .toolVersion(ToolVersion.version())
                .generatedAtIso(DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now()))
                .mode(mode)
                .mcVersionId(version.versionId())
                .mcClientJarPath(version.jarPath().toString())
                .candidateShaderDir(candidateDir != null ? candidateDir.toString() : null)
                .gpuAdapters(gpuAdapters)
                .cpuInfo(cpuInfo)
                .osInfo(osInfo)
                .environmentInfo(environmentInfo)
                .boundAdapterIndex(boundAdapterIndex)
                .contextCreationAttempts(contextAttempts)
                .contextRungLabel(contextRungLabel)
                .glContext(glContextInfo)
                .glCapabilities(glCapabilities)
                .shaderInventory(inventory)
                .inventoryBaseline(inventoryBaseline)
                .shaders(compileEntries)
                .programLinks(linkEntries)
                .lintFindings(lintFindings)
                .shaderPrecision(shaderPrecisionEntries)
                .debugMessages(debugMessages)
                .toolWarnings(toolWarnings)
                .knownQuirkNotes(knownQuirkNotes)
                .build();
    }

    private static void warn(ProgressSink sink, List<String> toolWarnings, String message) {
        toolWarnings.add(message);
        sink.onToolWarning(message);
    }

    private static void runShaderMatrix(
            ShaderExtractor extractor,
            List<ShaderInventoryEntry> inventory,
            Mode mode,
            Path candidateDir,
            Path shaderArchiveDir,
            List<ShaderCompileEntry> compileEntries,
            List<ProgramLinkEntry> linkEntries,
            List<DebugMessage> debugMessages,
            List<LintFinding> lintFindings,
            ProgressSink sink) {

        for (ShaderInventoryEntry entry : inventory) {
            for (String variant : DefineVariants.variantsFor(entry.name())) {
                sink.onStep("Compiling " + entry.name() + " [" + variant + "]...");

                Integer vertexHandle = null;
                Integer fragmentHandle = null;

                if (entry.hasVertex()) {
                    vertexHandle = compileStage(extractor, entry.name(), "vsh", "vertex", variant, mode,
                            candidateDir, shaderArchiveDir, compileEntries, debugMessages, lintFindings, sink);
                }
                if (entry.hasFragment()) {
                    fragmentHandle = compileStage(extractor, entry.name(), "fsh", "fragment", variant, mode,
                            candidateDir, shaderArchiveDir, compileEntries, debugMessages, lintFindings, sink);
                }

                if (vertexHandle != null && fragmentHandle != null) {
                    ProgramLinker.LinkResult link = ProgramLinker.link(vertexHandle, fragmentHandle);
                    GlErrorChecker.checkAndRecord("link " + entry.name() + " [" + variant + "]", debugMessages);

                    ProgramIntrospection introspection = null;
                    DrawTestResult drawTest = null;
                    List<UboRoundTripResult> uboTests = List.of();

                    if (link.success()) {
                        introspection = ProgramIntrospector.introspect(link.program());
                        GlErrorChecker.checkAndRecord("introspect " + entry.name() + " [" + variant + "]", debugMessages);

                        // Built once and shared between DrawCallTester and UboRoundTripTester - a
                        // real, confirmed bug caught via a live run against this machine's actual GPU
                        // was each independently building its own VAO but never binding it before
                        // drawing (GL_INVALID_OPERATION: no VAO bound). See both classes' javadoc.
                        try (VertexFormatSynthesizer synthesized = VertexFormatSynthesizer.build(introspection.attributes())) {
                            drawTest = DrawCallTester.run(link.program(), synthesized.vao(), shaderArchiveDir,
                                    entry.name(), "program", variant, debugMessages);
                            GlErrorChecker.checkGraphicsResetStatus("draw " + entry.name() + " [" + variant + "]", debugMessages);

                            uboTests = UboRoundTripTester.runAllKnownBlocks(
                                    link.program(), synthesized.vao(), introspection.uniformBlocks(), debugMessages);
                        }

                        sink.onStep("  " + entry.name() + " [" + variant + "] draw test: "
                                + (drawTest.drawSucceededNoNewError() ? "OK" : "FAILED"));
                    }

                    ProgramLinkEntry linkEntry = new ProgramLinkEntry(
                            entry.name(), variant, link.success(), link.infoLog(), introspection, drawTest, uboTests);
                    linkEntries.add(linkEntry);
                    sink.onProgramLinked(linkEntry);
                    ProgramLinker.delete(link.program());
                }

                if (vertexHandle != null) {
                    ShaderCompiler.delete(vertexHandle);
                }
                if (fragmentHandle != null) {
                    ShaderCompiler.delete(fragmentHandle);
                }
            }
        }
    }

    /** Returns the compiled shader's GL handle, or {@code null} if the source couldn't be found at all. */
    private static Integer compileStage(
            ShaderExtractor extractor,
            String name,
            String extension,
            String stage,
            String variant,
            Mode mode,
            Path candidateDir,
            Path shaderArchiveDir,
            List<ShaderCompileEntry> compileEntries,
            List<DebugMessage> debugMessages,
            List<LintFinding> lintFindings,
            ProgressSink sink) {

        RootSource root = resolveRootSource(extractor, name, extension, mode, candidateDir, sink);
        if (root == null) {
            ShaderCompileEntry entry = new ShaderCompileEntry(name, stage, variant, "none", true, List.of(), false, null, null, null);
            compileEntries.add(entry);
            sink.onShaderCompiled(entry);
            return null;
        }

        lintFindings.addAll(GlslLinter.lintRaw(root.source(), name, stage));

        MojImportResolver.ResolvedShader resolved = MojImportResolver.resolve(root.source(), extractor);
        String defined = DefineVariants.applyDefine(resolved.source(), name, variant);

        lintFindings.addAll(GlslLinter.lintResolved(defined, name, stage));

        String resolvedSourcePath = null;
        String sourceSha256 = ShaderSourceArchiver.sha256Hex(defined);
        try {
            ShaderSourceArchiver.ArchivedSource archived = ShaderSourceArchiver.persist(shaderArchiveDir, name, stage, variant, defined);
            resolvedSourcePath = archived.relativePath();
        } catch (Exception e) {
            sink.onStep("  Failed to archive resolved source for " + name + "." + extension + ": " + e);
        }

        int glType = "vertex".equals(stage) ? GL20.GL_VERTEX_SHADER : GL20.GL_FRAGMENT_SHADER;
        ShaderCompiler.CompiledShader compiled = ShaderCompiler.compile(glType, defined);
        GlErrorChecker.checkAndRecord("compile " + name + "." + extension + " [" + variant + "]", debugMessages);

        if (!resolved.missingImports().isEmpty()) {
            for (String missing : resolved.missingImports()) {
                debugMessages.add(new DebugMessage("moj_import-resolver", "MISSING_IMPORT", -1, "N/A",
                        name + "." + extension + ": " + missing));
            }
        }

        ShaderCompileEntry entry = new ShaderCompileEntry(
                name, stage, variant, root.origin(), false,
                resolved.importChain(), resolved.importCycleDetected(), compiled.result(),
                resolvedSourcePath, sourceSha256);
        compileEntries.add(entry);
        sink.onShaderCompiled(entry);

        sink.onStep("  " + name + "." + extension + " [" + variant + "]: " + (compiled.result().success() ? "OK" : "FAILED"));

        return compiled.handle();
    }

    private record RootSource(String source, String origin) {
    }

    private static RootSource resolveRootSource(
            ShaderExtractor extractor, String name, String extension, Mode mode, Path candidateDir, ProgressSink sink) {
        if (mode == Mode.CANDIDATE && candidateDir != null) {
            Path candidateFile = candidateDir.resolve(name + "." + extension);
            if (Files.isRegularFile(candidateFile)) {
                try {
                    return new RootSource(Files.readString(candidateFile, StandardCharsets.UTF_8), "candidate-file");
                } catch (Exception e) {
                    sink.onStep("  Failed to read candidate file " + candidateFile + ": " + e);
                }
            }
        }
        try {
            Optional<String> jarSource = extractor.readCoreShaderSource(name, extension);
            return jarSource.map(s -> new RootSource(s, "baseline-jar")).orElse(null);
        } catch (Exception e) {
            sink.onStep("  Failed to read " + name + "." + extension + " from jar: " + e);
            return null;
        }
    }
}
