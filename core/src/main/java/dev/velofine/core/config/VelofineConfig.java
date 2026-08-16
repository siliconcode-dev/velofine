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
import java.util.HashMap;
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
    public UpdaterSection updater = new UpdaterSection();

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
         * the hardware as weak, rather than defaulting to "on" everywhere. Phase 6's features each
         * consult {@code SafeDefaultsPolicy.shouldStartSafe()} individually rather than this field
         * gating them directly, so a user can still turn any one of them on themselves.
         */
        public boolean safeDefaultsOnWeakHardware = true;

        public RenderingSection rendering = new RenderingSection();
        public ZoomSection zoom = new ZoomSection();
        public RenderDistanceSection renderDistance = new RenderDistanceSection();
        public FpsOverlaySection fpsOverlay = new FpsOverlaySection();
        public DynamicLightsSection dynamicLights = new DynamicLightsSection();
        public ShaderSection shader = new ShaderSection();
    }

    /**
     * Fog and anti-aliasing controls (Phase 6.1). Mipmap levels, anisotropic filtering and vsync
     * are <em>not</em> here - they are already real, live vanilla {@code Options} fields
     * ({@code mipmapLevels()}, {@code maxAnisotropyBit()}, {@code enableVsync()}), so Utility's
     * panel binds rows directly to those and lets vanilla's own {@code options.txt} persist them,
     * rather than shadowing them in a second config file.
     */
    public static final class RenderingSection {
        /** {@code FogRendererMixin} pushes fog start/end far past view distance when false. */
        public boolean fogEnabled = true;
        /** Off by default - a real (if usually small) per-frame GPU cost, unlike the other rows here. */
        public boolean antiAliasing = false;
    }

    /**
     * Modeled on Zoomify's fully-configurable-no-hardcoded-values approach: nothing about the zoom
     * curve is baked in.
     */
    public static final class ZoomSection {
        public boolean enabled = true;
        /** GLFW key code held to zoom. Unbound by default - Velofine should not steal a key. */
        public int key = -1;
        public double maxZoomFactor = 4.0;
        /** Higher = more gradual. Bumped from an initial 0.1 per real-world testing feedback wanting a noticeably softer feel (the actual stutter bug was a separate tick-vs-frame issue, see ZoomState). */
        public double smoothingSeconds = 0.2;
        public boolean scrollAdjustable = true;
    }

    public static final class RenderDistanceSection {
        /** {@code false} must be bit-identical to vanilla - a single, unsplit render distance. */
        public boolean enabled = false;
        /** 0 means "match horizontal" - vanilla's own single-slider behaviour. */
        public int verticalDistance = 0;
    }

    public static final class FpsOverlaySection {
        public boolean enabled = false;
        public int sampleWindowSeconds = 5;
    }

    /**
     * Modeled on LambDynamicLights: an item-&gt;light-level registry (0-15, vanilla block-light
     * scale), entity-carried items and burning entities included by default.
     */
    public static final class DynamicLightsSection {
        public boolean enabled = true;
        public boolean includeEntities = true;
        public boolean includeDroppedItems = true;
        public int updateIntervalTicks = 1;
    }

    /**
     * Phase 7 (OptiFine/Iris-format shader pipeline). {@code selectedPackName} is a folder/zip name
     * under {@code shaderpacks/}, not a path - packs are discovered by scanning that directory, the
     * same drop-in convention OptiFine/Iris themselves use, rather than an in-game file picker (no
     * such widget exists in {@code dev.velofine.core.gui} today). {@code packOptions} holds one flat
     * {@code optionName -> chosenValue} map per pack name so a user's in-game edits to a pack's own
     * {@code [SETTING]}-defined options survive a restart without needing per-pack schema baked into
     * this config file - {@code ShaderPackOptions} (shaders module) is the source of truth for what
     * each option's valid values/defaults are; a stale or unrecognized entry here is simply ignored
     * rather than treated as an error, since packs come and go independently of Velofine's own
     * version.
     */
    public static final class ShaderSection {
        public boolean enabled = false;
        public String selectedPackName = null;
        public Map<String, Map<String, String>> packOptions = new HashMap<>();
    }

    public static final class UiSection {
        /**
         * GLFW key code that opens the Velofine config screen in-game. {@code -1} is unbound,
         * which is the default - Velofine should not steal a key the player already uses.
         */
        public int openConfigKey = -1;
    }

    /**
     * Phase 8 (in-app updater). Unlike LegacySupport/Optimus/Utility, the updater has no vanilla+
     * feature to disable when "off" - it installs no mixins, so there is nothing for an
     * {@code engines.updater} master toggle to gate. {@link #autoCheckOnLaunch} alone covers "don't
     * check automatically"; a manual "Check for Updates" button always works regardless.
     */
    public static final class UpdaterSection {
        public boolean autoCheckOnLaunch = true;
        /** Epoch millis of the last check (auto or manual) - rate-limits the passive launch check. */
        public long lastCheckedEpochMillis = 0L;
        /** A version the user explicitly dismissed via "Later" - not re-announced until a newer one ships. */
        public String skippedVersion = null;
        /**
         * The vanilla Minecraft version this install is patching - written by
         * {@code ProfileInstaller.install()} at install time, since that is the one place the
         * actually-targeted version is known. Filters which releases' {@code manifest.json} the
         * updater considers, so a future parallel MC-version track never offers a cross-version
         * update. {@code null} until the profile has been installed at least once.
         */
        public String trackedMcVersion = null;
    }
}
