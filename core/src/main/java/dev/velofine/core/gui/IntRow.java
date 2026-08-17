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

package dev.velofine.core.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * An integer row. v1.5 settings-UI overhaul: now a real slider on the row's second line (filled
 * track + thumb, click-to-position, drag-to-adjust) instead of rendering identically to every other
 * row and only being steppable via the inherited value-box click. The value box (first line) still
 * works exactly as before - click its left/right half to step by {@code step} - for fine adjustment
 * or wrap-around; the track is for coarse, direct positioning.
 *
 * <p>Drag support needed a real vanilla hook: confirmed via javap against the real 26.2 jar that
 * {@code AbstractWidget.onDrag(MouseButtonEvent, double, double)} is the protected override point
 * (mirroring {@code onClick}'s pattern) - added to {@code mcstubs} for this, since nothing in this
 * codebase needed drag before now.
 *
 * <p>Clamped at the bounds by default; a row can opt into wrapping instead (past {@code max} rolls
 * to {@code min} and vice versa) via the extra-arg constructor - useful for a value like zoom level
 * where "one more click past the top" reading as dead/stuck is worse than looping back around. Wrap
 * only applies to the value-box's step click, never to the track (a click/drag on the track sets an
 * absolute position, where "wrapping" would be a confusing, unpredictable jump).
 */
public final class IntRow extends OptionRow {

    private static final int TRACK_HEIGHT = 3;
    private static final int THUMB_WIDTH = 3;
    /** Extra vertical hit-region around the thin track, so clicking/dragging it isn't fiddly. */
    private static final int TRACK_HIT_PADDING = 3;

    private final int min;
    private final int max;
    private final int step;
    private final IntSupplier getter;
    private final IntConsumer setter;
    private final String suffix;
    private final int defaultValue;
    private final boolean wrap;

    public IntRow(int x, int y, int width, String label, String description, Applies applies,
            int min, int max, int step, String suffix, IntSupplier getter, IntConsumer setter, int defaultValue) {
        this(x, y, width, label, description, applies, min, max, step, suffix, getter, setter, defaultValue, false);
    }

    public IntRow(int x, int y, int width, String label, String description, Applies applies,
            int min, int max, int step, String suffix, IntSupplier getter, IntConsumer setter, int defaultValue,
            boolean wrap) {
        super(x, y, width, label, description, applies);
        this.min = min;
        this.max = max;
        this.step = step;
        this.suffix = suffix;
        this.getter = getter;
        this.setter = setter;
        this.defaultValue = defaultValue;
        this.wrap = wrap;
    }

    @Override
    protected Component valueText() {
        return Component.literal(getter.getAsInt() + suffix);
    }

    @Override
    protected void activate(int direction) {
        int next = getter.getAsInt() + direction * step;
        if (wrap) {
            if (next > max) {
                next = min;
            } else if (next < min) {
                next = max;
            }
            setter.accept(next);
        } else {
            setter.accept(Math.max(min, Math.min(max, next)));
        }
    }

    @Override
    protected boolean valueIsOverride() {
        return getter.getAsInt() != defaultValue;
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        if (!active) {
            return;
        }
        if (isOnTrack(event.y())) {
            setter.accept(valueFromX(event.x()));
            return;
        }
        super.onClick(event, doubleClick);
    }

    @Override
    protected void onDrag(MouseButtonEvent event, double dragX, double dragY) {
        if (!active) {
            return;
        }
        setter.accept(valueFromX(event.x()));
    }

    @Override
    protected void renderSecondLine(GuiGraphicsExtractor extractor, boolean active) {
        int trackX = trackX();
        int trackWidth = trackWidth();
        int trackY = trackY();

        extractor.fill(trackX, trackY, trackX + trackWidth, trackY + TRACK_HEIGHT, VelofineTheme.PANEL_RAISED);
        VelofineTheme.border(extractor, trackX, trackY, trackWidth, TRACK_HEIGHT,
                active ? VelofineTheme.BORDER_BRIGHT : VelofineTheme.BORDER);

        double ratio = ratioFor(getter.getAsInt());
        int fillWidth = (int) Math.round(ratio * trackWidth);
        if (fillWidth > 0) {
            extractor.fill(trackX, trackY, trackX + fillWidth, trackY + TRACK_HEIGHT,
                    active ? VelofineTheme.ACCENT_DIM : VelofineTheme.BORDER);
        }

        int thumbX = Math.max(trackX, Math.min(trackX + trackWidth - THUMB_WIDTH, trackX + fillWidth - THUMB_WIDTH / 2));
        extractor.fill(thumbX, trackY - 2, thumbX + THUMB_WIDTH, trackY + TRACK_HEIGHT + 2,
                active ? VelofineTheme.ACCENT : VelofineTheme.BORDER_BRIGHT);
    }

    private double ratioFor(int value) {
        int clamped = Math.max(min, Math.min(max, value));
        return max == min ? 0.0 : (clamped - min) / (double) (max - min);
    }

    private int valueFromX(double x) {
        int trackX = trackX();
        int trackWidth = trackWidth();
        double ratio = trackWidth <= 0 ? 0.0 : Math.max(0.0, Math.min(1.0, (x - trackX) / (double) trackWidth));
        int raw = min + (int) Math.round(ratio * (max - min));
        int stepped = step <= 0 ? raw : min + Math.round((raw - min) / (float) step) * step;
        return Math.max(min, Math.min(max, stepped));
    }

    private boolean isOnTrack(double y) {
        int trackY = trackY();
        return y >= trackY - TRACK_HIT_PADDING && y <= trackY + TRACK_HEIGHT + TRACK_HIT_PADDING;
    }

    private int trackX() {
        return getX() + VelofineTheme.PADDING + 3;
    }

    private int trackWidth() {
        return getWidth() - VelofineTheme.PADDING * 2 - 3;
    }

    private int trackY() {
        return getY() + getHeight() - VelofineTheme.PADDING - TRACK_HEIGHT;
    }
}
