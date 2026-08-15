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
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Appends a "VELOFINE SETTINGS" row to the bottom of vanilla's Video Settings list.
 *
 * <p>This is the safest of Velofine's three entry points: {@code OptionsList} owns the row's
 * position and width, so there is no absolute layout arithmetic to get wrong and no way to land on
 * top of a vanilla widget. The row also scrolls with the rest of the list for free.
 *
 * <p>{@code list} is declared on {@code OptionsSubScreen}, the target's superclass, not on
 * {@code VideoSettingsScreen} itself - and Mixin 0.8.7's {@code @Shadow} only resolves fields
 * declared directly in the target class, never up the hierarchy (confirmed against the real
 * bytecode: {@code @Shadow} here throws {@code InvalidMixinException} at apply time; tracked
 * upstream as SpongePowered/Mixin#284, still open). The fix is to skip {@code @Shadow} for this
 * one field and read it directly through a cast to {@link OptionsSubScreen} instead - legal at
 * the real JVM level once Mixin merges this method into {@code VideoSettingsScreen}'s bytecode,
 * since it genuinely is a subclass of the real {@code OptionsSubScreen}. See that stub's own
 * javadoc for why its field is declared {@code public} despite the real one being
 * {@code protected}.
 */
@Mixin(targets = "net.minecraft.client.gui.screens.options.VideoSettingsScreen")
public abstract class VideoSettingsScreenMixin {

    @Inject(method = "addOptions", at = @At("RETURN"))
    private void velofine$addSettingsRow(CallbackInfo ci) {
        Screen self = (Screen) (Object) this;
        OptionsSubScreen subScreen = (OptionsSubScreen) (Object) this;
        subScreen.list.addBig(new ActionButton(0, 0, 200, 20, "VELOFINE SETTINGS", ActionButton.STYLE_PRIMARY,
                () -> Minecraft.getInstance().setScreenAndShow(new VelofineConfigScreen(self))));
    }
}
