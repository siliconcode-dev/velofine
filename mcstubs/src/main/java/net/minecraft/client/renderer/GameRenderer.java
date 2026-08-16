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

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.resources.Identifier;

/**
 * Confirmed real methods via javap against the real 26.2 jar. Mojang's own moddable
 * post-processing asset mechanism (already used internally for blur/nausea/creeper-flash effects,
 * confirmed via the real {@code assets/minecraft/post_effect/*.json} bundled in the client jar) -
 * Velofine's AA control drives it the same way vanilla drives blur, via
 * {@link #setPostEffect(Identifier)}/{@link #clearPostEffect()}, rather than a bytecode rewrite of
 * the render loop.
 */
public class GameRenderer {

    public void setPostEffect(Identifier id) {
        throw new UnsupportedOperationException("stub");
    }

    public void clearPostEffect() {
        throw new UnsupportedOperationException("stub");
    }

    public Identifier currentPostEffect() {
        throw new UnsupportedOperationException("stub");
    }

    public RenderTarget mainRenderTarget() {
        throw new UnsupportedOperationException("stub");
    }
}
