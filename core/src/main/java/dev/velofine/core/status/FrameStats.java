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
 * v1.5 settings-UI overhaul (item 11): a real numeric snapshot alongside {@link LiveStatus}'s
 * existing pre-formatted FPS string. Before this, {@code FrameTimeProfiler} computed avg/1%-low/
 * 0.1%-low/frame-time internally and immediately flattened them into one {@code String} - no
 * individual number was ever reachable outside that one method, which ruled out anything beyond a
 * single text line (a real stat block, a sparkline, per-stat color thresholds).
 *
 * <p>Plain primitives, no Minecraft or engine types - safe to sit in {@code core.status} for the
 * same one-way-dependency reason {@link LiveStatus}'s own class javadoc already documents.
 */
public record FrameStats(double avgFps, double low1Fps, double low01Fps, double avgMs) {

    public static FrameStats unknown() {
        return new FrameStats(0, 0, 0, 0);
    }
}
