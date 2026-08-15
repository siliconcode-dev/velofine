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
import net.minecraft.server.level.ParticleStatus;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

/**
 * Targets {@code net.minecraft.client.Options}'s constructor (confirmed via javap against the
 * real vanilla 26.2 client jar - this phase's plan notes), lowering the vanilla first-run defaults
 * for render distance, simulation distance, entity distance scaling, mipmap levels, and particle
 * status when {@code MEMORY_SAVING_DEFAULTS} is active.
 *
 * <p>This only ever changes what a *fresh* install starts at: vanilla loads {@code options.txt}
 * immediately after constructing this object, which overwrites every field touched here if the
 * file already exists - so an existing player's saved settings are never touched, and the reduced
 * defaults are always visible/adjustable in vanilla's own video settings menu going forward. No
 * "does options.txt exist" check is needed in this mixin itself; vanilla's own load order provides
 * that gate for free.
 *
 * <p>The constructor is one large method (~5000 bytecode instructions covering every option), so
 * each injection is scoped with {@link Slice} between the option's own translation-key string
 * constant (e.g. {@code "options.renderDistance"}) and its field assignment - both stable,
 * semantically-meaningful anchors - rather than a global ordinal, so an unrelated numeric literal
 * added elsewhere in the constructor in a future Minecraft version can't silently shift which
 * constant gets modified (Mixin fails loudly at apply time if a slice boundary can't be found,
 * rather than corrupting an unrelated option).
 */
@Mixin(targets = "net.minecraft.client.Options")
public abstract class OptionsMixin {

    private static final int RENDER_DISTANCE_LOW_MEMORY_DEFAULT = 6;
    private static final int SIMULATION_DISTANCE_LOW_MEMORY_DEFAULT = 6;
    private static final double ENTITY_DISTANCE_SCALING_LOW_MEMORY_DEFAULT = 0.5;
    private static final int MIPMAP_LEVELS_LOW_MEMORY_DEFAULT = 0;

    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 12),
            slice = @Slice(
                    from = @At(value = "CONSTANT", args = "stringValue=options.renderDistance"),
                    to = @At(value = "FIELD", target = "Lnet/minecraft/client/Options;renderDistance:Lnet/minecraft/client/OptionInstance;",
                            opcode = Opcodes.PUTFIELD)))
    private int velofine$renderDistanceDefault(int original) {
        return LegacySupportEngine.isFixActive(Fix.MEMORY_SAVING_DEFAULTS) ? RENDER_DISTANCE_LOW_MEMORY_DEFAULT : original;
    }

    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 12),
            slice = @Slice(
                    from = @At(value = "CONSTANT", args = "stringValue=options.simulationDistance"),
                    to = @At(value = "FIELD", target = "Lnet/minecraft/client/Options;simulationDistance:Lnet/minecraft/client/OptionInstance;",
                            opcode = Opcodes.PUTFIELD)))
    private int velofine$simulationDistanceDefault(int original) {
        return LegacySupportEngine.isFixActive(Fix.MEMORY_SAVING_DEFAULTS) ? SIMULATION_DISTANCE_LOW_MEMORY_DEFAULT : original;
    }

    @ModifyConstant(method = "<init>", constant = @Constant(doubleValue = 1.0),
            slice = @Slice(
                    from = @At(value = "CONSTANT", args = "stringValue=options.entityDistanceScaling"),
                    to = @At(value = "FIELD", target = "Lnet/minecraft/client/Options;entityDistanceScaling:Lnet/minecraft/client/OptionInstance;",
                            opcode = Opcodes.PUTFIELD)))
    private double velofine$entityDistanceScalingDefault(double original) {
        return LegacySupportEngine.isFixActive(Fix.MEMORY_SAVING_DEFAULTS) ? ENTITY_DISTANCE_SCALING_LOW_MEMORY_DEFAULT : original;
    }

    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 4, ordinal = 1),
            slice = @Slice(
                    from = @At(value = "CONSTANT", args = "stringValue=options.mipmapLevels"),
                    to = @At(value = "FIELD", target = "Lnet/minecraft/client/Options;mipmapLevels:Lnet/minecraft/client/OptionInstance;",
                            opcode = Opcodes.PUTFIELD)))
    private int velofine$mipmapLevelsDefault(int original) {
        return LegacySupportEngine.isFixActive(Fix.MEMORY_SAVING_DEFAULTS) ? MIPMAP_LEVELS_LOW_MEMORY_DEFAULT : original;
    }

    @Redirect(method = "<init>",
            at = @At(value = "FIELD", target = "Lnet/minecraft/server/level/ParticleStatus;ALL:Lnet/minecraft/server/level/ParticleStatus;",
                    opcode = Opcodes.GETSTATIC),
            slice = @Slice(
                    from = @At(value = "CONSTANT", args = "stringValue=options.particles"),
                    to = @At(value = "FIELD", target = "Lnet/minecraft/client/Options;particles:Lnet/minecraft/client/OptionInstance;",
                            opcode = Opcodes.PUTFIELD)))
    private ParticleStatus velofine$particlesDefault() {
        return LegacySupportEngine.isFixActive(Fix.MEMORY_SAVING_DEFAULTS) ? ParticleStatus.DECREASED : ParticleStatus.ALL;
    }
}
