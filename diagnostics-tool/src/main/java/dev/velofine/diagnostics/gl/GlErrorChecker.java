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

package dev.velofine.diagnostics.gl;

import dev.velofine.diagnostics.model.DebugMessage;
import java.util.List;
import org.lwjgl.opengl.ARBRobustness;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.KHRRobustness;

/**
 * Polls {@code glGetError()} and records any pending errors. Called after every GL call site
 * (context creation, each compile, each link, each capability query) <b>independent of</b>
 * {@code KHR_debug} support - old Intel drivers may not implement {@code KHR_debug} at all, and
 * error capture must not depend on an optional debug-extension being present. See
 * {@code KhrDebugCapture} for the complementary async-callback path. Not unit tested: requires a
 * real GPU/driver context.
 */
public final class GlErrorChecker {

    private GlErrorChecker() {
    }

    /** Drains every pending GL error (there can be more than one) and appends each to {@code sink}. */
    public static void checkAndRecord(String context, List<DebugMessage> sink) {
        int error;
        while ((error = GL11.glGetError()) != GL11.GL_NO_ERROR) {
            sink.add(new DebugMessage("glGetError-poll", errorName(error), error, "N/A", "after " + context, null));
        }
    }

    /**
     * Polls {@code glGetGraphicsResetStatus()} when {@code GL_KHR_robustness}/{@code GL_ARB_robustness}
     * is present - a Windows TDR-triggered context reset is a distinct condition plain
     * {@code glGetError()} doesn't reliably surface (behavior after a reset without robustness is
     * undefined per spec). Captured as an <b>additional</b> signal when the extension exists, not
     * relied upon as the primary crash/hang safety mechanism - that's the process-isolation
     * watchdog (see the {@code pipeline} package), sized specifically to let Windows' own 2-second
     * default TDR timeout get the first chance to recover a hung driver.
     */
    public static void checkGraphicsResetStatus(String context, List<DebugMessage> sink) {
        GLCapabilities caps = GL.getCapabilities();
        int status;
        if (caps.GL_KHR_robustness) {
            status = KHRRobustness.glGetGraphicsResetStatus();
        } else if (caps.GL_ARB_robustness) {
            status = ARBRobustness.glGetGraphicsResetStatusARB();
        } else {
            return;
        }
        if (status != GL11.GL_NO_ERROR) {
            sink.add(new DebugMessage("glGetGraphicsResetStatus", resetStatusName(status), status, "HIGH", "after " + context, null));
        }
    }

    private static String resetStatusName(int status) {
        if (status == KHRRobustness.GL_GUILTY_CONTEXT_RESET) {
            return "GL_GUILTY_CONTEXT_RESET";
        } else if (status == KHRRobustness.GL_INNOCENT_CONTEXT_RESET) {
            return "GL_INNOCENT_CONTEXT_RESET";
        } else if (status == KHRRobustness.GL_UNKNOWN_CONTEXT_RESET) {
            return "GL_UNKNOWN_CONTEXT_RESET";
        }
        return "GL_RESET_STATUS_0x" + Integer.toHexString(status);
    }

    private static String errorName(int error) {
        if (error == GL11.GL_INVALID_ENUM) {
            return "GL_INVALID_ENUM";
        } else if (error == GL11.GL_INVALID_VALUE) {
            return "GL_INVALID_VALUE";
        } else if (error == GL11.GL_INVALID_OPERATION) {
            return "GL_INVALID_OPERATION";
        } else if (error == GL11.GL_STACK_OVERFLOW) {
            return "GL_STACK_OVERFLOW";
        } else if (error == GL11.GL_STACK_UNDERFLOW) {
            return "GL_STACK_UNDERFLOW";
        } else if (error == GL11.GL_OUT_OF_MEMORY) {
            return "GL_OUT_OF_MEMORY";
        } else if (error == GL30.GL_INVALID_FRAMEBUFFER_OPERATION) {
            return "GL_INVALID_FRAMEBUFFER_OPERATION";
        }
        return "GL_ERROR_0x" + Integer.toHexString(error);
    }
}
