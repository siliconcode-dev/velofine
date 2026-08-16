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
import dev.velofine.core.BuildInfo;
import dev.velofine.core.config.ConfigManager;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Comparator;
import java.util.HexFormat;

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
 * <p>Validated (Phase 1) against a real vanilla 26.2 install cached by Legacy Launcher Stable — note
 * that its {@code arguments.jvm} entries are <em>all</em> normalized to {@code {"value": ...}} object
 * form, even unconditional flags with no {@code rules}, unlike the official Mojang manifest's usual
 * mix of bare strings and rule-gated objects. This class always appends new entries in object form
 * for that reason — valid under the schema either way, and the only form guaranteed compatible with
 * both a stock Mojang file and a Legacy-Launcher-normalized one.
 */
public final class ProfileInstaller {

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

        install(minecraftDir, vanillaVersion, Files.readAllBytes(currentJarPath()));
    }

    /**
     * Package-visible so {@code ProfileInstallerTest} can supply synthetic jar bytes -
     * {@link #currentJarPath()} resolves to an exploded {@code build/classes/...} directory (not
     * an actual jar file) under a normal Gradle test run, which {@code Files.readAllBytes} cannot
     * read, so real-jar-byte acquisition is deliberately kept out of the testable core here.
     */
    static void install(Path minecraftDir, String vanillaVersion, byte[] launcherJarBytes) throws IOException {
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
        libraries.add(velofineLibraryEntry(launcherJarBytes));

        // Element form, not bare strings: real-world version JSONs (confirmed against both a
        // stock Mojang-schema file and Legacy Launcher's own cached copy) mix bare strings and
        // {"rules": [...], "value": ...} objects in this array — some launchers (Legacy
        // Launcher's cache included) normalize *every* entry to object form, even unconditional
        // ones. Object-with-just-"value" is valid per the schema either way, so this is the
        // strictly safer form for a newly-appended entry.
        JsonArray jvmArgs = velofine.getAsJsonObject("arguments").getAsJsonArray("jvm");
        jvmArgs.add(jvmArgValue("-Djdk.attach.allowAttachSelf=true"));
        jvmArgs.add(jvmArgValue("-Dvelofine.vanillaMainClass=" + vanillaMainClass));
        // mixins.legacysupport.json has no refmap (not needed - real Mojang class names, not
        // SRG/obfuscated); without this flag Mixin logs a spurious "Invalid REFMAP JSON" error
        // for the unset field (confirmed via this phase's VerifyMixinsHarness).
        jvmArgs.add(jvmArgValue("-Dmixin.env.disableRefMap=true"));

        Path velofineJsonDir = minecraftDir.resolve("versions").resolve(velofineId);
        Files.createDirectories(velofineJsonDir);
        writeJsonObject(gson, velofineJsonDir.resolve(velofineId + ".json"), velofine);

        // Launchers derive the expected local client-jar filename from the version "id"
        // (versions/<id>/<id>.jar), not from the "downloads.client" URL in the JSON - since our
        // "id" is "<vanillaVersion>-velofine", not "<vanillaVersion>", that path is never
        // populated by anything vanilla install left behind. Without this, launchers see it
        // missing locally and re-download the *entire real client jar* from Mojang under the new
        // filename (confirmed the real cause of an observed slow-but-legitimate download,
        // distinct from the earlier broken-library-reference hang). The deep-copied
        // "downloads.client" sha1/size are still valid for this file (identical bytes, just a
        // new local path), so simply placing the copy is enough - no JSON changes needed here.
        Path vanillaJarPath = vanillaJsonPath.resolveSibling(vanillaVersion + ".jar");
        if (Files.isRegularFile(vanillaJarPath)) {
            Files.copy(vanillaJarPath, velofineJsonDir.resolve(velofineId + ".jar"),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }

        Path libraryDest = minecraftDir.resolve("libraries").resolve(LIBRARY_GROUP_PATH)
                .resolve(BuildInfo.velofineVersion()).resolve("velofine-launcher-" + BuildInfo.velofineVersion() + ".jar");
        Files.createDirectories(libraryDest.getParent());
        Files.write(libraryDest, launcherJarBytes);

        registerProfile(gson, minecraftDir, vanillaVersion, velofineId);

        // Phase 8: the one place the actually-targeted vanilla version is known - persisted so the
        // updater can filter releases to this install's own MC-version track (a release's
        // manifest.json targetMcVersion must match this to ever be offered).
        ConfigManager.load(minecraftDir);
        ConfigManager.get().updater.trackedMcVersion = vanillaVersion;
        ConfigManager.save();

        System.out.println("[Velofine] Profile installed: " + velofineId);
        System.out.println("[Velofine]   version JSON: " + velofineJsonDir.resolve(velofineId + ".json"));
        System.out.println("[Velofine]   client jar:   " + velofineJsonDir.resolve(velofineId + ".jar")
                + (Files.isRegularFile(vanillaJarPath) ? " (copied from vanilla)" : " (NOT copied - vanilla jar missing at " + vanillaJarPath + ")"));
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

    /**
     * A bare {@code {"name": ...}} entry with no {@code downloads} block (the original Phase 1
     * shape) makes a Mojang-schema-aware launcher fall back to constructing a download URL from
     * the Maven coordinates against the official libraries CDN, which obviously doesn't host our
     * jar - confirmed as the real cause of an observed indefinite "Downloading resources..." hang
     * on Legacy Launcher Stable. Including a real {@code downloads.artifact} block with this
     * exact jar's sha1/size lets the launcher verify the copy {@link #install} already wrote to
     * {@code libraries/<path>} and skip downloading entirely - the same pattern Fabric Loader's
     * own installer uses for its locally-provided libraries, including the empty {@code url}
     * (no remote source; local-only, matched-by-hash is enough for a launcher to accept it).
     */
    private static JsonObject velofineLibraryEntry(byte[] jarBytes) {
        String velofineVersion = BuildInfo.velofineVersion();
        JsonObject library = new JsonObject();
        library.addProperty("name", "dev.velofine:velofine-launcher:" + velofineVersion);

        JsonObject artifact = new JsonObject();
        artifact.addProperty("path", LIBRARY_GROUP_PATH + "/" + velofineVersion + "/velofine-launcher-" + velofineVersion + ".jar");
        artifact.addProperty("sha1", sha1Hex(jarBytes));
        artifact.addProperty("size", jarBytes.length);
        artifact.addProperty("url", "");

        JsonObject downloads = new JsonObject();
        downloads.add("artifact", artifact);
        library.add("downloads", downloads);

        return library;
    }

    private static String sha1Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 is a JDK-guaranteed MessageDigest algorithm", e);
        }
    }

    private static JsonObject jvmArgValue(String value) {
        JsonObject entry = new JsonObject();
        entry.addProperty("value", value);
        return entry;
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
