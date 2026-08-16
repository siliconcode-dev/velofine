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

import dev.velofine.diagnostics.model.ShaderCompileResult;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

/**
 * Thin wrapper around {@code glCompileShader} capturing real {@code GL_COMPILE_STATUS}/info-log
 * output. Not unit tested: requires a real GPU/driver context. Verify manually on real hardware.
 */
public final class ShaderCompiler {

    private ShaderCompiler() {
    }

    /** A compiled (or failed-to-compile) shader object - the handle stays valid either way. */
    public record CompiledShader(int handle, ShaderCompileResult result) {
    }

    public static CompiledShader compile(int glShaderType, String resolvedSource) {
        int handle = GL20.glCreateShader(glShaderType);
        GL20.glShaderSource(handle, resolvedSource);
        GL20.glCompileShader(handle);

        int status = GL20.glGetShaderi(handle, GL20.GL_COMPILE_STATUS);
        int logLength = GL20.glGetShaderi(handle, GL20.GL_INFO_LOG_LENGTH);
        String infoLog = logLength > 0 ? GL20.glGetShaderInfoLog(handle, logLength) : "";

        return new CompiledShader(handle, new ShaderCompileResult(status == GL11.GL_TRUE, infoLog, logLength));
    }

    public static void delete(int handle) {
        if (handle != 0) {
            GL20.glDeleteShader(handle);
        }
    }
}
