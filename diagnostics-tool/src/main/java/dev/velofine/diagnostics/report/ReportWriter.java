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
import com.google.gson.GsonBuilder;
import dev.velofine.diagnostics.model.DiagnosticReport;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Writes a {@link DiagnosticReport} as pretty-printed, timestamped JSON. Mirrors the atomic-write
 * pattern {@code core.config.ConfigManager} already establishes elsewhere in this project (temp
 * file + atomic move) - reimplemented standalone here rather than depended on, per this module's
 * zero-Velofine-module-dependency decision.
 */
public final class ReportWriter {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private ReportWriter() {
    }

    /**
     * A single {@code yyyy-MM-dd-HHmm} timestamp string, computed once per run - callers that also
     * need to name a sibling artifact after the same run (e.g. {@code report.ShaderSourceArchiver}'s
     * per-run shader folder) should call this once and pass the result to both, rather than each
     * independently calling {@code now()} and risking a folder/filename mismatch across a minute
     * boundary.
     */
    public static String newTimestamp() {
        return TIMESTAMP_FORMAT.format(LocalDateTime.now());
    }

    /** Writes {@code velofine-diagnosis-<timestamp>.json} into {@code outputDir}, atomically. */
    public static Path write(DiagnosticReport report, Path outputDir) throws IOException {
        return write(report, outputDir, newTimestamp());
    }

    /** Same as {@link #write(DiagnosticReport, Path)} but with an explicit, pre-computed timestamp. */
    public static Path write(DiagnosticReport report, Path outputDir, String timestamp) throws IOException {
        Files.createDirectories(outputDir);

        String filename = "velofine-diagnosis-" + timestamp + ".json";
        Path target = outputDir.resolve(filename);
        Path temp = outputDir.resolve(filename + ".tmp");

        Files.writeString(temp, GSON.toJson(report), StandardCharsets.UTF_8);
        try {
            Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return target;
    }
}
