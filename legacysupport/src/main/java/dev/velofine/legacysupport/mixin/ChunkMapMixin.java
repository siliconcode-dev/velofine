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

package dev.velofine.legacysupport.mixin;

import dev.velofine.core.hardware.Fix;
import dev.velofine.legacysupport.LegacySupportEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Targets {@code net.minecraft.server.level.ChunkMap.saveChunksEagerly(BooleanSupplier)}
 * (confirmed via javap against the real vanilla 26.2 client jar). This method runs every tick as
 * part of {@code ChunkMap.tick()}/{@code processUnloads()} and eagerly re-saves up to {@code
 * CHUNK_SAVED_EAGERLY_PER_TICK} (a real vanilla constant, inlined by javac as the literal {@code
 * 20} at its one use site here - confirmed there's no other {@code 20} literal in this method to
 * collide with) dirty chunks per tick, queued onto the same single-threaded IO executor
 * ({@code IOWorker}) that also services chunk *loads*.
 *
 * <p>Originally this phase planned to reorder {@code IOWorker}'s internal load/save priority
 * directly, but javap research showed both load and save requests funnel through one private
 * {@code submitTask} method with a hardcoded priority baked in via a synthetic-lambda callback
 * with no caller-visible seam to redirect safely - reimplementing that internal wiring blind
 * against decompiled bytecode risked silently breaking chunk saves. This constant instead throttles
 * how many saves get queued *before* they ever reach the IO thread, which is real, single-purpose,
 * and named for exactly this behavior - a materially lower-risk way to reduce load/save contention
 * on a rotational disk's single IO thread without touching scheduler internals at all.
 *
 * <p>Like {@link GlDeviceMixin}'s shader patch, this is a bytecode-level hypothesis grounded in the
 * real class/constant, not yet confirmed to reduce stutter on the actual i3-3110M/i5-3470S
 * reference hardware - see this phase's manual tester verification steps.
 */
@Mixin(targets = "net.minecraft.server.level.ChunkMap")
public abstract class ChunkMapMixin {

    /**
     * Below vanilla's 20-per-tick eager-save cap, chosen to noticeably reduce IO-thread
     * contention on a rotational disk without starving saves entirely (a nonzero cap still lets
     * dirty chunks drain steadily rather than only via the periodic full {@code saveAllChunks}
     * pass).
     */
    private static final int EAGER_SAVE_CAP_ROTATIONAL_DISK = 6;

    @ModifyConstant(method = "saveChunksEagerly", constant = @Constant(intValue = 20))
    private int velofine$eagerSaveCap(int original) {
        return LegacySupportEngine.isFixActive(Fix.IO_STALL_SMOOTHING) ? EAGER_SAVE_CAP_ROTATIONAL_DISK : original;
    }
}
