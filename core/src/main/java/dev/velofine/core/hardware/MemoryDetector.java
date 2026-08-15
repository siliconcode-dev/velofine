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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.velofine.core.log.VelofineLog;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Detects total installed physical RAM on Windows via
 * {@code Get-CimInstance Win32_ComputerSystem} (PowerShell/WMI) — same query mechanism and
 * fail-open behavior as {@link dev.velofine.core.gpu.GpuDetector}.
 */
public final class MemoryDetector {

    private static final String POWERSHELL_COMMAND =
            "Get-CimInstance Win32_ComputerSystem | Select-Object TotalPhysicalMemory | ConvertTo-Json -Compress";

    private MemoryDetector() {
    }

    public static MemoryInfo detect() {
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
                throw new IOException("Get-CimInstance Win32_ComputerSystem query timed out");
            }

            return parse(output);
        } catch (Exception e) {
            VelofineLog.warn("core", "Memory detection failed, assuming unknown: " + e);
            return MemoryInfo.unknown();
        }
    }

    private static MemoryInfo parse(String json) {
        if (json == null || json.isBlank()) {
            return MemoryInfo.unknown();
        }
        JsonElement root = JsonParser.parseString(json);
        JsonObject obj = root.isJsonArray() ? root.getAsJsonArray().get(0).getAsJsonObject() : root.getAsJsonObject();
        if (!obj.has("TotalPhysicalMemory") || obj.get("TotalPhysicalMemory").isJsonNull()) {
            return MemoryInfo.unknown();
        }
        return new MemoryInfo(obj.get("TotalPhysicalMemory").getAsLong());
    }
}
