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

package dev.velofine.diagnostics.model;

/**
 * One error/diagnostic message, sourced either from a real {@code KHR_debug} async callback or
 * from a {@code glGetError()} poll (see {@code gl.GlErrorChecker}) - the tool captures both since
 * old Intel drivers may not implement {@code KHR_debug} at all, and error capture must not depend
 * on optional debug-extension support.
 */
public record DebugMessage(String source, String type, int id, String severity, String message) {
}
