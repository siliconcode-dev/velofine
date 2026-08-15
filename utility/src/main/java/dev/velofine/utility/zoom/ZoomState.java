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

package dev.velofine.utility.zoom;

import dev.velofine.core.config.ConfigManager;
import dev.velofine.core.config.VelofineConfig;
import dev.velofine.core.status.LiveStatus;

/**
 * Smoothed, scroll-adjustable zoom factor, modeled on Zoomify's approach: nothing about the curve
 * is hardcoded, everything comes from {@link VelofineConfig.ZoomSection}.
 *
 * <p>Held here as plain static state (mirroring {@code ConfigKeyBinding}'s edge-detect pattern)
 * rather than merged into {@code Camera} itself - {@code CameraMixin} only ever reads
 * {@link #currentFactor()}, so it stays a one-line delegation like every other pure-side-effect
 * mixin in this codebase.
 *
 * <p><b>The smoothing step itself must run per rendered frame, not per tick.</b> An earlier
 * version stepped the lerp inside {@link #onTick}, which only runs 20 times/second - but
 * {@code Camera.calculateFov} (and so {@link #currentFactor()}) is read once per rendered frame,
 * which is almost always a much higher rate. That mismatch meant the FOV value sat frozen for
 * ~50ms and then jumped, reading as stutter rather than smooth motion at any framerate above
 * ~20fps. {@link #currentFactor()} now measures real elapsed time between frames itself and steps
 * there instead - {@link #onTick} only edge-detects the key and updates {@link #targetFactor}.
 *
 * <p>The curve is exponential ease-out ({@code 1 - e^(-dt/smoothing)}) rather than a linear ramp -
 * a decelerating approach toward the target reads as noticeably smoother than a constant-speed
 * lerp that then stops abruptly once it reaches the target, and it is still exactly as
 * framerate-independent (driven by real {@code deltaSeconds}, not frame count).
 */
public final class ZoomState {

    private static boolean wasKeyDown;
    private static double targetFactor = 1.0;
    private static double currentFactor = 1.0;
    private static long lastFrameNanos;

    private ZoomState() {
    }

    /** Called once per client tick. Only edge-detects the zoom key - see class javadoc. */
    public static void onTick(boolean keyDown) {
        VelofineConfig.ZoomSection zoom = ConfigManager.get().utility.zoom;

        if (keyDown && !wasKeyDown) {
            targetFactor = zoom.maxZoomFactor;
        } else if (!keyDown && wasKeyDown) {
            targetFactor = 1.0;
        }
        wasKeyDown = keyDown;

        LiveStatus.setZoom(currentFactor > 1.01 ? String.format("zoom: %.1fx", currentFactor) : "zoom: idle");
    }

    /**
     * Adjusts the zoom target while the zoom key is held, in response to a real scroll delta from
     * {@code MouseScrollMixin}. Clamped to {@code [1, maxZoomFactor]} - never below 1x (that would
     * be zooming <em>out</em> past normal FOV, which is not what OptiFine/Zoomify-style zoom does).
     *
     * @return {@code true} if the scroll was consumed (zoom active), so the mixin can cancel
     *     vanilla's own hotbar-slot scroll for that event.
     */
    public static boolean onScroll(double yOffset) {
        VelofineConfig.ZoomSection zoom = ConfigManager.get().utility.zoom;
        if (!zoom.enabled || !zoom.scrollAdjustable || !wasKeyDown) {
            return false;
        }
        targetFactor = Math.max(1.0, Math.min(zoom.maxZoomFactor, targetFactor + yOffset));
        return true;
    }

    /**
     * Read by {@code CameraMixin} every {@code calculateFov} call - i.e. once per rendered frame.
     * Steps the exponential-ease-out lerp toward {@link #targetFactor} using real elapsed time
     * since the last frame, so the result is smooth at any framerate. Always &gt;= 1.0.
     */
    public static double currentFactor() {
        VelofineConfig.ZoomSection zoom = ConfigManager.get().utility.zoom;

        long now = System.nanoTime();
        double deltaSeconds = lastFrameNanos == 0 ? 0.0 : (now - lastFrameNanos) / 1_000_000_000.0;
        lastFrameNanos = now;

        double smoothing = Math.max(0.001, zoom.smoothingSeconds);
        double t = deltaSeconds <= 0 ? 0.0 : 1.0 - Math.exp(-deltaSeconds / smoothing);
        currentFactor += (targetFactor - currentFactor) * t;
        if (Math.abs(currentFactor - targetFactor) < 0.001) {
            currentFactor = targetFactor;
        }
        return currentFactor;
    }
}
