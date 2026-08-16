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

package dev.velofine.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * The one true answer to "what version am I" - generated at build time by {@code core}'s
 * {@code processResources} task from {@code project.version} (see {@code core/build.gradle.kts}),
 * not a second hardcoded literal. Before Phase 8, {@code ProfileInstaller} carried its own
 * independent {@code VELOFINE_VERSION} constant that could silently drift from the root Gradle
 * version; that duplicate is gone, and both {@code ProfileInstaller} and the updater now read this.
 *
 * <p>{@link #targetMcVersion()} is what makes the updater's "which release track am I on" check
 * possible - it is written into {@code VelofineConfig.UpdaterSection.trackedMcVersion} by
 * {@code ProfileInstaller.install()} at install time (the one place the actually-targeted vanilla
 * version is known), and compared against each release's {@code manifest.json}.
 */
public final class BuildInfo {

    private static final String VELOFINE_VERSION;
    private static final String TARGET_MC_VERSION;

    static {
        Properties props = new Properties();
        try (InputStream in = BuildInfo.class.getResourceAsStream("/velofine-build-info.properties")) {
            if (in == null) {
                throw new IllegalStateException(
                        "velofine-build-info.properties missing from the classpath - core's "
                                + "processResources task should always generate it from project.version");
            }
            props.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read velofine-build-info.properties", e);
        }
        VELOFINE_VERSION = props.getProperty("velofineVersion", "0.0.0");
        TARGET_MC_VERSION = props.getProperty("targetMcVersion", "unknown");
    }

    private BuildInfo() {
    }

    public static String velofineVersion() {
        return VELOFINE_VERSION;
    }

    public static String targetMcVersion() {
        return TARGET_MC_VERSION;
    }

    /** Masterdoc 1's display scheme: {@code "Velofine P<patch> MC <version>"}, e.g. "Velofine 0.1.0 MC 26.2". */
    public static String displayString() {
        return "Velofine " + VELOFINE_VERSION + " MC " + TARGET_MC_VERSION;
    }
}
