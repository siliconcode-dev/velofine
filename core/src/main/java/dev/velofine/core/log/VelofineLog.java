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

package dev.velofine.core.log;

/**
 * Standardizes the {@code "[Velofine][<engine>] ..."} console-log prefix used throughout the
 * codebase for tester-facing fix-engagement confirmation (per CLAUDE.md: testers without profiling
 * tools confirm fixes via plain log output). No log levels/files/framework — Velofine's whole
 * logging need today is "print a clearly-prefixed line to stdout/stderr."
 */
public final class VelofineLog {

    private VelofineLog() {
    }

    public static void info(String engine, String message) {
        System.out.println("[Velofine][" + engine + "] " + message);
    }

    public static void warn(String engine, String message) {
        System.err.println("[Velofine][" + engine + "] " + message);
    }
}
