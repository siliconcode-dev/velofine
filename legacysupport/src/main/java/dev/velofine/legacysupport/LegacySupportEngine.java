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

package dev.velofine.legacysupport;

import com.mojang.blaze3d.shaders.ShaderType;
import dev.velofine.core.config.ConfigManager;
import dev.velofine.core.config.Tri;
import dev.velofine.core.config.VelofineConfig;
import dev.velofine.core.crash.CrashRecovery;
import dev.velofine.core.gpu.CpuInfo;
import dev.velofine.core.gpu.GpuInfo;
import dev.velofine.core.hardware.DiskInfo;
import dev.velofine.core.hardware.Fix;
import dev.velofine.core.hardware.FixProfileRules;
import dev.velofine.core.hardware.HardwareProfile;
import dev.velofine.core.hardware.HardwareProfiles;
import dev.velofine.core.hardware.MemoryInfo;
import dev.velofine.core.log.VelofineLog;
import dev.velofine.core.mixin.MixinBridge;
import dev.velofine.core.shader.ShaderSourceInterceptors;
import dev.velofine.legacysupport.gl.HardwareConfirmation;
import dev.velofine.legacysupport.shader.EndPortalArrayIndexPatch;
import dev.velofine.legacysupport.shader.ShaderPatcher;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigSource;

