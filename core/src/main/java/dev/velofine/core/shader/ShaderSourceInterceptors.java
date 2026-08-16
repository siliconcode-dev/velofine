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

import com.mojang.blaze3d.shaders.ShaderType;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The single dispatch point for every real GLSL source lookup ({@code
 * com.mojang.blaze3d.opengl.GlDevice.compileShader}'s call to {@code ShaderSource.get(Identifier,
 * ShaderType)}), backing {@code core}'s own {@code GlDeviceMixin}.
 *
 * <p>Why this exists rather than each engine {@code @Redirect}ing that call site directly: Mixin's
 * {@code @Redirect} claims exclusive ownership of a single call site - two independent mixins both
 * redirecting the identical instruction conflict at apply time. Phase 2's {@code GlDeviceMixin}
 * originally lived in {@code legacysupport} and had sole ownership of this redirect for the Intel
 * {@code mix()} patch; Phase 7 needs the same seam for shader-pack GLSL substitution, and
 * critically needs it to fire on hardware where {@code LegacySupportEngine} installs <em>no</em>
 * mixins at all (its {@code onAgentAttached} returns early whenever no {@link
 * dev.velofine.core.hardware.Fix} is active - true on essentially all non-reference hardware, i.e.
 * exactly the hardware shader packs actually target). So the one real {@code @Redirect} now lives
 * in {@code core} (always installed, no engine gate - see {@code CoreEngine}'s own javadoc for the
 * precedent), and both {@code legacysupport} and {@code shaders} register a {@link
 * ShaderSourceInterceptor} into this registry instead of touching the call site themselves.
 *
 * <p>Interceptors are tried in ascending {@code priority} order; the first to return a non-empty
 * {@link Optional} wins and the rest are skipped. Shaders' gbuffers substitution registers at a
 * lower number (higher priority) than LegacySupport's {@code mix()} patch, since a shader-pack
 * program fully replaces vanilla's GLSL for that draw call - patching the vanilla source LegacySupport
 * would otherwise target is meaningless once a pack owns that identifier.
 */
public final class ShaderSourceInterceptors {

    public static final int PRIORITY_SHADER_PACK = 0;
    public static final int PRIORITY_LEGACY_SUPPORT = 100;

    private static final List<Entry> interceptors = new CopyOnWriteArrayList<>();

    private ShaderSourceInterceptors() {
    }

    public static void register(int priority, ShaderSourceInterceptor interceptor) {
        interceptors.add(new Entry(priority, interceptor));
        interceptors.sort((a, b) -> Integer.compare(a.priority, b.priority));
    }

    public static String resolve(Identifier id, ShaderType type, String vanillaSource) {
        for (Entry entry : interceptors) {
            Optional<String> override = entry.interceptor.intercept(id, type, vanillaSource);
            if (override.isPresent()) {
                return override.get();
            }
        }
        return vanillaSource;
    }

    private record Entry(int priority, ShaderSourceInterceptor interceptor) {
    }
}
