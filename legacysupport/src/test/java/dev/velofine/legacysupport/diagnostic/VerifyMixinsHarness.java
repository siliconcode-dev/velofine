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

package dev.velofine.legacysupport.diagnostic;

import dev.velofine.core.mixin.MixinBridge;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Manual diagnostic tool, not a JUnit test (Phase 2 predates the Phase 9 automated-test buildout
 * per CLAUDE.md). Run with {@code -javaagent:<shaded launcher jar>} (triggers the real self-attach
 * path, which now runs {@code CoreEngine}, {@code LegacySupportEngine}, {@code OptimusEngine} and
 * {@code UtilityEngine} in sequence - CoreEngine's mixins and Optimus's are unconditional given
 * default config, so no force-flag is needed for those) and {@code
 * -Dvelofine.legacysupport.forceFixes=GL_COMPATIBILITY_PROFILE,SHADER_MIX_PATCH,IO_STALL_SMOOTHING,MEMORY_SAVING_DEFAULTS} (so LegacySupport's mixins run regardless of this machine's real
 * GPU), then exercises Mixin's real transform pipeline directly against real vanilla 26.2 client
 * jar class bytes - without touching GLFW/the actual game. Running every engine's mixins in the
 * same JVM here also empirically confirms Phase 4's hoisted {@code MixinBridge}/{@code
 * VelofineMixinService} correctly applies multiple engines' independently-added
 * {@code Mixins.addConfiguration(...)} configs through the one shared transformer - Phase 5 adds a
 * third config ({@code mixins.velofine-core.json}) and two classes ({@code Minecraft},
 * {@code PauseScreen}... {@code VideoSettingsScreen}) targeted by more than one engine's mixins at
 * once, which is exactly the scenario that would expose a transformer-sharing regression.
 *
 * <p>Deliberately lives outside {@code dev.velofine.legacysupport.mixin}: Mixin forbids classes
 * inside an active mixin config's own package from being loaded directly, which would otherwise
 * break this class's own main-class loading (confirmed empirically before this fix existed).
 *
 * <p>The real vanilla 26.2 client jar must <em>also</em> be on the run classpath, not just passed
 * as an argument - Mixin's target-resolution step (building superclass/interface info for
 * {@code @Mixin} targets) loads the target class via the classloader, not just from the raw bytes
 * this harness reads directly.
 *
 * <p>Usage: {@code java -javaagent:launcher-<version>.jar -Dvelofine.legacysupport.forceFixes=GL_COMPATIBILITY_PROFILE,SHADER_MIX_PATCH,IO_STALL_SMOOTHING,MEMORY_SAVING_DEFAULTS
 * -cp <runtime classpath>;<path to 26.2.jar> dev.velofine.legacysupport.diagnostic.VerifyMixinsHarness
 * <path to 26.2.jar> <output dir>}
 */
public final class VerifyMixinsHarness {

    private static final String GL_BACKEND = "com.mojang.blaze3d.opengl.GlBackend";
    private static final String GL_DEVICE = "com.mojang.blaze3d.opengl.GlDevice";
    private static final String CHUNK_MAP = "net.minecraft.server.level.ChunkMap";
    private static final String OPTIONS = "net.minecraft.client.Options";
    private static final String MOB = "net.minecraft.world.entity.Mob";
    private static final String MINECRAFT = "net.minecraft.client.Minecraft";
    private static final String VIDEO_SETTINGS_SCREEN = "net.minecraft.client.gui.screens.options.VideoSettingsScreen";
    private static final String PAUSE_SCREEN = "net.minecraft.client.gui.screens.PauseScreen";
    private static final String CAMERA = "net.minecraft.client.Camera";
    private static final String FOG_RENDERER = "net.minecraft.client.renderer.fog.FogRenderer";
    private static final String MOUSE_HANDLER = "net.minecraft.client.MouseHandler";
    private static final String SECTION_OCCLUSION_GRAPH = "net.minecraft.client.renderer.SectionOcclusionGraph";
    private static final String GAME_RENDERER = "net.minecraft.client.renderer.GameRenderer";
    private static final String BLOCK_LIGHT_ENGINE = "net.minecraft.world.level.lighting.BlockLightEngine";

