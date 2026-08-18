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

package dev.velofine.diagnostics.gl;

import dev.velofine.diagnostics.model.ContextCreationAttempt;
import dev.velofine.diagnostics.model.GlContextInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLCapabilities;

/**
 * Creates a real, offscreen OpenGL context via LWJGL/GLFW - not a decorative window, but the actual
 * mechanism that lets this tool run {@code glCompileShader}/{@code glLinkProgram} against the
 * user's real driver. Not unit tested: requires a real GPU/driver, which CI runners (ubuntu-latest,
 * windows-latest) do not have. Verify manually via the packaged exe on real hardware.
 *
 * <p>The first attempt requests the exact context vanilla itself requests (confirmed via prior javap
 * research on {@code com.mojang.blaze3d.opengl.GlBackend.setWindowHints()}): OpenGL 3.3, Core
 * Profile, Forward-Compatible. If the driver refuses that exact context, this is itself a major
 * finding for old-Intel-iGPU hardware - rather than aborting with no data, a fallback ladder retries
 * with progressively relaxed hints purely to gather more signal about what the driver *does*
 * support. Every attempt (not just the final outcome) is recorded.
 */
public final class DiagnosticGlContext implements AutoCloseable {

    private record HintSet(String label, int major, int minor, int profile, boolean forwardCompat) {
    }

    private static final List<HintSet> FALLBACK_LADDER = buildLadder();

    /**
     * Rung 0 is always vanilla's own exact real request, so normal (non-swept) single-run behavior
     * - which just takes the first rung that succeeds - is unchanged from before this method
     * existed. The rest of the ladder is a real, exhaustive OpenGL 2.0-through-4.6 (the latest
     * OpenGL version) sweep, Core <b>and</b> Compatibility profile at every version 3.2+ (profiles
     * don't exist before 3.2 - GLFW requires {@code GLFW_OPENGL_ANY_PROFILE} below that) - not just
     * the handful of rungs vanilla itself happens to need. Only consumed exhaustively when the
     * opt-in "context rung sweep" is used ({@code ModeSelectionScreen}); a normal single run still
     * only ever attempts however many rungs it takes to find one that succeeds.
     */
    private static List<HintSet> buildLadder() {
        List<HintSet> ladder = new ArrayList<>();

        ladder.add(new HintSet("OpenGL 3.3 Core, Forward-Compatible (vanilla's exact request)",
                3, 3, GLFW.GLFW_OPENGL_CORE_PROFILE, true));
        ladder.add(new HintSet("OpenGL 3.3 Core, no forward-compat",
                3, 3, GLFW.GLFW_OPENGL_CORE_PROFILE, false));
        ladder.add(new HintSet("OpenGL 3.3 Compatibility Profile",
                3, 3, GLFW.GLFW_OPENGL_COMPAT_PROFILE, false));

        int[][] legacyVersions = {{2, 0}, {2, 1}, {3, 0}, {3, 1}};
        for (int[] v : legacyVersions) {
            ladder.add(new HintSet("OpenGL " + v[0] + "." + v[1] + ", any profile",
                    v[0], v[1], GLFW.GLFW_OPENGL_ANY_PROFILE, false));
        }

        int[][] profiledVersions = {{3, 2}, {3, 3}, {4, 0}, {4, 1}, {4, 2}, {4, 3}, {4, 4}, {4, 5}, {4, 6}};
        for (int[] v : profiledVersions) {
            if (v[0] == 3 && v[1] == 3) {
                continue; // already covered above (with/without forward-compat)
            }
            String label = "OpenGL " + v[0] + "." + v[1];
            ladder.add(new HintSet(label + " Core Profile", v[0], v[1], GLFW.GLFW_OPENGL_CORE_PROFILE, false));
            ladder.add(new HintSet(label + " Compatibility Profile", v[0], v[1], GLFW.GLFW_OPENGL_COMPAT_PROFILE, false));
        }

        ladder.add(new HintSet("Driver default (no version hints)",
                0, 0, GLFW.GLFW_OPENGL_ANY_PROFILE, false));

        return List.copyOf(ladder);
    }

    private final long window;

    private DiagnosticGlContext(long window) {
        this.window = window;
    }

