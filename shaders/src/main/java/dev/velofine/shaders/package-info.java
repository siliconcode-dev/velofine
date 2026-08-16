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

/**
 * OptiFine/Iris-format shader pack support (Phase 7): pack discovery, {@code shaders.properties}
 * and ID-map parsing, the shader-pack options data model, and the gbuffers/deferred/composite/
 * shadow render pipeline. A Utility Engine feature (Masterdoc 4.3) split into its own module for
 * build isolation given its scope - {@code dev.velofine.utility.UtilityEngine} owns the toggle,
 * GUI integration, and mixin-config wiring.
 */
package dev.velofine.shaders;
