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

package dev.velofine.shaders.format.option;

import dev.velofine.core.log.VelofineLog;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Every boolean/string option a shaderpack exposes, keyed by option name - the data the Phase 7
 * in-game shader-options screen renders against. Deliberately simpler than Iris's own
 * {@code OptionSet}: v1 doesn't yet track per-file {@code OptionLocation}s or merge conflicting
 * redefinitions of the same option across included files (that needs a real GLSL
 * {@code #include}-dependency graph, which nothing in this module builds yet - see
 * {@code dev.velofine.shaders.format.ShaderPack}'s class javadoc for the tracked follow-up). A
 * redefinition with a different default here simply keeps the first one seen and logs a warning,
 * rather than Iris's location-aware merge. Adapted from IrisShaders/Iris
 * ({@code common/.../shaderpack/option/OptionSet.java}), LGPL-3.0.
 */
public final class OptionSet {
    private final Map<String, BooleanOption> booleanOptions;
    private final Map<String, StringOption> stringOptions;

    private OptionSet(Builder builder) {
        this.booleanOptions = Collections.unmodifiableMap(new HashMap<>(builder.booleanOptions));
        this.stringOptions = Collections.unmodifiableMap(new HashMap<>(builder.stringOptions));
    }

    public static Builder builder() {
        return new Builder();
    }

    public Map<String, BooleanOption> getBooleanOptions() {
        return booleanOptions;
    }

    public Map<String, StringOption> getStringOptions() {
        return stringOptions;
    }

    public boolean isBooleanOption(String name) {
        return booleanOptions.containsKey(name);
    }

    public static final class Builder {
        private final Map<String, BooleanOption> booleanOptions = new HashMap<>();
        private final Map<String, StringOption> stringOptions = new HashMap<>();

        public void addBooleanOption(BooleanOption option) {
            if (stringOptions.containsKey(option.getName())) {
                VelofineLog.warn("Shaders", "Ignoring ambiguous option (both boolean and string): " + option.getName());
                return;
            }
            booleanOptions.putIfAbsent(option.getName(), option);
        }

        public void addStringOption(StringOption option) {
            if (booleanOptions.containsKey(option.getName())) {
                VelofineLog.warn("Shaders", "Ignoring ambiguous option (both boolean and string): " + option.getName());
                return;
            }
            stringOptions.putIfAbsent(option.getName(), option);
        }

        public OptionSet build() {
            return new OptionSet(this);
        }
    }
}
