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
 * Display names for GLFW key codes.
 *
 * <p>Vanilla can do this via {@code InputConstants.Key#getDisplayName()}, but reaching it would
 * mean stubbing {@code InputConstants.Type}, {@code Key}, and their translation-key plumbing for a
 * single label. A local table costs nothing, cannot break against a Minecraft change, and covers
 * every key a player would plausibly bind. Anything outside it degrades to {@code KEY <code>}
 * rather than failing.
 */
public final class GlfwKeyNames {

    public static final int UNBOUND = -1;

    private GlfwKeyNames() {
    }

    public static String of(int key) {
        if (key == UNBOUND) {
            return "NONE";
        }
        if (key >= 65 && key <= 90) {
            return String.valueOf((char) key);
        }
        if (key >= 48 && key <= 57) {
            return String.valueOf((char) key);
        }
        if (key >= 290 && key <= 301) {
            return "F" + (key - 289);
        }
        if (key >= 320 && key <= 329) {
            return "NUM " + (key - 320);
        }
        return switch (key) {
            case 32 -> "SPACE";
            case 39 -> "'";
            case 44 -> ",";
            case 45 -> "-";
            case 46 -> ".";
            case 47 -> "/";
            case 59 -> ";";
            case 61 -> "=";
            case 91 -> "[";
            case 92 -> "\\";
            case 93 -> "]";
            case 96 -> "`";
            case 256 -> "ESC";
            case 257 -> "ENTER";
            case 258 -> "TAB";
            case 259 -> "BACKSPACE";
            case 260 -> "INSERT";
            case 261 -> "DELETE";
            case 262 -> "RIGHT";
            case 263 -> "LEFT";
            case 264 -> "DOWN";
            case 265 -> "UP";
            case 266 -> "PAGE UP";
            case 267 -> "PAGE DOWN";
            case 268 -> "HOME";
            case 269 -> "END";
            case 280 -> "CAPS LOCK";
            case 284 -> "PAUSE";
            case 340 -> "LSHIFT";
            case 341 -> "LCTRL";
            case 342 -> "LALT";
            case 344 -> "RSHIFT";
            case 345 -> "RCTRL";
            case 346 -> "RALT";
            default -> "KEY " + key;
        };
    }
}
