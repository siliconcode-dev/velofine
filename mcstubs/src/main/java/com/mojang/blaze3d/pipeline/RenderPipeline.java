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

package com.mojang.blaze3d.pipeline;

import net.minecraft.resources.Identifier;

/**
 * Confirmed real via javap against the real 26.2 jar - a declarative object every draw call
 * references, bundling its vertex/fragment shader {@link Identifier}s, bind group layouts,
 * blend/depth state, and vertex format up front (the modern replacement for the classic
 * "currently bound shader program" model OptiFine/Iris originally intercepted). Only the three
 * getters Velofine's own gbuffers-redirect table needs are stubbed - the real class has many more
 * fields (bind group layouts, polygon mode, blend/depth state, ...) nothing here references.
 */
public class RenderPipeline {

    public Identifier getLocation() {
        throw new UnsupportedOperationException("stub");
    }

    public Identifier getVertexShader() {
        throw new UnsupportedOperationException("stub");
    }

    public Identifier getFragmentShader() {
        throw new UnsupportedOperationException("stub");
    }
}
