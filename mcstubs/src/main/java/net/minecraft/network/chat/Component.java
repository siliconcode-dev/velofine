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

package net.minecraft.network.chat;

/**
 * Real {@code Component} is an interface, so these static factories must stay static-on-interface
 * for javac to emit an interface {@code invokestatic}.
 *
 * <p>{@code translatable} is included because Minecraft renders a missing translation key as the
 * key text itself - that fallback is what lets Velofine ship readable labels with no language file.
 */
public interface Component extends FormattedText {

    static MutableComponent literal(String text) {
        throw new UnsupportedOperationException("stub");
    }

    static MutableComponent translatable(String key) {
        throw new UnsupportedOperationException("stub");
    }

    static MutableComponent empty() {
        throw new UnsupportedOperationException("stub");
    }
}
