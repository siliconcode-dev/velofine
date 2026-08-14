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

package dev.velofine.core.agent;

import java.lang.instrument.Instrumentation;

/**
 * Holds the {@link Instrumentation} handed to the agent at attach time, plus the name of
 * vanilla's real main class, so LegacySupport/Optimus/Utility can register their own class
 * transformers later without depending on the launcher module directly.
 */
public final class AgentContext {

    private static volatile Instrumentation instrumentation;
    private static volatile String vanillaMainClass;

    private AgentContext() {
    }

    public static void init(Instrumentation inst, String vanillaMainClassName) {
        instrumentation = inst;
        vanillaMainClass = vanillaMainClassName;
    }

    public static Instrumentation instrumentation() {
        return instrumentation;
    }

    public static String vanillaMainClass() {
        return vanillaMainClass;
    }

    public static boolean isAttached() {
        return instrumentation != null;
    }
}
