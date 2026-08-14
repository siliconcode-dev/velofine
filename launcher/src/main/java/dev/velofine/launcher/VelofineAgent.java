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

package dev.velofine.launcher;

import dev.velofine.core.agent.AgentContext;

import java.lang.instrument.Instrumentation;

/**
 * Velofine's java.lang.instrument agent. Loaded via {@link Main}'s self-attach at normal
 * Minecraft-launch time (so {@link #agentmain} runs), and also exposes {@link #premain} for
 * completeness/manual testing with a literal {@code -javaagent:} flag.
 *
 * <p>Phase 1 scope only: acquire {@link Instrumentation}, publish it via {@link AgentContext} for
 * later engines to use, and prove attachment with a log line. It registers zero class transformers
 * and does not touch Mixin — see CLAUDE.md's "Mixin tooling decision" for why that's deferred to
 * Phase 2.
 */
public final class VelofineAgent {

    private VelofineAgent() {
    }

    public static void premain(String agentArgs, Instrumentation inst) {
        init(inst);
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        init(inst);
    }

    private static void init(Instrumentation inst) {
        String vanillaMainClass = System.getProperty("velofine.vanillaMainClass", "<unset>");
        AgentContext.init(inst, vanillaMainClass);
        System.out.println("[Velofine] agent attached: Instrumentation acquired "
                + "(canRetransform=" + inst.isRetransformClassesSupported()
                + ", canRedefine=" + inst.isRedefineClassesSupported() + "); "
                + "0 transformers registered (Phase 1 — pipeline validation only)");
    }
}
