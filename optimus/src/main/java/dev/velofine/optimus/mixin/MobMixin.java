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

package dev.velofine.optimus.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Targets {@code net.minecraft.world.entity.Mob.serverAiStep()} (confirmed via javap against the
 * real vanilla 26.2 client jar). Vanilla already staggers full goal-selector re-evaluation to
 * every {@code UPDATE_GOAL_SELECTOR_EVERY_N_TICKS} (confirmed {@code ConstantValue: int 2}) ticks
 * per entity (offset by {@code getId()} so not every mob re-evaluates on the same tick) - on the
 * ticks in between, only currently-running goals continue ({@code tickRunningGoals(false)}, the
 * cheaper path) rather than the full {@code GoalSelector.tick()} re-evaluation. The literal
 * {@code 2} is javac-inlined at its one use site in this method (a compile-time
 * {@code ConstantValue} constant, not a field read) with no other colliding {@code 2} literal in
 * the method, confirmed by hand-disassembling {@code serverAiStep()}'s ~270-instruction body.
 *
 * <p>Same category of optimization Lithium ships (reduced goal-selector re-evaluation frequency) -
 * applied to vanilla's own existing throttle rather than building new AI infrastructure. Modest
 * increase (2 -&gt; 3 ticks, i.e. ~100ms -&gt; ~150ms between full re-evaluations) chosen to be a
 * real but conservative CPU saving, not a change likely to read as sluggish AI to a player.
 *
 * <p>Unconditional (Optimus has no hardware-gating model, unlike LegacySupport's {@code Fix}
 * system) - this is a plain constant swap, no runtime branch needed.
 */
@Mixin(targets = "net.minecraft.world.entity.Mob")
public abstract class MobMixin {

    private static final int GOAL_SELECTOR_UPDATE_INTERVAL_TICKS = 3;

    @ModifyConstant(method = "serverAiStep", constant = @Constant(intValue = 2))
    private int velofine$goalSelectorUpdateInterval(int original) {
        return GOAL_SELECTOR_UPDATE_INTERVAL_TICKS;
    }
}
