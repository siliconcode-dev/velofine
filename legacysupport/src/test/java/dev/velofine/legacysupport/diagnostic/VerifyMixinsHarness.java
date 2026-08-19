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
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 9: converted from Phase 2's manual {@code main()}-method diagnostic into a real JUnit 5
 * test class, with its exact original verification logic unchanged - byte-diffing a real vanilla
 * 26.2 class against {@link MixinBridge#transform} and checking every marker string listed below
 * still appears in the transformed bytes.
 *
 * <p><b>Skips cleanly (not fails) whenever no real jar is configured</b> - {@code
 * @EnabledIfSystemProperty} disables the whole class unless {@code velofine.test.mcJarPath} is set
 * to a non-empty value, matching the "no reference hardware, so document a manual/optional
 * verification path" pattern this project already applies to LegacySupport's own hardware-specific
 * claims. This is the default for CI and for anyone without the (proprietary, non-committable)
 * client jar.
 *
 * <p><b>How the real jar and a live Mixin bootstrap reach this test</b>: {@code
 * legacysupport/build.gradle.kts}'s {@code tasks.test} block conditionally attaches the real agent
 * (<code>-javaagent:&lt;launcher's shaded jar&gt;</code>) to this module's whole test JVM whenever
 * {@code velofine.test.mcJarPath}/{@code VELOFINE_TEST_MC_JAR} is set - {@code
 * VelofineAgent.premain} then runs automatically before any test code does, so
 * {@code MixinBootstrap.init()}/every engine's {@code Mixins.addConfiguration(...)}/{@code
 * MixinBridge.install(...)} are already done by the time these {@code @Test} methods run, exactly
 * as they were when this class was a hand-run {@code java -javaagent:...} invocation.
 *
 * <p>Deliberately simplified from the original: no longer writes {@code .original.class}/{@code
 * .transformed.class} dumps to disk - that existed for manual byte-level inspection convenience,
 * not correctness, and is not needed for automated pass/fail.
 *
 * <p><b>Known gap, confirmed by actually running this live against the real 26.2 jar during
 * Phase 9</b>: the two markers that depend on {@code mixins.shaders.json} (Shaders'
 * {@code CompositeRenderMixin}/{@code GameRendererAccessor}/{@code ShaderManagerAccessor}) SKIP
 * rather than run, since that config only installs once {@code ShaderEngine} sees a real game
 * directory with a pack actually selected - a fixture this conversion does not construct. Every
 * other marker (14 of 16, since v1.5 Phase 2's {@code TitleScreenMixin} addition) runs and passes
 * live. See {@link #assumeShaderPipelineMixinsAreInstalled()}.
 */
@EnabledIfSystemProperty(named = "velofine.test.mcJarPath", matches = ".+")
final class VerifyMixinsHarness {

    @Test
    void glBackendMixinForcesCompatibilityProfile() throws IOException {
        assertMixinApplied("com.mojang.blaze3d.opengl.GlBackend", "GlBackendMixin",
                "compatibility profile forced");
    }

    @Test
    void coreGlDeviceMixinRedirectsShaderSource() throws IOException {
        // Phase 7 moved this mixin from legacysupport to core (see ShaderSourceInterceptors' class
        // javadoc) - it now installs unconditionally via CoreEngine, no forceFixes needed.
        //
        // v1.8-Beta adds the second, post-#define redirect on GlslPreprocessor.injectDefines. This
        // assertion is the release gate for it: mixins.core-shader.json is "required": true, so a
        // wrong INVOKE descriptor throws InvalidInjectionException here rather than silently doing
        // nothing in a tester's log - which is exactly how v1.7-Beta's end-portal fix shipped broken.
        // Note the limit of this check: it proves the redirect *applies*, not that the source it
        // receives actually contains the defines. EndPortalArrayIndexPatchTest covers that half.
        assertMixinApplied("com.mojang.blaze3d.opengl.GlDevice", "core.mixin.GlDeviceMixin",
                "velofine$resolveShaderSource", "velofine$resolvePostDefineShaderSource");
    }

    @Test
    void chunkMapMixinCapsEagerSaves() throws IOException {
        assertMixinApplied("net.minecraft.server.level.ChunkMap", "ChunkMapMixin",
                "velofine$eagerSaveCap");
    }

    @Test
    void optionsMixinLowersFirstRunDefaults() throws IOException {
        assertMixinApplied("net.minecraft.client.Options", "OptionsMixin",
                "velofine$renderDistanceDefault", "velofine$simulationDistanceDefault",
                "velofine$entityDistanceScalingDefault", "velofine$mipmapLevelsDefault", "velofine$particlesDefault");
    }

    @Test
    void spriteContentsAccessorExposesByMipLevel() throws IOException {
        assertMixinApplied("net.minecraft.client.renderer.texture.SpriteContents", "SpriteContentsAccessor",
                "velofine$getByMipLevel");
    }

    @Test
    void spriteContentsAnimatedTextureMixinFixesMc308593() throws IOException {
        assertMixinApplied("net.minecraft.client.renderer.texture.SpriteContents$AnimatedTexture",
                "SpriteContentsAnimatedTextureMixin", "velofine$fixAnimationUpload");
    }

    @Test
    void mobMixinThrottlesGoalSelectorUpdates() throws IOException {
        assertMixinApplied("net.minecraft.world.entity.Mob", "MobMixin",
                "velofine$goalSelectorUpdateInterval");
    }

    @Test
    void minecraftIsSharedByThreeIndependentMixinConfigsAtOnce() throws IOException {
        // Optimus's tick profiler/governor hook, core's config-keybind poll, and Utility's
        // UtilityTickMixin - every marker must survive in the same transform, the real test of the
        // shared-transformer design (Phase 4/5/6, stress-tested further each phase since).
        assertMixinApplied("net.minecraft.client.Minecraft", "MinecraftMixin + MinecraftKeybindMixin + UtilityTickMixin",
                "velofine$onTickStart", "velofine$onTickEnd", "velofine$pollConfigKeybind");
    }

    @Test
    void videoSettingsScreenMixinAddsTheVelofineSettingsRow() throws IOException {
        assertMixinApplied("net.minecraft.client.gui.screens.options.VideoSettingsScreen", "VideoSettingsScreenMixin",
                "velofine$addSettingsRow", "VELOFINE SETTINGS");
    }

    @Test
    void pauseScreenMixinAddsTheVelofineButton() throws IOException {
        assertMixinApplied("net.minecraft.client.gui.screens.PauseScreen", "PauseScreenMixin",
                "velofine$addPauseMenuButton", "VELOFINE");
    }

    @Test
    void titleScreenMixinRebrandsVersionWatermark() throws IOException {
        assertMixinApplied("net.minecraft.client.gui.screens.TitleScreen", "TitleScreenMixin",
                "velofine$rebrandVersionText", "Velofine ");
    }

    @Test
    void cameraMixinAppliesZoom() throws IOException {
        assertMixinApplied("net.minecraft.client.Camera", "CameraMixin", "velofine$applyZoom");
    }

    @Test
    void fogRendererMixinAppliesFogControl() throws IOException {
        assertMixinApplied("net.minecraft.client.renderer.fog.FogRenderer", "FogRendererMixin",
                "velofine$applyFogControl");
    }

    @Test
    void mouseScrollMixinRoutesZoomScroll() throws IOException {
        assertMixinApplied("net.minecraft.client.MouseHandler", "MouseScrollMixin",
                "velofine$routeZoomScroll");
    }

    @Test
    void renderDistanceMixinAppliesVerticalDistance() throws IOException {
        assertMixinApplied("net.minecraft.client.renderer.SectionOcclusionGraph", "RenderDistanceMixin",
                "velofine$applyVerticalDistance");
    }

    @Test
    void blockLightEngineMixinAppliesDynamicLight() throws IOException {
        assertMixinApplied("net.minecraft.world.level.lighting.BlockLightEngine", "BlockLightEngineMixin",
                "velofine$applyDynamicLight");
    }

    @Test
    void gameRendererIsSharedByThreeIndependentMixinsAtOnce() throws IOException {
        assumeShaderPipelineMixinsAreInstalled();
        // Utility's GameRenderMixin (Phase 6) and Shaders' CompositeRenderMixin +
        // GameRendererAccessor (both mixins.shaders.json) - another shared-transformer stress test.
        assertMixinApplied("net.minecraft.client.renderer.GameRenderer",
                "GameRenderMixin + CompositeRenderMixin + GameRendererAccessor",
                "velofine$onFrame", "velofine$processCompositePipeline", "velofine$getResourcePool");
    }

    @Test
    void shaderManagerAccessorExposesPostChainInternals() throws IOException {
        assumeShaderPipelineMixinsAreInstalled();
        assertMixinApplied("net.minecraft.client.renderer.ShaderManager", "ShaderManagerAccessor",
                "velofine$getPostChainProjection", "velofine$getPostChainProjectionMatrixBuffer");
    }

    /**
     * {@code mixins.shaders.json} (CompositeRenderMixin/GameRendererAccessor/ShaderManagerAccessor)
     * only installs when {@code ShaderEngine.onAgentAttached} sees a real game directory with
     * {@code utility.shader.enabled=true} and a pack actually selected (see that class's own
     * javadoc) - this JUnit conversion does not construct that full environment (a synthetic
     * shaderpack + pre-seeded config, on top of everything else here), so these two markers are
     * skipped rather than asserted, honestly, until that fixture exists - flagged as a real,
     * tracked gap rather than silently faked green.
     */
    private static void assumeShaderPipelineMixinsAreInstalled() {
        String gameDir = System.getProperty("velofine.gameDir");
        Assumptions.assumeTrue(gameDir != null && !gameDir.isBlank(),
                "mixins.shaders.json needs a real game directory with a shaderpack selected - "
                        + "not constructed by this test run, see this method's javadoc");
    }

    private static void assertMixinApplied(String className, String mixinLabel, String... mustContain) throws IOException {
        Path clientJar = Path.of(System.getProperty("velofine.test.mcJarPath"));
        byte[] original = readClassFromJar(clientJar, className);
        assertTrue(original != null, className + " not found in " + clientJar);

        byte[] transformed = MixinBridge.transform(className, className, original);

        assertTrue(!java.util.Arrays.equals(original, transformed),
                className + "'s bytes were not changed by " + mixinLabel);
        for (String marker : mustContain) {
            assertTrue(containsAscii(transformed, marker),
                    className + "'s transformed bytes are missing the \"" + marker + "\" marker (" + mixinLabel + ")");
        }
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
        byte[] needleBytes = needle.getBytes(StandardCharsets.US_ASCII);
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
}
