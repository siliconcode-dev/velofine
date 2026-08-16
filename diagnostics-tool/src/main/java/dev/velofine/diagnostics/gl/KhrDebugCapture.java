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
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLDebugMessageCallback;
import org.lwjgl.opengl.KHRDebug;

/**
 * Installs a real {@code GL_KHR_debug} async message callback when the extension is present (the
 * context was requested with {@code GLFW_CONTEXT_DEBUG_CONTEXT}, see {@code DiagnosticGlContext}).
 * Targets the {@code KHRDebug} extension class, not core GL43's debug output, since this tool only
 * ever requests up to an OpenGL 3.3 context (matching vanilla) - {@code KHR_debug} is available as
 * an extension well below GL 4.3 on drivers that implement it.
 *
 * <p>Complementary to {@code GlErrorChecker}'s {@code glGetError()} polling, not a replacement for
 * it - old Intel drivers may simply not implement this extension, in which case
 * {@link #isSupported()} is {@code false} and the caller must rely on polling alone. Not unit
 * tested: requires a real GPU/driver context.
 */
public final class KhrDebugCapture implements AutoCloseable {

    private final boolean supported;
    private GLDebugMessageCallback callback;

    private KhrDebugCapture(boolean supported) {
        this.supported = supported;
    }

    public static KhrDebugCapture install(List<DebugMessage> sink) {
        boolean supported = GL.getCapabilities().GL_KHR_debug;
        KhrDebugCapture capture = new KhrDebugCapture(supported);

        if (supported) {
            GL11.glEnable(KHRDebug.GL_DEBUG_OUTPUT);
            GL11.glEnable(KHRDebug.GL_DEBUG_OUTPUT_SYNCHRONOUS);
            // Held as a field (not a throwaway lambda) and freed in close() - a documented LWJGL
            // callback-lifetime footgun: native code can call back into a GC'd callback otherwise.
            capture.callback = GLDebugMessageCallback.create((source, type, id, severity, length, message, userParam) ->
                    sink.add(new DebugMessage(
                            sourceName(source), typeName(type), id, severityName(severity),
                            GLDebugMessageCallback.getMessage(length, message))));
            KHRDebug.glDebugMessageCallback(capture.callback, 0L);
        }

        return capture;
    }

    public boolean isSupported() {
        return supported;
    }

    @Override
    public void close() {
        if (callback != null) {
            callback.free();
        }
    }

    private static String sourceName(int source) {
        if (source == KHRDebug.GL_DEBUG_SOURCE_API) {
            return "GL_DEBUG_SOURCE_API";
        } else if (source == KHRDebug.GL_DEBUG_SOURCE_WINDOW_SYSTEM) {
            return "GL_DEBUG_SOURCE_WINDOW_SYSTEM";
        } else if (source == KHRDebug.GL_DEBUG_SOURCE_SHADER_COMPILER) {
            return "GL_DEBUG_SOURCE_SHADER_COMPILER";
        } else if (source == KHRDebug.GL_DEBUG_SOURCE_THIRD_PARTY) {
            return "GL_DEBUG_SOURCE_THIRD_PARTY";
        } else if (source == KHRDebug.GL_DEBUG_SOURCE_APPLICATION) {
            return "GL_DEBUG_SOURCE_APPLICATION";
        }
        return "GL_DEBUG_SOURCE_OTHER";
    }

    private static String typeName(int type) {
        if (type == KHRDebug.GL_DEBUG_TYPE_ERROR) {
            return "GL_DEBUG_TYPE_ERROR";
        } else if (type == KHRDebug.GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR) {
            return "GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR";
        } else if (type == KHRDebug.GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR) {
            return "GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR";
        } else if (type == KHRDebug.GL_DEBUG_TYPE_PORTABILITY) {
            return "GL_DEBUG_TYPE_PORTABILITY";
        } else if (type == KHRDebug.GL_DEBUG_TYPE_PERFORMANCE) {
            return "GL_DEBUG_TYPE_PERFORMANCE";
        } else if (type == KHRDebug.GL_DEBUG_TYPE_MARKER) {
            return "GL_DEBUG_TYPE_MARKER";
        }
        return "GL_DEBUG_TYPE_OTHER";
    }

    private static String severityName(int severity) {
        if (severity == KHRDebug.GL_DEBUG_SEVERITY_HIGH) {
            return "HIGH";
        } else if (severity == KHRDebug.GL_DEBUG_SEVERITY_MEDIUM) {
            return "MEDIUM";
        } else if (severity == KHRDebug.GL_DEBUG_SEVERITY_LOW) {
            return "LOW";
        }
        return "NOTIFICATION";
    }
}
