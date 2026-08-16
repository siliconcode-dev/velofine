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

package dev.velofine.shaders.format;

/**
 * A namespace:path pair as written in {@code block,item,entity}.properties, kept deliberately
 * decoupled from any real Minecraft {@code Identifier} type - ID-map parsing runs before a
 * shaderpack is matched against a running game, and validation/conversion to a real identifier is
 * the caller's job. Adapted from IrisShaders/Iris
 * ({@code common/.../shaderpack/materialmap/NamespacedId.java}), LGPL-3.0.
 */
public record NamespacedId(String namespace, String path) {

    public NamespacedId {
        if (namespace == null || namespace.isEmpty()) {
            namespace = "minecraft";
        }
    }

    public static NamespacedId parse(String raw) {
        int colon = raw.indexOf(':');
        if (colon == -1) {
            return new NamespacedId("minecraft", raw);
        }
        return new NamespacedId(raw.substring(0, colon), raw.substring(colon + 1));
    }

    @Override
    public String toString() {
        return namespace + ":" + path;
    }
}
