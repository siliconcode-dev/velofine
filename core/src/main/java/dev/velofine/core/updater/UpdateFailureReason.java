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

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.nio.file.NoSuchFileException;

/**
 * v1.5 settings-UI overhaul (item 12): classifies a caught updater exception into a short,
 * plain-language reason for {@code LiveStatus}/{@code UpdaterPage} - before this,
 * {@code UpdateOrchestrator} interpolated {@code e.getMessage()} directly (e.g.
 * {@code GitHubReleaseClient}'s own "Download failed for &lt;url&gt;: HTTP 404"), putting a raw
 * URL and status code directly in front of the player. The full technical string is still kept
 * (see {@code LiveStatus.updaterDetail()}), just no longer as the primary, always-visible message.
 */
public final class UpdateFailureReason {

    private UpdateFailureReason() {
    }

    public static String friendly(Throwable t) {
        if (t instanceof UnknownHostException || t instanceof ConnectException) {
            return "Network connection failed.";
        }
        String message = t.getMessage();
        if (message != null && message.contains("HTTP")) {
            return "The update server returned an error.";
        }
        if (t instanceof NoSuchFileException) {
            return "A required file was missing.";
        }
        if (t instanceof java.io.IOException) {
            return "A network or file error occurred.";
        }
        if (t instanceof SecurityException) {
            return "The downloaded update could not be verified.";
        }
        return "An unexpected error occurred.";
    }
}
