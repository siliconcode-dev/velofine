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

package dev.velofine.core.updater;

import dev.velofine.core.BuildInfo;
import dev.velofine.core.config.ConfigManager;
import dev.velofine.core.config.VelofineConfig;
import dev.velofine.core.log.VelofineLog;
import dev.velofine.core.status.LiveStatus;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Sequences check -&gt; download -&gt; verify -&gt; (user confirms) -&gt; apply on a single
 * dedicated background thread - the first background-thread consumer in this codebase (confirmed:
 * no {@code ExecutorService}/{@code Thread}/{@code CompletableFuture} existed anywhere before
 * Phase 8). Every state transition is pushed to {@link LiveStatus} so the GUI can poll it, per the
 * same pattern {@code PerformanceGovernor} already established for the Optimus panel's footnote.
 *
 * <p>Check, download and verify all chain automatically off one "Check for Updates" click (or the
 * passive launch check) - the only step that needs a separate, explicit user confirmation is
 * {@link #apply()}, since that one closes the game. Bandwidth for an unwanted download is a much
 * smaller cost than a second round of user interaction for every check.
 *
 * <p>Called from two places: {@code CoreEngine.onAgentAttached} (the passive, rate-limited
 * auto-check - pure IO, no {@code core.gui}/Minecraft references anywhere in that call chain, per
 * {@code CoreEngine}'s documented restriction) and {@code UpdaterPage} (manual actions, which is
 * free to reference Minecraft types since a screen is already open by the time it runs).
 */
public final class UpdateOrchestrator {

    private static final long AUTO_CHECK_INTERVAL_MILLIS = 24L * 60 * 60 * 1000;

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "velofine-updater");
        thread.setDaemon(true);
        return thread;
    });

    private static volatile UpdatePhase phase = UpdatePhase.IDLE;
    private static volatile Path verifiedInstallerPath;

    private UpdateOrchestrator() {
    }

    public static UpdatePhase phase() {
        return phase;
    }

    /**
     * Called once on every launch, before {@link #checkIfDueOnLaunch()} - surfaces a one-time
     * "update applied successfully" confirmation if the previous session ended by spawning a
     * silent installer (see {@link UpdateApplier#spawnInstaller}), then clears the marker so it is
     * not shown again. No in-game toast/dialog system exists anywhere in this codebase yet, so
     * this reaches the user the same two ways every other engine's fix-confirmation already does:
     * a log line, and {@link LiveStatus} for whoever opens the Updates page.
     */
    public static void reportIfJustApplied() {
        String installerFilename = UpdateApplier.consumeAppliedMarkerIfPresent();
        if (installerFilename == null) {
            return;
        }
        String message = "update applied successfully (" + installerFilename + ") - now running "
                + BuildInfo.velofineVersion();
        LiveStatus.setUpdater(message);
        VelofineLog.info("updater", message);
    }

    /** Passive entry point - only checks if {@code autoCheckOnLaunch} is on and the interval has elapsed. */
    public static void checkIfDueOnLaunch() {
        VelofineConfig config = ConfigManager.get();
        if (!config.updater.autoCheckOnLaunch) {
            return;
        }
        long elapsed = System.currentTimeMillis() - config.updater.lastCheckedEpochMillis;
        if (elapsed < AUTO_CHECK_INTERVAL_MILLIS) {
            return;
        }
        checkNow();
    }

    /** Manual "Check for Updates" entry point - also what the passive check ultimately calls. */
    public static void checkNow() {
        if (phase == UpdatePhase.CHECKING || phase == UpdatePhase.DOWNLOADING || phase == UpdatePhase.VERIFYING) {
            return;
        }
        phase = UpdatePhase.CHECKING;
        LiveStatus.setUpdater("checking...");
        EXECUTOR.submit(UpdateOrchestrator::runCheck);
    }

    /**
     * Spawns the verified installer. Synchronous and fast ({@code ProcessBuilder.start()} returns
     * immediately) - deliberately not routed through the background executor, since the caller
     * (a GUI button handler) needs to know spawning succeeded before it calls
     * {@code Minecraft.getInstance().stop()} right afterward.
     */
    public static void apply() throws IOException {
        Path installer = verifiedInstallerPath;
        if (installer == null || phase != UpdatePhase.VERIFIED) {
            throw new IllegalStateException("No verified update is ready to apply.");
        }
        phase = UpdatePhase.APPLYING;
        LiveStatus.setUpdater("applying update - Minecraft will now close...");
        UpdateApplier.spawnInstaller(installer);
    }

    private static void runCheck() {
        try {
            Optional<AvailableUpdate> result = new UpdateChecker().check();
            if (result.isPresent()) {
                AvailableUpdate update = result.get();
                phase = UpdatePhase.UPDATE_AVAILABLE;
                LiveStatus.setUpdater("update available: " + update.manifest().velofineVersion + " - downloading...");
                runDownloadAndVerify(update);
            } else {
                phase = UpdatePhase.UP_TO_DATE;
                LiveStatus.setUpdater("up to date (" + BuildInfo.velofineVersion() + ")");
            }
        } catch (Exception e) {
            phase = UpdatePhase.FAILED;
            LiveStatus.setUpdater("check failed: " + e.getMessage());
            VelofineLog.warn("updater", "Update check failed: " + e);
        }
    }

    private static void runDownloadAndVerify(AvailableUpdate update) {
        phase = UpdatePhase.DOWNLOADING;
        LiveStatus.setUpdater("downloading 0%");
        Path stagedInstaller = null;
        try {
            stagedInstaller = new UpdateDownloader().download(update,
                    percent -> LiveStatus.setUpdater("downloading " + percent + "%"));

            phase = UpdatePhase.VERIFYING;
            LiveStatus.setUpdater("verifying...");

            Path stagingDir = stagedInstaller.getParent();
            Path checksumsFile = stagingDir.resolve(ReleaseAssets.CHECKSUMS);
            Path signatureFile = stagingDir.resolve(ReleaseAssets.CHECKSUMS_SIGNATURE);

            boolean checksumOk = new ChecksumVerifier().verify(stagedInstaller, checksumsFile);
            boolean signatureOk = checksumOk && new SignatureVerifier().verify(checksumsFile, signatureFile);

            if (!checksumOk || !signatureOk) {
                phase = UpdatePhase.VERIFICATION_FAILED;
                LiveStatus.setUpdater("verification failed - update rejected, will retry next check");
                VelofineLog.warn("updater", "Downloaded update failed verification (checksum="
                        + checksumOk + ", signature=" + signatureOk + ") - discarding.");
                UpdateDownloader.deleteStaged(stagingDir);
                return;
            }

            verifiedInstallerPath = stagedInstaller;
            phase = UpdatePhase.VERIFIED;
            LiveStatus.setUpdater("verified " + update.manifest().velofineVersion + " - click Update & Restart");
        } catch (Exception e) {
            phase = UpdatePhase.FAILED;
            LiveStatus.setUpdater("download/verify failed: " + e.getMessage());
            VelofineLog.warn("updater", "Update download/verify failed: " + e);
            if (stagedInstaller != null) {
                UpdateDownloader.deleteStaged(stagedInstaller.getParent());
            }
        }
    }
}
