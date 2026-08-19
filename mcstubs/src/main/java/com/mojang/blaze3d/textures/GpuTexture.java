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

package com.mojang.blaze3d.textures;

/**
 * Confirmed real via javap against the real 26.2 jar - the real class has several more instance
 * methods (getWidth/getHeight/getFormat/...) Velofine's own code never calls; only the two real
 * {@code USAGE_*} bit-flag constants LegacySupport's animated-texture-upload fix passes to
 * {@code GpuDevice.createTexture} are stubbed alongside the bare type itself.
 *
 * <p><b>These fields are deliberately non-{@code final} and deliberately uninitialized</b>, and that
 * is load-bearing - do not "tidy" them into constants. The real values (confirmed via
 * {@code javap -constants}) are {@code USAGE_COPY_DST=1, USAGE_COPY_SRC=2, USAGE_TEXTURE_BINDING=4,
 * USAGE_RENDER_ATTACHMENT=8, USAGE_CUBEMAP_COMPATIBLE=16}, but this stub intentionally does not
 * assert them. A {@code static final} primitive <i>with an initializer</i> is a JLS 4.12.4 constant
 * variable, which javac <b>inlines into consumer bytecode</b> - so the real class is never consulted
 * at runtime. v1.7-Beta shipped with both declared {@code = 0}, which compiled
 * {@code USAGE_TEXTURE_BINDING | USAGE_COPY_DST} down to a literal {@code 0} and made every
 * animated-texture upload fail on real hardware with {@code IllegalStateException: Color texture must
 * have USAGE_COPY_DST to be a destination for a write} (63 failures per launch in the tester log).
 *
 * <p>Declared this way, javac must emit a {@code getstatic} that resolves against the real class at
 * runtime - and it fails loudly with {@code NoSuchFieldError} if Mojang ever renames a flag, rather
 * than silently computing a wrong mask. See {@code mcstubs/build.gradle.kts}'s header for the general
 * rule and {@code StubConstantInliningTest} for the guard that enforces it.
 */
public abstract class GpuTexture implements AutoCloseable {

    public static int USAGE_COPY_DST;
    public static int USAGE_TEXTURE_BINDING;
}
