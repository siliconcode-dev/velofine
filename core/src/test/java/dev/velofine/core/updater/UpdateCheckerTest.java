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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link BuildInfo#velofineVersion()}'s actual value depends on {@code project.version} at build
 * time, so tests compare against deliberately extreme version numbers ("999.0.0" / "0.0.1") rather
 * than a hardcoded literal that could silently drift from whatever this build's real version is.
 */
final class UpdateCheckerTest {

    @Test
    void aNewerMatchingReleaseIsReturned(@TempDir Path gameDir) throws Exception {
        ConfigManager.load(gameDir);
        ConfigManager.get().updater.trackedMcVersion = "26.2";

        try (TestHttpServer server = new TestHttpServer()) {
            serveOneRelease(server, "v999.0.0", "999.0.0", "26.2");
            UpdateChecker checker = new UpdateChecker(new GitHubReleaseClient(server.urlFor("/releases")));

            Optional<AvailableUpdate> result = checker.check();

            assertTrue(result.isPresent());
            assertEquals("999.0.0", result.get().manifest().velofineVersion);
        }
    }

    @Test
    void anOlderOrEqualReleaseIsNotOfferedAsAnUpdate(@TempDir Path gameDir) throws Exception {
        ConfigManager.load(gameDir);
        ConfigManager.get().updater.trackedMcVersion = "26.2";

        try (TestHttpServer server = new TestHttpServer()) {
            serveOneRelease(server, "v0.0.1", "0.0.1", "26.2");
            UpdateChecker checker = new UpdateChecker(new GitHubReleaseClient(server.urlFor("/releases")));

            assertTrue(checker.check().isEmpty());
        }
    }

    @Test
    void aReleaseForADifferentMcVersionTrackIsFilteredOutEvenIfNewer(@TempDir Path gameDir) throws Exception {
        ConfigManager.load(gameDir);
        ConfigManager.get().updater.trackedMcVersion = "26.2";

        try (TestHttpServer server = new TestHttpServer()) {
            serveOneRelease(server, "v999.0.0", "999.0.0", "27.0");
            UpdateChecker checker = new UpdateChecker(new GitHubReleaseClient(server.urlFor("/releases")));

            assertTrue(checker.check().isEmpty());
        }
    }

    @Test
    void draftAndPrereleaseReleasesAreIgnored(@TempDir Path gameDir) throws Exception {
        ConfigManager.load(gameDir);
        ConfigManager.get().updater.trackedMcVersion = "26.2";

        try (TestHttpServer server = new TestHttpServer()) {
            server.respondJson("/manifest-draft.json", manifestJson("999.0.0", "26.2"));
            server.respondJson("/manifest-pre.json", manifestJson("999.0.1", "26.2"));
            server.respondJson("/releases", """
                    [
                      { "tag_name": "v999.0.0", "draft": true, "prerelease": false,
                        "assets": [ { "name": "manifest.json", "browser_download_url": "%s" } ] },
                      { "tag_name": "v999.0.1", "draft": false, "prerelease": true,
                        "assets": [ { "name": "manifest.json", "browser_download_url": "%s" } ] }
                    ]
                    """.formatted(server.urlFor("/manifest-draft.json"), server.urlFor("/manifest-pre.json")));

            UpdateChecker checker = new UpdateChecker(new GitHubReleaseClient(server.urlFor("/releases")));
            assertTrue(checker.check().isEmpty());
        }
    }

    @Test
    void aVersionTheUserExplicitlySkippedIsNotReOffered(@TempDir Path gameDir) throws Exception {
        ConfigManager.load(gameDir);
        ConfigManager.get().updater.trackedMcVersion = "26.2";
        ConfigManager.get().updater.skippedVersion = "999.0.0";

        try (TestHttpServer server = new TestHttpServer()) {
            serveOneRelease(server, "v999.0.0", "999.0.0", "26.2");
            UpdateChecker checker = new UpdateChecker(new GitHubReleaseClient(server.urlFor("/releases")));

            assertTrue(checker.check().isEmpty());
        }
    }

    @Test
    void theHighestVersionAmongMultipleMatchesWins(@TempDir Path gameDir) throws Exception {
        ConfigManager.load(gameDir);
        ConfigManager.get().updater.trackedMcVersion = "26.2";

        try (TestHttpServer server = new TestHttpServer()) {
            server.respondJson("/manifest-1.json", manifestJson("999.0.1", "26.2"));
            server.respondJson("/manifest-2.json", manifestJson("999.0.9", "26.2"));
            server.respondJson("/releases", """
                    [
                      { "tag_name": "v999.0.1", "draft": false, "prerelease": false,
                        "assets": [ { "name": "manifest.json", "browser_download_url": "%s" } ] },
                      { "tag_name": "v999.0.9", "draft": false, "prerelease": false,
                        "assets": [ { "name": "manifest.json", "browser_download_url": "%s" } ] }
                    ]
                    """.formatted(server.urlFor("/manifest-1.json"), server.urlFor("/manifest-2.json")));

            UpdateChecker checker = new UpdateChecker(new GitHubReleaseClient(server.urlFor("/releases")));
            Optional<AvailableUpdate> result = checker.check();

            assertTrue(result.isPresent());
            assertEquals("999.0.9", result.get().manifest().velofineVersion);
        }
    }

    @Test
    void checkingUpdatesTheLastCheckedTimestamp(@TempDir Path gameDir) throws Exception {
        ConfigManager.load(gameDir);
        ConfigManager.get().updater.trackedMcVersion = "26.2";
        ConfigManager.get().updater.lastCheckedEpochMillis = 0L;

        try (TestHttpServer server = new TestHttpServer()) {
            server.respondJson("/releases", "[]");
            new UpdateChecker(new GitHubReleaseClient(server.urlFor("/releases"))).check();
        }

        assertTrue(ConfigManager.get().updater.lastCheckedEpochMillis > 0L);
    }

    private static void serveOneRelease(TestHttpServer server, String tag, String velofineVersion, String targetMcVersion) {
        server.respondJson("/manifest.json", manifestJson(velofineVersion, targetMcVersion));
        server.respondJson("/releases", """
                [
                  { "tag_name": "%s", "draft": false, "prerelease": false,
                    "assets": [ { "name": "manifest.json", "browser_download_url": "%s" } ] }
                ]
                """.formatted(tag, server.urlFor("/manifest.json")));
    }

    private static String manifestJson(String velofineVersion, String targetMcVersion) {
        return "{ \"velofineVersion\": \"" + velofineVersion + "\", \"targetMcVersion\": \"" + targetMcVersion + "\" }";
    }
}
