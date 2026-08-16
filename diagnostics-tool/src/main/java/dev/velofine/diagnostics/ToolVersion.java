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

package dev.velofine.diagnostics;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * This tool's own independent version (see {@code build.gradle.kts}'s {@code version = "0.1.0"}),
 * separate from Velofine's own release line. Read from a build-time-templated properties file
 * rather than a second hardcoded literal in source - mirrors {@code core.BuildInfo}'s pattern.
 */
public final class ToolVersion {

    private static final String VERSION = load();

    private ToolVersion() {
    }

    public static String version() {
        return VERSION;
    }

    private static String load() {
        Properties props = new Properties();
        try (InputStream in = ToolVersion.class.getResourceAsStream("/diagnostic-build-info.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException ignored) {
            // Falls through to the default below - a missing/unreadable resource must not crash the tool.
        }
        return props.getProperty("toolVersion", "0.0.0-dev");
    }
}
