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

package dev.velofine.core.config;

import dev.velofine.core.hardware.Fix;

import java.util.EnumMap;
import java.util.Map;

/**
 * The whole of Velofine's persisted configuration, as a plain mutable POJO tree.
 *
 * <p>Deliberately plain fields with initializers rather than records or builders: Gson constructs
 * these through their implicit no-arg constructors, so field initializers run and any key missing
 * from an older {@code config.json} keeps its default instead of arriving as {@code null}. Nested
 * objects behave the same way. The one case that needs care is {@link LegacySupportSection#fixes} -
 * Gson replaces a present map wholesale, so a {@link Fix} added in a later Velofine version would
 * simply be absent; {@link LegacySupportSection#mode(Fix)} therefore treats a missing entry as
 * {@link Tri#AUTO} instead of dereferencing null.
 */
public final class VelofineConfig {

    /** Bumped only for a breaking layout change that needs migration, not for added fields. */
    public static final int CURRENT_VERSION = 1;

    public int configVersion = CURRENT_VERSION;

    public EnginesSection engines = new EnginesSection();
    public LegacySupportSection legacySupport = new LegacySupportSection();
    public OptimusSection optimus = new OptimusSection();
    public UtilitySection utility = new UtilitySection();
    public UiSection ui = new UiSection();

    /** Master on/off per engine. Turning one off skips its mixin config entirely at next launch. */
    public static final class EnginesSection {
        public boolean legacySupport = true;
        public boolean optimus = true;
        public boolean utility = true;
    }

    public static final class LegacySupportSection {
        /**
         * Per-fix override. Every fix defaults to {@link Tri#AUTO}, i.e. "whatever
         * {@code FixProfileRules} resolved from the detected hardware".
         */
        public Map<Fix, Tri> fixes = defaultFixes();

        public Tri mode(Fix fix) {
            if (fixes == null) {
                return Tri.AUTO;
            }
            return fixes.getOrDefault(fix, Tri.AUTO);
        }

        public void setMode(Fix fix, Tri mode) {
            if (fixes == null) {
                fixes = defaultFixes();
            }
            fixes.put(fix, mode);
        }

        private static Map<Fix, Tri> defaultFixes() {
            EnumMap<Fix, Tri> map = new EnumMap<>(Fix.class);
            for (Fix fix : Fix.values()) {
                map.put(fix, Tri.AUTO);
            }
            return map;
        }
    }

    public static final class OptimusSection {
        /** Pins {@code -Dmax.bg.threads}; read once when vanilla's {@code Util} class-loads. */
        public boolean threadPoolTuning = true;
        /** Raises vanilla's goal-selector re-evaluation interval from 2 ticks to 3. */
        public boolean goalSelectorThrottle = true;
        /** Off by default - it is a diagnostic that logs every 100 ticks, not a feature. */
        public boolean tickProfiler = false;

        public GovernorSection governor = new GovernorSection();
    }

    /**
     * Tuning for the adaptive render-distance governor. The defaults follow DRD (Dynamic Render
     * Distance), the closest actively-maintained prior art: sample once a second, evaluate every
     * five, and descend faster than you climb so a struggling machine recovers quickly while a
     * recovering one does not immediately oscillate back down.
     */
    public static final class GovernorSection {
        public GovernorMode mode = GovernorMode.OFF;
        public int minFps = 45;
        public int maxFps = 75;
        public int decreaseStep = 2;
        public int increaseStep = 1;
        public int checkIntervalSeconds = 5;
        public int cooldownSeconds = 10;
        public int floorRenderDistance = 4;

        /**
         * The user's own render distance - the ceiling the governor may never exceed. Held here
         * rather than read from {@code options.txt} because vanilla persists that file on options
         * screen close, which would otherwise let a governor-lowered value masquerade as the
         * user's choice. {@code 0} means "not captured yet".
         */
        public int baselineRenderDistance = 0;

        /** Used only by {@link GovernorMode#MANUAL}. */
        public int manualRenderDistance = 8;
    }

    public static final class UtilitySection {
        /**
         * Masterdoc 4.3: Utility features should default to off/reduced when LegacySupport flags
         * the hardware as weak, rather than defaulting to "on" everywhere. No Utility features
         * exist until Phase 6 - this is the policy switch they will consult.
         */
        public boolean safeDefaultsOnWeakHardware = true;
    }

    public static final class UiSection {
        /**
         * GLFW key code that opens the Velofine config screen in-game. {@code -1} is unbound,
         * which is the default - Velofine should not steal a key the player already uses.
         */
        public int openConfigKey = -1;
    }
}
