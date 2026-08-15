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

package dev.velofine.utility.mixin;

import dev.velofine.utility.zoom.ZoomState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Targets {@code net.minecraft.client.MouseHandler.onScroll(long, double, double)} (confirmed
 * real, private method via javap against the real 26.2 client jar - the GLFW scroll callback
 * registered in {@code setup(Window)}).
 *
 * <p>{@code @Inject at HEAD, cancellable}: when the zoom key is held and scroll-adjust is on,
 * {@link ZoomState#onScroll} consumes the event for zoom level and this cancels the method,
 * pre-empting vanilla's own hotbar-slot scroll for that event - a deliberate behaviour change, but
 * one that only applies while the player has actively opted into zoom mode by holding the key, not
 * to vanilla's default scroll behaviour otherwise.
 */
@Mixin(targets = "net.minecraft.client.MouseHandler")
public abstract class MouseScrollMixin {

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void velofine$routeZoomScroll(long window, double xOffset, double yOffset, CallbackInfo ci) {
        if (ZoomState.onScroll(yOffset)) {
            ci.cancel();
        }
    }
}
