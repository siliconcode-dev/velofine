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

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import dev.velofine.core.BuildInfo;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * Thin {@code java.net.http.HttpClient} wrapper over GitHub's public Releases API - metadata only,
 * no user data sent, consistent with CLAUDE.md's explicit no-telemetry carve-out for update checks.
 * No auth token: public-repo unauthenticated reads are rate-limited to 60/hr, comfortably above
 * what an individual client's rate-limited (~24h) auto-check plus occasional manual checks needs.
 */
final class GitHubReleaseClient {

    private static final String DEFAULT_RELEASES_URL =
            "https://api.github.com/repos/siliconcode-dev/velofine/releases?per_page=10";

    private static final Gson GSON = new Gson();

    // followRedirects is load-bearing, not boilerplate: HttpClient defaults to Redirect.NEVER, and
    // every GitHub release-asset URL (github.com/.../releases/download/...) 302s to
    // objects.githubusercontent.com. Since download()/downloadToFile() both reject anything != 200,
    // the in-app updater could never fetch a manifest or installer on ANY shipped release - confirmed
    // by a real v1.7-Beta tester log ("Update check failed: ... manifest.json: HTTP 302").
    // NORMAL rather than ALWAYS: it follows HTTPS->HTTPS but refuses an HTTPS->HTTP downgrade, which
    // is exactly the right posture for a project that ships unsigned binaries and leans on the
    // SHA-256 + Ed25519 chain instead.
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private final String releasesUrl;

    GitHubReleaseClient() {
        this(DEFAULT_RELEASES_URL);
    }

    /** Package-visible so {@code GitHubReleaseClientTest} can point this at a local HTTP stub. */
    GitHubReleaseClient(String releasesUrl) {
        this.releasesUrl = releasesUrl;
    }

    List<GitHubRelease> listRecentReleases() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(releasesUrl))
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("User-Agent", "Velofine-Updater/" + BuildInfo.velofineVersion())
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("GitHub releases request failed: HTTP " + response.statusCode());
        }

        GitHubRelease[] releases = GSON.fromJson(response.body(), GitHubRelease[].class);
        return releases != null ? List.of(releases) : List.of();
    }

    byte[] download(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", "Velofine-Updater/" + BuildInfo.velofineVersion())
                .timeout(Duration.ofMinutes(5))
                .GET()
                .build();
        HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200) {
            throw new IOException("Download failed for " + url + ": HTTP " + response.statusCode());
        }
        return response.body();
    }

    /** Streams to {@code dest}, reporting whole-percent progress - used for the multi-MB installer exe. */
    void downloadToFile(String url, Path dest, IntConsumer percentCallback) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", "Velofine-Updater/" + BuildInfo.velofineVersion())
                .timeout(Duration.ofMinutes(5))
                .GET()
                .build();
        HttpResponse<InputStream> response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            throw new IOException("Download failed for " + url + ": HTTP " + response.statusCode());
        }

        long contentLength = response.headers().firstValueAsLong("Content-Length").orElse(-1L);
        Path temp = dest.resolveSibling(dest.getFileName() + ".part");
        try (InputStream in = response.body()) {
            Files.createDirectories(dest.getParent());
            try (var out = Files.newOutputStream(temp)) {
                byte[] buffer = new byte[64 * 1024];
                long totalRead = 0;
                int lastReportedPercent = -1;
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                    totalRead += read;
                    if (contentLength > 0) {
                        int percent = (int) Math.min(100, totalRead * 100 / contentLength);
                        if (percent != lastReportedPercent) {
                            lastReportedPercent = percent;
                            percentCallback.accept(percent);
                        }
                    }
                }
            }
        }
        Files.move(temp, dest, StandardCopyOption.REPLACE_EXISTING);
    }

    /** One entry from {@code GET /repos/{owner}/{repo}/releases} - only the fields Velofine needs. */
    static final class GitHubRelease {
        @SerializedName("tag_name")
        String tagName;
        boolean draft;
        boolean prerelease;
        List<Asset> assets;

        String assetUrl(String filename) {
            if (assets == null) {
                return null;
            }
            for (Asset asset : assets) {
                if (filename.equals(asset.name)) {
                    return asset.browserDownloadUrl;
                }
            }
            return null;
        }

        static final class Asset {
            String name;
            @SerializedName("browser_download_url")
            String browserDownloadUrl;
        }
    }
}
