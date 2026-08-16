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

/** One shader's compile-outcome classification across two compared reports - see {@code report.ReportComparator}. */
public record ShaderComparisonEntry(
        String shaderName, String stage, String defineVariant, Classification classification,
        Boolean successInA, Boolean successInB) {

    public enum Classification {
        /** Same pass/fail outcome in both reports. */
        UNCHANGED,
        /** Passed in A, now fails in B - a real regression. */
        REGRESSED,
        /** Failed in A, now passes in B - progress, e.g. Phase 4's fix iteration working. */
        FIXED,
        /** Present in report A only (e.g. removed from a later inventory). */
        ONLY_IN_A,
        /** Present in report B only (e.g. added in a later inventory). */
        ONLY_IN_B
    }
}
