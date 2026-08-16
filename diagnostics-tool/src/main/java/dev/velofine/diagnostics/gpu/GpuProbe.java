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

package dev.velofine.diagnostics.gpu;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.velofine.diagnostics.model.GpuInfo;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Queries the real graphics adapter(s) via {@code Get-CimInstance Win32_VideoController} (WMI).
 *
 * <p>Deliberately duplicates the shape of {@code core.gpu.GpuDetector}'s WMI query rather than
 * depending on {@code :core} - {@code diagnostics-tool} is a standalone tool with zero
 * {@code project(":...")} dependencies (see {@code build.gradle.kts}), so this is a small,
 * intentional, from-scratch reimplementation, not an oversight to "fix" into a shared dependency
 * later. Unlike {@code GpuDetector}, this probe doesn't classify hardware into a fix profile - Phase
 * 1 only needs to capture raw facts for the report, not decide anything.
 *
 * <p>Also selects {@code PNPDeviceID} (parsed for real vendor/device IDs, the WMI-native
 * equivalent of what a DXGI adapter description would report - see {@code model.GpuInfo}'s
 * javadoc) and {@code AdapterRAM} (VRAM in bytes) - extending the same one WMI round-trip rather
 * than adding a second query.
 *
 * <p>Not unit tested: real WMI behavior depends on the host machine, same precedent as
 * {@code core.gpu.GpuDetector} having no test either. {@link #parsePnpId(String)} itself IS unit
 * tested (see {@code GpuProbeTest}) since it's pure string parsing with no WMI/process dependency.
 */
public final class GpuProbe {

    private static final String POWERSHELL_COMMAND = "Get-CimInstance Win32_VideoController | "
            + "Select-Object Name,DriverVersion,PNPDeviceID,AdapterRAM | ConvertTo-Json -Compress";

    private static final Pattern PNP_VEN_DEV = Pattern.compile("VEN_([0-9A-Fa-f]{4}).*?DEV_([0-9A-Fa-f]{4})");

    private GpuProbe() {
    }

    /** Returns every adapter WMI reports, or a single {@link GpuInfo#unknown()} entry on failure. */
    public static List<GpuInfo> queryAdapters() {
        try {
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

            List<GpuInfo> adapters = parseAdapters(output);
            return adapters.isEmpty() ? List.of(GpuInfo.unknown()) : adapters;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return List.of(GpuInfo.unknown());
        }
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
        String name = stringField(obj, "Name");
        String driverVersion = stringField(obj, "DriverVersion");
        String pnpDeviceIdRaw = stringField(obj, "PNPDeviceID");
        Long adapterRamBytes = longField(obj, "AdapterRAM");

        String[] venDev = parsePnpId(pnpDeviceIdRaw);
        result.add(new GpuInfo(name, driverVersion, venDev[0], venDev[1], adapterRamBytes));
    }

    private static String stringField(JsonObject obj, String field) {
        return obj.has(field) && !obj.get(field).isJsonNull() ? obj.get(field).getAsString() : null;
    }

    private static Long longField(JsonObject obj, String field) {
        return obj.has(field) && !obj.get(field).isJsonNull() ? obj.get(field).getAsLong() : null;
    }

    /**
     * Parses {@code VEN_xxxx}/{@code DEV_xxxx} out of a raw WMI {@code PNPDeviceID} string (e.g.
     * {@code PCI\VEN_8086&DEV_0166&SUBSYS_...}). Returns {@code {null, null}} when absent or
     * unparsable rather than throwing - a malformed/missing PNPDeviceID is a real possibility on
     * some hardware and must not fail the whole adapter query.
     */
    static String[] parsePnpId(String pnpDeviceId) {
        if (pnpDeviceId == null) {
            return new String[] {null, null};
        }
        Matcher matcher = PNP_VEN_DEV.matcher(pnpDeviceId);
        if (matcher.find()) {
            return new String[] {"0x" + matcher.group(1).toUpperCase(), "0x" + matcher.group(2).toUpperCase()};
        }
        return new String[] {null, null};
    }
}
