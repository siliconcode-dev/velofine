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

package net.minecraft.client.gui.components;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Confirmed via javap that the real class has exactly two abstract methods - so a custom-drawn
 * Velofine widget still inherits all of vanilla's geometry, hover, focus, tab-order and
 * click-dispatch machinery, and only the drawing is ours.
 */
public abstract class AbstractWidget implements GuiEventListener, Renderable, NarratableEntry, LayoutElement {

    protected int width;
    protected int height;
    protected Component message;
    protected boolean isHovered;
    public boolean active;
    public boolean visible;
    protected float alpha;

    public AbstractWidget(int x, int y, int width, int height, Component message) {
        throw new UnsupportedOperationException("stub");
    }

    /**
     * Final in the real class - it does the hover bookkeeping and tooltip handling, then delegates
     * to {@link #extractWidgetRenderState}. Velofine calls it directly for widgets it renders
     * itself (registered with {@code Screen.addWidget} for events only, so they can be clipped
     * inside a scissored, scrollable region).
     */
    public final void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY,
            float partialTick) {
        throw new UnsupportedOperationException("stub");
    }

    protected abstract void extractWidgetRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY,
            float partialTick);

    protected abstract void updateWidgetNarration(NarrationElementOutput output);

    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        throw new UnsupportedOperationException("stub");
    }

    /**
     * v1.5 settings-UI overhaul: confirmed via javap against the real 26.2 jar - {@code
     * mouseDragged(MouseButtonEvent, double, double)} is the public vanilla dispatcher (mirroring
     * {@code mouseClicked}/{@code onClick}'s pattern) and delegates to this protected hook.
     * {@code IntRow}'s slider needs this for drag-to-adjust; nothing else in this codebase did
     * before now, which is why it wasn't stubbed until this change.
     */
    protected void onDrag(MouseButtonEvent event, double dragX, double dragY) {
        throw new UnsupportedOperationException("stub");
    }

    public boolean keyPressed(KeyEvent event) {
        throw new UnsupportedOperationException("stub");
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        throw new UnsupportedOperationException("stub");
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        throw new UnsupportedOperationException("stub");
    }

    public int getX() {
        throw new UnsupportedOperationException("stub");
    }

    public void setX(int x) {
        throw new UnsupportedOperationException("stub");
    }

    public int getY() {
        throw new UnsupportedOperationException("stub");
    }

    public void setY(int y) {
        throw new UnsupportedOperationException("stub");
    }

    public int getWidth() {
        throw new UnsupportedOperationException("stub");
    }

    public void setWidth(int width) {
        throw new UnsupportedOperationException("stub");
    }

    public int getHeight() {
        throw new UnsupportedOperationException("stub");
    }

    public void setHeight(int height) {
        throw new UnsupportedOperationException("stub");
    }

    public int getRight() {
        throw new UnsupportedOperationException("stub");
    }

    public int getBottom() {
        throw new UnsupportedOperationException("stub");
    }

    public void setSize(int width, int height) {
        throw new UnsupportedOperationException("stub");
    }

    public void setMessage(Component message) {
        throw new UnsupportedOperationException("stub");
    }

    public Component getMessage() {
        throw new UnsupportedOperationException("stub");
    }

    public boolean isHovered() {
        throw new UnsupportedOperationException("stub");
    }

    public boolean isFocused() {
        throw new UnsupportedOperationException("stub");
    }

    public boolean isHoveredOrFocused() {
        throw new UnsupportedOperationException("stub");
    }

    public boolean isActive() {
        throw new UnsupportedOperationException("stub");
    }
}
