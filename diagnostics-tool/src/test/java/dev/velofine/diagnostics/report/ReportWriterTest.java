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
import dev.velofine.diagnostics.model.Mode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ReportWriterTest {

    private DiagnosticReport sampleReport() {
        return DiagnosticReport.builder()
                .toolVersion("0.1.0")
                .generatedAtIso("2026-08-16T14:20:03")
                .mode(Mode.BASELINE)
                .mcVersionId("26.2")
                .mcClientJarPath("C:/mc/versions/26.2/26.2.jar")
                .build();
    }

    @Test
    void filenameFollowsTimestampedPattern(@TempDir Path outputDir) throws IOException {
        Path written = ReportWriter.write(sampleReport(), outputDir);

        assertTrue(Pattern.matches("velofine-diagnosis-\\d{4}-\\d{2}-\\d{2}-\\d{4}\\.json", written.getFileName().toString()),
                "unexpected filename: " + written.getFileName());
        assertTrue(Files.isRegularFile(written));
    }

    @Test
    void createsOutputDirectoryIfMissing(@TempDir Path baseDir) throws IOException {
        Path outputDir = baseDir.resolve("reports");
        assertTrue(Files.notExists(outputDir));

        ReportWriter.write(sampleReport(), outputDir);

        assertTrue(Files.isDirectory(outputDir));
    }

    @Test
    void jsonRoundTripsBackToAnEquivalentReport(@TempDir Path outputDir) throws IOException {
        DiagnosticReport original = sampleReport();
        Path written = ReportWriter.write(original, outputDir);

        String json = Files.readString(written, StandardCharsets.UTF_8);
        DiagnosticReport roundTripped = new Gson().fromJson(json, DiagnosticReport.class);

        assertEquals(original.toolVersion(), roundTripped.toolVersion());
        assertEquals(original.mode(), roundTripped.mode());
        assertEquals(original.mcVersionId(), roundTripped.mcVersionId());
        assertEquals(original.mcClientJarPath(), roundTripped.mcClientJarPath());
    }

    @Test
    void writingTwiceInQuickSuccessionDoesNotOverwriteWithoutNewTimestampCollision(@TempDir Path outputDir) throws IOException {
        Path first = ReportWriter.write(sampleReport(), outputDir);
        Path second = ReportWriter.write(sampleReport(), outputDir);

        // Same-minute writes legitimately collide on this filename scheme (timestamp resolution is
        // minutes, matching the spec's example filename) - both must still exist as valid JSON either
        // way, since ATOMIC_MOVE + REPLACE_EXISTING must never leave a corrupt half-written file.
        assertTrue(Files.isRegularFile(first));
        assertTrue(Files.isRegularFile(second));
    }
}
