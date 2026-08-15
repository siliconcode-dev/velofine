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

package dev.velofine.core.gui;

/**
 * When a setting actually starts having an effect.
 *
 * <p>Velofine's gates are read at different points in the launch: some mixin handlers re-evaluate
 * on every call and so respond instantly, while others fire exactly once (GLFW window hints at
 * context creation, shader compilation at startup, {@code Options.&lt;init&gt;} on first run, a
 * system property read when a vanilla class first loads). Showing the difference is not a nicety -
 * quietly implying a toggle took effect when it did not would be its own kind of silent misbehaviour.
 */
public enum Applies {
    LIVE("APPLIES LIVE"),
    RESTART("NEEDS RESTART");

    private final String badge;

    Applies(String badge) {
        this.badge = badge;
    }

    public String badge() {
        return badge;
    }
}
