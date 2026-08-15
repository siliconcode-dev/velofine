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

package dev.velofine.core.status;

/**
 * A one-line, plain-{@code String} noticeboard the config UI can read without depending on the
 * engine that writes it.
 *
 * <p>{@code core} sits below every engine, so the Optimus panel cannot call into
 * {@code PerformanceGovernor} directly. The obvious fix - registering a {@code Supplier} at agent
 * attach - would risk resolving engine classes that reference {@code Minecraft} before the mixin
 * transformer is installed. Pushing a finished string from the client tick instead keeps the
 * dependency one-way and touches no Minecraft type from {@code core} at all.
 *
 * <p>Written from the client thread, read from the render thread while a screen is open, hence
 * {@code volatile}.
 */
public final class LiveStatus {

    private static volatile String governor = "governor: idle";

    private LiveStatus() {
    }

    public static void setGovernor(String status) {
        governor = status;
    }

    public static String governor() {
        return governor;
    }
}
