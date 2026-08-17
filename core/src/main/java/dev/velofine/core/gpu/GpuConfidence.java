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

/**
 * How confidently {@link LegacyGpuRegistry} believes the detected hardware is one of v1.5's two
 * personally-verified reference machines, versus just a family-similar match. Per
 * {@code Masterdoc_v1.5.md} S4's "confidence-tiered fixes" policy: {@link #EXACT_VERIFIED}
 * hardware is eligible for the full targeted shader-patch fix (Phase 4, not yet built);
 * {@link #FAMILY_MATCH} hardware is eligible only for the conservative compatibility-renderer
 * fallback (Phase 3, not yet built). Phase 2 only computes and exposes this signal - it does not
 * yet gate any Phase 3/4 behavior that doesn't exist.
 */
public enum GpuConfidence {
    NONE,
    FAMILY_MATCH,
    EXACT_VERIFIED
}
