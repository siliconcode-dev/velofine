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

import dev.velofine.diagnostics.model.GpuInfo;
import java.util.List;
import java.util.Optional;

/**
 * Generates a tool-warning when the GL context bound to a GPU that couldn't be confidently matched
 * against WMI's adapter list on a multi-GPU (hybrid/switchable-graphics) system.
 *
 * <p><b>Scope is honestly limited to detect+warn, not a true forced override.</b> jpackage's
 * app-image native launcher can't be given custom Win32 exports (e.g. {@code NvOptimusEnablement})
 * without a custom native launcher stub - out of proportion for a diagnostic tool. The only real
 * lever available is telling the tester to set Windows' own per-app Graphics preference and
 * re-run, which this advisory text does.
 */
public final class GpuSelectionAdvisor {

    private GpuSelectionAdvisor() {
    }

    /** Returns advisory text when warranted, or empty when there's nothing worth flagging. */
    public static Optional<String> adviseIfNeeded(List<GpuInfo> adapters, Integer boundAdapterIndex, String glRenderer) {
        if (boundAdapterIndex != null || adapters == null || adapters.size() <= 1) {
            return Optional.empty();
        }
        return Optional.of("Could not confidently match GL_RENDERER (\"" + glRenderer + "\") to one of the "
                + adapters.size() + " WMI-reported adapters - possible hybrid/switchable-graphics laptop. "
                + "If this looks like the wrong GPU, set Windows Graphics preference for "
                + "VelofineDiagnostic.exe to \"High performance\" and re-run.");
    }
}