    /**
     * Attempts context creation, walking {@link #FALLBACK_LADDER} until one succeeds. Every attempt
     * (hints tried + outcome) is appended to {@code attemptsOut} regardless of overall success, so a
     * total failure still produces a rich report rather than nothing.
     */
    public static Optional<DiagnosticGlContext> create(List<ContextCreationAttempt> attemptsOut) {
        if (!GLFW.glfwInit()) {
            attemptsOut.add(new ContextCreationAttempt("glfwInit", false, "GLFW.glfwInit() returned false"));
            return Optional.empty();
        }

        for (HintSet hints : FALLBACK_LADDER) {
            Optional<DiagnosticGlContext> context = attemptOne(hints, attemptsOut);
            if (context.isPresent()) {
                return context;
            }
        }
        return Optional.empty();
    }

    /** The human-readable label for every rung in the fallback ladder, in order - see {@link #createSpecific}. */
    public static List<String> hintLabels() {
        return FALLBACK_LADDER.stream().map(HintSet::label).toList();
    }

    /**
     * Tries exactly one specific rung of the fallback ladder (by index into {@link #hintLabels()}),
     * with no fallback walk to a different rung on failure - used by the opt-in context-rung sweep
     * (item 24) to compare behavior Core vs. Compatibility profile without the "just try the next
     * one" logic {@link #create} uses for a normal run.
     */
    public static Optional<DiagnosticGlContext> createSpecific(int rungIndex, List<ContextCreationAttempt> attemptsOut) {
        if (!GLFW.glfwInit()) {
            attemptsOut.add(new ContextCreationAttempt("glfwInit", false, "GLFW.glfwInit() returned false"));
            return Optional.empty();
        }
        return attemptOne(FALLBACK_LADDER.get(rungIndex), attemptsOut);
    }

    private static Optional<DiagnosticGlContext> attemptOne(HintSet hints, List<ContextCreationAttempt> attemptsOut) {
        String[] lastError = new String[1];
        GLFWErrorCallback errorCallback = GLFWErrorCallback.create((error, description) ->
                lastError[0] = "GLFW error 0x" + Integer.toHexString(error) + ": "
                        + GLFWErrorCallback.getDescription(description));
        GLFWErrorCallback previous = GLFW.glfwSetErrorCallback(errorCallback);

        long win = 0L;
        try {
            applyHints(hints);
            win = GLFW.glfwCreateWindow(1, 1, "VelofineDiagnostic", 0L, 0L);
        } finally {
            GLFW.glfwSetErrorCallback(previous);
            errorCallback.free();
        }

        if (win != 0L) {
            GLFW.glfwMakeContextCurrent(win);
            GL.createCapabilities();
            attemptsOut.add(new ContextCreationAttempt(hints.label(), true, null));
            return Optional.of(new DiagnosticGlContext(win));
        }
        attemptsOut.add(new ContextCreationAttempt(
                hints.label(), false, lastError[0] != null ? lastError[0] : "glfwCreateWindow returned NULL"));
        return Optional.empty();
    }

    private static void applyHints(HintSet hints) {
        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_DEBUG_CONTEXT, GLFW.GLFW_TRUE);
        if (hints.major() > 0) {
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, hints.major());
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, hints.minor());
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, hints.profile());
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, hints.forwardCompat() ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);
        }
    }

    /** Reads {@code glGetString}/{@code glGetStringi} output from the now-current context. */
    public GlContextInfo describe() {
        String vendor = GL11.glGetString(GL11.GL_VENDOR);
        String renderer = GL11.glGetString(GL11.GL_RENDERER);
        String version = GL11.glGetString(GL11.GL_VERSION);
        String glslVersion = GL11.glGetString(GL20.GL_SHADING_LANGUAGE_VERSION);
        boolean khrDebugSupported = GL.getCapabilities().GL_KHR_debug;
        return new GlContextInfo(vendor, renderer, version, glslVersion, extensionList(), khrDebugSupported);
    }

    private List<String> extensionList() {
        List<String> extensions = new ArrayList<>();
        GLCapabilities caps = GL.getCapabilities();
        if (caps.OpenGL30) {
            // Core-profile-correct enumeration - the deprecated single-string GL_EXTENSIONS query is
            // not guaranteed to work on a core-profile context.
            int count = GL11.glGetInteger(GL30.GL_NUM_EXTENSIONS);
            for (int i = 0; i < count; i++) {
                extensions.add(GL30.glGetStringi(GL11.GL_EXTENSIONS, i));
            }
        } else {
            String raw = GL11.glGetString(GL11.GL_EXTENSIONS);
            if (raw != null && !raw.isBlank()) {
                extensions.addAll(List.of(raw.trim().split("\\s+")));
            }
        }
        return extensions;
    }

    @Override
    public void close() {
        if (window != 0L) {
            GLFW.glfwDestroyWindow(window);
        }
        GLFW.glfwTerminate();
    }
}
