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

package dev.velofine.diagnostics.shader;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves Mojang's real {@code #moj_import} preprocessor directive, which real vanilla core
 * shaders use and which is <b>not valid raw GLSL</b>. Feeding an extracted {@code .fsh}/{@code .vsh}
 * straight to {@code glCompileShader} without resolving these first fails to compile on every
 * machine (an unresolved directive, not a driver bug) - this class exists so
 * {@code diagnostic.exe}'s compile results are meaningful at all.
 *
 * <p>Supported forms, confirmed by reading the real 26.2 client jar's shipped shaders:
 * <ul>
 *   <li>{@code #moj_import <path.glsl>} - resolves against
 *       {@code assets/minecraft/shaders/include/<path>.glsl} (default namespace {@code minecraft}).
 *   <li>{@code #moj_import <namespace:path.glsl>} - resolves against
 *       {@code assets/<namespace>/shaders/include/<path>.glsl}.
 *   <li>{@code #moj_import "path.glsl"} - resolves against
 *       {@code assets/minecraft/shaders/core/<path>.glsl}.
 * </ul>
 *
 * <p>Confirmed (also by reading real bytes: {@code fog.glsl}, {@code globals.glsl},
 * {@code chunksection.glsl} all shipped in the real 26.2 jar) that included files are themselves
 * complete-looking snippets starting with their own {@code #version 330} line. A real GLSL compiler
 * only tolerates one {@code #version} directive, as the very first line of the final source - so
 * this resolver strips a leading {@code #version} line from every imported file's content before
 * splicing it in. This is not guessable from the spec docs alone; it was verified against real
 * shader bytes before this class was written.
 *
 * <p><b>Whole-file import deduplication, confirmed real</b>: real {@code rendertype_end_portal.vsh}
 * imports {@code projection.glsl} <i>twice</i> at the same nesting level (not a nested
 * self-reference, so the cycle-detection stack alone doesn't catch it) - {@code projection.glsl}
 * declares a named {@code uniform Projection {...}} block, and GLSL rejects declaring the same
 * named block twice, so splicing it in twice fails to compile with a real
 * {@code 'Projection' : redefinition block} error. Real Minecraft's own loader evidently
 * deduplicates identical imports across the whole file (the shipped file compiles fine in the real
 * game), so this resolver does too: once a given import key has been fully resolved anywhere in a
 * root shader's resolution, every later occurrence of that same key splices a no-op placeholder
 * instead of re-including the content. This was caught live, by actually compiling every one of
 * the 34 real shaders against a real GPU driver - not something guessable from static inspection.
 *
 * <p><b>Known scope limitation, confirmed real</b>: this resolver does not track {@code #if}/
 * {@code #ifdef}/{@code #endif} conditional-compilation state at all - a {@code #moj_import} line
 * is spliced unconditionally wherever it appears, even inside an active conditional block. Real
 * vanilla source does this: {@code entity.vsh} guards {@code #moj_import <minecraft:light.glsl>}
 * behind {@code #if defined(PER_FACE_LIGHTING) || !defined(NO_CARDINAL_LIGHTING)}. So far this is
 * benign - an unused-but-declared GLSL function isn't a compile error - but it means the resolved
 * source is not always byte-identical to what real Minecraft's own preprocessor would produce for
 * every combination of pipeline defines. {@code shader.GlslLinter#lintRaw} flags every such case
 * found, so it's surfaced in the report rather than silently relied upon.
 *
 * <p>Pure string transform - takes an {@link ImportSource} abstraction rather than touching a jar
 * or filesystem directly, so it is fully unit-testable with in-memory fakes.
 */
public final class MojImportResolver {

    private static final Pattern ANGLE_IMPORT =
            Pattern.compile("^\\s*#moj_import\\s+<(?:([A-Za-z0-9_.\\-]+):)?([A-Za-z0-9_/]+\\.glsl)>\\s*$");
    private static final Pattern QUOTED_IMPORT =
            Pattern.compile("^\\s*#moj_import\\s+\"([A-Za-z0-9_/]+\\.glsl)\"\\s*$");
    private static final Pattern VERSION_LINE = Pattern.compile("^\\s*#version\\b.*$");
    private static final String DEFAULT_NAMESPACE = "minecraft";

    private MojImportResolver() {
    }

    /** Result of resolving one root shader source, including every include splice attempted. */
    public record ResolvedShader(
            String source, List<String> importChain, List<String> missingImports, boolean importCycleDetected) {
    }

    /** Supplies the raw text of an included {@code .glsl} file, backed by a real client jar. */
    public interface ImportSource {

        /** Resolves {@code #moj_import <namespace:path.glsl>} / {@code <path.glsl>}. */
        String readAngleImport(String namespace, String path) throws ImportResolutionException;

        /** Resolves {@code #moj_import "path.glsl"} against {@code shaders/core/}. */
        String readQuotedImport(String path) throws ImportResolutionException;
    }

    public static ResolvedShader resolve(String rootSource, ImportSource importSource) {
        Context ctx = new Context();
        String resolved = resolveInternal(rootSource, importSource, ctx);
        return new ResolvedShader(resolved, ctx.importChain, ctx.missingImports, ctx.cycleDetected);
    }

    private static String resolveInternal(String source, ImportSource importSource, Context ctx) {
        String[] lines = source.split("\n", -1);
        List<String> segments = new ArrayList<>(lines.length);

        for (String line : lines) {
            Matcher angle = ANGLE_IMPORT.matcher(line);
            Matcher quoted = QUOTED_IMPORT.matcher(line);

            if (angle.matches()) {
                String namespace = angle.group(1) != null ? angle.group(1) : DEFAULT_NAMESPACE;
                String path = angle.group(2);
                segments.add(spliceImport(importSource, ctx, namespace + ":" + path,
                        () -> importSource.readAngleImport(namespace, path)));
            } else if (quoted.matches()) {
                String path = quoted.group(1);
                segments.add(spliceImport(importSource, ctx, "core:" + path, () -> importSource.readQuotedImport(path)));
            } else {
                segments.add(line);
            }
        }

        // Joined with a single "\n" between segments, matching the original per-line structure
        // exactly when nothing was spliced - byte-exact passthrough for the common no-import case,
        // not just "close enough". A spliced segment embeds its own extra trailing "\n" (see
        // spliceImport) so that, combined with this join separator, it ends up followed by a genuine
        // blank line before whatever comes next.
        return String.join("\n", segments);
    }

    @FunctionalInterface
    private interface ImportRead {
        String read() throws ImportResolutionException;
    }

    private static String spliceImport(ImportSource importSource, Context ctx, String key, ImportRead read) {
        ctx.importChain.add(key);

        if (ctx.stack.contains(key)) {
            ctx.cycleDetected = true;
            return "// [moj_import cycle detected: " + key + "]";
        }
        if (ctx.alreadyResolved.contains(key)) {
            // Real, confirmed behavior - see the class javadoc's "whole-file import deduplication"
            // paragraph. Not a cycle (key isn't a currently-open ancestor), just a repeat inclusion
            // elsewhere in the same file, which real Minecraft's loader also only includes once.
            return "// [moj_import already included: " + key + "]";
        }

        String rawImportedContent;
        try {
            rawImportedContent = read.read();
        } catch (ImportResolutionException e) {
            ctx.missingImports.add(key + " (" + e.getMessage() + ")");
            return "// [moj_import not found: " + key + "]";
        }

        String stripped = stripLeadingVersionLine(rawImportedContent);

        ctx.stack.push(key);
        String resolvedNested = resolveInternal(stripped, importSource, ctx);
        ctx.stack.pop();
        ctx.alreadyResolved.add(key);

        // Defends against the documented real-loader quirk where a missing trailing blank line
        // breaks resolution - proactively normalize rather than reproduce the footgun. The extra
        // trailing "\n" here plus the "\n" resolveInternal's caller joins with next produces exactly
        // one genuine blank line separating this splice from whatever follows it.
        return resolvedNested.stripTrailing() + "\n";
    }

    private static String stripLeadingVersionLine(String content) {
        int newline = content.indexOf('\n');
        String firstLine = newline >= 0 ? content.substring(0, newline) : content;
        if (VERSION_LINE.matcher(firstLine).matches()) {
            return newline >= 0 ? content.substring(newline + 1) : "";
        }
        return content;
    }

    private static final class Context {
        final Deque<String> stack = new ArrayDeque<>();
        final Set<String> alreadyResolved = new HashSet<>();
        final List<String> importChain = new ArrayList<>();
        final List<String> missingImports = new ArrayList<>();
        boolean cycleDetected = false;
    }
}
