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

package dev.velofine.utility.light;

import dev.velofine.core.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;

/**
 * Architecture modeled on LambDynamicLights, confirmed via this session's research: an
 * item-&gt;light-level registry (0-15, vanilla block-light scale), not a hardcoded per-mixin
 * table. V1 scope covers the player's own held item only (main + off hand) - the single most
 * common real use case (holding a torch/lava bucket in the dark) - not yet entity-carried items or
 * dropped items on the ground (both real LambDynamicLights features, left for a follow-up).
 *
 * <p>Injects a <em>query-time</em> light-level override into
 * {@code BlockLightEngine.getEmission(long, BlockState)} (see {@code BlockLightEngineMixin}) at
 * the tracked position, rather than writing to any persisted chunk light data - this is the real
 * mechanism confirmed via javap (the light engine already re-derives everything from this one
 * query each time it propagates), so when the player moves, calling {@code checkBlock} on both the
 * old and new position makes light decay/appear exactly the way it would if a real block's
 * emission had changed there - no manual bookkeeping to "undo" anything, and nothing is ever
 * written to real world/chunk data (purely a client-side rendering effect).
 */
public final class DynamicLightsState {

    private static final Map<Item, Integer> LIGHT_LEVELS = builtInLightLevels();

    private static long trackedPos = Long.MIN_VALUE;
    private static int trackedLevel;

    private DynamicLightsState() {
    }

    /** Read by {@code BlockLightEngineMixin} for every position the light engine queries. */
    public static int emissionAt(long packedPos) {
        return packedPos == trackedPos ? trackedLevel : 0;
    }

    public static void onTick(Minecraft minecraft) {
        if (!ConfigManager.get().utility.dynamicLights.enabled) {
            clear(minecraft);
            return;
        }

        LocalPlayer player = minecraft.player;
        if (player == null) {
            clear(minecraft);
            return;
        }

        int level = Math.max(levelOf(player.getMainHandItem()), levelOf(player.getOffhandItem()));
        long pos = player.blockPosition().asLong();

        if (level == trackedLevel && pos == trackedPos) {
            return;
        }

        long oldPos = trackedPos;
        trackedPos = level > 0 ? pos : Long.MIN_VALUE;
        trackedLevel = level;

        if (oldPos != Long.MIN_VALUE && oldPos != trackedPos) {
            minecraft.level.getLightEngine().checkBlock(BlockPos.of(oldPos));
        }
        if (trackedPos != Long.MIN_VALUE) {
            minecraft.level.getLightEngine().checkBlock(BlockPos.of(trackedPos));
        }
    }

    private static void clear(Minecraft minecraft) {
        if (trackedPos == Long.MIN_VALUE) {
            return;
        }
        long oldPos = trackedPos;
        trackedPos = Long.MIN_VALUE;
        trackedLevel = 0;
        if (minecraft.level != null) {
            minecraft.level.getLightEngine().checkBlock(BlockPos.of(oldPos));
        }
    }

    private static int levelOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        return LIGHT_LEVELS.getOrDefault(stack.getItem(), 0);
    }

    private static Map<Item, Integer> builtInLightLevels() {
        Map<Item, Integer> map = new HashMap<>();
        map.put(Items.TORCH, 14);
        map.put(Items.SOUL_TORCH, 10);
        map.put(Items.REDSTONE_TORCH, 7);
        map.put(Items.LANTERN, 15);
        map.put(Items.SOUL_LANTERN, 10);
        map.put(Items.GLOWSTONE, 15);
        map.put(Items.SEA_LANTERN, 15);
        map.put(Items.SHROOMLIGHT, 15);
        map.put(Items.JACK_O_LANTERN, 15);
        map.put(Items.LAVA_BUCKET, 15);
        return map;
    }
}
