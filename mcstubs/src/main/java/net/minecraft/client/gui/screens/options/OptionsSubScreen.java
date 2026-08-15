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

package net.minecraft.client.gui.screens.options;

import net.minecraft.client.gui.components.OptionsList;

/**
 * Exists only as a cast target for {@code VideoSettingsScreenMixin}'s field read below - not for
 * {@code @Shadow} (Mixin 0.8.7's field shadowing only searches the exact target class, not its
 * superclasses; confirmed against SpongePowered/Mixin#284, which is still open).
 *
 * <p>{@code list} is declared {@code public} here even though the real field is {@code protected}.
 * That is deliberate: this stub is never loaded at runtime, so its modifiers only have to satisfy
 * javac for code that ultimately gets merged into the real {@code VideoSettingsScreen}'s bytecode -
 * a genuine subclass of the real (protected) {@code OptionsSubScreen.list} - where the JVM
 * verifier permits the access regardless of what this fictitious type declares. Not extending
 * {@code Screen}, for the same reason every stub here keeps only what is actually referenced:
 * nothing that touches this cast needs any of {@code Screen}'s members through it.
 */
public abstract class OptionsSubScreen {

    public OptionsList list;
}
