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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ChecksumVerifierTest {

    private final ChecksumVerifier verifier = new ChecksumVerifier();

    @Test
    void matchingHashVerifies(@TempDir Path dir) throws IOException, NoSuchAlgorithmException {
        Path installer = dir.resolve("Velofine-Setup-1.0.0-Beta.exe");
        Files.write(installer, "pretend installer bytes".getBytes(StandardCharsets.UTF_8));

        Path checksums = dir.resolve("SHA256SUMS.txt");
        Files.writeString(checksums, sha256Hex(installer) + "  Velofine-Setup-1.0.0-Beta.exe\n");

        assertTrue(verifier.verify(installer, checksums));
    }

    @Test
    void tamperedInstallerFailsVerification(@TempDir Path dir) throws IOException, NoSuchAlgorithmException {
        Path installer = dir.resolve("Velofine-Setup-1.0.0-Beta.exe");
        Files.write(installer, "pretend installer bytes".getBytes(StandardCharsets.UTF_8));

        Path checksums = dir.resolve("SHA256SUMS.txt");
        Files.writeString(checksums, sha256Hex(installer) + "  Velofine-Setup-1.0.0-Beta.exe\n");

        // Simulates a corrupted/tampered download landing after the checksums file was fetched.
        Files.write(installer, "tampered bytes".getBytes(StandardCharsets.UTF_8));

        assertFalse(verifier.verify(installer, checksums));
    }

    @Test
    void aChecksumsFileWithNoMatchingFilenameFails(@TempDir Path dir) throws IOException {
        Path installer = dir.resolve("Velofine-Setup-1.0.0-Beta.exe");
        Files.write(installer, "bytes".getBytes(StandardCharsets.UTF_8));

        Path checksums = dir.resolve("SHA256SUMS.txt");
        Files.writeString(checksums, "deadbeef  SomeOtherFile.exe\n");

        assertFalse(verifier.verify(installer, checksums));
    }

    @Test
    void singleSpaceFormIsAlsoAccepted(@TempDir Path dir) throws IOException, NoSuchAlgorithmException {
        // Real sha256sum output uses two spaces (text mode); accept a single space too.
        Path installer = dir.resolve("Velofine-Setup-1.0.0-Beta.exe");
        Files.write(installer, "pretend installer bytes".getBytes(StandardCharsets.UTF_8));

        Path checksums = dir.resolve("SHA256SUMS.txt");
        Files.writeString(checksums, sha256Hex(installer) + " Velofine-Setup-1.0.0-Beta.exe\n");

        assertTrue(verifier.verify(installer, checksums));
    }

    private static String sha256Hex(Path file) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(Files.readAllBytes(file)));
    }
}
