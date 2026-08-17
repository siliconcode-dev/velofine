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

package net.minecraft.client.gui.screens;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

/**
 * Abstract in the real jar but declares no abstract methods, so a Velofine screen only has to
 * override {@code init()} and {@code extractRenderState(...)}.
 *
 * <p>{@code addRenderableWidget}'s type parameter keeps all three real bounds: the <em>first</em>
 * bound determines the erasure, so narrowing it to {@code AbstractWidget} would emit a methodref
 * that does not exist at runtime.
 *
 * <p>{@code implements GuiEventListener}: real {@code Screen} does implement it (confirmed via
 * javap - {@code keyPressed} is redeclared directly on {@code Screen} itself, but {@code
 * charTyped} is not, meaning {@code Screen} relies on {@code GuiEventListener}'s own default for
 * that one). Added for the v1.5 search overlay's typed-character capture.
 */
public abstract class Screen implements GuiEventListener {

    protected Component title;
    protected Minecraft minecraft;
    public int width;
    public int height;
    protected Font font;

    protected Screen(Component title) {
        throw new UnsupportedOperationException("stub");
    }

    public Component getTitle() {
        throw new UnsupportedOperationException("stub");
    }

    protected void init() {
        throw new UnsupportedOperationException("stub");
    }

    public void tick() {
        throw new UnsupportedOperationException("stub");
    }

    public void removed() {
        throw new UnsupportedOperationException("stub");
    }

    public void onClose() {
        throw new UnsupportedOperationException("stub");
    }

    public boolean isPauseScreen() {
        throw new UnsupportedOperationException("stub");
    }

    public boolean keyPressed(KeyEvent event) {
        throw new UnsupportedOperationException("stub");
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        throw new UnsupportedOperationException("stub");
    }

    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        throw new UnsupportedOperationException("stub");
    }

    public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        throw new UnsupportedOperationException("stub");
    }

    protected <T extends GuiEventListener & Renderable & NarratableEntry> T addRenderableWidget(T widget) {
        throw new UnsupportedOperationException("stub");
    }

    /** Registers a widget for events/narration only - the caller renders it itself. */
    protected <T extends GuiEventListener & NarratableEntry> T addWidget(T widget) {
        throw new UnsupportedOperationException("stub");
    }

    protected void clearWidgets() {
        throw new UnsupportedOperationException("stub");
    }

    protected void repositionElements() {
        throw new UnsupportedOperationException("stub");
    }

    /** Clears every registered widget and calls {@link #init()} again. */
    protected void rebuildWidgets() {
        throw new UnsupportedOperationException("stub");
    }
}
