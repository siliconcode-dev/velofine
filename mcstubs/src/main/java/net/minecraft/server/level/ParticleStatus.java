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

package net.minecraft.server.level;

/**
 * Vanilla's particle-density setting. Only the three constant names matter (OptionsMixin's
 * {@code @Redirect} returns {@code DECREASED} in place of {@code ALL}); the enum's real ordering
 * is preserved so the names line up with the real class.
 */
public enum ParticleStatus {
    ALL,
    DECREASED,
    MINIMAL
}
