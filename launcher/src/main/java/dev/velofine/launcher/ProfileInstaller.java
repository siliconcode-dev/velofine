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
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;

/**
 * Generates (and removes) Velofine's custom launcher profile, following the same
 * side-by-side-with-vanilla convention OptiFine used pre-1.13: clone the vanilla version JSON,
 * point {@code mainClass} at {@link Main}, and register a profile in {@code launcher_profiles.json}.
 *
 * <p>Assumes the standard {@code <minecraftDir>/versions/<id>/<id>.json} + {@code launcher_profiles.json}
 * layout used by the official Mojang launcher and most third-party launchers (TLauncher/Legacy
 * Launcher/etc). Targets the modern version-json schema ({@code arguments.jvm}/{@code arguments.game}
 * arrays) — refuses to touch pre-1.13-style {@code minecraftArguments} string profiles rather than
 * risk corrupting them.
 *
 * <p><b>Not yet validated against a real vanilla 26.2 version JSON</b> — this dev environment has no
 * vanilla 26.2 install to test against (see README/CLAUDE.md). Field names/structure follow the
 * documented modern Mojang schema; treat this as needing a real-world validation pass once a genuine
 * vanilla 26.2 install is available.
 */
public final class ProfileInstaller {

    private static final String VELOFINE_VERSION = "0.1.0";
    private static final String VELOFINE_SUFFIX = "-velofine";
    private static final String LIBRARY_GROUP_PATH = "dev/velofine/velofine-launcher";

    private ProfileInstaller() {
    }

