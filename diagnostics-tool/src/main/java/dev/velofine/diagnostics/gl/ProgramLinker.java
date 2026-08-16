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

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

/**
 * Links a shader name's vertex+fragment pair via a real {@code glLinkProgram} call - a shader can
 * compile cleanly per-stage and still fail at link time (attribute/varying mismatches), which
 * compile-only testing would silently miss. Not unit tested: requires a real GPU/driver context.
 *
 * <p>{@link #link} deliberately does <b>not</b> delete the linked program - the 24-item extension's
 * real rendering reproduction work ({@code ProgramIntrospector}, {@code DrawCallTester},
 * {@code UboRoundTripTester}, {@code ShaderPrecisionProbe}) all need the program to stay alive
 * after linking. Callers must call {@link #delete(int)} themselves once every downstream step for
 * that program has finished - this is a real behavior change from the original Phase 1 build, and
 * missing it would silently break every one of those features.
 */
public final class ProgramLinker {

    private ProgramLinker() {
    }

    /** {@code program} stays alive (attached shaders detached, but not deleted) - see class javadoc. */
    public record LinkResult(boolean success, String infoLog, int program) {
    }

    public static LinkResult link(int vertexHandle, int fragmentHandle) {
        int program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vertexHandle);
        GL20.glAttachShader(program, fragmentHandle);
        GL20.glLinkProgram(program);

        int status = GL20.glGetProgrami(program, GL20.GL_LINK_STATUS);
        int logLength = GL20.glGetProgrami(program, GL20.GL_INFO_LOG_LENGTH);
        String infoLog = logLength > 0 ? GL20.glGetProgramInfoLog(program, logLength) : "";

        GL20.glDetachShader(program, vertexHandle);
        GL20.glDetachShader(program, fragmentHandle);

        return new LinkResult(status == GL11.GL_TRUE, infoLog, program);
    }

    /** Deletes a program handle returned by {@link #link} - call once every downstream step is done. */
    public static void delete(int program) {
        if (program != 0) {
            GL20.glDeleteProgram(program);
        }
    }
}
