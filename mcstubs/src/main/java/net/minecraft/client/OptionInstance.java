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

package net.minecraft.client;

public final class OptionInstance<T> {

    public T get() {
        throw new UnsupportedOperationException("stub");
    }

    /**
     * Verified against the real 26.2 bytecode: validates the value, and when it actually changed
     * and the game is running, fires the option's own {@code ValueUpdateListener}. For
     * {@code renderDistance} that listener is what rebuilds the chunk graph and re-sends the
     * server view distance - which is why Velofine's governor needs no mixin to actuate.
     */
    public void set(T value) {
        throw new UnsupportedOperationException("stub");
    }
}
