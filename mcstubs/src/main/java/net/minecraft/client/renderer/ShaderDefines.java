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

package net.minecraft.client.renderer;

import java.util.Map;
import java.util.Set;

/**
 * Confirmed real via javap against the real 26.2 jar (the real type is a record with these three
 * accessors plus a canonical constructor and codec). Velofine never constructs one - it only needs
 * the type to exist so {@code core.mixin.GlDeviceMixin}'s post-define {@code @Redirect} handler can
 * declare {@code GlslPreprocessor.injectDefines}'s second parameter and pass the real instance
 * straight back through. No constructor is stubbed, deliberately, so nothing can accidentally
 * fabricate one.
 *
 * <p>Record-vs-class does not matter here: the only thing emitted against this stub is the type name
 * in a method descriptor.
 */
public final class ShaderDefines {

    public Map<String, String> values() {
        throw new UnsupportedOperationException("stub");
    }

    public Set<String> flags() {
        throw new UnsupportedOperationException("stub");
    }

    public String asSourceDirectives() {
        throw new UnsupportedOperationException("stub");
    }
}
