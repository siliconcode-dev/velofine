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

package net.minecraft.client.gui.components.events;

import net.minecraft.client.input.CharacterEvent;

/**
 * Real interface declares ~18 default methods; still mostly an empty marker (it exists primarily
 * because it is the erasure of {@code Screen.addRenderableWidget}'s type parameter) except for
 * {@code charTyped}, added for the v1.5 search overlay - confirmed via javap that real
 * {@code Screen} does NOT redeclare {@code charTyped} the way it redeclares {@code keyPressed}, so
 * it must be reachable here for {@code Screen} (which the real class really does implement this
 * interface through) to expose an overridable {@code charTyped} at all.
 */
public interface GuiEventListener {

    default boolean charTyped(CharacterEvent event) {
        return false;
    }
}
