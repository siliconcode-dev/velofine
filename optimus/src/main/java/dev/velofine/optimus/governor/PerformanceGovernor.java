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

package dev.velofine.optimus.governor;

import dev.velofine.core.config.ConfigManager;
import dev.velofine.core.config.GovernorMode;
import dev.velofine.core.config.VelofineConfig;
import dev.velofine.core.log.VelofineLog;
import dev.velofine.core.status.LiveStatus;

/**
 * Adjusts render distance in real time to hold a target frame rate.
 *
 * <p>Actuation is a single call: {@code Options.renderDistance().set(n)}. Confirmed against the
 * real 26.2 bytecode that {@code OptionInstance.set} fires the option's own
 * {@code ValueUpdateListener} whenever the value actually changed and the game is running, and
 * that {@code renderDistance} is constructed with one - so vanilla itself rebuilds the chunk graph
 * and re-sends the server view distance. The governor needs no bytecode patching of its own.
 *
 * <p>Shape of the control loop, following DRD (the closest actively-maintained prior art):
 * <ul>
 *   <li>Descend by two chunks, climb back by one. A struggling machine recovers quickly; a
 *       borderline one settles instead of oscillating between two distances forever.</li>
 *   <li>A cooldown after every change, so the effect of a change is measured before the next.</li>
 *   <li>A grace period after world join, because the frame rate during initial chunk loading says
 *       nothing about the frame rate afterwards.</li>
 *   <li>A hard ceiling at the user's own render distance. The governor is allowed to take away
 *       and give back; it is never allowed to hand out more than was asked for.</li>
 * </ul>
 *
 * <p>The ceiling is kept in Velofine's config rather than read from {@code options.txt}: vanilla
 * persists that file when the options screen closes, so a governor-lowered value would otherwise
 * come back next session looking like the user's own choice, and the original would be lost.
 */
public final class PerformanceGovernor {

    private static final int TICKS_PER_SECOND = 20;
    /** Ten seconds. Initial chunk loading dominates the frame rate well past the loading screen. */
    private static final int WORLD_JOIN_GRACE_TICKS = 200;
    private static final int ABSOLUTE_MIN_RENDER_DISTANCE = 2;

    private final GovernorHost host;
    private final FpsSampler sampler;

    private boolean wasInWorld;
    private int graceTicks;
    private int cooldownTicks;
    private int ticksSinceEvaluation;

    /** What this governor last set, so a value that differs must have come from the user. */
    private int lastAppliedDistance = -1;

    public PerformanceGovernor(GovernorHost host, int windowSeconds) {
        this.host = host;
        this.sampler = new FpsSampler(windowSeconds);
    }

    public void onClientTick() {
        VelofineConfig config = ConfigManager.get();
        VelofineConfig.GovernorSection governor = config.optimus.governor;

        if (!host.inWorld()) {
            if (wasInWorld) {
                leaveWorld();
            }
            return;
        }
        if (!wasInWorld) {
            enterWorld(governor);
        }

        if (!config.engines.optimus || governor.mode == GovernorMode.OFF) {
            releaseControl(governor);
            LiveStatus.setGovernor("governor: off");
            return;
        }
        if (governor.mode == GovernorMode.MANUAL) {
            LiveStatus.setGovernor("governor: MANUAL, fixed at " + governor.manualRenderDistance + " chunks");
            return;
        }

        // Frame rate in a menu is not the frame rate in the world, so sampling pauses while any
        // screen is open - including Velofine's own config screen, which is why that screen does
        // not pause the game either.
        if (host.screenOpen()) {
            return;
        }
        if (graceTicks > 0) {
            graceTicks--;
            LiveStatus.setGovernor("governor: ADAPTIVE, settling (" + (graceTicks / TICKS_PER_SECOND + 1) + "s)");
            return;
        }

        sampler.onTick(host.fps());
        if (cooldownTicks > 0) {
            cooldownTicks--;
        }

        ticksSinceEvaluation++;
        int interval = Math.max(1, governor.checkIntervalSeconds) * TICKS_PER_SECOND;
        if (ticksSinceEvaluation < interval) {
            publishStatus(governor, sampler.average(), host.renderDistance());
            return;
        }
        ticksSinceEvaluation = 0;

        if (!sampler.ready()) {
            publishStatus(governor, sampler.average(), host.renderDistance());
            return;
        }

        evaluate(governor);
    }

