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

    private static final String POWERSHELL_COMMAND =
            "Get-CimInstance Win32_VideoController | Select-Object Name,DriverVersion | ConvertTo-Json -Compress";

    private GpuDetector() {
    }

    public static GpuInfo detect() {
        try {
            List<GpuInfo> adapters = queryAdapters();
            for (GpuInfo adapter : adapters) {
                if (adapter.adapterName() != null && INTEL_GEN7_PATTERN.matcher(adapter.adapterName()).find()) {
                    return new GpuInfo(adapter.adapterName(), adapter.driverVersion(), GpuInfo.FixProfile.INTEL_GEN7);
                }
            }
            for (GpuInfo adapter : adapters) {
                if (adapter.adapterName() != null && INTEL_HD_GRAPHICS_PATTERN.matcher(adapter.adapterName()).find()) {
                    return new GpuInfo(adapter.adapterName(), adapter.driverVersion(), GpuInfo.FixProfile.GENERIC_OLD);
                }
            }
            return adapters.isEmpty() ? GpuInfo.unknown() : adapters.get(0);
        } catch (Exception e) {
            System.err.println("[Velofine][core] GPU detection failed, assuming no fix profile: " + e);
            return GpuInfo.unknown();
        }
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
        result.add(new GpuInfo(name, driverVersion, GpuInfo.FixProfile.NONE));
    }
}
