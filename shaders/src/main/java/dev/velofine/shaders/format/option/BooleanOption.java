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

package dev.velofine.shaders.format.option;

import java.util.Optional;

/**
 * A shader-pack-defined boolean toggle - e.g. {@code #define BLOOM // Enables bloom}. Adapted from
 * IrisShaders/Iris ({@code common/.../shaderpack/option/BooleanOption.java}), LGPL-3.0.
 */
public final class BooleanOption {
    private final OptionType type;
    private final String name;
    private final String comment;
    private final boolean defaultValue;

    public BooleanOption(OptionType type, String name, String comment, boolean defaultValue) {
        this.type = type;
        this.name = name;
        this.comment = (comment == null || comment.isEmpty()) ? null : comment;
        this.defaultValue = defaultValue;
    }

    public OptionType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public Optional<String> getComment() {
        return Optional.ofNullable(comment);
    }

    public boolean getDefaultValue() {
        return defaultValue;
    }
}
