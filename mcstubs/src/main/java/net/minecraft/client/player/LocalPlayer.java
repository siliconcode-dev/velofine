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

package net.minecraft.client.player;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

/**
 * Confirmed real members via javap: {@code getMainHandItem()}/{@code getOffhandItem()} are
 * declared on the real {@code LivingEntity} superclass, {@code blockPosition()} on {@code Entity} -
 * declared flat here rather than modelling the real inheritance chain, matching this codebase's
 * "only the members actually referenced" stub convention (nothing else needs the intermediate
 * types yet).
 */
public class LocalPlayer {

    public ItemStack getMainHandItem() {
        throw new UnsupportedOperationException("stub");
    }

    public ItemStack getOffhandItem() {
        throw new UnsupportedOperationException("stub");
    }

    public BlockPos blockPosition() {
        throw new UnsupportedOperationException("stub");
    }
}
