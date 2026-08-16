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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A shader-pack-defined enum/numeric option - e.g. {@code #define SHADOW_QUALITY 1 // [0 1 2 3]}.
 * {@link #create} returns {@code null} when the comment has no {@code [...]} allowed-value list,
 * meaning the line is a plain numeric {@code #define}, not a selectable option. Adapted from
 * IrisShaders/Iris ({@code common/.../shaderpack/option/StringOption.java}), LGPL-3.0.
 */
public final class StringOption {
    private final OptionType type;
    private final String name;
    private final String comment;
    private final String defaultValue;
    private final List<String> allowedValues;

    private StringOption(OptionType type, String name, String comment, String defaultValue, List<String> allowedValues) {
        this.type = type;
        this.name = name;
        this.comment = (comment == null || comment.isEmpty()) ? null : comment;
        this.defaultValue = Objects.requireNonNull(defaultValue);
        this.allowedValues = allowedValues;
    }

    public static StringOption create(OptionType type, String name, String comment, String defaultValue) {
        if (comment == null) {
            return null;
        }

        int open = comment.indexOf('[');
        if (open == -1) {
            return null;
        }
        int close = comment.indexOf(']', open);
        if (close == -1) {
            return null;
        }

        List<String> allowedValues = new ArrayList<>(List.of(comment.substring(open + 1, close).trim().split("\\s+")));
        String trimmedComment = (comment.substring(0, open) + comment.substring(close + 1)).trim();

        if (!allowedValues.contains(defaultValue)) {
            allowedValues.add(defaultValue);
        }

        return new StringOption(type, name, trimmedComment, defaultValue, Collections.unmodifiableList(allowedValues));
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

    public String getDefaultValue() {
        return defaultValue;
    }

    public List<String> getAllowedValues() {
        return allowedValues;
    }
}
