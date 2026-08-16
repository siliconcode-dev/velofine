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

package dev.velofine.launcher;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import dev.velofine.core.BuildInfo;
import dev.velofine.core.config.ConfigManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the real patcher-pipeline logic (CLAUDE.md's explicit Phase 9 test mandate) against a
 * synthetic {@code @TempDir} "Minecraft directory" with a hand-authored fake vanilla version JSON
 * - not the real proprietary client jar, which {@link ProfileInstaller} never needs to read the
 * bytecode of (it only copies jar bytes verbatim and manipulates JSON structure).
 *
 * <p>Uses the package-visible {@code install(Path, String, byte[])} overload rather than the
 * public {@code install(String[])} - the public entry point reads the real running jar's own
 * bytes via {@code currentJarPath()}, which resolves to an exploded {@code build/classes/...}
 * directory (not an actual jar) under a Gradle test run, not something a unit test can supply.
 */
final class ProfileInstallerTest {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final byte[] FAKE_LAUNCHER_JAR_BYTES = "pretend launcher jar bytes".getBytes(StandardCharsets.UTF_8);

    @Test
    void installCreatesAVelofineVersionAlongsideVanilla(@TempDir Path minecraftDir) throws IOException {
        writeVanillaVersionJson(minecraftDir, "26.2");

        ProfileInstaller.install(minecraftDir, "26.2", FAKE_LAUNCHER_JAR_BYTES);

        Path velofineJson = minecraftDir.resolve("versions").resolve("26.2-velofine").resolve("26.2-velofine.json");
        assertTrue(Files.isRegularFile(velofineJson));

        JsonObject parsed = GSON.fromJson(Files.readString(velofineJson), JsonObject.class);
        assertEquals("26.2-velofine", parsed.get("id").getAsString());
        assertEquals("dev.velofine.launcher.Main", parsed.get("mainClass").getAsString());
    }

    @Test
    void installPreservesVanillasRealMainClassAsAJvmArgument(@TempDir Path minecraftDir) throws IOException {
        writeVanillaVersionJson(minecraftDir, "26.2");
        ProfileInstaller.install(minecraftDir, "26.2", FAKE_LAUNCHER_JAR_BYTES);

        JsonObject parsed = readVelofineJson(minecraftDir, "26.2");
        String jvmArgsText = parsed.getAsJsonObject("arguments").getAsJsonArray("jvm").toString();

        assertTrue(jvmArgsText.contains("-Dvelofine.vanillaMainClass=net.minecraft.client.main.Main"));
        assertTrue(jvmArgsText.contains("-Djdk.attach.allowAttachSelf=true"));
        assertTrue(jvmArgsText.contains("-Dmixin.env.disableRefMap=true"));
    }

    @Test
    void installWritesTheLibraryJarUnderTheCurrentVelofineVersion(@TempDir Path minecraftDir) throws IOException {
        writeVanillaVersionJson(minecraftDir, "26.2");
        ProfileInstaller.install(minecraftDir, "26.2", FAKE_LAUNCHER_JAR_BYTES);

        Path libraryJar = minecraftDir.resolve("libraries").resolve("dev/velofine/velofine-launcher")
                .resolve(BuildInfo.velofineVersion())
                .resolve("velofine-launcher-" + BuildInfo.velofineVersion() + ".jar");

        assertTrue(Files.isRegularFile(libraryJar));
        assertEquals("pretend launcher jar bytes", Files.readString(libraryJar, StandardCharsets.UTF_8));
    }

    @Test
    void installRegistersAProfileInLauncherProfilesJson(@TempDir Path minecraftDir) throws IOException {
        writeVanillaVersionJson(minecraftDir, "26.2");
        ProfileInstaller.install(minecraftDir, "26.2", FAKE_LAUNCHER_JAR_BYTES);

        JsonObject profiles = GSON.fromJson(
                Files.readString(minecraftDir.resolve("launcher_profiles.json")), JsonObject.class);

        assertTrue(profiles.getAsJsonObject("profiles").has("velofine-26.2"));
        assertEquals("26.2-velofine", profiles.getAsJsonObject("profiles")
                .getAsJsonObject("velofine-26.2").get("lastVersionId").getAsString());
    }

    @Test
    void installCopiesTheVanillaClientJarUnderTheNewVersionId(@TempDir Path minecraftDir) throws IOException {
        writeVanillaVersionJson(minecraftDir, "26.2");
        Files.writeString(minecraftDir.resolve("versions/26.2/26.2.jar"), "pretend vanilla client bytes");

        ProfileInstaller.install(minecraftDir, "26.2", FAKE_LAUNCHER_JAR_BYTES);

        Path copiedJar = minecraftDir.resolve("versions/26.2-velofine/26.2-velofine.jar");
        assertTrue(Files.isRegularFile(copiedJar));
        assertEquals("pretend vanilla client bytes", Files.readString(copiedJar));
    }

    @Test
    void installTracksTheTargetedMcVersionInConfig(@TempDir Path minecraftDir) throws IOException {
        writeVanillaVersionJson(minecraftDir, "26.2");
        ProfileInstaller.install(minecraftDir, "26.2", FAKE_LAUNCHER_JAR_BYTES);

        ConfigManager.load(minecraftDir);
        assertEquals("26.2", ConfigManager.get().updater.trackedMcVersion);
    }

    @Test
    void installRefusesWhenVanillaIsNotInstalled(@TempDir Path minecraftDir) {
        assertThrows(IllegalStateException.class,
                () -> ProfileInstaller.install(minecraftDir, "26.2", FAKE_LAUNCHER_JAR_BYTES));
    }

    @Test
    void installRefusesAPreModernSchemaVersionJson(@TempDir Path minecraftDir) throws IOException {
        Path versionDir = minecraftDir.resolve("versions").resolve("1.12.2");
        Files.createDirectories(versionDir);
        JsonObject oldSchema = new JsonObject();
        oldSchema.addProperty("mainClass", "net.minecraft.client.main.Main");
        oldSchema.addProperty("minecraftArguments", "--username ${auth_player_name}");
        Files.writeString(versionDir.resolve("1.12.2.json"), GSON.toJson(oldSchema));

        assertThrows(IllegalStateException.class,
                () -> ProfileInstaller.install(minecraftDir, "1.12.2", FAKE_LAUNCHER_JAR_BYTES));
    }

    @Test
    void uninstallRemovesEverythingInstallLeftBehind(@TempDir Path minecraftDir) throws IOException {
        writeVanillaVersionJson(minecraftDir, "26.2");
        ProfileInstaller.install(minecraftDir, "26.2", FAKE_LAUNCHER_JAR_BYTES);

        ProfileInstaller.uninstall(new String[] {minecraftDir.toString()});

        assertFalse(Files.exists(minecraftDir.resolve("versions/26.2-velofine")));
        assertFalse(Files.exists(minecraftDir.resolve("libraries/dev/velofine")));
        // Vanilla's own version must be left completely untouched.
        assertTrue(Files.exists(minecraftDir.resolve("versions/26.2/26.2.json")));

        JsonObject profiles = GSON.fromJson(
                Files.readString(minecraftDir.resolve("launcher_profiles.json")), JsonObject.class);
        assertFalse(profiles.getAsJsonObject("profiles").has("velofine-26.2"));
    }

    @Test
    void uninstallOnAMinecraftDirWithNothingInstalledDoesNotThrow(@TempDir Path minecraftDir) throws IOException {
        ProfileInstaller.uninstall(new String[] {minecraftDir.toString()});
    }

    private static JsonObject readVelofineJson(Path minecraftDir, String vanillaVersion) throws IOException {
        Path path = minecraftDir.resolve("versions").resolve(vanillaVersion + "-velofine").resolve(vanillaVersion + "-velofine.json");
        return GSON.fromJson(Files.readString(path), JsonObject.class);
    }

    /** A minimal but real-shaped modern-schema vanilla version JSON. */
    private static void writeVanillaVersionJson(Path minecraftDir, String vanillaVersion) throws IOException {
        Path versionDir = minecraftDir.resolve("versions").resolve(vanillaVersion);
        Files.createDirectories(versionDir);

        String json = """
                {
                  "id": "%s",
                  "mainClass": "net.minecraft.client.main.Main",
                  "arguments": {
                    "jvm": [ { "value": "-Xmx2G" } ],
                    "game": [ "--username", "${auth_player_name}" ]
                  }
                }
                """.formatted(vanillaVersion);
        Files.writeString(versionDir.resolve(vanillaVersion + ".json"), json, StandardCharsets.UTF_8);
    }
}
