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

package dev.velofine.core.config;

/** How Optimus's performance governor behaves. */
public enum GovernorMode {
    /** Never touches render distance. Vanilla behaviour. */
    OFF,
    /** Real-time FPS-driven render-distance adjustment within the user's own ceiling. */
    ADAPTIVE,
    /** Applies one fixed render distance on world join and never adapts. */
    MANUAL;

    public GovernorMode next() {
        return switch (this) {
            case OFF -> ADAPTIVE;
            case ADAPTIVE -> MANUAL;
            case MANUAL -> OFF;
        };
    }
}
