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

package dev.velofine.legacysupport.gl;

import dev.velofine.core.log.VelofineLog;
import org.lwjgl.opengl.GL11;

import java.util.Optional;

/**
 * Post-context confirmation, v1.5 Phase 2: captures {@code GL_VENDOR}/{@code GL_RENDERER}/
 * {@code GL_VERSION} exactly once, lazily, the first time it's asked for.
 *
 * <p>Deliberately not driven by a dedicated context-creation mixin - {@code core.mixin.GlDeviceMixin}
 * already redirects {@code GlDevice.compileShader}'s call to {@code ShaderSource.get(...)}, and
 * shader compilation cannot happen without a real GL context already current on that thread. This
 * class's {@link #capture()} is called from inside {@code LegacySupportEngine}'s registered
 * {@code ShaderSourceInterceptor} lambda (which only ever runs from within that already-proven
 * seam), so no new mixin/javap research was needed to get a "post-context" hook - the existing one
 * already guarantees a current context by construction.
 */
public final class GlContextSignature {

    public record Signature(String vendor, String renderer, String version) {
    }

    private static volatile Optional<Signature> captured;

    private GlContextSignature() {
    }

    /**
     * Returns the captured signature, calling {@code glGetString} on first use only. Empty if
     * capture hasn't happened yet and the GL call itself fails (e.g. no context actually current -
     * defensive, should not happen given where this is called from, but a shader-swap gate must
     * fail safe rather than throw).
     */
    public static Optional<Signature> get() {
        Optional<Signature> local = captured;
        if (local != null) {
            return local;
        }
        return capture();
    }

    private static synchronized Optional<Signature> capture() {
        if (captured != null) {
            return captured;
        }
        try {
            String vendor = GL11.glGetString(GL11.GL_VENDOR);
            String renderer = GL11.glGetString(GL11.GL_RENDERER);
            String version = GL11.glGetString(GL11.GL_VERSION);
            captured = Optional.of(new Signature(vendor, renderer, version));
        } catch (Throwable t) {
            VelofineLog.warn("LegacySupport", "Post-context GL signature capture failed: " + t);
            captured = Optional.empty();
        }
        return captured;
    }
}
