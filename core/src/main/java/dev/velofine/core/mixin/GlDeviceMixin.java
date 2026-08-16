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

package dev.velofine.core.mixin;

import com.mojang.blaze3d.shaders.ShaderSource;
import com.mojang.blaze3d.shaders.ShaderType;
import dev.velofine.core.shader.ShaderSourceInterceptors;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Targets {@code com.mojang.blaze3d.opengl.GlDevice.compileShader(...)} (confirmed via javap
 * against the real vanilla 26.2 client jar), redirecting its single call to {@code
 * ShaderSource.get(Identifier, ShaderType)} - the one place GLSL source text is obtained before
 * compilation - through {@link ShaderSourceInterceptors}.
 *
 * <p>Originally a {@code legacysupport}-owned mixin (Phase 2, for the Intel {@code mix()} patch),
 * moved here in Phase 7: this is the single, exclusive owner of this call site (Mixin's
 * {@code @Redirect} does not allow two independent mixins to redirect the same instruction), and it
 * must fire regardless of which engines are enabled/active - see {@link ShaderSourceInterceptors}'s
 * class javadoc for the full reasoning, in particular why this could not stay gated behind
 * LegacySupport's own conditional install.
 *
 * <p>Redirecting the interface call (rather than injecting into whatever concrete class implements
 * {@code ShaderSource}) means this works regardless of which class actually backs the shader-source
 * lookup - identifying it was never necessary.
 */
@Mixin(targets = "com.mojang.blaze3d.opengl.GlDevice")
public abstract class GlDeviceMixin {

    @Redirect(method = "compileShader",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/shaders/ShaderSource;get(Lnet/minecraft/resources/Identifier;"
                            + "Lcom/mojang/blaze3d/shaders/ShaderType;)Ljava/lang/String;"))
    private String velofine$resolveShaderSource(ShaderSource source, Identifier id, ShaderType type) {
        String vanilla = source.get(id, type);
        return ShaderSourceInterceptors.resolve(id, type, vanilla);
    }
}
