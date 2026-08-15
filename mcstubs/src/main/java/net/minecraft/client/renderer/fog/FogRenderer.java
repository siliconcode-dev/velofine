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

package net.minecraft.client.renderer.fog;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;

/**
 * Confirmed via javap against the real 26.2 jar: {@code setupFog} sets
 * {@code FogData.renderDistanceStart}/{@code renderDistanceEnd} from {@code renderDistance * 16}
 * at the very end of the method, which is what {@code FogRendererMixin} overrides.
 */
public class FogRenderer {

    public FogData setupFog(Camera camera, int renderDistance, DeltaTracker deltaTracker, float partialTick,
            ClientLevel level) {
        throw new UnsupportedOperationException("stub");
    }
}
