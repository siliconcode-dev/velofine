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

package dev.velofine.core.mixin;

import org.spongepowered.asm.service.IGlobalPropertyService;
import org.spongepowered.asm.service.IPropertyKey;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mixin's "blackboard" (a simple global key-value store it uses internally for bootstrap state).
 * Discovered via {@code ServiceLoader} through {@code META-INF/services/
 * org.spongepowered.asm.service.IGlobalPropertyService} - required, Mixin bootstrap hard-fails
 * without one registered. Shared across every engine for the same reason as
 * {@link VelofineMixinService} - only one can be registered per JVM.
 */
public final class VelofinePropertyService implements IGlobalPropertyService {

    private final Map<String, VelofinePropertyKey> keys = new ConcurrentHashMap<>();
    private final Map<String, Object> values = new ConcurrentHashMap<>();

    @Override
    public IPropertyKey resolveKey(String name) {
        return keys.computeIfAbsent(name, VelofinePropertyKey::new);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getProperty(IPropertyKey key) {
        return (T) values.get(key.toString());
    }

    @Override
    public void setProperty(IPropertyKey key, Object value) {
        values.put(key.toString(), value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getProperty(IPropertyKey key, T defaultValue) {
        Object value = values.get(key.toString());
        return value != null ? (T) value : defaultValue;
    }

    @Override
    public String getPropertyString(IPropertyKey key, String defaultValue) {
        Object value = values.get(key.toString());
        return value != null ? value.toString() : defaultValue;
    }
}
