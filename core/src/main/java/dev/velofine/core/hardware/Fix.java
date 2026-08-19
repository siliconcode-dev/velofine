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

/**
 * A single named LegacySupport fix. Mixins/engine code check {@code
 * LegacySupportEngine.isFixActive(Fix)} instead of branching on raw hardware-detection output
 * directly, so adding a new hardware class only means adding a {@link FixProfileRules} entry, not
 * touching every call site (Build_plan Phase 3's "generalize... without re-architecting" goal).
 */
public enum Fix {
    /** Force a 3.3 Compatibility Profile GL context instead of vanilla's Core Forward-Compatible one. */
    GL_COMPATIBILITY_PROFILE,
    /** Defensive GLSL {@code mix()} constant-folding workaround (Phase 2). */
    SHADER_MIX_PATCH,
    /** Chunk-load-stall smoothing for rotational (HDD) game-directory drives. */
    IO_STALL_SMOOTHING,
    /** Lower first-run video-setting defaults for 4GB-class systems. */
    MEMORY_SAVING_DEFAULTS,
    /**
     * v1.6-Beta: re-implements animated-texture frame uploads (water, lava, fire, lanterns) the old
     * synchronous way instead of Mojang's newer GPU staging-buffer path, which some Intel Gen7
     * drivers upload from the wrong offset (MC-308593). See
     * {@code legacysupport.texture.AnimatedTextureUploadFix} for the confirmed root cause.
     */
    ANIMATED_TEXTURE_UPLOAD_FIX,
    /**
     * v1.7-Beta: unrolls a compile-time-bounded loop in {@code rendertype_end_portal}'s fragment
     * shader that indexes a {@code const vec3[]} array with the loop variable - a real, non-false-
     * positive finding from tester diagnostic reports, and a known cross-vendor GPU-driver-compiler
     * weak point. See {@code legacysupport.shader.EndPortalArrayIndexPatch} for the confirmed source
     * and mechanism.
     */
    END_PORTAL_ARRAY_INDEX_PATCH
}
