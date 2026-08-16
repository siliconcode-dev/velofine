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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

final class ManifestFetcherTest {

    @Test
    void fetchesAndParsesARealManifestShape() throws Exception {
        try (TestHttpServer server = new TestHttpServer()) {
            server.respondJson("/manifest.json", """
                    { "velofineVersion": "1.2.0", "targetMcVersion": "26.2", "notes": "bug fixes" }
                    """);

            GitHubReleaseClient.GitHubRelease release = releaseWithAsset(server.urlFor("/manifest.json"));
            UpdateManifest manifest = new ManifestFetcher(new GitHubReleaseClient()).fetch(release);

            assertEquals("1.2.0", manifest.velofineVersion);
            assertEquals("26.2", manifest.targetMcVersion);
            assertEquals("bug fixes", manifest.notes);
        }
    }

    @Test
    void aReleaseWithNoManifestAssetReturnsNull() throws Exception {
        GitHubReleaseClient.GitHubRelease release = new GitHubReleaseClient.GitHubRelease();
        release.assets = java.util.List.of();

        assertNull(new ManifestFetcher(new GitHubReleaseClient()).fetch(release));
    }

    private static GitHubReleaseClient.GitHubRelease releaseWithAsset(String manifestUrl) {
        GitHubReleaseClient.GitHubRelease release = new GitHubReleaseClient.GitHubRelease();
        GitHubReleaseClient.GitHubRelease.Asset asset = new GitHubReleaseClient.GitHubRelease.Asset();
        asset.name = ReleaseAssets.MANIFEST;
        asset.browserDownloadUrl = manifestUrl;
        release.assets = java.util.List.of(asset);
        return release;
    }
}
