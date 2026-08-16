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
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ShaderSourceArchiverTest {

    @Test
    void persistsFileWithExpectedNameAndContent(@TempDir Path baseDir) throws IOException {
        Path shaderDir = baseDir.resolve("shaders");
        String source = "#version 330\nvoid main() {}\n";

        ShaderSourceArchiver.ArchivedSource archived = ShaderSourceArchiver.persist(shaderDir, "terrain", "fragment", "default", source);

        Path written = shaderDir.resolve("terrain.fragment.default.glsl");
        assertTrue(Files.isRegularFile(written));
        assertEquals(source, Files.readString(written));
        assertEquals("shaders/terrain.fragment.default.glsl", archived.relativePath());
    }

    @Test
    void sha256IsDeterministicAndKnownAnswer() {
        // Known SHA-256("") test vector - confirms the digest is wired correctly, not just "runs".
        String hash = ShaderSourceArchiver.sha256Hex("");
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", hash);
    }

    @Test
    void differentSourceProducesDifferentHash() {
        String a = ShaderSourceArchiver.sha256Hex("#version 330\n");
        String b = ShaderSourceArchiver.sha256Hex("#version 330\nvoid main() {}\n");
        assertTrue(!a.equals(b));
    }
}
