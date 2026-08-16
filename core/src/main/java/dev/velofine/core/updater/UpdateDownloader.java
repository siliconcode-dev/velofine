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

import dev.velofine.core.config.ConfigManager;
import dev.velofine.core.updater.GitHubReleaseClient.GitHubRelease;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.function.IntConsumer;

/**
 * Stages a release's installer exe + checksums + signature into
 * {@code <gameDir>/velofine/update-staging/<version>/} - alongside {@code config.json}, so it
 * lives under the same "survives a Velofine reinstall" location {@link ConfigManager} already
 * uses, rather than under {@code libraries/dev/velofine} which {@code ProfileInstaller.uninstall()}
 * deletes recursively.
 */
final class UpdateDownloader {

    private static final String STAGING_DIR_NAME = "update-staging";

    private final GitHubReleaseClient client = new GitHubReleaseClient();

    /** @return the path to the downloaded (not yet verified) installer exe. */
    Path download(AvailableUpdate update, IntConsumer percentCallback) throws IOException, InterruptedException {
        Path stagingRoot = stagingRoot();
        String version = update.manifest().velofineVersion;
        Path versionDir = stagingRoot.resolve(version);
        clearOtherStagedVersions(stagingRoot, version);
        Files.createDirectories(versionDir);

        GitHubRelease release = update.release();
        String installerName = ReleaseAssets.installerName(version);

        String installerUrl = release.assetUrl(installerName);
        String checksumsUrl = release.assetUrl(ReleaseAssets.CHECKSUMS);
        String signatureUrl = release.assetUrl(ReleaseAssets.CHECKSUMS_SIGNATURE);
        if (installerUrl == null || checksumsUrl == null || signatureUrl == null) {
            throw new IOException("Release " + release.tagName + " is missing one or more required assets "
                    + "(" + installerName + " / " + ReleaseAssets.CHECKSUMS + " / " + ReleaseAssets.CHECKSUMS_SIGNATURE + ")");
        }

        Path installerPath = versionDir.resolve(installerName);
        Path checksumsPath = versionDir.resolve(ReleaseAssets.CHECKSUMS);
        Path signaturePath = versionDir.resolve(ReleaseAssets.CHECKSUMS_SIGNATURE);

        Files.write(checksumsPath, client.download(checksumsUrl));
        Files.write(signaturePath, client.download(signatureUrl));
        client.downloadToFile(installerUrl, installerPath, percentCallback);

        return installerPath;
    }

    static void deleteStaged(Path versionDir) {
        if (versionDir == null || !Files.isDirectory(versionDir)) {
            return;
        }
        try (var walk = Files.walk(versionDir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // Best-effort cleanup - a leftover staged file is harmless, just wasted disk.
                }
            });
        } catch (IOException ignored) {
            // Same as above.
        }
    }

    private static void clearOtherStagedVersions(Path stagingRoot, String keepVersion) {
        if (!Files.isDirectory(stagingRoot)) {
            return;
        }
        try (var entries = Files.newDirectoryStream(stagingRoot)) {
            for (Path entry : entries) {
                if (Files.isDirectory(entry) && !entry.getFileName().toString().equals(keepVersion)) {
                    deleteStaged(entry);
                }
            }
        } catch (IOException ignored) {
            // Best-effort cleanup only.
        }
    }

    private static Path stagingRoot() {
        Path configPath = ConfigManager.configPath();
        if (configPath == null) {
            throw new IllegalStateException("No game directory known; cannot stage an update download.");
        }
        return configPath.getParent().resolve(STAGING_DIR_NAME);
    }
}
