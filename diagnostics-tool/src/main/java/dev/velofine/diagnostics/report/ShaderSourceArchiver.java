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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Persists each shader's fully-resolved GLSL (post-{@code #moj_import}, post-define-injection) to
 * disk next to the report, and computes SHA-256 checksums - lets a human inspect exactly what text
 * was fed to the driver on a failing machine without reconstructing it by hand, and lets two
 * testers' reports be confirmed as testing byte-identical input.
 */
public final class ShaderSourceArchiver {

    private ShaderSourceArchiver() {
    }

    /** One persisted shader source: where it was written, and its SHA-256 (hex, lowercase). */
    public record ArchivedSource(String relativePath, String sha256Hex) {
    }

    /**
     * Writes {@code <reportShaderDir>/<name>.<stage>.<variant>.glsl} and returns its path (relative
     * to {@code reportShaderDir}'s parent, i.e. the report directory) plus its checksum.
     */
    public static ArchivedSource persist(Path reportShaderDir, String shaderName, String stage, String variant, String resolvedSource)
            throws IOException {
        Files.createDirectories(reportShaderDir);
        String fileName = shaderName + "." + stage + "." + variant + ".glsl";
        Path target = reportShaderDir.resolve(fileName);
        Files.writeString(target, resolvedSource, StandardCharsets.UTF_8);

        String relativePath = reportShaderDir.getFileName() + "/" + fileName;
        return new ArchivedSource(relativePath, sha256Hex(resolvedSource));
    }

    /** Checksums a source string without writing it anywhere - used for the raw-extracted source too. */
    public static String sha256Hex(String source) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(source.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed present on every JDK per the Java Cryptography spec - this is
            // unreachable in practice, but a checked exception still needs a defined behavior.
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
