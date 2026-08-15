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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.velofine.core.hardware.HardwareProfiles;
import dev.velofine.core.log.VelofineLog;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Loads and saves {@code &lt;gameDir&gt;/velofine/config.json}.
 *
 * <p>Location matters: it is under the game directory (the instance folder vanilla passes as
 * {@code --gameDir}), <em>not</em> under {@code libraries/dev/velofine}, which
 * {@code ProfileInstaller.uninstall()} deletes recursively - a user's settings should survive a
 * Velofine reinstall.
 *
 * <p>Every failure path here is deliberately non-fatal. This runs in {@code Main.main()} before
 * Minecraft has started; a malformed config file must never be the reason the game won't launch,
 * so a bad file is moved aside and defaults are used instead.
 */
public final class ConfigManager {

    private static final String CONFIG_DIR = "velofine";
    private static final String CONFIG_FILE = "config.json";
    private static final String LOG_TAG = "config";

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private static volatile VelofineConfig config = new VelofineConfig();
    private static volatile Path configPath;

    private ConfigManager() {
    }

    /**
     * @param gameDir the instance directory from {@code --gameDir}; may be {@code null}, in which
     *     case Velofine runs on in-memory defaults and {@link #save()} becomes a no-op (there is
     *     nowhere sensible to write).
     */
    public static void load(Path gameDir) {
        if (gameDir == null) {
            VelofineLog.warn(LOG_TAG, "No game directory known; running with default settings (nothing will be saved).");
            config = withFirstRunDefaults(new VelofineConfig());
            return;
        }

        configPath = gameDir.resolve(CONFIG_DIR).resolve(CONFIG_FILE);

        if (!Files.isRegularFile(configPath)) {
            config = withFirstRunDefaults(new VelofineConfig());
            VelofineLog.info(LOG_TAG, "No config found; writing first-run defaults to " + configPath);
            save();
            return;
        }

        try {
            String json = Files.readString(configPath, StandardCharsets.UTF_8);
            VelofineConfig parsed = GSON.fromJson(json, VelofineConfig.class);
            if (parsed == null) {
                throw new IOException("config file parsed to null (empty or literal 'null')");
            }
            config = parsed;
            VelofineLog.info(LOG_TAG, "Loaded " + configPath);
        } catch (Exception e) {
            quarantineBadConfig(e);
            config = withFirstRunDefaults(new VelofineConfig());
            save();
        }
    }

    public static VelofineConfig get() {
        return config;
    }

    public static void save() {
        Path path = configPath;
        if (path == null) {
            return;
        }
        try {
            Files.createDirectories(path.getParent());
            // Write-then-rename so a crash or a full disk mid-write leaves the previous good file
            // intact rather than a half-written one that would be quarantined on next launch.
            Path temp = path.resolveSibling(CONFIG_FILE + ".tmp");
            Files.writeString(temp, GSON.toJson(config), StandardCharsets.UTF_8);
            try {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            VelofineLog.warn(LOG_TAG, "Failed to save " + path + ": " + e);
        }
    }

    /** Test/diagnostic seam: where the config would be written, or {@code null} if nowhere. */
    public static Path configPath() {
        return configPath;
    }

    /**
     * A detached copy of the live config for the UI to edit.
     *
     * <p>Serialising through Gson rather than hand-writing a deep copy is deliberate: it cannot
     * drift out of sync when a field is added, and anything Gson cannot round-trip would not have
     * survived being written to disk either.
     */
    public static VelofineConfig copyOfLive() {
        VelofineConfig copy = GSON.fromJson(GSON.toJson(config), VelofineConfig.class);
        return copy != null ? copy : new VelofineConfig();
    }

    /** Promotes an edited copy to the live config and persists it. */
    public static void replaceAndSave(VelofineConfig replacement) {
        config = replacement;
        save();
        VelofineLog.info(LOG_TAG, "Settings applied.");
    }

    /** Stable textual form, used by the UI purely to tell "edited" from "unchanged". */
    public static String snapshot(VelofineConfig value) {
        return GSON.toJson(value);
    }

    private static void quarantineBadConfig(Exception cause) {
        Path path = configPath;
        VelofineLog.warn(LOG_TAG, "Could not read " + path + " (" + cause + "); falling back to defaults.");
        try {
            Path backup = path.resolveSibling(CONFIG_FILE + ".bak");
            Files.move(path, backup, StandardCopyOption.REPLACE_EXISTING);
            VelofineLog.warn(LOG_TAG, "Unreadable config moved to " + backup);
        } catch (IOException e) {
            VelofineLog.warn(LOG_TAG, "Could not move the unreadable config aside: " + e);
        }
    }

    /**
     * The only defaults that depend on the machine rather than being constants.
     *
     * <p>The governor turns itself on only where it is likely to help - the old-Intel /
     * low-memory / rotational-disk class the reference machines belong to. On anything else it
     * stays off, because a modern-hardware user watching their render distance move on its own
     * without asking would be exactly the kind of silent behaviour change Velofine promises not to
     * make.
     *
     * <p>Hardware detection is only probed here, on first run, so repeat launches keep the same
     * startup cost they had before Phase 5.
     */
    private static VelofineConfig withFirstRunDefaults(VelofineConfig fresh) {
        try {
            if (HardwareProfiles.isWeakHardware()) {
                fresh.optimus.governor.mode = GovernorMode.ADAPTIVE;
                VelofineLog.info(LOG_TAG, "Weak hardware detected on first run; performance governor defaults to ADAPTIVE.");
            }
        } catch (Throwable t) {
            VelofineLog.warn(LOG_TAG, "Hardware probe failed while building first-run defaults: " + t);
        }
        return fresh;
    }
}
