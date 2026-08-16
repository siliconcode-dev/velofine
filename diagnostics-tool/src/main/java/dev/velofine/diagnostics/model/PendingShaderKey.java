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
 * One shader that was expected (per the real shader inventory + define variants) but never
 * confirmed compiled before a supervised run ended in {@link RunOutcome#TIMED_OUT} or
 * {@link RunOutcome#PROCESS_CRASHED}. Kept separate from {@link ShaderCompileEntry} so that type's
 * meaning - "this is a real attempt result" - stays uncontaminated by synthetic placeholders for
 * work that was never actually attempted or never confirmed to finish.
 */
public record PendingShaderKey(String shaderName, String stage, String defineVariant, RunOutcome reason) {
}
