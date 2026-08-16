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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Checks a downloaded installer's SHA-256 against the matching line in the release's
 * {@code SHA256SUMS.txt} (standard {@code sha256sum}-compatible {@code <hex>␣␣<filename>} format).
 * This alone only proves the download matches what the release published - {@link
 * SignatureVerifier} is what proves the release itself is genuine; both must pass.
 */
final class ChecksumVerifier {

    boolean verify(Path installerExe, Path checksumsFile) throws IOException {
        String expectedHex = findHash(checksumsFile, installerExe.getFileName().toString());
        if (expectedHex == null) {
            return false;
        }
        String actualHex = sha256Hex(installerExe);
        return expectedHex.equalsIgnoreCase(actualHex);
    }

    private static String findHash(Path checksumsFile, String filename) throws IOException {
        for (String line : Files.readAllLines(checksumsFile, StandardCharsets.UTF_8)) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            // "<hex>  <filename>" (two spaces, or one - both real sha256sum output forms) or
            // "<hex> *<filename>" (binary-mode marker).
            String[] parts = trimmed.split("\\s+\\*?", 2);
            if (parts.length == 2 && parts[1].trim().equals(filename)) {
                return parts[0].trim();
            }
        }
        return null;
    }

    private static String sha256Hex(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (var in = Files.newInputStream(file)) {
                byte[] buffer = new byte[64 * 1024];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is a JDK-guaranteed MessageDigest algorithm", e);
        }
    }
}
