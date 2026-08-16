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
import dev.velofine.core.updater.GitHubReleaseClient.GitHubRelease;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Fetches recent releases, filters to the ones matching this install's tracked MC version, and
 * picks the highest Velofine-versioned match that is newer than what is currently running.
 *
 * <p>Pure HTTP/IO/Gson - no {@code core.gui} or Minecraft-type references, so it is safe to call
 * from {@code CoreEngine.onAgentAttached} (see that class's javadoc on why touching
 * {@code core.gui} early breaks every mixin).
 */
final class UpdateChecker {

    private final GitHubReleaseClient client = new GitHubReleaseClient();
    private final ManifestFetcher manifests = new ManifestFetcher(client);
    private final VersionComparator versions = new VersionComparator();

    Optional<AvailableUpdate> check() throws IOException, InterruptedException {
        VelofineConfig config = ConfigManager.get();
        String trackedMcVersion = config.updater.trackedMcVersion;

        List<GitHubRelease> releases = client.listRecentReleases();
        AvailableUpdate best = null;

        for (GitHubRelease release : releases) {
            if (release.draft || release.prerelease) {
                continue;
            }
            UpdateManifest manifest = manifests.fetch(release);
            if (manifest == null || manifest.velofineVersion == null) {
                continue;
            }
            if (trackedMcVersion != null && !trackedMcVersion.equals(manifest.targetMcVersion)) {
                continue;
            }
            if (best == null || versions.isNewer(manifest.velofineVersion, best.manifest().velofineVersion)) {
                best = new AvailableUpdate(release, manifest);
            }
        }

        config.updater.lastCheckedEpochMillis = System.currentTimeMillis();
        ConfigManager.save();

        if (best == null || !versions.isNewer(best.manifest().velofineVersion, BuildInfo.velofineVersion())) {
            return Optional.empty();
        }
        if (best.manifest().velofineVersion.equals(config.updater.skippedVersion)) {
            return Optional.empty();
        }
        return Optional.of(best);
    }
}