    private void evaluate(VelofineConfig.GovernorSection governor) {
        int current = host.renderDistance();
        adoptUserChanges(governor, current);

        int average = sampler.average();
        int target = current;
        if (average < governor.minFps) {
            target = current - Math.max(1, governor.decreaseStep);
        } else if (average > governor.maxFps) {
            target = current + Math.max(1, governor.increaseStep);
        }

        int floor = Math.max(ABSOLUTE_MIN_RENDER_DISTANCE, governor.floorRenderDistance);
        int ceiling = Math.max(floor, governor.baselineRenderDistance);
        target = Math.max(floor, Math.min(ceiling, target));

        if (target != current) {
            if (cooldownTicks > 0) {
                publishStatus(governor, average, current);
                return;
            }
            host.setRenderDistance(target);
            lastAppliedDistance = target;
            cooldownTicks = Math.max(0, governor.cooldownSeconds) * TICKS_PER_SECOND;
            VelofineLog.info("Optimus", "governor: " + average + " fps avg -> render distance "
                    + current + " to " + target + " (ceiling " + ceiling + ", floor " + floor + ")");
            current = target;
        }
        publishStatus(governor, average, current);
    }

    /**
     * Keeps the ceiling in step with the player. If render distance is not what this governor last
     * set it, the player changed it themselves, and that new value becomes the ceiling - otherwise
     * raising the slider in Video Settings would appear to do nothing the moment the governor next
     * pulled it back down.
     */
    private void adoptUserChanges(VelofineConfig.GovernorSection governor, int current) {
        boolean firstRun = governor.baselineRenderDistance <= 0;
        boolean userMoved = lastAppliedDistance >= 0 && current != lastAppliedDistance;
        boolean aboveCeiling = current > governor.baselineRenderDistance;

        if (firstRun || userMoved || aboveCeiling) {
            governor.baselineRenderDistance = current;
            lastAppliedDistance = -1;
            ConfigManager.save();
            VelofineLog.info("Optimus", "governor: ceiling set to " + current + " chunks (your render distance)");
        }
    }

    /** Hands render distance back to the user, used when the governor is switched off mid-session. */
    private void releaseControl(VelofineConfig.GovernorSection governor) {
        if (lastAppliedDistance < 0 || governor.baselineRenderDistance <= 0) {
            return;
        }
        if (host.renderDistance() != governor.baselineRenderDistance) {
            host.setRenderDistance(governor.baselineRenderDistance);
            VelofineLog.info("Optimus", "governor: disabled, render distance restored to "
                    + governor.baselineRenderDistance);
        }
        lastAppliedDistance = -1;
    }

    private void enterWorld(VelofineConfig.GovernorSection governor) {
        wasInWorld = true;
        graceTicks = WORLD_JOIN_GRACE_TICKS;
        ticksSinceEvaluation = 0;
        cooldownTicks = 0;
        sampler.reset();

        if (ConfigManager.get().engines.optimus && governor.mode == GovernorMode.MANUAL) {
            int fixed = Math.max(ABSOLUTE_MIN_RENDER_DISTANCE, governor.manualRenderDistance);
            if (host.renderDistance() != fixed) {
                host.setRenderDistance(fixed);
                VelofineLog.info("Optimus", "governor: MANUAL, render distance pinned to " + fixed);
            }
            lastAppliedDistance = fixed;
        }
    }

    private void leaveWorld() {
        wasInWorld = false;
        lastAppliedDistance = -1;
        sampler.reset();
        LiveStatus.setGovernor("governor: idle (no world)");
    }

    private void publishStatus(VelofineConfig.GovernorSection governor, int average, int current) {
        String cooldown = cooldownTicks > 0 ? ", cooldown " + (cooldownTicks / TICKS_PER_SECOND + 1) + "s" : "";
        LiveStatus.setGovernor("governor: ADAPTIVE, " + average + " fps avg, distance " + current
                + "/" + Math.max(current, governor.baselineRenderDistance) + cooldown);
    }
}
