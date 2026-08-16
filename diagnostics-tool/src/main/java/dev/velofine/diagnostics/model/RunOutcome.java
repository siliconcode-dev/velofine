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

/** How a supervised pipeline run ended - see {@code pipeline.ChildProcessSupervisor}. */
public enum RunOutcome {
    /** The child process finished normally and emitted a complete report. */
    COMPLETED,
    /** No output for longer than the watchdog timeout - the child was killed. */
    TIMED_OUT,
    /** The child exited abnormally (or its stdout stream closed) before emitting a complete report. */
    PROCESS_CRASHED,
    /** The child process could not even be started. */
    SPAWN_FAILED
}
