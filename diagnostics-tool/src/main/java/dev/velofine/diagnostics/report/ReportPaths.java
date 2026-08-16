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

package dev.velofine.diagnostics.report;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Resolves where reports get written: a {@code reports/} folder sitting next to the running
 * jar/exe, so a tester can find them "right next to the tool" without hunting through
 * {@code %APPDATA%} - this tool saves locally only, no network calls, ever.
 */
public final class ReportPaths {

    private ReportPaths() {
    }

    public static Path reportsDirectory() {
        return runningLocation().resolve("reports");
    }

    private static Path runningLocation() {
        try {
            Path codeSource = Paths.get(ReportPaths.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            return codeSource.toFile().isDirectory() ? codeSource : codeSource.getParent();
        } catch (URISyntaxException | NullPointerException e) {
            return Paths.get(System.getProperty("user.dir"));
        }
    }
}