import java.lang.instrument.Instrumentation;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Entry point for the LegacySupport engine, called from {@code VelofineAgent} after the agent
 * self-attaches. Reads the {@link HardwareProfile} (GPU, RAM, disk type - detected once in
 * {@code core}, see {@link HardwareProfiles}), resolves it to a set of {@link Fix}es via
 * {@link FixProfileRules} adjusted by the user's per-fix {@link Tri} overrides, and - only if at
 * least one fix is active - boots Mixin and installs {@code mixins.legacysupport.json}.
 *
 * <p>Since Phase 5, {@code engines.legacySupport} gates the whole engine (mirroring Optimus and
 * Utility), and each {@link Fix} can be forced {@code ON}/{@code OFF} independently of what
 * detection decided, or left at {@code AUTO}. {@link #isFixActive(Fix)} remains the single
 * decision point every mixin reads at runtime - the config system changes what feeds it, not the
 * mixins themselves, exactly as planned when this class was first written.
 */
public final class LegacySupportEngine {

    private static volatile HardwareProfile hardwareProfile = HardwareProfile.unknown();
    private static volatile Set<Fix> activeFixes = EnumSet.noneOf(Fix.class);
    private static final AtomicBoolean hardwareMismatchWarned = new AtomicBoolean();

    private LegacySupportEngine() {
    }

    public static void onAgentAttached(Instrumentation instrumentation) {
        hardwareProfile = HardwareProfiles.get();
        activeFixes = resolveFixes(hardwareProfile);

        VelofineLog.info("LegacySupport", "CPU detected: " + describe(hardwareProfile.cpu()));
        VelofineLog.info("LegacySupport", "GPU detected: " + describe(hardwareProfile.gpu())
                + " [confidence: " + hardwareProfile.gpu().confidence() + "]");
        VelofineLog.info("LegacySupport", "RAM detected: " + describe(hardwareProfile.memory()));
        VelofineLog.info("LegacySupport", "disk type: " + describe(hardwareProfile.disk()));

        if (!ConfigManager.get().engines.legacySupport) {
            VelofineLog.info("LegacySupport", "Engine disabled in config; LegacySupport mixins not applied.");
            return;
        }

        if (activeFixes.isEmpty()) {
            VelofineLog.info("LegacySupport", "No fixes active (detection found nothing, and none were forced on); "
                    + "LegacySupport mixins not applied.");
            return;
        }

        try {
            MixinBootstrap.init();
            // The 1-arg addConfiguration(String) passes a null fallback MixinEnvironment
            // internally, which crashes MixinConfig.onLoad() with an NPE when the config JSON
            // has no explicit "environment"/"selector" - confirmed via this phase's
            // VerifyMixinsHarness. The 2-arg overload correctly resolves
            // MixinEnvironment.getDefaultEnvironment() as the fallback instead.
            Mixins.addConfiguration("mixins.legacysupport.json", (IMixinConfigSource) null);
            MixinBridge.install(instrumentation);

            // core owns the actual @Redirect on GlDevice.compileShader's ShaderSource.get() call and
            // on its later GlslPreprocessor.injectDefines() call (see ShaderSourceInterceptors' class
            // javadoc for why these moved out of legacysupport in Phase 7, and for the two-stage
            // split added in v1.8-Beta) - we just register our patches as candidates.
            //
            // v1.5 Phase 2: gated behind HardwareConfirmation.isConfirmedMatch() first - the
            // Masterdoc's "post-context glGetString confirmation as the actual gate before any
            // shader swap is applied" requirement. Safe by construction: compileShader cannot run
            // without a real GL context already current, so the confirmation check never needs its
            // own mixin (see GlContextSignature's class javadoc).
            if (activeFixes.contains(Fix.SHADER_MIX_PATCH)) {
                // Stage 1 (raw asset text). The end-portal shader is skipped here because stage 2
                // owns it; ShaderPatcher's mix()-literal regex wouldn't match it anyway (apply_fog's
                // mix() call uses a computed blend factor, not a literal).
                ShaderSourceInterceptors.register(ShaderSourceInterceptors.PRIORITY_LEGACY_SUPPORT,
                        (id, type, vanillaSource) -> {
                            if (!confirmedForShaderPatch()) {
                                return Optional.empty();
                            }
                            if (isEndPortalFragment(id, type)) {
                                return Optional.empty();
                            }
                            return Optional.of(ShaderPatcher.patch(vanillaSource, String.valueOf(id)));
                        });
            }

            if (activeFixes.contains(Fix.END_PORTAL_ARRAY_INDEX_PATCH)) {
                // Stage 2 (post-#define text). v1.7-Beta registered this at stage 1 and it therefore
                // never fired: its loop bound PORTAL_LAYERS is a ShaderDefines value on
                // RenderPipelines.END_PORTAL, injected by GlslPreprocessor.injectDefines *after*
                // ShaderSource.get returns, so at stage 1 the identifier simply isn't in the text and
                // the patch bailed out on every launch ("could not resolve loop bound" in the real
                // tester log).
                ShaderSourceInterceptors.registerPostDefines(ShaderSourceInterceptors.PRIORITY_LEGACY_SUPPORT,
                        (id, type, injectedSource) -> {
                            if (!confirmedForShaderPatch() || !isEndPortalFragment(id, type)) {
                                return Optional.empty();
                            }
                            return Optional.of(EndPortalArrayIndexPatch.patch(injectedSource));
                        });
            }

            VelofineLog.info("LegacySupport", "active fixes: "
                    + activeFixes.stream().map(Enum::name).collect(Collectors.joining(", ")));
        } catch (Throwable t) {
            VelofineLog.warn("LegacySupport", "Failed to initialize Mixin pipeline; LegacySupport disabled: " + t);
            t.printStackTrace();
        }
    }

    public static HardwareProfile hardwareProfile() {
        return hardwareProfile;
    }

    public static boolean isFixActive(Fix fix) {
        return activeFixes.contains(fix);
    }

    /** The real {@code minecraft:core/rendertype_end_portal} fragment shader - confirmed via javap. */
    private static boolean isEndPortalFragment(Identifier id, ShaderType type) {
        return type == ShaderType.FRAGMENT
                && id != null
                && "minecraft".equals(id.getNamespace())
                && "core/rendertype_end_portal".equals(id.getPath());
    }

    /**
     * Shared by both interceptor stages, which are registered independently of each other. Cheap to
     * repeat ({@code GlContextSignature} caches after the first {@code glGetString}), but the warning
     * is one-shot: a hybrid/switchable-graphics mismatch is a property of the machine, not of each
     * shader, so logging it per shader per stage would be pure noise.
     */
    private static boolean confirmedForShaderPatch() {
        if (HardwareConfirmation.isConfirmedMatch(hardwareProfile)) {
            return true;
        }
        if (hardwareMismatchWarned.compareAndSet(false, true)) {
            VelofineLog.warn("LegacySupport", "Post-context GL renderer does not confirm the pre-context "
                    + "hardware detection (possible hybrid/switchable-graphics mismatch); skipping all "
                    + "LegacySupport shader patches this launch.");
        }
        return false;
    }

    /**
     * Detection first, then the per-fix {@link Tri} override on top, then (for dev testing only)
     * the {@code -Dvelofine.legacysupport.forceFixes} system property, which replaces the whole
     * set rather than layering - it exists so the Mixin pipeline can be exercised on machines
     * without the reference hardware, same as before Phase 5, and takes priority over the config
     * UI so a diagnostic run is never silently overridden by whatever is saved on disk.
     */
    private static Set<Fix> resolveFixes(HardwareProfile profile) {
        String forced = System.getProperty("velofine.legacysupport.forceFixes");
        if (forced != null) {
            EnumSet<Fix> forcedFixes = EnumSet.noneOf(Fix.class);
            for (String name : forced.split(",")) {
                if (!name.isBlank()) {
                    forcedFixes.add(Fix.valueOf(name.trim()));
                }
            }
            return forcedFixes;
        }

        Set<Fix> detected = FixProfileRules.resolve(profile);
        VelofineConfig.LegacySupportSection overrides = ConfigManager.get().legacySupport;
        EnumSet<Fix> resolved = EnumSet.noneOf(Fix.class);
        for (Fix fix : Fix.values()) {
            if (overrides.mode(fix).resolve(detected.contains(fix))) {
                resolved.add(fix);
            }
        }

        // Phase 9 safe mode: a real crash loop escalates past whatever detection/overrides decided
        // for this one launch only - never written back to config.json, and it reverts on its own
        // the moment a session reaches a healthy running state. See CrashRecovery's class javadoc.
        if (CrashRecovery.isSafeModeActiveThisLaunch()) {
            resolved.add(Fix.GL_COMPATIBILITY_PROFILE);
            resolved.add(Fix.SHADER_MIX_PATCH);
        }

        return resolved;
    }

    private static String describe(CpuInfo info) {
        return info.name() != null ? info.name() : "<unknown>";
    }

    private static String describe(GpuInfo info) {
        String name = info.adapterName() != null ? info.adapterName() : "<unknown>";
        String driver = info.driverVersion() != null ? info.driverVersion() : "?";
        return name + " (driver " + driver + ")";
    }

    private static String describe(MemoryInfo info) {
        if (info.totalPhysicalBytes() <= 0) {
            return "<unknown>";
        }
        double gb = info.totalPhysicalBytes() / (1024.0 * 1024.0 * 1024.0);
        return String.format("%.1f GB%s", gb, info.isLowMemory() ? " (low-memory)" : "");
    }

    private static String describe(DiskInfo info) {
        return info.rotational() ? "HDD (rotational)" : "SSD/unknown (non-rotational)";
    }
}
