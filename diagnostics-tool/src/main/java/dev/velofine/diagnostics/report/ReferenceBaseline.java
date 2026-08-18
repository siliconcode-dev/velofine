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

import com.google.gson.Gson;
import dev.velofine.diagnostics.model.DiagnosticReport;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Loads the bundled "known-good" reference report, captured once via a real {@code diagnostic.exe}
 * BASELINE run on non-Intel-Gen7 hardware (this bug is architecture-specific, so any modern GPU
 * qualifies as "known-good" for this purpose) and checked in at
 * {@value #RESOURCE_PATH} - see this class's own javadoc note below on why it may be absent.
 *
 * <p>{@link #load()} makes every run's {@code ResultsScreen} able to auto-diff against this
 * reference via {@code report.ReportComparator}, turning "compiled/linked/drew without a crash"
 * into an actual "does this look like normal hardware" verdict - the gap a real tester report
 * exposed (see this class's git history / the v1.5 diagnostic-tool rework plan). Returns
 * {@link Optional#empty()}, not an exception, when the resource is missing - capturing the real
 * reference requires a live GL context on real non-buggy hardware, which isn't always available
 * (e.g. a fresh checkout before anyone has run the capture step) and its absence must degrade
 * gracefully, not break every other run.
 */
public final class ReferenceBaseline {

    private static final String RESOURCE_PATH = "/reference/velofine-reference-26.2.json";
    private static final Gson GSON = new Gson();

    private ReferenceBaseline() {
    }

    public static Optional<DiagnosticReport> load() {
        try (InputStream in = ReferenceBaseline.class.getResourceAsStream(RESOURCE_PATH)) {
            if (in == null) {
                return Optional.empty();
            }
            try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return Optional.ofNullable(GSON.fromJson(reader, DiagnosticReport.class));
            }
        } catch (IOException | RuntimeException e) {
            // A malformed/unreadable bundled reference must not break an otherwise-good run - same
            // "degrade, don't crash" precedent ConfigManager already sets for a corrupt config.json.
            return Optional.empty();
        }
    }
}
