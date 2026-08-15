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
import dev.velofine.core.hardware.DiskDetector;
import dev.velofine.core.hardware.DiskInfo;
import dev.velofine.core.hardware.Fix;
import dev.velofine.core.hardware.FixProfileRules;
import dev.velofine.core.hardware.HardwareProfile;
import dev.velofine.core.hardware.MemoryDetector;
import dev.velofine.core.hardware.MemoryInfo;
import dev.velofine.core.log.VelofineLog;
import dev.velofine.legacysupport.mixin.MixinBridge;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigSource;

import java.lang.instrument.Instrumentation;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Entry point for the LegacySupport engine, called from {@code VelofineAgent} after the agent
 * self-attaches. Builds a {@link HardwareProfile} (GPU, RAM, disk type), resolves it to a set of
 * {@link Fix}es via {@link FixProfileRules}, and - only if at least one fix is active - boots
 * Mixin and installs {@code mixins.legacysupport.json}.
 *
 * <p>No config/toggle system exists yet (that's Phase 5) - LegacySupport runs unconditionally for
 * now. {@link #isFixActive(Fix)} is the single decision point every mixin reads at runtime, so a
 * future config system only needs to change what feeds it, not the mixins themselves.
 */
public final class LegacySupportEngine {

    private static volatile HardwareProfile hardwareProfile = HardwareProfile.unknown();
    private static volatile Set<Fix> activeFixes = EnumSet.noneOf(Fix.class);

    private LegacySupportEngine() {
    }

    public static void onAgentAttached(Instrumentation instrumentation) {
        hardwareProfile = buildHardwareProfile();
        activeFixes = resolveFixes(hardwareProfile);

        VelofineLog.info("LegacySupport", "GPU detected: " + describe(hardwareProfile.gpu()));
        VelofineLog.info("LegacySupport", "RAM detected: " + describe(hardwareProfile.memory()));
        VelofineLog.info("LegacySupport", "disk type: " + describe(hardwareProfile.disk()));

        if (activeFixes.isEmpty()) {
            VelofineLog.info("LegacySupport", "No known-bad hardware detected; LegacySupport mixins not applied.");
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

    private static Set<Fix> resolveFixes(HardwareProfile profile) {
        String forced = System.getProperty("velofine.legacysupport.forceFixes");
        if (forced == null) {
            return FixProfileRules.resolve(profile);
        }
        EnumSet<Fix> forcedFixes = EnumSet.noneOf(Fix.class);
        for (String name : forced.split(",")) {
            if (!name.isBlank()) {
                forcedFixes.add(Fix.valueOf(name.trim()));
            }
        }
        return forcedFixes;
    }

    /**
     * Real detection, unless overridden via {@code -Dvelofine.legacysupport.forceFixes=...} (a
     * comma-separated list of {@link Fix} names) - a small testability hook so the Mixin pipeline
     * can be exercised on dev machines that don't have the actual reference hardware. Not used by
     * any real install/launch path. When forcing fixes, hardware detection still runs (for
     * accurate logging) but its result is ignored in favor of the forced set.
     */
    private static HardwareProfile buildHardwareProfile() {
        GpuInfo gpu = GpuDetector.detect();
        MemoryInfo memory = MemoryDetector.detect();
        String gameDir = System.getProperty("velofine.gameDir");
        DiskInfo disk = gameDir != null ? DiskDetector.detect(Path.of(gameDir)) : DiskInfo.unknown();
        return new HardwareProfile(gpu, memory, disk);
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
