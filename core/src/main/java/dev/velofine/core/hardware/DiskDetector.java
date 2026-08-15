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

import dev.velofine.core.log.VelofineLog;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * Detects whether the drive backing a given game directory is a rotational HDD, via
 * {@code Get-Partition}/{@code Get-PhysicalDisk} (PowerShell/Windows Storage Management) —
 * resolves the directory's drive letter to its partition's disk number, then reads that disk's
 * {@code MediaType} ({@code "HDD"}/{@code "SSD"}/{@code "SCM"}/{@code "Unspecified"} — a friendly
 * string from {@code Get-PhysicalDisk}, not the raw ambiguous WMI numeric code).
 *
 * <p>The game directory path is passed as a script argument (not string-interpolated into the
 * command) to avoid any quoting/injection concerns.
 */
public final class DiskDetector {

    private static final String POWERSHELL_SCRIPT =
            "$drive = (Get-Item -LiteralPath $args[0]).PSDrive.Name; "
                    + "$diskNumber = (Get-Partition -DriveLetter $drive | Select-Object -First 1).DiskNumber; "
                    + "Get-PhysicalDisk | Where-Object DeviceId -eq $diskNumber | Select-Object MediaType | ConvertTo-Json -Compress";

    private DiskDetector() {
    }

    public static DiskInfo detect(Path gameDirectory) {
        if (gameDirectory == null) {
            return DiskInfo.unknown();
        }
        try {
            ProcessBuilder builder = new ProcessBuilder(
                    "powershell", "-NoProfile", "-NonInteractive", "-Command", POWERSHELL_SCRIPT,
                    gameDirectory.toAbsolutePath().toString());
            builder.redirectErrorStream(false);
            Process process = builder.start();

            String output;
            try (InputStream in = process.getInputStream()) {
                output = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IOException("disk-type query timed out");
            }

            return parse(output);
        } catch (Exception e) {
            VelofineLog.warn("core", "Disk type detection failed, assuming not rotational: " + e);
            return DiskInfo.unknown();
        }
    }

    private static DiskInfo parse(String json) {
        if (json == null || json.isBlank()) {
            return DiskInfo.unknown();
        }
        boolean rotational = json.toUpperCase(java.util.Locale.ROOT).contains("\"MEDIATYPE\":\"HDD\"");
        return new DiskInfo(rotational);
    }
}
