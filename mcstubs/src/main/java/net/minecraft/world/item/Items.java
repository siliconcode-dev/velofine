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

package net.minecraft.world.item;

/**
 * Confirmed real constants via javap against the real 26.2 jar - only the subset
 * {@code DynamicLightsState}'s built-in light-source table needs, not the full vanilla item list.
 * Values here are never loaded at runtime ({@code mcstubs} is {@code compileOnly}), so the
 * placeholder initializers just need to satisfy {@code javac}, not hold real registry identity.
 */
public class Items {
    public static final Item TORCH = new Item();
    public static final Item SOUL_TORCH = new Item();
    public static final Item REDSTONE_TORCH = new Item();
    public static final Item LANTERN = new Item();
    public static final Item SOUL_LANTERN = new Item();
    public static final Item GLOWSTONE = new Item();
    public static final Item SEA_LANTERN = new Item();
    public static final Item SHROOMLIGHT = new Item();
    public static final Item JACK_O_LANTERN = new Item();
    public static final Item LAVA_BUCKET = new Item();
}
