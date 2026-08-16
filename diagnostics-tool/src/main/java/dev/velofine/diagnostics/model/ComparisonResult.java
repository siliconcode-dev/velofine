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

import java.util.List;

/**
 * Result of {@code report.ReportComparator} diffing two {@link DiagnosticReport}s - Phase 4's
 * fix-iteration loop's actual use case (before/after a candidate shader fix). {@code sameMachine}
 * is checked <b>first</b>: comparing shader results across two genuinely different machines
 * (different GPU/CPU) would be a meaningless comparison, so this is surfaced loudly rather than
 * silently producing a diff that looks like it means something.
 */
public record ComparisonResult(
        boolean sameMachine,
        String machineMismatchNote,
        List<ShaderComparisonEntry> entries) {

    public long regressedCount() {
        return entries.stream().filter(e -> e.classification() == ShaderComparisonEntry.Classification.REGRESSED).count();
    }

    public long fixedCount() {
        return entries.stream().filter(e -> e.classification() == ShaderComparisonEntry.Classification.FIXED).count();
    }
}
