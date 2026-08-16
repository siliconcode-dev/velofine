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

package dev.velofine.core.crash;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.velofine.core.log.VelofineLog;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Detects a "startup crash" (the JVM never reached a healthy running state) across launches and
 * escalates to a transient, always-logged safety net after a real crash loop - never a silent or
 * permanent change, per CLAUDE.md's "never let a feature silently degrade vanilla fidelity" (this
 * is the inverse: a temporary escalation, not a degradation, and it always reverts on its own).
 *
 * <p>Deliberately separate from {@link dev.velofine.core.config.VelofineConfig} - this is
 * transient operational state (did the last session crash), not a user setting, so it lives in its
 * own small file rather than in {@code config.json}.
 *
 * <p><b>How "did the last session crash" is actually detected</b>: a plain "did a shutdown hook
 * run" signal is not enough - a hook fires on every orderly JVM exit (normal return, any
 * {@code System.exit()} call buried in vanilla, <em>and</em> an uncaught exception propagating out
 * of {@code main} - exactly the self-attach/handoff failure modes {@code launcher.Main} can hit).
 * What actually carries the signal is the *content* the marker held when the JVM went down: {@link
 * #beginSession} writes {@code "started"} before self-attach even happens; {@link
 * #markSessionHealthy()} (called once the game reaches its first client tick, well past
 * GL-context/mixin-init startup risk) updates it to {@code "healthy"}. The registered shutdown
 * hook deletes the file only if it still reads {@code "healthy"} at that point - otherwise it is
 * left exactly as {@code "started"}, so the next launch correctly sees an unhealthy exit. A genuine
 * native/driver crash (no JVM shutdown at all - precisely the class of failure LegacySupport exists
 * for) never runs the hook either way, and leaves whatever was last written on disk, which this
 * scheme already handles correctly without needing to distinguish "hook ran" from "hook didn't".
 *
 * <p>Only counts crashes that happen <em>before</em> reaching a healthy state as "startup crashes"
 * (matching Build_plan's literal wording) - a hard crash later in a session that had already gone
 * healthy is not escalated by this mechanism, since it is far less likely to be evidence of a
 * GL-context/driver incompatibility than an early failure is.
 */
public final class CrashRecovery {

    private static final String STATE_DIR = "velofine";
    private static final String STATE_FILE = "crash-state.json";
    private static final String MARKER_STARTED = "started";
    private static final String MARKER_HEALTHY = "healthy";

    /** A single blip is not a crash loop - only escalate once a real pattern shows up. */
    private static final int CRASH_LOOP_THRESHOLD = 2;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static volatile Path statePath;
    private static volatile State state = new State();
    private static volatile boolean safeModeActive;
    private static final AtomicBoolean healthyMarked = new AtomicBoolean(false);

    private CrashRecovery() {
    }

    /**
     * Called once, right after the game directory is known and before self-attach - the earliest
     * possible point, so even a self-attach failure is captured. No-op if {@code gameDir} is
     * {@code null} (mirrors {@code ConfigManager.load(null)}'s in-memory-only fallback).
     */
    public static void beginSession(Path gameDir) {
        if (gameDir == null) {
            return;
        }
        statePath = gameDir.resolve(STATE_DIR).resolve(STATE_FILE);
        state = load(statePath);
        // Re-armed per session: each launch needs its own "first tick" detection, not a one-shot
        // guard that silently stops working after the first session in a given JVM (relevant for
        // CrashRecoveryTest, which calls beginSession more than once in the same test JVM).
        healthyMarked.set(false);

        String forced = System.getProperty("velofine.crashrecovery.forceCount");
        if (forced != null) {
            // Dev-testing hook, mirrors -Dvelofine.legacysupport.forceFixes - exercises safe mode
            // without actually crash-looping the JVM twice.
            state.consecutiveStartupCrashes = Integer.parseInt(forced.trim());
        } else if (MARKER_STARTED.equals(state.marker)) {
            state.consecutiveStartupCrashes++;
            VelofineLog.warn("crash-recovery", "Previous session did not reach a healthy running "
                    + "state (consecutive startup crashes: " + state.consecutiveStartupCrashes + ")");
        } else {
            state.consecutiveStartupCrashes = 0;
        }

        safeModeActive = state.consecutiveStartupCrashes >= CRASH_LOOP_THRESHOLD;
        if (safeModeActive) {
            VelofineLog.warn("crash-recovery", "SAFE MODE ACTIVE this launch ("
                    + state.consecutiveStartupCrashes + " consecutive startup crashes) - "
                    + "LegacySupport's GL-compatibility fixes are being forced on for this launch only.");
        }

        state.marker = MARKER_STARTED;
        save(statePath, state);

        Runtime.getRuntime().addShutdownHook(new Thread(CrashRecovery::onShutdown, "velofine-crash-recovery-shutdown"));
    }

    /** Package-visible so {@code CrashRecoveryTest} can exercise the decision logic directly,
     *  without actually triggering a real JVM shutdown. */
    static void onShutdown() {
        if (statePath == null) {
            return;
        }
        if (MARKER_HEALTHY.equals(state.marker)) {
            try {
                Files.deleteIfExists(statePath);
            } catch (IOException e) {
                VelofineLog.warn("crash-recovery", "Failed to clear crash-recovery state on clean exit: " + e);
            }
        }
        // Otherwise deliberately left as "started" - the whole point is for the next launch to see it.
    }

    /**
     * Called once (idempotent) from the first client tick - well past GL-context/mixin-init
     * startup risk. See {@code core.gui.mixin.MinecraftKeybindMixin}, which already runs every
     * tick for the config keybind poll.
     */
    public static void markSessionHealthy() {
        if (!healthyMarked.compareAndSet(false, true)) {
            return;
        }
        if (statePath == null) {
            return;
        }
        state.marker = MARKER_HEALTHY;
        save(statePath, state);
    }

    /** Decided once, at {@link #beginSession}; does not change mid-launch even after going healthy. */
    public static boolean isSafeModeActiveThisLaunch() {
        return safeModeActive;
    }

    public static int consecutiveStartupCrashes() {
        return state.consecutiveStartupCrashes;
    }

    private static State load(Path path) {
        if (!Files.isRegularFile(path)) {
            return new State();
        }
        try {
            String json = Files.readString(path, StandardCharsets.UTF_8);
            State parsed = GSON.fromJson(json, State.class);
            return parsed != null ? parsed : new State();
        } catch (Exception e) {
            VelofineLog.warn("crash-recovery", "Could not read " + path + " (" + e + "); starting fresh.");
            return new State();
        }
    }

    private static void save(Path path, State value) {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(value), StandardCharsets.UTF_8);
        } catch (IOException e) {
            VelofineLog.warn("crash-recovery", "Failed to save " + path + ": " + e);
        }
    }

    /** Package-visible for {@code CrashRecoveryTest} - not part of the public API. */
    static final class State {
        String marker = null;
        int consecutiveStartupCrashes = 0;
    }
}
