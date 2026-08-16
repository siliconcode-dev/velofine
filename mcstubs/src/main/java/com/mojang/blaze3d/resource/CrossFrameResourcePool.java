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
 * Confirmed real via javap - {@code GameRenderer.resourcePool}'s actual declared field type.
 * {@code @Accessor} mixins need the accessor method's return type to match the real field's
 * declared type exactly (confirmed empirically: typing {@code GameRendererAccessor} to the
 * {@link GraphicsResourceAllocator} interface instead threw {@code InvalidAccessorException} at
 * apply time, caught by {@code VerifyMixinsHarness}) - widening happens naturally afterward
 * through ordinary Java polymorphism when the value is passed to {@code PostChain.process}, which
 * only needs the interface.
 */
public class CrossFrameResourcePool implements GraphicsResourceAllocator {
}
