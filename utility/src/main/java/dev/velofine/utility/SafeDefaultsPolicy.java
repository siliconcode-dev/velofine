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
import dev.velofine.core.hardware.HardwareProfiles;

/**
 * Masterdoc 4.3: "Utility features should be automatically disabled or reduced when LegacySupport
 * detects weak/flagged hardware, rather than defaulting to 'on' everywhere."
 *
 * <p>No Utility feature exists yet to consult this - that is Phase 6 - so today it is exercised
 * only by the Utility config panel's footnote, as a preview of what a feature would see. It is
 * written now, rather than deferred, because retrofitting a policy check into six features later
 * is more error-prone than giving Phase 6 one method to call from the start.
 */
public final class SafeDefaultsPolicy {

    private SafeDefaultsPolicy() {
    }

    /** Whether a Utility feature should start in its reduced/off state on this machine right now. */
    public static boolean shouldStartSafe() {
        return ConfigManager.get().utility.safeDefaultsOnWeakHardware && HardwareProfiles.isWeakHardware();
    }
}
