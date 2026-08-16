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

package dev.velofine.diagnostics.model;

/**
 * One WMI-reported graphics adapter, as captured by {@code gpu.GpuProbe}.
 *
 * <p>{@code pnpVendorId}/{@code pnpDeviceId} are parsed from WMI's {@code PNPDeviceID} field
 * (e.g. {@code PCI\VEN_8086&DEV_0166&...}) - the same vendor/device ID a real DXGI adapter
 * description would report, obtained without COM interop (this project avoids JNA/JNI entirely,
 * see {@code core.gpu.GpuDetector}'s own javadoc for the same call on the main app side).
 */
public record GpuInfo(
        String adapterName, String driverVersion, String pnpVendorId, String pnpDeviceId, Long adapterRamBytes) {

    public static GpuInfo unknown() {
        return new GpuInfo(null, null, null, null, null);
    }
}
