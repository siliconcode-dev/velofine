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

import dev.velofine.core.CoreEngine;
import dev.velofine.core.agent.AgentContext;
import dev.velofine.legacysupport.LegacySupportEngine;
import dev.velofine.optimus.OptimusEngine;
import dev.velofine.utility.UtilityEngine;

import java.lang.instrument.Instrumentation;

/**
 * Velofine's java.lang.instrument agent. Loaded via {@link Main}'s self-attach at normal
 * Minecraft-launch time (so {@link #agentmain} runs), and also exposes {@link #premain} for
 * completeness/manual testing with a literal {@code -javaagent:} flag.
 *
 * <p>Acquires {@link Instrumentation}, publishes it via {@link AgentContext} for engines to use,
 * then hands off to each engine. {@link CoreEngine} goes first and unconditionally - it installs
 * the config UI's entry-point mixins, which must stay reachable even when every other engine below
 * it is switched off in {@code config.json}, otherwise a user who disables all three has locked
 * themselves out of the only place to turn them back on.
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
                + ", canRedefine=" + inst.isRedefineClassesSupported() + ")");

        CoreEngine.onAgentAttached(inst);
        LegacySupportEngine.onAgentAttached(inst);
        OptimusEngine.onAgentAttached(inst);
        UtilityEngine.onAgentAttached(inst);
    }
}
