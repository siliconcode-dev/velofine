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
import java.util.concurrent.TimeUnit;

/**
 * Detects the CPU model via {@code Get-CimInstance Win32_Processor} (PowerShell/WMI) - mirrors
 * {@link GpuDetector}'s exact {@code ProcessBuilder}/timeout/error-handling shape. Exists so
 * {@link LegacyGpuRegistry} can distinguish reference machine A (i3-3110M + HD Graphics 4000 +
 * driver 15.33.53.5161) from any other Ivy Bridge laptop sharing the same GPU and driver version -
 * see {@code Masterdoc_v1.5.md} S3.
 */
public final class CpuDetector {

    private static final String POWERSHELL_COMMAND =
            "Get-CimInstance Win32_Processor | Select-Object Name | ConvertTo-Json -Compress";

    private CpuDetector() {
    }

    public static CpuInfo detect() {
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
                throw new IOException("Get-CimInstance Win32_Processor query timed out");
            }

            return parse(output);
        } catch (Exception e) {
            VelofineLog.warn("core", "CPU detection failed, assuming unknown: " + e);
            return CpuInfo.unknown();
        }
    }

    private static CpuInfo parse(String json) {
        if (json == null || json.isBlank()) {
            return CpuInfo.unknown();
        }
        JsonElement root = JsonParser.parseString(json);
        // Multi-socket systems report an array - real reference laptops are single-socket, so the
        // first entry is the right one; still handled generically rather than assuming a shape.
        JsonObject obj = root.isJsonArray() ? root.getAsJsonArray().get(0).getAsJsonObject() : root.getAsJsonObject();

        String name = obj.has("Name") && !obj.get("Name").isJsonNull() ? obj.get("Name").getAsString().strip() : null;
        return new CpuInfo(name);
    }
}
