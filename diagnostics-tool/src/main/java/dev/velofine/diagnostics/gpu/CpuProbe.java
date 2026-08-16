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
import dev.velofine.diagnostics.model.CpuInfo;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Queries the real CPU model via {@code Get-CimInstance Win32_Processor} (WMI) - mirrors
 * {@link GpuProbe}'s exact {@code ProcessBuilder}/JSON pattern. Correlates hardware identity with
 * the two confirmed reference CPUs (i3-3110M / i5-3470S) alongside the GPU string, since a GPU
 * string alone can be ambiguous on some systems.
 *
 * <p>Not unit tested end-to-end (real WMI behavior depends on the host machine, same precedent as
 * {@code GpuProbe}); {@link #parse(String)} itself is pure JSON parsing and is unit tested.
 */
public final class CpuProbe {

    private static final String POWERSHELL_COMMAND = "Get-CimInstance Win32_Processor | "
            + "Select-Object Name,NumberOfCores,NumberOfLogicalProcessors | ConvertTo-Json -Compress";

    private CpuProbe() {
    }

    public static CpuInfo query() {
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
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return CpuInfo.unknown();
        }
    }

    static CpuInfo parse(String json) {
        if (json == null || json.isBlank()) {
            return CpuInfo.unknown();
        }
        JsonElement root = JsonParser.parseString(json);
        // Multi-socket systems report an array - real reference laptops are single-socket, so the
        // first entry is the right one; still handled generically rather than assuming a shape.
        JsonObject obj = root.isJsonArray() ? root.getAsJsonArray().get(0).getAsJsonObject() : root.getAsJsonObject();

        String name = obj.has("Name") && !obj.get("Name").isJsonNull() ? obj.get("Name").getAsString().strip() : null;
        Integer cores = obj.has("NumberOfCores") && !obj.get("NumberOfCores").isJsonNull() ? obj.get("NumberOfCores").getAsInt() : null;
        Integer threads = obj.has("NumberOfLogicalProcessors") && !obj.get("NumberOfLogicalProcessors").isJsonNull()
                ? obj.get("NumberOfLogicalProcessors").getAsInt() : null;
        return new CpuInfo(name, cores, threads);
    }
}
