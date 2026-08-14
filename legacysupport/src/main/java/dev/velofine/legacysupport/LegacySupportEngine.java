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

import dev.velofine.core.gpu.GpuDetector;
import dev.velofine.core.gpu.GpuInfo;
import dev.velofine.legacysupport.mixin.MixinBridge;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigSource;

import java.lang.instrument.Instrumentation;

/**
 * Entry point for the LegacySupport engine, called from {@code VelofineAgent} after the agent
 * self-attaches. Detects the graphics adapter, decides which fix profile (if any) applies, and -
 * only if one does - boots Mixin and installs {@code mixins.legacysupport.json}.
 *
 * <p>No config/toggle system exists yet (that's Phase 5) - LegacySupport runs unconditionally for
 * now. {@link #shouldForceCompatibilityProfile()}/{@link #shouldPatchShaders()} are the two
 * decision points the actual mixins ({@code GlBackendMixin}/{@code GlDeviceMixin}) read at
 * runtime, so a future config system only needs to change what feeds these, not the mixins
 * themselves.
 */
public final class LegacySupportEngine {

    private static volatile GpuInfo detectedGpu = GpuInfo.unknown();

    private LegacySupportEngine() {
    }

    public static void onAgentAttached(Instrumentation instrumentation) {
        detectedGpu = detectGpu();
        System.out.println("[Velofine][LegacySupport] GPU detected: " + describe(detectedGpu));

        if (detectedGpu.fixProfile() == GpuInfo.FixProfile.NONE) {
            System.out.println("[Velofine][LegacySupport] No known-bad hardware detected; LegacySupport mixins not applied.");
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

            System.out.println("[Velofine][LegacySupport] fix profile: " + detectedGpu.fixProfile());
            System.out.println("[Velofine][LegacySupport] GL context: compatibility profile forced");
            System.out.println("[Velofine][LegacySupport] shader mix() patch: " + (shouldPatchShaders() ? "enabled" : "disabled"));
        } catch (Throwable t) {
            System.err.println("[Velofine][LegacySupport] Failed to initialize Mixin pipeline; LegacySupport disabled: " + t);
            t.printStackTrace();
        }
    }

    public static GpuInfo detectedGpu() {
        return detectedGpu;
    }

    public static boolean shouldForceCompatibilityProfile() {
        return detectedGpu.fixProfile() != GpuInfo.FixProfile.NONE;
    }

    public static boolean shouldPatchShaders() {
        return detectedGpu.fixProfile() == GpuInfo.FixProfile.INTEL_GEN7;
    }

    /**
     * Real detection, unless overridden via {@code -Dvelofine.legacysupport.forceProfile=...}
     * (one of {@link GpuInfo.FixProfile}'s names) - a small testability hook so the Mixin
     * pipeline can be exercised on dev machines that don't have the actual reference hardware.
     * Not used by any real install/launch path.
     */
    private static GpuInfo detectGpu() {
        String forced = System.getProperty("velofine.legacysupport.forceProfile");
        if (forced != null) {
            return new GpuInfo("(forced for testing)", null, GpuInfo.FixProfile.valueOf(forced));
        }
        return GpuDetector.detect();
    }

    private static String describe(GpuInfo info) {
        String name = info.adapterName() != null ? info.adapterName() : "<unknown>";
        String driver = info.driverVersion() != null ? info.driverVersion() : "?";
        return name + " (driver " + driver + ")";
    }
}
