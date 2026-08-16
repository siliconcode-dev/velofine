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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.BiConsumer;

/**
 * {@link Properties} preserves insertion order in practice (backed by a {@link java.util.Hashtable}
 * it is not supposed to), but a shaderpack's own directive ordering is significant - e.g. profile
 * precedence in {@code profile.<name>=} is "later wins" per pack author intent. Backing this with a
 * real {@link LinkedHashMap} makes that guarantee explicit rather than relying on JDK internals.
 * Adapted from IrisShaders/Iris ({@code common/.../shaderpack/option/OrderBackedProperties.java}),
 * LGPL-3.0.
 */
public class OrderBackedProperties extends Properties {

    private final transient Map<Object, Object> backing = new LinkedHashMap<>();

    @Override
    public synchronized Object put(Object key, Object value) {
        backing.put(key, value);
        return super.put(key, value);
    }

    @Override
    public synchronized void forEach(BiConsumer<? super Object, ? super Object> action) {
        backing.forEach(action);
    }
}
