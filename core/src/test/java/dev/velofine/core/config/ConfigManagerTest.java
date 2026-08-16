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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ConfigManagerTest {

    @Test
    void missingConfigWritesFirstRunDefaults(@TempDir Path gameDir) {
        ConfigManager.load(gameDir);

        Path expected = gameDir.resolve("velofine").resolve("config.json");
        assertTrue(Files.isRegularFile(expected), "first-run defaults should be written to disk immediately");
        assertTrue(ConfigManager.get().engines.legacySupport, "field-initializer defaults should apply");
    }

    @Test
    void saveThenLoadRoundTripsEditedValues(@TempDir Path gameDir) {
        ConfigManager.load(gameDir);
        ConfigManager.get().engines.optimus = false;
        ConfigManager.get().ui.openConfigKey = 66;
        ConfigManager.save();

        ConfigManager.load(gameDir);
        assertFalse(ConfigManager.get().engines.optimus);
        assertEquals(66, ConfigManager.get().ui.openConfigKey);
    }

    @Test
    void malformedConfigIsQuarantinedAndDefaultsAreUsedInstead(@TempDir Path gameDir) throws IOException {
        Path configDir = gameDir.resolve("velofine");
        Files.createDirectories(configDir);
        Path configFile = configDir.resolve("config.json");
        Files.writeString(configFile, "{ not valid json ][", StandardCharsets.UTF_8);

        ConfigManager.load(gameDir);

        assertTrue(Files.isRegularFile(configDir.resolve("config.json.bak")), "the bad file should be moved aside");
        assertTrue(Files.isRegularFile(configFile), "a fresh default config should be written in its place");
        assertTrue(ConfigManager.get().engines.legacySupport, "should have fallen back to defaults, not crashed");
    }

    @Test
    void emptyJsonNullIsTreatedAsMalformed(@TempDir Path gameDir) throws IOException {
        Path configDir = gameDir.resolve("velofine");
        Files.createDirectories(configDir);
        Files.writeString(configDir.resolve("config.json"), "null", StandardCharsets.UTF_8);

        ConfigManager.load(gameDir);

        assertTrue(Files.isRegularFile(configDir.resolve("config.json.bak")));
        assertTrue(ConfigManager.get().engines.legacySupport);
    }

    @Test
    void nullGameDirRunsInMemoryOnlyAndSaveIsANoOp() {
        ConfigManager.load(null);
        ConfigManager.get().engines.utility = false;
        ConfigManager.save(); // must not throw despite there being nowhere to write

        assertFalse(ConfigManager.get().engines.utility);
    }

    @Test
    void copyOfLiveIsDetachedFromTheLiveConfig(@TempDir Path gameDir) {
        ConfigManager.load(gameDir);
        VelofineConfig copy = ConfigManager.copyOfLive();
        copy.engines.optimus = false;

        assertTrue(ConfigManager.get().engines.optimus, "editing the detached copy must not affect the live config");
    }

    @Test
    void replaceAndSavePromotesAndPersistsTheEditedCopy(@TempDir Path gameDir) {
        ConfigManager.load(gameDir);
        VelofineConfig copy = ConfigManager.copyOfLive();
        copy.ui.openConfigKey = 77;

        ConfigManager.replaceAndSave(copy);
        assertEquals(77, ConfigManager.get().ui.openConfigKey);

        ConfigManager.load(gameDir);
        assertEquals(77, ConfigManager.get().ui.openConfigKey, "should have been persisted to disk too");
    }
}
