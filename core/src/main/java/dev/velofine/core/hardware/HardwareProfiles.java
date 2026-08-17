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

package dev.velofine.core.hardware;

import dev.velofine.core.gpu.CpuDetector;
import dev.velofine.core.gpu.CpuInfo;
import dev.velofine.core.gpu.GpuDetector;
import dev.velofine.core.gpu.GpuInfo;

import java.nio.file.Path;

/**
 * The one place the machine is inspected, and the one place the answer is cached.
 *
 * <p>Before Phase 5 this lived as a {@code volatile} field on {@code LegacySupportEngine}, which
 * meant Optimus and Utility could not reach it without depending on LegacySupport - a dependency
 * that would have broken the engine-independence rule the moment Utility's "safe-by-default on
 * weak hardware" policy and the governor's auto-on default both needed the same answer.
 *
 * <p>Detection shells out to PowerShell three times (GPU, RAM, disk), so it is done lazily and at
 * most once per JVM.
 */
public final class HardwareProfiles {

    private static final Object LOCK = new Object();

    private static volatile HardwareProfile cached;

    private HardwareProfiles() {
    }

    public static HardwareProfile get() {
        HardwareProfile local = cached;
        if (local != null) {
            return local;
        }
        synchronized (LOCK) {
            if (cached == null) {
                cached = detect();
            }
            return cached;
        }
    }

    /**
     * True for the hardware class Velofine exists for: an old Intel iGPU, a 4GB-class machine, or
     * a rotational game-directory disk. Any one of the three is enough - the reference machines
     * differ from each other (the i3-3110M is an HDD laptop, the i5-3470S an SSD desktop), so this
     * has to be a union rather than an intersection.
     */
    public static boolean isWeakHardware() {
        HardwareProfile profile = get();
        return profile.gpu().fixProfile() != GpuInfo.FixProfile.NONE
                || profile.memory().isLowMemory()
                || profile.disk().rotational();
    }

    private static HardwareProfile detect() {
        // v1.5 Phase 2: CPU is detected alongside GPU/RAM/disk so GpuDetector can compute a
        // confidence tier (LegacyGpuRegistry needs both CPU and GPU to recognize reference
        // machine A specifically, not just its GPU family).
        CpuInfo cpu = CpuDetector.detect();
        GpuInfo gpu = GpuDetector.detect(cpu);
        MemoryInfo memory = MemoryDetector.detect();
        // The disk check needs to know which drive the game directory lives on, which is only
        // known per-launch (Main publishes it from the real --gameDir arg before self-attaching).
        String gameDir = System.getProperty("velofine.gameDir");
        DiskInfo disk = gameDir != null ? DiskDetector.detect(Path.of(gameDir)) : DiskInfo.unknown();
        return new HardwareProfile(cpu, gpu, memory, disk);
    }
}
