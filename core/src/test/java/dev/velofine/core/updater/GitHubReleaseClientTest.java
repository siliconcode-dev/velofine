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
}
