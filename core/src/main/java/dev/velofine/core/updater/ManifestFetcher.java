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
import dev.velofine.core.updater.GitHubReleaseClient.GitHubRelease;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Downloads and parses one release's {@code manifest.json} asset (see {@link UpdateManifest}). */
final class ManifestFetcher {

    private static final Gson GSON = new Gson();

    private final GitHubReleaseClient client;

    ManifestFetcher(GitHubReleaseClient client) {
        this.client = client;
    }

    /** @return the parsed manifest, or {@code null} if this release has none (e.g. an unrelated tag). */
    UpdateManifest fetch(GitHubRelease release) throws IOException, InterruptedException {
        String url = release.assetUrl(ReleaseAssets.MANIFEST);
        if (url == null) {
            return null;
        }
        byte[] bytes = client.download(url);
        return GSON.fromJson(new String(bytes, StandardCharsets.UTF_8), UpdateManifest.class);
    }
}
