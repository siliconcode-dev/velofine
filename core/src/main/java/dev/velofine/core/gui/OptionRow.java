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

import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * One settings row: name+value on a first line, a short inline description (or, for {@link IntRow},
 * a slider track) on a second line beneath it.
 *
 * <p>v1.5 settings-UI overhaul: activation used to be directional across the <em>whole</em> row
 * (left half back, right half forward) - selecting a row (which drives the description panel below)
 * and changing its value were the same click. They're now split: hovering/focusing a row already
 * selects it for the description panel (unchanged, see {@code VelofineConfigScreen.drawRows}); only
 * a click that actually lands in the value box on the right changes the value. Clicking the
 * label/description area is a no-op rather than an accidental value change.
 */
public abstract class OptionRow extends VelofineWidget {

    /** Width of the value box on the right-hand side of the row's first line. */
    public static final int VALUE_WIDTH = 74;

    private final String label;
    private final String description;
    private final Applies applies;
    /**
     * The longer explanation shown in the hover detail panel. Defaults to {@link #description} but
     * can be overridden via {@link #withDetail(String)} where the inline one-liner necessarily
     * leaves out useful context - e.g. exact ranges/units, why a fix exists, what it trades off.
     */
    private String detail;
    /**
     * Whether the row's current value matches what Velofine would suggest for the detected
     * hardware (item 9/10: "give them AUTO - recommended for most systems" instead of raw enum
     * cycling). {@code false} by default - most rows have no hardware-derived recommendation at
     * all, and it would be dishonest to badge them as if they did.
     */
    private BooleanSupplier recommended = () -> false;

    protected OptionRow(int x, int y, int width, String label, String description, Applies applies) {
        super(x, y, width, VelofineTheme.CONTENT_ROW_HEIGHT, Component.literal(label));
        this.label = label;
        this.description = description;
        this.detail = description;
        this.applies = applies;
    }

    public String label() {
        return label;
    }

    /** The short, one-line inline description shown under the row itself. */
    public String description() {
        return description;
    }

    /** The (possibly longer) text shown in the hover detail panel. */
    public String detail() {
        return detail;
    }

    /** Sets a longer hover-panel explanation distinct from the short inline {@link #description()}. */
    public final OptionRow withDetail(String detail) {
        this.detail = detail;
        return this;
    }

    public Applies applies() {
        return applies;
    }

    /** Marks this row as having a hardware-derived recommendation, shown as a green indicator. */
    public final OptionRow withRecommended(BooleanSupplier recommended) {
        this.recommended = recommended;
        return this;
    }

    /** True when the current value matches what Velofine would suggest for this hardware. */
    public boolean isRecommended() {
        return active && recommended.getAsBoolean();
    }

    /** Rendered inside the value box. */
    protected abstract Component valueText();

    /** @param direction {@code +1} to step forwards, {@code -1} backwards. */
    protected abstract void activate(int direction);

    /** True when the value is a deliberate user override rather than the default. */
    protected boolean valueIsOverride() {
        return false;
    }

    /** Left edge of the value box - shared by rendering and hit-testing so they can't drift apart. */
    protected int valueBoxX() {
        return getX() + getWidth() - VALUE_WIDTH - VelofineTheme.PADDING;
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        if (!active) {
            return;
        }
        int boxX = valueBoxX();
        if (event.x() < boxX) {
            // Label/description area: selection already happened via hover, this is a no-op.
            return;
        }
        activate(event.x() < boxX + VALUE_WIDTH / 2.0 ? -1 : 1);
    }

    // Deliberately does NOT override mouseScrolled. Rows cover most of the panel, so consuming
    // scroll here would make the option list unscrollable wherever the cursor actually is - and
    // worse, would let a player change a setting while trying to scroll past it.

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY,
            float partialTick) {
        boolean highlight = isHoveredOrFocused() && active;

        extractor.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(),
                highlight ? VelofineTheme.PANEL_RAISED : VelofineTheme.PANEL);
        if (highlight) {
            VelofineTheme.marker(extractor, getX(), getY(), getHeight());
        }

        int firstLineY = getY() + 5;
        int labelColor = active ? (highlight ? VelofineTheme.TEXT : VelofineTheme.TEXT_DIM)
                : VelofineTheme.TEXT_DISABLED;
        String shownLabel = applies == Applies.RESTART ? label + " *" : label;
        extractor.text(font(), shownLabel, getX() + VelofineTheme.PADDING + 3, firstLineY, labelColor);

        int boxX = valueBoxX();
        int boxHeight = font().lineHeight + 4;
        int boxBorder;
        int valueColor;
        if (!active) {
            boxBorder = VelofineTheme.BORDER;
            valueColor = VelofineTheme.TEXT_DISABLED;
        } else if (valueIsOverride()) {
            boxBorder = VelofineTheme.ACCENT;
            valueColor = VelofineTheme.TEXT;
        } else {
            boxBorder = highlight ? VelofineTheme.BORDER_BRIGHT : VelofineTheme.BORDER;
            valueColor = VelofineTheme.TEXT;
        }

        VelofineTheme.panel(extractor, boxX, getY() + 2, VALUE_WIDTH, boxHeight,
                VelofineTheme.PANEL, boxBorder);
        int valueTextY = getY() + 2 + (boxHeight - 8) / 2;
        extractor.centeredText(font(), valueText(), boxX + VALUE_WIDTH / 2, valueTextY, valueColor);

        if (isRecommended()) {
            extractor.fill(boxX + VALUE_WIDTH - 5, getY() + 1, boxX + VALUE_WIDTH - 1, getY() + 3,
                    VelofineTheme.SUCCESS);
        }

        renderSecondLine(extractor, active);
    }

    /**
     * The row's second line - by default a one-line truncated description (full text remains
     * available in the hover detail panel via {@link #description()}, so truncation here loses
     * nothing). {@link IntRow} overrides this to draw a slider track instead.
     */
    protected void renderSecondLine(GuiGraphicsExtractor extractor, boolean active) {
        if (description == null || description.isEmpty()) {
            return;
        }
        int textX = getX() + VelofineTheme.PADDING + 3;
        int y = getY() + 5 + font().lineHeight + 4;
        int maxWidth = getWidth() - VelofineTheme.PADDING * 2 - 3;
        List<String> oneLine = TextWrap.wrap(font(), description, maxWidth, 1);
        if (!oneLine.isEmpty()) {
            extractor.text(font(), oneLine.get(0), textX, y, active ? VelofineTheme.TEXT_DIM : VelofineTheme.TEXT_DISABLED);
        }
    }
}
