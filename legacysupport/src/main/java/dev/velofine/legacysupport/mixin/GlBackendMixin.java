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

import dev.velofine.legacysupport.LegacySupportEngine;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * Targets {@code com.mojang.blaze3d.opengl.GlBackend.setWindowHints()} (confirmed via javap
 * against the real vanilla 26.2 client jar - see Build_plan.md / this phase's plan notes).
 * Vanilla requests OpenGL 3.3, Core Profile, Forward-Compatible. On detected Ivy Bridge Gen7
 * Intel iGPUs (HD Graphics 4000/2500) and the broader old-Intel-iGPU fallback, this overwrite
 * instead requests 3.3 Compatibility Profile with forward-compat dropped - compatibility profile
 * is a strict superset of core at the same GL version, so nothing Minecraft's core-profile-only
 * code does should break; the hypothesis (grounded in OptiFine's historical mirror-image Intel GL
 * profile issue, and Mojang's own precedent of driver-specific workarounds elsewhere in this same
 * class) is that Intel's compatibility-profile driver path is simply better-tested than its core
 * path for this hardware generation, which Intel stopped patching after driver build 5161.
 */
@Mixin(targets = "com.mojang.blaze3d.opengl.GlBackend")
public abstract class GlBackendMixin {

    @Overwrite
    public void setWindowHints() {
        GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_OPENGL_API);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_CREATION_API, GLFW.GLFW_NATIVE_CONTEXT_API);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);

        if (LegacySupportEngine.shouldForceCompatibilityProfile()) {
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_COMPAT_PROFILE);
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_FALSE);
            System.out.println("[Velofine][LegacySupport] GlBackend.setWindowHints(): compatibility profile forced (3.3, no forward-compat)");
        } else {
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);
        }
    }
}
