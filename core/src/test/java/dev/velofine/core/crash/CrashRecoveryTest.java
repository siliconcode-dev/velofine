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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the crash-recovery state machine directly (no real JVM shutdown, no real Minecraft) -
 * {@link CrashRecovery#onShutdown()} is package-visible for exactly this reason, standing in for
 * what the real registered shutdown hook would do.
 */
final class CrashRecoveryTest {

    @Test
    void freshInstallHasNoCrashHistory(@TempDir Path gameDir) {
        CrashRecovery.beginSession(gameDir);

        assertEquals(0, CrashRecovery.consecutiveStartupCrashes());
        assertFalse(CrashRecovery.isSafeModeActiveThisLaunch());
    }

    @Test
    void cleanExitClearsStateAndLeavesNoCrashHistoryNextLaunch(@TempDir Path gameDir) {
        CrashRecovery.beginSession(gameDir);
        CrashRecovery.markSessionHealthy();
        CrashRecovery.onShutdown();

        Path stateFile = gameDir.resolve("velofine").resolve("crash-state.json");
        assertFalse(Files.exists(stateFile), "a healthy session's state file should be deleted on clean exit");

        CrashRecovery.beginSession(gameDir);
        assertEquals(0, CrashRecovery.consecutiveStartupCrashes());
        assertFalse(CrashRecovery.isSafeModeActiveThisLaunch());
    }

    @Test
    void oneStartupCrashIsNotYetSafeMode(@TempDir Path gameDir) {
        CrashRecovery.beginSession(gameDir);
        // Never reaches healthy, so a real "shutdown" (or a hard crash that skips it entirely -
        // functionally identical here) leaves the marker as "started".
        CrashRecovery.onShutdown();

        CrashRecovery.beginSession(gameDir);
        assertEquals(1, CrashRecovery.consecutiveStartupCrashes());
        assertFalse(CrashRecovery.isSafeModeActiveThisLaunch(), "a single crash should not yet trigger safe mode");
    }

    @Test
    void twoConsecutiveStartupCrashesTriggerSafeMode(@TempDir Path gameDir) {
        CrashRecovery.beginSession(gameDir);
        CrashRecovery.onShutdown();
        CrashRecovery.beginSession(gameDir);
        CrashRecovery.onShutdown();

        CrashRecovery.beginSession(gameDir);
        assertEquals(2, CrashRecovery.consecutiveStartupCrashes());
        assertTrue(CrashRecovery.isSafeModeActiveThisLaunch());
    }

    @Test
    void safeModeDecisionDoesNotChangeMidLaunchAfterGoingHealthy(@TempDir Path gameDir) {
        CrashRecovery.beginSession(gameDir);
        CrashRecovery.onShutdown();
        CrashRecovery.beginSession(gameDir);
        CrashRecovery.onShutdown();

        CrashRecovery.beginSession(gameDir);
        assertTrue(CrashRecovery.isSafeModeActiveThisLaunch());

        // Reaching healthy this launch does not retroactively un-trigger the decision already made
        // for it - it just resets the counter for the *next* launch (see the following test).
        CrashRecovery.markSessionHealthy();
        assertTrue(CrashRecovery.isSafeModeActiveThisLaunch());
    }

    @Test
    void aHealthySessionResetsTheCrashCounterForTheNextLaunch(@TempDir Path gameDir) {
        CrashRecovery.beginSession(gameDir);
        CrashRecovery.onShutdown();
        CrashRecovery.beginSession(gameDir);
        CrashRecovery.onShutdown();
        CrashRecovery.beginSession(gameDir);
        assertTrue(CrashRecovery.isSafeModeActiveThisLaunch());

        CrashRecovery.markSessionHealthy();
        CrashRecovery.onShutdown();

        CrashRecovery.beginSession(gameDir);
        assertEquals(0, CrashRecovery.consecutiveStartupCrashes());
        assertFalse(CrashRecovery.isSafeModeActiveThisLaunch());
    }

    @Test
    void aMidSessionCrashAfterGoingHealthyIsNotCountedAsAStartupCrash(@TempDir Path gameDir) {
        CrashRecovery.beginSession(gameDir);
        CrashRecovery.markSessionHealthy();
        // No onShutdown() call at all here - simulates a hard/native crash after already going
        // healthy (the hook never runs either way), leaving the marker as "healthy" on disk.

        CrashRecovery.beginSession(gameDir);
        assertEquals(0, CrashRecovery.consecutiveStartupCrashes(),
                "a crash after reaching a healthy state should not count as a startup crash");
        assertFalse(CrashRecovery.isSafeModeActiveThisLaunch());
    }

    @Test
    void markSessionHealthyIsIdempotent(@TempDir Path gameDir) {
        CrashRecovery.beginSession(gameDir);
        CrashRecovery.markSessionHealthy();
        CrashRecovery.markSessionHealthy();
        CrashRecovery.markSessionHealthy();
        CrashRecovery.onShutdown();

        assertFalse(Files.exists(gameDir.resolve("velofine").resolve("crash-state.json")));
    }

    @Test
    void nullGameDirIsANoOp() {
        // Mirrors ConfigManager.load(null)'s in-memory-only fallback - must not throw.
        CrashRecovery.beginSession(null);
        CrashRecovery.markSessionHealthy();
        CrashRecovery.onShutdown();
    }

    @Test
    void forceCountSystemPropertyOverridesDetection(@TempDir Path gameDir) {
        System.setProperty("velofine.crashrecovery.forceCount", "2");
        try {
            CrashRecovery.beginSession(gameDir);
            assertEquals(2, CrashRecovery.consecutiveStartupCrashes());
            assertTrue(CrashRecovery.isSafeModeActiveThisLaunch());
        } finally {
            System.clearProperty("velofine.crashrecovery.forceCount");
        }
    }
}
