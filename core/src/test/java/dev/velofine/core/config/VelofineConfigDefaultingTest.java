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

package dev.velofine.core.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Confirms {@link VelofineConfig}'s core design guarantee (see its own class javadoc): a
 * {@code config.json} written by an older Velofine version, missing an entire section a later
 * version added (e.g. {@code updater}, which didn't exist before Phase 8), must still populate
 * that section's field-initializer defaults rather than leaving it {@code null} - Gson only calls
 * the no-arg constructor and lets field initializers run for anything absent from the JSON.
 */
final class VelofineConfigDefaultingTest {

    @Test
    void aConfigFileMissingAWholeNewerSectionStillGetsItsDefaults(@TempDir Path gameDir) throws IOException {
        Path configDir = gameDir.resolve("velofine");
        Files.createDirectories(configDir);
        // A plausible "Phase 6 era" config.json: real content, but with no "updater" key at all -
        // that section did not exist until Phase 8.
        String oldConfigJson = """
                {
                  "configVersion": 1,
                  "engines": { "legacySupport": false, "optimus": true, "utility": true }
                }
                """;
        Files.writeString(configDir.resolve("config.json"), oldConfigJson, StandardCharsets.UTF_8);

        ConfigManager.load(gameDir);
        VelofineConfig config = ConfigManager.get();

        assertFalseEngine(config);
        assertNotNull(config.updater, "a missing section must default via its field initializer, not be null");
        assertTrue(config.updater.autoCheckOnLaunch, "the section's own field defaults should apply");
        assertEquals(0L, config.updater.lastCheckedEpochMillis);
        assertNotNull(config.utility.shader, "same guarantee for a section that predates updater but postdates this hand-authored JSON");
    }

    private static void assertFalseEngine(VelofineConfig config) {
        assertEquals(false, config.engines.legacySupport, "values actually present in the file should still be honored");
    }
}
