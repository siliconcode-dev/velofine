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

import net.minecraft.network.chat.Component;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * Captures a key for the "open Velofine settings" bind.
 *
 * <p>The bind lives here rather than in vanilla's Controls list because registering into
 * {@code Options.keyMappings} would mean injecting into a final array built inside
 * {@code Options.<init>} by a synthetic lambda array-factory - the same class of unstable javac
 * implementation detail Phase 3 rejected for {@code IOWorker}. The trade-off is honest and
 * one-directional: the bind is fully functional and rebindable, it just will not appear in
 * vanilla's own key-bind screen.
 *
 * <p>While listening, key events are routed here by the screen rather than through widget focus,
 * so capture works regardless of what vanilla's focus traversal happens to be doing.
 */
public final class KeybindRow extends OptionRow {

    private final IntSupplier getter;
    private final IntConsumer setter;

    private boolean listening;

    public KeybindRow(int x, int y, int width, String label, String description, IntSupplier getter,
            IntConsumer setter) {
        super(x, y, width, label, description, Applies.LIVE);
        this.getter = getter;
        this.setter = setter;
    }

    public boolean isListening() {
        return listening;
    }

    /**
     * @param key the GLFW key code, or {@link GlfwKeyNames#UNBOUND} to clear the bind (which is
     *     what Escape does - a player cancelling out of capture almost always means "don't bind
     *     anything", and leaving them with no way to unbind would be worse).
     */
    public void captureKey(int key) {
        listening = false;
        setter.accept(key);
    }

    @Override
    protected Component valueText() {
        return Component.literal(listening ? "> ? <" : GlfwKeyNames.of(getter.getAsInt()));
    }

    @Override
    protected void activate(int direction) {
        listening = true;
    }

    @Override
    protected boolean valueIsOverride() {
        return listening || getter.getAsInt() != GlfwKeyNames.UNBOUND;
    }
}
