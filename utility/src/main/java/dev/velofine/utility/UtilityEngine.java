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

package dev.velofine.utility;

import dev.velofine.core.config.ConfigManager;
import dev.velofine.core.log.VelofineLog;

import java.lang.instrument.Instrumentation;

/**
 * Entry point for the Utility engine.
 *
 * <p>Phase 5 brings this engine into existence - the master toggle, the config panel, and
 * {@link SafeDefaultsPolicy} - without shipping any of the OptiFine-parity QoL features
 * themselves; those are Phase 6. There is deliberately no {@code mixins.utility.json} yet: booting
 * Mixin and installing an empty config would be pure overhead with nothing to verify, and
 * {@code VerifyMixinsHarness} has nothing to check until a real mixin exists. The moment Phase 6
 * adds one, this method gains the same {@code MixinBootstrap.init()} /
 * {@code Mixins.addConfiguration(...)} / {@code MixinBridge.install(...)} sequence every other
 * engine already uses.
 */
public final class UtilityEngine {

    private UtilityEngine() {
    }

    public static void onAgentAttached(Instrumentation instrumentation) {
        if (!ConfigManager.get().engines.utility) {
            VelofineLog.info("Utility", "Engine disabled in config.");
            return;
        }
        VelofineLog.info("Utility", "Engine enabled; no features ship until Phase 6. Safe-defaults policy: "
                + (SafeDefaultsPolicy.shouldStartSafe() ? "would start SAFE on this hardware" : "would start FULL"));
    }
}
