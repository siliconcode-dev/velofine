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

package dev.velofine.core.gui.page;

import dev.velofine.core.config.GovernorMode;
import dev.velofine.core.config.VelofineConfig;
import dev.velofine.core.gui.Applies;
import dev.velofine.core.gui.ConfigPage;
import dev.velofine.core.gui.CycleRow;
import dev.velofine.core.gui.IntRow;
import dev.velofine.core.gui.OptionRow;
import dev.velofine.core.status.LiveStatus;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.List;

/**
 * Optimus's panel: the Phase 4 optimizations plus the Phase 5 performance governor.
 *
 * <p>The footnote is the live governor read-out - current rolling FPS, current and baseline render
 * distance, cooldown - which is what makes Build_plan's "see the governor visibly respond to FPS
 * changes in real time" exit criterion actually observable without the Phase 6 benchmark overlay.
 */
public final class OptimusPage extends ConfigPage {

    private static final List<GovernorMode> MODES =
            List.of(GovernorMode.OFF, GovernorMode.ADAPTIVE, GovernorMode.MANUAL);

    @Override
    public String title() {
        return "OPTIMUS";
    }

    @Override
    public boolean engineEnabled(VelofineConfig working) {
        return working.engines.optimus;
    }

    @Override
    public List<OptionRow> buildRows(VelofineConfig working, RowCursor cursor, Screen screen) {
        List<OptionRow> rows = new ArrayList<>();
        VelofineConfig.GovernorSection governor = working.optimus.governor;

        rows.add(CycleRow.ofBoolean(cursor.x(), cursor.nextY(), cursor.width(),
                "Optimus engine",
                "Master switch for all performance work: thread-pool sizing, the AI tick throttle "
                        + "and the governor.",
                Applies.RESTART,
                () -> working.engines.optimus,
                value -> working.engines.optimus = value,
                true));

        rows.add(CycleRow.ofBoolean(cursor.x(), cursor.nextY(), cursor.width(),
                "Background thread pool",
                "Pins vanilla's background worker count to (CPU cores - 1). Vanilla already uses "
                        + "that formula implicitly; this makes it explicit and auditable. Read once "
                        + "when the game's Util class loads, so it cannot change mid-session.",
                Applies.RESTART,
                () -> working.optimus.threadPoolTuning,
                value -> working.optimus.threadPoolTuning = value,
                true));

        rows.add(CycleRow.ofBoolean(cursor.x(), cursor.nextY(), cursor.width(),
                "AI goal throttle",
                "Re-evaluates each mob's goal selector every 3 ticks instead of vanilla's 2. "
                        + "Currently-running goals still tick every tick, so behaviour is unchanged - "
                        + "only how often mobs reconsider.",
                Applies.LIVE,
                () -> working.optimus.goalSelectorThrottle,
                value -> working.optimus.goalSelectorThrottle = value,
                true));

        rows.add(CycleRow.ofBoolean(cursor.x(), cursor.nextY(), cursor.width(),
                "Tick-time profiler",
                "Logs min/avg/max client tick time every 100 ticks. A diagnostic for reporting "
                        + "back on slow hardware, not a speed-up - off by default.",
                Applies.LIVE,
                () -> working.optimus.tickProfiler,
                value -> working.optimus.tickProfiler = value,
                false));

        rows.add(new CycleRow<>(cursor.x(), cursor.nextY(), cursor.width(),
                "Governor mode",
                "OFF leaves render distance alone. ADAPTIVE lowers it when FPS drops and restores "
                        + "it as FPS recovers, never going above your own setting. MANUAL just pins "
                        + "one fixed distance on world join.",
                Applies.LIVE,
                MODES, Enum::name,
                () -> governor.mode,
                value -> governor.mode = value,
                GovernorMode.OFF));

        rows.add(new IntRow(cursor.x(), cursor.nextY(), cursor.width(),
                "Lower below FPS",
                "When the 5-second average frame rate falls under this, the governor drops render "
                        + "distance by two chunks.",
                Applies.LIVE,
                15, 240, 5, " fps",
                () -> governor.minFps,
                value -> governor.minFps = value,
                45));

        rows.add(new IntRow(cursor.x(), cursor.nextY(), cursor.width(),
                "Raise above FPS",
                "When the average climbs past this, the governor gives back one chunk at a time - "
                        + "slower than it takes away, so a borderline machine settles instead of "
                        + "oscillating.",
                Applies.LIVE,
                20, 300, 5, " fps",
                () -> governor.maxFps,
                value -> governor.maxFps = value,
                75));

        rows.add(new IntRow(cursor.x(), cursor.nextY(), cursor.width(),
                "Minimum distance",
                "The governor will never lower render distance below this, however bad the frame "
                        + "rate gets.",
                Applies.LIVE,
                2, 16, 1, " chunks",
                () -> governor.floorRenderDistance,
                value -> governor.floorRenderDistance = value,
                4));

        rows.add(new IntRow(cursor.x(), cursor.nextY(), cursor.width(),
                "Manual distance",
                "The fixed render distance applied on world join in MANUAL mode. Ignored in the "
                        + "other two modes.",
                Applies.LIVE,
                2, 32, 1, " chunks",
                () -> governor.manualRenderDistance,
                value -> governor.manualRenderDistance = value,
                8));

        return rows;
    }

    @Override
    public String footnote() {
        return LiveStatus.governor();
    }
}
