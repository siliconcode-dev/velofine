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

package net.minecraft.server.level;

/**
 * Signature-only stand-in for the real {@code net.minecraft.server.level.ParticleStatus} enum
 * (compile-only, never bundled - see {@code legacysupport/build.gradle.kts}). Unlike {@code
 * ShaderType} (Phase 2's zero-constant stub), {@code OptionsMixin} references {@code ALL}/{@code
 * DECREASED} by name, so those two constants must be present here for javac to resolve them.
 */
public enum ParticleStatus {
    ALL,
    DECREASED,
    MINIMAL
}
