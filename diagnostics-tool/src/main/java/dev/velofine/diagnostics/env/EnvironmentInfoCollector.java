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

package dev.velofine.diagnostics.env;

import dev.velofine.diagnostics.model.EnvironmentInfo;

/**
 * Captures JVM/LWJGL/heap facts about the running tool itself - no PowerShell/WMI needed, purely
 * `System`/`Runtime`/LWJGL properties. Cheap, and helps triage which build a tester's report came
 * from without a separate back-and-forth.
 */
public final class EnvironmentInfoCollector {

    private EnvironmentInfoCollector() {
    }

    public static EnvironmentInfo collect() {
        Runtime runtime = Runtime.getRuntime();
        return new EnvironmentInfo(
                System.getProperty("java.version"),
                System.getProperty("java.vendor"),
                org.lwjgl.Version.getVersion(),
                System.getProperty("os.arch"),
                runtime.availableProcessors(),
                runtime.maxMemory());
    }
}
