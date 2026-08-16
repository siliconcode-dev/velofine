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

package dev.velofine.core.shader;

import java.util.List;

/**
 * A plain, module-agnostic snapshot of one shader-pack-defined option, for {@code core}'s GUI code
 * to render without depending on the {@code shaders} module's own richer {@code BooleanOption}/
 * {@code StringOption} types - deliberately flattened, the same reasoning as {@link
 * ShaderSourceInterceptors} for why the dependency has to run this direction (the GUI framework
 * lives in {@code core}; {@code shaders} already depends on {@code core}, so {@code core} depending
 * back on {@code shaders} would be circular).
 *
 * @param allowedValues empty for a boolean option (its two values are implicit); the pack's
 *                       declared choices for a string/enum option.
 */
public record ShaderPackOptionInfo(String name, boolean booleanOption, String comment,
        String currentValue, List<String> allowedValues) {
}
