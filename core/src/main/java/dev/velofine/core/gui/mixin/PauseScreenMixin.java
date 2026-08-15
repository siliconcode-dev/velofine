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

package dev.velofine.core.gui.mixin;

import dev.velofine.core.gui.ActionButton;
import dev.velofine.core.gui.VelofineConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Consumer;

/**
 * Adds a "VELOFINE" button under the pause menu.
 *
 * <p>Redirecting {@code GridLayout.visitWidgets} is the seam that makes this safe. Confirmed
 * against the real 26.2 bytecode, {@code createPauseMenu} ends with
 * {@code arrangeElements()} then {@code FrameLayout.alignInRectangle(...)} then
 * {@code visitWidgets(this::addRenderableWidget)} - so by the time this handler runs the grid has
 * its final position and size, and the consumer it is handed is exactly the registration function
 * vanilla uses for its own buttons. Anchoring to {@code grid.getY() + grid.getHeight()} therefore
 * cannot overlap a vanilla button, however many rows the menu ended up with (server custom-dialog
 * buttons included).
 *
 * <p>Two approaches were rejected on the way here. Capturing the local {@code RowHelper} with
 * {@code LocalCapture} is brittle against any recompile of the method. Creating a <em>fresh</em>
 * {@code RowHelper} looks tempting but is outright wrong: its cell index restarts at zero, so the
 * button would be placed on top of "Back to Game" rather than after the last row.
 */
@Mixin(targets = "net.minecraft.client.gui.screens.PauseScreen")
public abstract class PauseScreenMixin {

    private static final int GAP_BELOW_MENU = 4;
    private static final int BUTTON_HEIGHT = 20;

    @Redirect(method = "createPauseMenu",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/layouts/GridLayout;"
                            + "visitWidgets(Ljava/util/function/Consumer;)V"))
    private void velofine$addPauseMenuButton(GridLayout grid, Consumer<AbstractWidget> register) {
        grid.visitWidgets(register);

        Screen self = (Screen) (Object) this;
        register.accept(new ActionButton(
                grid.getX(),
                grid.getY() + grid.getHeight() + GAP_BELOW_MENU,
                grid.getWidth(),
                BUTTON_HEIGHT,
                "VELOFINE",
                ActionButton.STYLE_ACCENT_OUTLINE,
                () -> Minecraft.getInstance().setScreenAndShow(new VelofineConfigScreen(self))));
    }
}
