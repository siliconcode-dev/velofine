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

package dev.velofine.diagnostics.mc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class McInstallationScannerTest {

    private void createFakeVersion(Path mcDir, String versionId) throws IOException {
        Path versionDir = mcDir.resolve("versions").resolve(versionId);
        Files.createDirectories(versionDir);
        Files.writeString(versionDir.resolve(versionId + ".jar"), "not a real jar");
    }

    @Test
    void isValidInstallationRequiresAVersionsFolder(@TempDir Path mcDir) throws IOException {
        assertFalse(McInstallationScanner.isValidInstallation(mcDir));
        Files.createDirectories(mcDir.resolve("versions"));
        assertTrue(McInstallationScanner.isValidInstallation(mcDir));
    }

    @Test
    void preferredVersionIsSortedFirst(@TempDir Path mcDir) throws IOException {
        createFakeVersion(mcDir, "1.19.4");
        createFakeVersion(mcDir, "26.2");
        createFakeVersion(mcDir, "24.1");

        List<McVersionEntry> versions = McInstallationScanner.scan(mcDir);

        assertEquals(3, versions.size());
        assertEquals("26.2", versions.get(0).versionId());
    }

    @Test
    void versionDirectoryWithoutMatchingJarIsSkipped(@TempDir Path mcDir) throws IOException {
        Files.createDirectories(mcDir.resolve("versions").resolve("incomplete"));
        createFakeVersion(mcDir, "26.2");

        List<McVersionEntry> versions = McInstallationScanner.scan(mcDir);

        assertEquals(1, versions.size());
        assertEquals("26.2", versions.get(0).versionId());
    }

    @Test
    void emptyVersionsDirectoryReturnsEmptyList(@TempDir Path mcDir) throws IOException {
        Files.createDirectories(mcDir.resolve("versions"));

        List<McVersionEntry> versions = McInstallationScanner.scan(mcDir);

        assertTrue(versions.isEmpty());
    }

    @Test
    void missingVersionsDirectoryReturnsEmptyListRatherThanThrowing(@TempDir Path mcDir) throws IOException {
        List<McVersionEntry> versions = McInstallationScanner.scan(mcDir);

        assertTrue(versions.isEmpty());
    }
}
