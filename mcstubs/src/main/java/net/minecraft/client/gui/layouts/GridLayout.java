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

package net.minecraft.client.gui.layouts;

import net.minecraft.client.gui.components.AbstractWidget;

import java.util.function.Consumer;

/**
 * The geometry getters really live on {@code AbstractLayout}/{@code LayoutElement} in the real jar.
 * Declaring them here emits an {@code invokevirtual} against {@code GridLayout}, which the JVM
 * resolves up the hierarchy - the stub does not need to reproduce the hierarchy for that to work.
 */
public class GridLayout {

    public void arrangeElements() {
        throw new UnsupportedOperationException("stub");
    }

    public void visitWidgets(Consumer<AbstractWidget> consumer) {
        throw new UnsupportedOperationException("stub");
    }

    public int getX() {
        throw new UnsupportedOperationException("stub");
    }

    public int getY() {
        throw new UnsupportedOperationException("stub");
    }

    public int getWidth() {
        throw new UnsupportedOperationException("stub");
    }

    public int getHeight() {
        throw new UnsupportedOperationException("stub");
    }

    public RowHelper createRowHelper(int columns) {
        throw new UnsupportedOperationException("stub");
    }

    public static final class RowHelper {

        public <T extends LayoutElement> T addChild(T child) {
            throw new UnsupportedOperationException("stub");
        }

        public <T extends LayoutElement> T addChild(T child, int occupiedColumns) {
            throw new UnsupportedOperationException("stub");
        }
    }
}
