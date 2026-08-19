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
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GitHubReleaseClientTest {

    @Test
    void listRecentReleasesParsesRealGitHubReleaseShape() throws Exception {
        try (TestHttpServer server = new TestHttpServer()) {
            server.respondJson("/releases", """
                    [
                      {
                        "tag_name": "v1.2.0",
                        "draft": false,
                        "prerelease": false,
                        "assets": [
                          { "name": "manifest.json", "browser_download_url": "%s" }
                        ]
                      },
                      {
                        "tag_name": "v1.1.0-rc1",
                        "draft": false,
                        "prerelease": true,
                        "assets": []
                      }
                    ]
                    """.formatted(server.urlFor("/v1.2.0/manifest.json")));

            GitHubReleaseClient client = new GitHubReleaseClient(server.urlFor("/releases"));
            List<GitHubReleaseClient.GitHubRelease> releases = client.listRecentReleases();

            assertEquals(2, releases.size());
            assertEquals("v1.2.0", releases.get(0).tagName);
            assertEquals(false, releases.get(0).draft);
            assertEquals(true, releases.get(1).prerelease);
            assertTrue(releases.get(0).assetUrl("manifest.json").endsWith("/v1.2.0/manifest.json"));
            assertEquals(null, releases.get(0).assetUrl("does-not-exist.exe"));
        }
    }

    @Test
    void emptyReleaseListParsesToEmptyList() throws Exception {
        try (TestHttpServer server = new TestHttpServer()) {
            server.respondJson("/releases", "[]");

            GitHubReleaseClient client = new GitHubReleaseClient(server.urlFor("/releases"));
            assertEquals(List.of(), client.listRecentReleases());
        }
    }

    @Test
    void aNon200ResponseThrows() throws Exception {
        try (TestHttpServer server = new TestHttpServer()) {
            server.respond("/releases", 500, "server error", "text/plain");
            GitHubReleaseClient client = new GitHubReleaseClient(server.urlFor("/releases"));

            org.junit.jupiter.api.Assertions.assertThrows(java.io.IOException.class, client::listRecentReleases);
        }
    }

    @Test
    void downloadReturnsRawBytes() throws Exception {
        try (TestHttpServer server = new TestHttpServer()) {
            server.respond("/asset.bin", 200, "hello velofine", "application/octet-stream");
            GitHubReleaseClient client = new GitHubReleaseClient(server.urlFor("/releases"));

            byte[] bytes = client.download(server.urlFor("/asset.bin"));
            assertArrayEquals("hello velofine".getBytes(StandardCharsets.UTF_8), bytes);
        }
    }

    @Test
    void downloadToFileStreamsToDiskAndReportsProgress(@TempDir Path tempDir) throws Exception {
        try (TestHttpServer server = new TestHttpServer()) {
            String payload = "x".repeat(200_000);
            server.respond("/installer.exe", 200, payload, "application/octet-stream");
            GitHubReleaseClient client = new GitHubReleaseClient(server.urlFor("/releases"));

            AtomicInteger lastPercent = new AtomicInteger(-1);
            Path dest = tempDir.resolve("installer.exe");
            client.downloadToFile(server.urlFor("/installer.exe"), dest, lastPercent::set);

            assertEquals(payload, Files.readString(dest, StandardCharsets.UTF_8));
            assertEquals(100, lastPercent.get(), "progress should reach 100% once the full body is written");
        }
    }

    /**
     * Regression test for the v1.7-Beta field failure: a real tester log showed
     * {@code Update check failed: java.io.IOException: Download failed for
     * https://github.com/.../releases/download/v1.7-Beta/manifest.json: HTTP 302}. Java's
     * {@code HttpClient} defaults to {@code Redirect.NEVER}, and every GitHub release-asset URL
     * 302s to {@code objects.githubusercontent.com} - so the in-app updater had never successfully
     * downloaded anything on any shipped release. Fails on pre-fix code with that exact message.
     */
    @Test
    void downloadFollowsA302ToTheRealAssetHost() throws Exception {
        try (TestHttpServer server = new TestHttpServer()) {
            server.respondRedirect("/releases/download/manifest.json", "/objects/manifest.json");
            server.respond("/objects/manifest.json", 200, "{\"velofineVersion\":\"1.8-Beta\"}", "application/json");
            GitHubReleaseClient client = new GitHubReleaseClient(server.urlFor("/releases"));

            byte[] bytes = client.download(server.urlFor("/releases/download/manifest.json"));

            assertArrayEquals("{\"velofineVersion\":\"1.8-Beta\"}".getBytes(StandardCharsets.UTF_8), bytes);
        }
    }

    /** Same redirect path for the streaming download - the installer .exe is the one users feel. */
    @Test
    void downloadToFileFollowsA302(@TempDir Path tempDir) throws Exception {
        try (TestHttpServer server = new TestHttpServer()) {
            String payload = "installer-bytes";
            server.respondRedirect("/releases/download/Velofine-Setup.exe", "/objects/Velofine-Setup.exe");
            server.respond("/objects/Velofine-Setup.exe", 200, payload, "application/octet-stream");
            GitHubReleaseClient client = new GitHubReleaseClient(server.urlFor("/releases"));

            Path dest = tempDir.resolve("Velofine-Setup.exe");
            client.downloadToFile(server.urlFor("/releases/download/Velofine-Setup.exe"), dest, percent -> { });

            assertEquals(payload, Files.readString(dest, StandardCharsets.UTF_8));
        }
    }
}
