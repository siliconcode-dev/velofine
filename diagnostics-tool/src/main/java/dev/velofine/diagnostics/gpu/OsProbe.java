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
import dev.velofine.diagnostics.model.OsInfo;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Queries the real Windows OS build/version via {@code Get-CimInstance Win32_OperatingSystem}
 * (WMI) - mirrors {@link GpuProbe}'s exact pattern. The driver source (Windows Update vs.
 * OEM-provided) differs across the two confirmed reference machines, so the exact build matters.
 *
 * <p>Not unit tested end-to-end (real WMI behavior depends on the host machine); {@link #parse}
 * itself is pure JSON parsing and is unit tested.
 */
public final class OsProbe {

    private static final String POWERSHELL_COMMAND = "Get-CimInstance Win32_OperatingSystem | "
            + "Select-Object Caption,Version,BuildNumber,OSArchitecture | ConvertTo-Json -Compress";

    private OsProbe() {
    }

    public static OsInfo query() {
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
                throw new IOException("Get-CimInstance Win32_OperatingSystem query timed out");
            }

            return parse(output);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return OsInfo.unknown();
        }
    }

    static OsInfo parse(String json) {
        if (json == null || json.isBlank()) {
            return OsInfo.unknown();
        }
        JsonElement root = JsonParser.parseString(json);
        JsonObject obj = root.isJsonArray() ? root.getAsJsonArray().get(0).getAsJsonObject() : root.getAsJsonObject();

        return new OsInfo(
                stringField(obj, "Caption"), stringField(obj, "Version"),
                stringField(obj, "BuildNumber"), stringField(obj, "OSArchitecture"));
    }

    private static String stringField(JsonObject obj, String field) {
        return obj.has(field) && !obj.get(field).isJsonNull() ? obj.get(field).getAsString() : null;
    }
}
