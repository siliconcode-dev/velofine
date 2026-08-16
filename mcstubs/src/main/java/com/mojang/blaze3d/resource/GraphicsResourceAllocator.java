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

package com.mojang.blaze3d.resource;

/**
 * Confirmed real via javap - {@code GameRenderer}'s own {@code resourcePool} field (type {@code
 * CrossFrameResourcePool}, which implements this interface) is exposed via an {@code @Accessor}
 * mixin typed to this interface rather than the concrete class, since nothing here needs anything
 * {@code CrossFrameResourcePool} adds beyond the interface.
 */
public interface GraphicsResourceAllocator {
}