    public static void install(String[] args) throws IOException {
        if (args.length < 1) {
            throw new IllegalArgumentException(
                    "Usage: --install-profile <minecraftDir> [vanillaVersionId] (default vanillaVersionId: 26.2)");
        }
        Path minecraftDir = Path.of(args[0]).toAbsolutePath();
        String vanillaVersion = args.length >= 2 ? args[1] : "26.2";

        Path vanillaJsonPath = minecraftDir.resolve("versions").resolve(vanillaVersion).resolve(vanillaVersion + ".json");
        if (!Files.isRegularFile(vanillaJsonPath)) {
            throw new IllegalStateException("Vanilla Minecraft " + vanillaVersion + " was not found at "
                    + vanillaJsonPath + ". Velofine requires vanilla " + vanillaVersion
                    + " to already be installed and launched at least once before installing Velofine.");
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        JsonObject vanilla = readJsonObject(gson, vanillaJsonPath);

        if (!vanilla.has("mainClass")) {
            throw new IllegalStateException("Vanilla version JSON at " + vanillaJsonPath
                    + " has no \"mainClass\" field; unrecognized format, refusing to proceed.");
        }
        if (!vanilla.has("arguments") || !vanilla.getAsJsonObject("arguments").has("jvm")) {
            throw new IllegalStateException("Vanilla version JSON at " + vanillaJsonPath
                    + " does not use the modern \"arguments.jvm\" schema (pre-1.13-style \"minecraftArguments\"?)."
                    + " Velofine's Phase 1 profile generator only supports the modern schema; refusing to proceed"
                    + " rather than risk corrupting an unfamiliar format.");
        }

        String vanillaMainClass = vanilla.get("mainClass").getAsString();
        String velofineId = vanillaVersion + VELOFINE_SUFFIX;

        JsonObject velofine = vanilla.deepCopy();
        velofine.addProperty("id", velofineId);
        velofine.addProperty("mainClass", "dev.velofine.launcher.Main");

        JsonArray libraries = velofine.has("libraries") ? velofine.getAsJsonArray("libraries") : new JsonArray();
        if (!velofine.has("libraries")) {
            velofine.add("libraries", libraries);
        }
        libraries.add(velofineLibraryEntry());

        JsonArray jvmArgs = velofine.getAsJsonObject("arguments").getAsJsonArray("jvm");
        jvmArgs.add("-Djdk.attach.allowAttachSelf=true");
        jvmArgs.add("-Dvelofine.vanillaMainClass=" + vanillaMainClass);

        Path velofineJsonDir = minecraftDir.resolve("versions").resolve(velofineId);
        Files.createDirectories(velofineJsonDir);
        writeJsonObject(gson, velofineJsonDir.resolve(velofineId + ".json"), velofine);

        Path libraryDest = minecraftDir.resolve("libraries").resolve(LIBRARY_GROUP_PATH)
                .resolve(VELOFINE_VERSION).resolve("velofine-launcher-" + VELOFINE_VERSION + ".jar");
        Files.createDirectories(libraryDest.getParent());
        Files.copy(currentJarPath(), libraryDest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        registerProfile(gson, minecraftDir, vanillaVersion, velofineId);

        System.out.println("[Velofine] Profile installed: " + velofineId);
        System.out.println("[Velofine]   version JSON: " + velofineJsonDir.resolve(velofineId + ".json"));
        System.out.println("[Velofine]   library jar:  " + libraryDest);
        System.out.println("[Velofine]   vanilla mainClass preserved as: " + vanillaMainClass);
    }

    public static void uninstall(String[] args) throws IOException {
        if (args.length < 1) {
            throw new IllegalArgumentException("Usage: --uninstall-profile <minecraftDir>");
        }
        Path minecraftDir = Path.of(args[0]).toAbsolutePath();
        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

        Path versionsDir = minecraftDir.resolve("versions");
        if (Files.isDirectory(versionsDir)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(versionsDir)) {
                for (Path entry : stream) {
                    if (Files.isDirectory(entry) && entry.getFileName().toString().endsWith(VELOFINE_SUFFIX)) {
                        deleteRecursively(entry);
                        System.out.println("[Velofine] Removed version: " + entry.getFileName());
                    }
                }
            }
        }

        Path libraryDir = minecraftDir.resolve("libraries").resolve("dev").resolve("velofine");
        if (Files.isDirectory(libraryDir)) {
            deleteRecursively(libraryDir);
            System.out.println("[Velofine] Removed library directory: " + libraryDir);
        }

        Path profilesPath = minecraftDir.resolve("launcher_profiles.json");
        if (Files.isRegularFile(profilesPath)) {
            JsonObject root = readJsonObject(gson, profilesPath);
            if (root.has("profiles")) {
                JsonObject profiles = root.getAsJsonObject("profiles");
                profiles.keySet().removeIf(key -> key.startsWith("velofine-"));
            }
            writeJsonObject(gson, profilesPath, root);
            System.out.println("[Velofine] Removed profile entries from " + profilesPath);
        }

        System.out.println("[Velofine] Uninstall complete.");
    }

    private static JsonObject velofineLibraryEntry() {
        JsonObject library = new JsonObject();
        library.addProperty("name", "dev.velofine:velofine-launcher:" + VELOFINE_VERSION);
        return library;
    }

    private static void registerProfile(Gson gson, Path minecraftDir, String vanillaVersion, String velofineId)
            throws IOException {
        Path profilesPath = minecraftDir.resolve("launcher_profiles.json");
        JsonObject root;
        if (Files.isRegularFile(profilesPath)) {
            root = readJsonObject(gson, profilesPath);
        } else {
            root = new JsonObject();
            root.addProperty("version", 3);
            root.add("profiles", new JsonObject());
            root.add("settings", new JsonObject());
        }
        if (!root.has("profiles")) {
            root.add("profiles", new JsonObject());
        }

        JsonObject profile = new JsonObject();
        profile.addProperty("name", "Velofine (MC " + vanillaVersion + ")");
        profile.addProperty("type", "custom");
        profile.addProperty("created", Instant.now().toString());
        profile.addProperty("lastUsed", Instant.now().toString());
        profile.addProperty("lastVersionId", velofineId);
        profile.addProperty("icon", "Furnace");

        root.getAsJsonObject("profiles").add("velofine-" + vanillaVersion, profile);
        writeJsonObject(gson, profilesPath, root);
    }

    private static JsonObject readJsonObject(Gson gson, Path path) throws IOException {
        String content = Files.readString(path, StandardCharsets.UTF_8);
        try {
            return gson.fromJson(content, JsonObject.class);
        } catch (JsonSyntaxException e) {
            throw new IOException("Malformed JSON in " + path, e);
        }
    }

    private static void writeJsonObject(Gson gson, Path path, JsonObject object) throws IOException {
        Files.writeString(path, gson.toJson(object), StandardCharsets.UTF_8);
    }

    private static Path currentJarPath() {
        try {
            return Path.of(ProfileInstaller.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Could not resolve Velofine's own jar path", e);
        }
    }

    private static void deleteRecursively(Path root) throws IOException {
        try (var walk = Files.walk(root)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}
