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

package dev.velofine.core.hardware;

import dev.velofine.core.gpu.GpuInfo;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Maps a {@link HardwareProfile} to the {@link Fix}es it activates. Each rule is independent and
 * additive (a machine can match several at once, e.g. an Intel Gen7 laptop that also has an HDD
 * and 4GB RAM matches three rules) — this is the extensibility point Build_plan Phase 3 asks for:
 * adding support for a newly-discovered problem hardware class means adding one entry here, not
 * touching {@code GlBackendMixin}/{@code GlDeviceMixin}/etc.
 */
public final class FixProfileRules {

    private record Rule(Predicate<HardwareProfile> matches, Set<Fix> fixes) {
    }

    private static final List<Rule> RULES = List.of(
            new Rule(p -> p.gpu().fixProfile() == GpuInfo.FixProfile.INTEL_GEN7,
                    EnumSet.of(Fix.GL_COMPATIBILITY_PROFILE, Fix.SHADER_MIX_PATCH)),
            new Rule(p -> p.gpu().fixProfile() == GpuInfo.FixProfile.GENERIC_OLD,
                    EnumSet.of(Fix.GL_COMPATIBILITY_PROFILE)),
            new Rule(p -> p.disk().rotational(),
                    EnumSet.of(Fix.IO_STALL_SMOOTHING)),
            new Rule(p -> p.memory().isLowMemory(),
                    EnumSet.of(Fix.MEMORY_SAVING_DEFAULTS))
    );

    private FixProfileRules() {
    }

    public static Set<Fix> resolve(HardwareProfile profile) {
        EnumSet<Fix> active = EnumSet.noneOf(Fix.class);
        for (Rule rule : RULES) {
            if (rule.matches().test(profile)) {
                active.addAll(rule.fixes());
            }
        }
        return active;
    }
}
