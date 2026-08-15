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

package dev.velofine.optimus.governor;

import net.minecraft.client.Minecraft;

/**
 * The real-game {@link GovernorHost}.
 *
 * <p>Only instantiated from inside the client tick, never during agent attach - it touches
 * {@code Minecraft}, and resolving that class before the mixin transformer is installed would
 * quietly defeat every mixin targeting it.
 *
 * <p>26.2 keeps the current screen on {@code Minecraft.gui}, not on {@code Minecraft} itself;
 * there is no {@code Minecraft.screen} field or {@code getScreen()} in this version.
 */
public final class MinecraftGovernorHost implements GovernorHost {

    @Override
    public boolean inWorld() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft != null && minecraft.level != null;
    }

    @Override
    public boolean screenOpen() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.gui != null && minecraft.gui.screen() != null;
    }

    @Override
    public int fps() {
        return Minecraft.getInstance().getFps();
    }

    @Override
    public int renderDistance() {
        return Minecraft.getInstance().options.renderDistance().get();
    }

    @Override
    public void setRenderDistance(int chunks) {
        Minecraft.getInstance().options.renderDistance().set(chunks);
    }
}
