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

package dev.velofine.core.gpu;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.velofine.core.log.VelofineLog;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Detects the active graphics adapter on Windows via {@code Get-CimInstance Win32_VideoController}
 * (PowerShell/WMI) and classifies known-bad Ivy Bridge Gen7 Intel iGPUs.
 *
 * <p>Deliberately avoids native calls (JNA/JNI): Sodium detects this same hardware via low-level
 * D3DKMT adapter queries for the {@code ig7icd32/64.dll} OpenGL ICD driver name (see
 * {@code IntelWorkarounds.java} in CaffeineMC/sodium) — more precise, but Velofine is Windows-only
 * for v1 already, so shelling out to PowerShell for the adapter name + driver version is a much
 * simpler, lower-risk way to identify the same hardware class.
 */
public final class GpuDetector {

    private static final Pattern INTEL_GEN7_PATTERN =
            Pattern.compile("Intel.*HD Graphics (2500|4000)", Pattern.CASE_INSENSITIVE);

    // Broader net for the "fallback safe mode" case: any other Intel HD Graphics-branded iGPU
    // (HD 2000/3000, 4400/4600, etc.) - same driver-support-cutoff era as Gen7, no confirmed bug
    // of our own, but a reasonable safety net per Build_plan's "fallback for other unknown/old
    // GPUs" scope.
    private static final Pattern INTEL_HD_GRAPHICS_PATTERN =
            Pattern.compile("Intel.*HD Graphics", Pattern.CASE_INSENSITIVE);

    // v1.8-Beta: WMI does not always report the GPU model number. On the real i5-3470S/HD Graphics
    // 2500 reference machine, Win32_VideoController.Name comes back as the bare string
    // "Intel(R) HD Graphics" - no "2500" - so INTEL_GEN7_PATTERN above misses and the machine fell
    // through to GENERIC_OLD, which grants only GL_COMPATIBILITY_PROFILE. (Confirmed in a real
    // v1.7-Beta tester log; the post-context GL_RENDERER string on the same machine *does* say
    // "Intel(R) HD Graphics 2500", so the model number exists, just not at pre-context WMI time.)
    //
    // The CPU resolves the ambiguity rigorously rather than by guessing: Ivy Bridge (3rd-gen Core,
    // model numbers 3xxx) only ever shipped two iGPUs - HD 2500 and HD 4000 - and both are already
    // INTEL_GEN7. So a generic "Intel ... HD Graphics" adapter on an Ivy Bridge CPU is necessarily
    // one of the two.
    private static final Pattern IVY_BRIDGE_CPU_PATTERN =
            Pattern.compile("Core.*i[357]-3\\d{3}", Pattern.CASE_INSENSITIVE);

    private static final String POWERSHELL_COMMAND =
            "Get-CimInstance Win32_VideoController | Select-Object Name,DriverVersion | ConvertTo-Json -Compress";

    private GpuDetector() {
    }

    public static GpuInfo detect(CpuInfo cpu) {
        try {
            List<GpuInfo> adapters = queryAdapters();
            GpuInfo resolved = null;
            for (GpuInfo adapter : adapters) {
                if (classifyFixProfile(adapter.adapterName(), cpu) == GpuInfo.FixProfile.INTEL_GEN7) {
                    resolved = new GpuInfo(adapter.adapterName(), adapter.driverVersion(), GpuInfo.FixProfile.INTEL_GEN7, GpuConfidence.NONE);
                    break;
                }
            }
            if (resolved == null) {
                for (GpuInfo adapter : adapters) {
                    if (classifyFixProfile(adapter.adapterName(), cpu) == GpuInfo.FixProfile.GENERIC_OLD) {
                        resolved = new GpuInfo(adapter.adapterName(), adapter.driverVersion(), GpuInfo.FixProfile.GENERIC_OLD, GpuConfidence.NONE);
                        break;
                    }
                }
            }
            if (resolved == null) {
                resolved = adapters.isEmpty() ? GpuInfo.unknown() : adapters.get(0);
            }

            GpuConfidence confidence = LegacyGpuRegistry.classify(resolved, cpu);
            return new GpuInfo(resolved.adapterName(), resolved.driverVersion(), resolved.fixProfile(), confidence);
        } catch (Exception e) {
            VelofineLog.warn("core", "GPU detection failed, assuming no fix profile: " + e);
            return GpuInfo.unknown();
        }
    }

    /**
     * Pure classification of one adapter name (plus the CPU, which disambiguates generic Intel
     * adapter names - see {@link #IVY_BRIDGE_CPU_PATTERN}). Extracted from {@link #detect(CpuInfo)}
     * in v1.8-Beta so this is unit-testable at all: {@code detect} shells out to PowerShell, so the
     * classification logic could never be exercised without real WMI before.
     */
    static GpuInfo.FixProfile classifyFixProfile(String adapterName, CpuInfo cpu) {
        if (adapterName == null) {
            return GpuInfo.FixProfile.NONE;
        }
        if (INTEL_GEN7_PATTERN.matcher(adapterName).find()) {
            return GpuInfo.FixProfile.INTEL_GEN7;
        }
        if (INTEL_HD_GRAPHICS_PATTERN.matcher(adapterName).find()) {
            // Generic "Intel ... HD Graphics" with no model number: an Ivy Bridge CPU pins it to
            // HD 2500 or HD 4000, the only iGPUs that generation ever shipped.
            String cpuName = cpu != null ? cpu.name() : null;
            if (cpuName != null && IVY_BRIDGE_CPU_PATTERN.matcher(cpuName).find()) {
                return GpuInfo.FixProfile.INTEL_GEN7;
            }
            return GpuInfo.FixProfile.GENERIC_OLD;
        }
        return GpuInfo.FixProfile.NONE;
    }

    private static List<GpuInfo> queryAdapters() throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder("powershell", "-NoProfile", "-NonInteractive", "-Command", POWERSHELL_COMMAND);
        builder.redirectErrorStream(false);
        Process process = builder.start();

        String output;
        try (InputStream in = process.getInputStream()) {
            output = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        boolean finished = process.waitFor(10, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IOException("Get-CimInstance query timed out");
        }

        return parseAdapters(output);
    }

    private static List<GpuInfo> parseAdapters(String json) {
        List<GpuInfo> result = new ArrayList<>();
        if (json == null || json.isBlank()) {
            return result;
        }

        JsonElement root = JsonParser.parseString(json);
        if (root.isJsonArray()) {
            for (JsonElement element : root.getAsJsonArray()) {
                addAdapter(result, element.getAsJsonObject());
            }
        } else if (root.isJsonObject()) {
            addAdapter(result, root.getAsJsonObject());
        }
        return result;
    }

    private static void addAdapter(List<GpuInfo> result, JsonObject obj) {
        String name = obj.has("Name") && !obj.get("Name").isJsonNull() ? obj.get("Name").getAsString() : null;
        String driverVersion =
                obj.has("DriverVersion") && !obj.get("DriverVersion").isJsonNull() ? obj.get("DriverVersion").getAsString() : null;
        result.add(new GpuInfo(name, driverVersion, GpuInfo.FixProfile.NONE, GpuConfidence.NONE));
    }
}
