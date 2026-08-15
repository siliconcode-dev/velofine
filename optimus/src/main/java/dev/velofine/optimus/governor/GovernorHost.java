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

/**
 * Everything {@link PerformanceGovernor} needs from the running game, behind an interface.
 *
 * <p>This exists so the governor's decision logic can be exercised without Minecraft at all. The
 * governor is the one piece of Phase 5 whose correctness is genuinely subtle - hysteresis,
 * cooldown, a grace period, a ceiling that must never be exceeded - and it is also the piece
 * nobody on this project can test on the hardware it is for. Being able to drive it with a
 * scripted frame-rate sequence and assert the exact render distances it chooses is the only real
 * verification available.
 */
public interface GovernorHost {

    /** False in menus and while loading; the governor stays completely idle then. */
    boolean inWorld();

    /** True while any screen is open, including Velofine's own. */
    boolean screenOpen();

    int fps();

    int renderDistance();

    void setRenderDistance(int chunks);
}