    private VerifyMixinsHarness() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: VerifyMixinsHarness <path to 26.2.jar> <output dir>");
            System.exit(2);
            return;
        }
        Path clientJar = Path.of(args[0]);
        Path outDir = Path.of(args[1]);
        Files.createDirectories(outDir);

        System.out.println("=== VerifyMixinsHarness ===");
        boolean ok = true;
        ok &= verifyClass(clientJar, outDir, GL_BACKEND, "GlBackendMixin",
                new String[] {"compatibility profile forced"});
        ok &= verifyClass(clientJar, outDir, GL_DEVICE, "GlDeviceMixin",
                new String[] {"velofine$maybePatchShaderSource"});
        ok &= verifyClass(clientJar, outDir, CHUNK_MAP, "ChunkMapMixin",
                new String[] {"velofine$eagerSaveCap"});
        ok &= verifyClass(clientJar, outDir, OPTIONS, "OptionsMixin",
                new String[] {"velofine$renderDistanceDefault", "velofine$simulationDistanceDefault",
                        "velofine$entityDistanceScalingDefault", "velofine$mipmapLevelsDefault", "velofine$particlesDefault"});
        ok &= verifyClass(clientJar, outDir, MOB, "MobMixin",
                new String[] {"velofine$goalSelectorUpdateInterval"});
        // Minecraft is targeted by three independent mixin configs as of Phase 6 (Optimus's tick
        // profiler/governor hook, core's config-keybind poll, and Utility's UtilityTickMixin) -
        // every marker must survive in the same transform, the real test of the shared-transformer
        // claim above, now stress-tested with a third independent injector on the same method.
        ok &= verifyClass(clientJar, outDir, MINECRAFT, "MinecraftMixin + MinecraftKeybindMixin + UtilityTickMixin",
                new String[] {"velofine$onTickStart", "velofine$onTickEnd", "velofine$pollConfigKeybind"});
        ok &= verifyClass(clientJar, outDir, VIDEO_SETTINGS_SCREEN, "VideoSettingsScreenMixin",
                new String[] {"velofine$addSettingsRow", "VELOFINE SETTINGS"});
        ok &= verifyClass(clientJar, outDir, PAUSE_SCREEN, "PauseScreenMixin",
                new String[] {"velofine$addPauseMenuButton", "VELOFINE"});

        // Phase 6 - Utility's first real mixins.
        ok &= verifyClass(clientJar, outDir, CAMERA, "CameraMixin",
                new String[] {"velofine$applyZoom"});
        ok &= verifyClass(clientJar, outDir, FOG_RENDERER, "FogRendererMixin",
                new String[] {"velofine$applyFogControl"});
        ok &= verifyClass(clientJar, outDir, MOUSE_HANDLER, "MouseScrollMixin",
                new String[] {"velofine$routeZoomScroll"});
        ok &= verifyClass(clientJar, outDir, SECTION_OCCLUSION_GRAPH, "RenderDistanceMixin",
                new String[] {"velofine$applyVerticalDistance"});
        ok &= verifyClass(clientJar, outDir, GAME_RENDERER, "GameRenderMixin",
                new String[] {"velofine$onFrame"});
        ok &= verifyClass(clientJar, outDir, BLOCK_LIGHT_ENGINE, "BlockLightEngineMixin",
                new String[] {"velofine$applyDynamicLight"});

        System.out.println("=== " + (ok ? "PASS" : "FAIL") + " ===");
        System.exit(ok ? 0 : 1);
    }

    private static boolean verifyClass(Path clientJar, Path outDir, String className, String mixinLabel, String[] mustContain)
            throws IOException {
        byte[] original = readClassFromJar(clientJar, className);
        if (original == null) {
            System.out.println("[FAIL] " + className + " not found in " + clientJar);
            return false;
        }

        byte[] transformed = MixinBridge.transform(className, className, original);

        Files.write(outDir.resolve(simpleName(className) + ".original.class"), original);
        Files.write(outDir.resolve(simpleName(className) + ".transformed.class"), transformed);

        boolean changed = !java.util.Arrays.equals(original, transformed);
        System.out.println((changed ? "[OK]  " : "[FAIL]") + " " + className + " bytes changed by " + mixinLabel + ": " + changed);

        boolean allMarkersFound = true;
        for (String marker : mustContain) {
            boolean found = containsAscii(transformed, marker);
            System.out.println((found ? "[OK]  " : "[FAIL]") + " " + className + " transformed bytes contain \"" + marker + "\": " + found);
            allMarkersFound &= found;
        }

        System.out.println("  original size=" + original.length + " transformed size=" + transformed.length
                + " (class files written to " + outDir + ")");

        return changed && allMarkersFound;
    }

    private static byte[] readClassFromJar(Path jarPath, String className) throws IOException {
        String entryName = className.replace('.', '/') + ".class";
        try (ZipFile zip = new ZipFile(jarPath.toFile())) {
            ZipEntry entry = zip.getEntry(entryName);
            if (entry == null) {
                return null;
            }
            try (var in = zip.getInputStream(entry)) {
                return in.readAllBytes();
            }
        }
    }

    private static boolean containsAscii(byte[] haystack, String needle) {
        byte[] needleBytes = needle.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        outer:
        for (int i = 0; i <= haystack.length - needleBytes.length; i++) {
            for (int j = 0; j < needleBytes.length; j++) {
                if (haystack[i + j] != needleBytes[j]) {
                    continue outer;
                }
            }
            return true;
        }
        return false;
    }

    private static String simpleName(String className) {
        int idx = className.lastIndexOf('.');
        return idx < 0 ? className : className.substring(idx + 1);
    }
}
