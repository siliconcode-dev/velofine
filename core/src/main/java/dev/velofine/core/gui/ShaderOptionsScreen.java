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

import dev.velofine.core.shader.ShaderPackOptionInfo;
import dev.velofine.core.shader.ShaderPackBrowserRegistry;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The currently-selected shader pack's own {@code #define}-discovered options (Phase 7.7's "full
 * generic options UI" - see {@code dev.velofine.shaders.format.option.OptionDiscovery}), reusing
 * {@link CycleRow} for both boolean and string/enum options rather than a new widget type -
 * matching Phase 6's resolved finding that the existing row vocabulary stretches further than
 * expected (see CLAUDE.md).
 *
 * <p>Deliberately not staged behind UNDO/APPLY like {@link VelofineConfigScreen}: every option
 * here is inherently a "reload the pack" operation already ({@link
 * dev.velofine.core.shader.ShaderPackBrowser#setOptionValue} re-activates the pack with the new
 * value baked into preprocessing), so applying on click - the same live-on-click precedent {@code
 * UtilityPage} already sets for AA/mipmap/Vsync rows bound straight to vanilla {@code Options} - is
 * consistent rather than a special case. Each row's getter re-reads {@link
 * ShaderPackBrowserRegistry} fresh on every render, so a value changed by one row (or an external
 * reload) is always shown correctly without an explicit rebuild step.
 */
public final class ShaderOptionsScreen extends Screen {

    private static final int ESCAPE_KEY = 256;
    private static final int MARGIN = 48;
    private static final int MIN_WIDTH = 360;
    private static final int MIN_HEIGHT = 200;
    private static final int HEADER_HEIGHT = 22;
    private static final int ACTION_HEIGHT = 30;

    private final Screen parent;
    private final List<OptionRow> rows = new ArrayList<>();

    private int boxLeft;
    private int boxTop;
    private int boxWidth;
    private int boxHeight;
    private int listTop;
    private int listBottom;
    private int scroll;

    public ShaderOptionsScreen(Screen parent) {
        super(Component.literal("Velofine Shader Options"));
        this.parent = parent;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        boxWidth = Math.max(MIN_WIDTH, Math.min(width - MARGIN * 2, 420));
        boxHeight = Math.max(MIN_HEIGHT, height - MARGIN * 2);
        boxLeft = (width - boxWidth) / 2;
        boxTop = (height - boxHeight) / 2;
        listTop = boxTop + HEADER_HEIGHT + VelofineTheme.PADDING;
        listBottom = boxTop + boxHeight - ACTION_HEIGHT;

        buildRows();

        addRenderableWidget(new ActionButton(boxLeft + boxWidth - 78 - VelofineTheme.PADDING,
                boxTop + boxHeight - ACTION_HEIGHT + 4, 78, 20, "CLOSE", ActionButton.STYLE_NORMAL, this::onClose));
    }

    private void buildRows() {
        rows.clear();
        int x = boxLeft + VelofineTheme.PADDING;
        int width = boxWidth - VelofineTheme.PADDING * 2;
        int y = listTop;

        List<ShaderPackOptionInfo> options = ShaderPackBrowserRegistry.listOptionsForSelectedPack();
        for (ShaderPackOptionInfo option : options) {
            rows.add(buildRow(option, x, y, width));
            y += VelofineTheme.ROW_HEIGHT + VelofineTheme.ROW_SPACING;
        }

        for (OptionRow row : rows) {
            addWidget(row);
        }
    }

    private OptionRow buildRow(ShaderPackOptionInfo option, int x, int y, int width) {
        String name = option.name();
        List<String> values = option.booleanOption() ? List.of("true", "false") : option.allowedValues();

        return new CycleRow<>(x, y, width, name, option.comment(), Applies.LIVE,
                values,
                value -> option.booleanOption() ? ("true".equals(value) ? "ON" : "OFF") : value,
                () -> currentValue(name).orElse(values.isEmpty() ? "" : values.get(0)),
                value -> ShaderPackBrowserRegistry.setOptionValue(name, value),
                option.currentValue());
    }

    private Optional<String> currentValue(String optionName) {
        return ShaderPackBrowserRegistry.listOptionsForSelectedPack().stream()
                .filter(o -> o.name().equals(optionName))
                .map(ShaderPackOptionInfo::currentValue)
                .findFirst();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == ESCAPE_KEY) {
            onClose();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseY < listTop || mouseY > listBottom || scrollY == 0.0) {
            return false;
        }
        scroll -= (int) Math.signum(scrollY) * (VelofineTheme.ROW_HEIGHT + VelofineTheme.ROW_SPACING);
        clampScroll();
        return true;
    }

    private void clampScroll() {
        int contentHeight = rows.size() * (VelofineTheme.ROW_HEIGHT + VelofineTheme.ROW_SPACING);
        int visibleHeight = listBottom - listTop;
        scroll = Math.max(0, Math.min(scroll, Math.max(0, contentHeight - visibleHeight)));
    }

    @Override
    public void onClose() {
        minecraft.setScreenAndShow(parent);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        extractor.fill(0, 0, width, height, VelofineTheme.BACKDROP);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        VelofineTheme.panel(extractor, boxLeft, boxTop, boxWidth, boxHeight, VelofineTheme.PANEL, VelofineTheme.BORDER_BRIGHT);
        extractor.fill(boxLeft + 1, boxTop + 1, boxLeft + boxWidth - 1, boxTop + HEADER_HEIGHT, 0xFF000000);
        extractor.text(font, "SHADER OPTIONS", boxLeft + VelofineTheme.PADDING, boxTop + 7, VelofineTheme.TEXT);
        VelofineTheme.horizontalRule(extractor, boxLeft + 1, boxTop + HEADER_HEIGHT, boxWidth - 2, VelofineTheme.BORDER);

        if (rows.isEmpty()) {
            extractor.text(font, "This pack declares no discovered options.",
                    boxLeft + VelofineTheme.PADDING, listTop, VelofineTheme.TEXT_DIM);
        } else {
            extractor.enableScissor(boxLeft, listTop, boxLeft + boxWidth, listBottom);
            int y = listTop - scroll;
            for (OptionRow row : rows) {
                row.setY(y);
                boolean visible = y + VelofineTheme.ROW_HEIGHT > listTop && y < listBottom;
                row.visible = visible;
                row.active = visible;
                if (visible) {
                    row.extractRenderState(extractor, mouseX, mouseY, partialTick);
                }
                y += VelofineTheme.ROW_HEIGHT + VelofineTheme.ROW_SPACING;
            }
            extractor.disableScissor();
        }

        super.extractRenderState(extractor, mouseX, mouseY, partialTick);
    }
}
