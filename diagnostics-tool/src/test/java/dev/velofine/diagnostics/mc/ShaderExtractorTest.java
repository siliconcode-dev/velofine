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

import dev.velofine.diagnostics.model.ShaderInventoryEntry;
import dev.velofine.diagnostics.shader.ImportResolutionException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ShaderExtractorTest {

    private Path buildFakeClientJar(Path dir) throws IOException {
        Path jar = dir.resolve("fake-client.jar");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(jar))) {
            writeEntry(zip, "assets/minecraft/shaders/core/terrain.fsh", "#version 330\nvoid main() {}\n");
            writeEntry(zip, "assets/minecraft/shaders/core/terrain.vsh", "#version 330\nvoid main() {}\n");
            writeEntry(zip, "assets/minecraft/shaders/core/rendertype_end_portal.fsh", "#version 330\nvoid main() {}\n");
            writeEntry(zip, "assets/minecraft/shaders/core/screenquad.vsh", "#version 330\nvoid main() {}\n");
            writeEntry(zip, "assets/minecraft/shaders/include/fog.glsl", "#version 330\nfloat fog() { return 1.0; }\n");
            writeEntry(zip, "assets/minecraft/shaders/core/shared_helper.glsl", "#version 330\nfloat helper() { return 1.0; }\n");
        }
        return jar;
    }

    private void writeEntry(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    @Test
    void discoversEveryRealShaderPairNotAHardcodedSubset(@TempDir Path dir) throws IOException {
        Path jar = buildFakeClientJar(dir);

        try (ShaderExtractor extractor = ShaderExtractor.open(jar)) {
            List<ShaderInventoryEntry> inventory = extractor.discoverCoreShaders();

            assertEquals(3, inventory.size());
            assertTrue(inventory.stream().anyMatch(e -> e.name().equals("terrain") && e.hasVertex() && e.hasFragment()));
            assertTrue(inventory.stream().anyMatch(e -> e.name().equals("rendertype_end_portal") && !e.hasVertex() && e.hasFragment()));
            assertTrue(inventory.stream().anyMatch(e -> e.name().equals("screenquad") && e.hasVertex() && !e.hasFragment()));
        }
    }

    @Test
    void flaggedShadersAreSortedFirst(@TempDir Path dir) throws IOException {
        Path jar = buildFakeClientJar(dir);

        try (ShaderExtractor extractor = ShaderExtractor.open(jar)) {
            List<ShaderInventoryEntry> inventory = extractor.discoverCoreShaders();

            assertTrue(inventory.get(0).flagged());
            assertTrue(inventory.get(1).flagged());
            assertFalse(inventory.get(2).flagged());
        }
    }

    @Test
    void missingShaderReturnsEmptyRatherThanThrowing(@TempDir Path dir) throws IOException {
        Path jar = buildFakeClientJar(dir);

        try (ShaderExtractor extractor = ShaderExtractor.open(jar)) {
            Optional<String> missing = extractor.readCoreShaderSource("does_not_exist", "fsh");
            assertTrue(missing.isEmpty());

            Optional<String> present = extractor.readCoreShaderSource("terrain", "fsh");
            assertTrue(present.isPresent());
        }
    }

    @Test
    void readAngleImportResolvesFromIncludeDirectory(@TempDir Path dir) throws IOException, ImportResolutionException {
        Path jar = buildFakeClientJar(dir);

        try (ShaderExtractor extractor = ShaderExtractor.open(jar)) {
            String content = extractor.readAngleImport("minecraft", "fog.glsl");
            assertTrue(content.contains("float fog()"));
        }
    }

    @Test
    void readQuotedImportResolvesFromCoreDirectory(@TempDir Path dir) throws IOException, ImportResolutionException {
        Path jar = buildFakeClientJar(dir);

        try (ShaderExtractor extractor = ShaderExtractor.open(jar)) {
            String content = extractor.readQuotedImport("shared_helper.glsl");
            assertTrue(content.contains("float helper()"));
        }
    }

    @Test
    void readAngleImportThrowsForMissingFile(@TempDir Path dir) throws IOException {
        Path jar = buildFakeClientJar(dir);

        try (ShaderExtractor extractor = ShaderExtractor.open(jar)) {
            assertThrows(ImportResolutionException.class, () -> extractor.readAngleImport("minecraft", "nope.glsl"));
        }
    }
}
