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
 * One GL context creation attempt tried by {@code gl.DiagnosticGlContext}'s fallback ladder.
 *
 * <p>A driver refusing the exact context vanilla requests is itself a major finding - recording
 * every attempt (not just the first success or the final failure) means a total context-creation
 * failure on reference hardware still produces a rich, actionable report instead of an empty one.
 */
public record ContextCreationAttempt(String hints, boolean succeeded, String errorMessage) {
}
