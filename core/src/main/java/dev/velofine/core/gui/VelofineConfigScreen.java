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

import dev.velofine.core.config.ConfigManager;
import dev.velofine.core.config.VelofineConfig;
import dev.velofine.core.gui.page.LegacySupportPage;
import dev.velofine.core.gui.page.OptimusPage;
import dev.velofine.core.gui.page.OverviewPage;
import dev.velofine.core.gui.page.UpdaterPage;
import dev.velofine.core.gui.page.UtilityPage;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Velofine's config screen: a left rail of engine panels, the selected panel's rows on the right,
 * a description box for whatever the cursor is over, and a staged UNDO/APPLY bar.
 *
 * <p>The structure is modelled on Sodium's video-settings screen, which solves the same problem
 * (many independent toggles, each needing an explanation and some needing a restart) better than a
 * flat list would. The visual language is Velofine's own: flat black panels, one-pixel borders,
 * and a red marker block for anything selected or overridden.
 *
 * <p>Edits go to a working copy and only reach {@link ConfigManager} on APPLY. That matters more
 * here than in a normal settings screen, because several of these toggles are read by mixin
 * handlers on the very next tick - a half-typed intermediate state should never leak into the
 * running game.
 */
public final class VelofineConfigScreen extends Screen {

    private static final int HEADER_HEIGHT = 22;
    private static final int RAIL_WIDTH = 112;
    /** Reserved space below the row list for the footnote line - includes a real gap above the
     * text itself, not just the text's own height, so a full-to-the-edge row list never reads as
     * colliding with it. */
    private static final int FOOTNOTE_HEIGHT = 20;
    /**
     * Floor for the description panel, not its size - {@link #infoHeight} is computed per-{@link
     * #init()} from whatever vertical space the current page's content doesn't need (v1.5 UI
     * overhaul: previously a fixed 40px regardless of content, which is why long descriptions
     * silently truncated with "..." and short pages left the panel starved next to a wall of dead
     * space above it).
     */
    private static final int MIN_INFO_HEIGHT = 46;
    /** Description panel never eats more than this share of the box, even on a near-empty page. */
    private static final double MAX_INFO_HEIGHT_RATIO = 0.35;
    private static final int ACTION_HEIGHT = 30;
    /** Gap kept between the panel and the window edge, so it reads as a panel, not a wallpaper. */
    private static final int MARGIN = 32;
    /** Floor, not a cap - guards against a degenerate layout on a tiny/minimized window. */
    private static final int MIN_WIDTH = 420;
    private static final int MIN_HEIGHT = 260;
    private static final int ESCAPE_KEY = 256;
    // Raw GLFW key codes - same convention ESCAPE_KEY above already uses (this file has never
    // pulled in an LWJGL/GLFW dependency just for named constants; legacysupport does that, core
    // doesn't).
    private static final int ENTER_KEY = 257;
    private static final int BACKSPACE_KEY = 259;
    private static final int DOWN_KEY = 264;
    private static final int UP_KEY = 265;
    private static final int F_KEY = 70;
    private static final int MOD_CONTROL = 0x0002;

    private final Screen parent;
    private final List<ConfigPage> pages =
            List.of(new OverviewPage(), new LegacySupportPage(), new OptimusPage(), new UtilityPage(),
                    new UpdaterPage());
    private final List<OptionRow> rows = new ArrayList<>();
    private final SearchOverlay search = new SearchOverlay();

    private VelofineConfig working = ConfigManager.copyOfLive();
    private String appliedSnapshot = ConfigManager.snapshot(ConfigManager.get());
    private boolean dirty;

    private int pageIndex = 1;
    private int scroll;

    private int boxLeft;
    private int boxTop;
    private int boxWidth;
    private int boxHeight;
    private int listTop;
    private int listBottom;
    private int infoHeight;

    private ActionButton undoButton;
    private ActionButton applyButton;

    public VelofineConfigScreen(Screen parent) {
        super(Component.literal("Velofine"));
        this.parent = parent;
    }

    /**
     * Never pauses the game. A paused singleplayer world stops ticking, which would freeze the
     * governor read-out on the Optimus panel - the one place a user can actually watch it react.
     */
    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        // Fills the window minus a fixed margin, so it scales with resolution/window resize
        // instead of sitting at a small fixed size on a large screen - MIN_WIDTH/MIN_HEIGHT only
        // kick in below that (a tiny window), never capping it on a big one. The box itself stays
        // window-relative (resizing the whole panel to content would be visually jarring); what's
        // content-aware is how that fixed box is split between the row list and the description
        // panel below it - see contentHeight/listBottom/infoHeight below.
        boxWidth = Math.max(MIN_WIDTH, width - MARGIN * 2);
        boxHeight = Math.max(MIN_HEIGHT, height - MARGIN * 2);
        boxLeft = (width - boxWidth) / 2;
        boxTop = (height - boxHeight) / 2;

        listTop = boxTop + HEADER_HEIGHT + VelofineTheme.PADDING;

        int contentHeight = measureContentHeight();
        int maxListHeight = boxTop + boxHeight - ACTION_HEIGHT - FOOTNOTE_HEIGHT - MIN_INFO_HEIGHT - listTop;
        int listHeight = Math.min(maxListHeight, Math.max(VelofineTheme.CONTENT_ROW_HEIGHT, contentHeight));
        listBottom = listTop + listHeight;

        int infoAvailable = boxTop + boxHeight - ACTION_HEIGHT - FOOTNOTE_HEIGHT - listBottom;
        int infoMax = (int) (boxHeight * MAX_INFO_HEIGHT_RATIO);
        infoHeight = Math.max(MIN_INFO_HEIGHT, Math.min(infoMax, infoAvailable));

        buildTabs();
        buildRows();
        buildActions();
    }

    /**
     * Dry-runs the selected page's {@code buildRows} against a throwaway cursor purely to learn how
     * tall its content is. Safe to discard: {@code buildRows} implementations only construct and
     * return rows, they don't mutate {@code working} or register anything - the real call in
     * {@link #buildRows()} (with final coordinates) redoes this properly once the layout above is
     * settled.
     */
    private int measureContentHeight() {
        int listWidth = boxWidth - RAIL_WIDTH - VelofineTheme.PADDING;
        ConfigPage.RowCursor probe = new ConfigPage.RowCursor(0, 0, listWidth);
        pages.get(pageIndex).buildRows(working, probe, this);
        return probe.usedHeight();
    }

    private void buildTabs() {
        int y = listTop;
        for (int i = 0; i < pages.size(); i++) {
            ConfigPage page = pages.get(i);
            int index = i;
            addRenderableWidget(new TabButton(boxLeft + VelofineTheme.PADDING, y,
                    RAIL_WIDTH - VelofineTheme.PADDING * 2, page.title(),
                    () -> pageIndex == index,
                    () -> page.engineEnabled(working),
                    () -> selectPage(index)));
            y += VelofineTheme.ROW_HEIGHT + VelofineTheme.ROW_SPACING;
        }
    }

    private void buildRows() {
        rows.clear();
        int listX = boxLeft + RAIL_WIDTH;
        int listWidth = boxWidth - RAIL_WIDTH - VelofineTheme.PADDING;
        ConfigPage.RowCursor cursor = new ConfigPage.RowCursor(listX, listTop, listWidth);
        rows.addAll(pages.get(pageIndex).buildRows(working, cursor, this));
        for (OptionRow row : rows) {
            // addWidget, not addRenderableWidget: rows are drawn by this screen inside a scissored
            // region so a long page can scroll without the rail and button bar scrolling with it.
            addWidget(row);
        }
        clampScroll();
    }

    private void buildActions() {
        int buttonWidth = 78;
        int y = boxTop + boxHeight - ACTION_HEIGHT + 4;
        int right = boxLeft + boxWidth - VelofineTheme.PADDING;

        ActionButton close = new ActionButton(right - buttonWidth, y, buttonWidth, 20, "CLOSE",
                ActionButton.STYLE_NORMAL, this::onClose);
        applyButton = new ActionButton(right - buttonWidth * 2 - 4, y, buttonWidth, 20, "APPLY",
                ActionButton.STYLE_PRIMARY, this::apply);
        undoButton = new ActionButton(right - buttonWidth * 3 - 8, y, buttonWidth, 20, "UNDO",
                ActionButton.STYLE_NORMAL, this::undo);

        addRenderableWidget(undoButton);
        addRenderableWidget(applyButton);
        addRenderableWidget(close);
        refreshDirty();
    }

    private void selectPage(int index) {
        if (index == pageIndex) {
            return;
        }
        pageIndex = index;
        scroll = 0;
        rebuildWidgets();
    }

    private void apply() {
        ConfigManager.replaceAndSave(working);
        working = ConfigManager.copyOfLive();
        appliedSnapshot = ConfigManager.snapshot(ConfigManager.get());
        rebuildWidgets();
    }

    private void undo() {
        working = ConfigManager.copyOfLive();
        rebuildWidgets();
    }

    @Override
    public void tick() {
        // Recomputed once a tick rather than once a frame - it is a string comparison over the
        // whole config, which is cheap but not free, and nothing here changes between ticks.
        refreshDirty();
    }

    private void refreshDirty() {
        dirty = !ConfigManager.snapshot(working).equals(appliedSnapshot);
        if (applyButton != null) {
            applyButton.active = dirty;
        }
        if (undoButton != null) {
            undoButton.active = dirty;
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (search.isOpen()) {
            if (event.key() == ESCAPE_KEY) {
                search.close();
                return true;
            }
            if (event.key() == BACKSPACE_KEY) {
                search.backspace();
                return true;
            }
            if (event.key() == DOWN_KEY) {
                search.moveSelection(1);
                return true;
            }
            if (event.key() == UP_KEY) {
                search.moveSelection(-1);
                return true;
            }
            if (event.key() == ENTER_KEY) {
                SearchOverlay.Entry chosen = search.selectedEntry();
                search.close();
                if (chosen != null) {
                    selectPage(chosen.pageIndex());
                }
                return true;
            }
            // Swallow everything else while search is open (e.g. so typing doesn't leak into a
            // KeybindRow that happened to be mid-capture underneath it).
            return true;
        }

        if (event.key() == F_KEY && (event.modifiers() & MOD_CONTROL) != 0) {
            search.open(buildSearchIndex());
            return true;
        }

        // Key capture is handled here rather than through widget focus so it works no matter what
        // vanilla's focus traversal is doing at the time.
        KeybindRow listening = listeningRow();
        if (listening != null) {
            listening.captureKey(event.key() == ESCAPE_KEY ? GlfwKeyNames.UNBOUND : event.key());
            refreshDirty();
            return true;
        }
        if (event.key() == ESCAPE_KEY) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (!search.isOpen() && event.codepoint() == '/' && listeningRow() == null) {
            search.open(buildSearchIndex());
            return true;
        }
        return search.charTyped(event.codepoint());
    }

    /**
     * Item 8: dry-runs every page's {@code buildRows} against a throwaway cursor (same safe,
     * side-effect-free pattern {@link #measureContentHeight()} already relies on) to build a flat,
     * searchable index - category headers and plain info lines are filtered out since they aren't
     * real settings.
     */
    private List<SearchOverlay.Entry> buildSearchIndex() {
        List<SearchOverlay.Entry> entries = new ArrayList<>();
        for (int i = 0; i < pages.size(); i++) {
            ConfigPage page = pages.get(i);
            ConfigPage.RowCursor probe = new ConfigPage.RowCursor(0, 0, 200);
            for (OptionRow row : page.buildRows(working, probe, this)) {
                if (row instanceof CategoryHeaderRow || row instanceof InfoRow) {
                    continue;
                }
                entries.add(new SearchOverlay.Entry(i, page.title(), row.label(), row.description()));
            }
        }
        return entries;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseY < listTop || mouseY > listBottom || scrollY == 0.0) {
            return false;
        }
        scroll -= (int) Math.signum(scrollY) * (VelofineTheme.CONTENT_ROW_HEIGHT + VelofineTheme.ROW_SPACING);
        clampScroll();
        return true;
    }

    private void clampScroll() {
        int contentHeight = rows.size() * (VelofineTheme.CONTENT_ROW_HEIGHT + VelofineTheme.ROW_SPACING);
        int visibleHeight = listBottom - listTop;
        scroll = Math.max(0, Math.min(scroll, Math.max(0, contentHeight - visibleHeight)));
    }

    @Override
    public void onClose() {
        // parent is null when opened from the in-game keybind, which is exactly how vanilla
        // expects "return to the game" to be expressed.
        minecraft.setScreenAndShow(parent);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        extractor.fill(0, 0, width, height, VelofineTheme.BACKDROP);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        drawChrome(extractor);

        OptionRow hovered = drawRows(extractor, mouseX, mouseY, partialTick);

        // Rail buttons and the action bar are ordinary renderable widgets, so vanilla draws them.
        super.extractRenderState(extractor, mouseX, mouseY, partialTick);

        drawFootnote(extractor);
        drawInfoBox(extractor, hovered);

        search.render(extractor, font, width, height);
    }

    private void drawChrome(GuiGraphicsExtractor extractor) {
        VelofineTheme.panel(extractor, boxLeft, boxTop, boxWidth, boxHeight, VelofineTheme.PANEL,
                VelofineTheme.BORDER_BRIGHT);

        // Header: solid red block, wordmark, and the Minecraft version this build targets.
        extractor.fill(boxLeft + 1, boxTop + 1, boxLeft + boxWidth - 1, boxTop + HEADER_HEIGHT, 0xFF000000);
        extractor.fill(boxLeft + VelofineTheme.PADDING, boxTop + 7, boxLeft + VelofineTheme.PADDING + 16,
                boxTop + 15, VelofineTheme.ACCENT);
        extractor.text(font, "VELOFINE", boxLeft + VelofineTheme.PADDING + 22, boxTop + 7, VelofineTheme.TEXT);
        String pageTitle = pages.get(pageIndex).title();
        extractor.text(font, pageTitle,
                boxLeft + boxWidth - VelofineTheme.PADDING - font.width(pageTitle), boxTop + 7,
                VelofineTheme.TEXT_DIM);
        VelofineTheme.horizontalRule(extractor, boxLeft + 1, boxTop + HEADER_HEIGHT, boxWidth - 2,
                VelofineTheme.BORDER);

        // Vertical rule between the rail and the option list.
        extractor.fill(boxLeft + RAIL_WIDTH - 2, boxTop + HEADER_HEIGHT + 1, boxLeft + RAIL_WIDTH - 1,
                listBottom + FOOTNOTE_HEIGHT, VelofineTheme.BORDER);
    }

    private OptionRow drawRows(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        OptionRow hovered = null;
        int listX = boxLeft + RAIL_WIDTH;
        int listWidth = boxWidth - RAIL_WIDTH - VelofineTheme.PADDING;

        extractor.enableScissor(listX, listTop, listX + listWidth, listBottom);
        int y = listTop - scroll;
        for (OptionRow row : rows) {
            row.setY(y);
            // Full containment only - a row that would only partially fit at the bottom edge is
            // skipped entirely rather than clipped mid-row. Scroll always moves in whole
            // CONTENT_ROW_HEIGHT+ROW_SPACING steps (see mouseScrolled), so at rest every row lands
            // exactly on a boundary; this just stops a would-be-clipped last row's second line from
            // reading as if it were colliding with the footnote directly below the list.
            boolean visible = y >= listTop && y + VelofineTheme.CONTENT_ROW_HEIGHT <= listBottom;
            row.visible = visible;
            row.active = visible;
            if (visible) {
                row.extractRenderState(extractor, mouseX, mouseY, partialTick);
                // Category headers and plain info lines have nothing to show in the detail panel -
                // never eligible to become the "hovered" row.
                boolean selectable = !(row instanceof CategoryHeaderRow) && !(row instanceof InfoRow);
                if (selectable && row.containsPoint(mouseX, mouseY)
                        && mouseY >= listTop && mouseY < listBottom) {
                    hovered = row;
                }
            }
            y += VelofineTheme.CONTENT_ROW_HEIGHT + VelofineTheme.ROW_SPACING;
        }
        extractor.disableScissor();
        return hovered;
    }

    private KeybindRow listeningRow() {
        for (OptionRow row : rows) {
            if (row instanceof KeybindRow keybind && keybind.isListening()) {
                return keybind;
            }
        }
        return null;
    }

    private void drawFootnote(GuiGraphicsExtractor extractor) {
        String footnote = pages.get(pageIndex).footnote();
        if (footnote == null) {
            return;
        }
        extractor.text(font, footnote, boxLeft + RAIL_WIDTH, listBottom + 3, VelofineTheme.TEXT_DIM);
    }

    private void drawInfoBox(GuiGraphicsExtractor extractor, OptionRow hovered) {
        int infoTop = boxTop + boxHeight - ACTION_HEIGHT - infoHeight;
        int infoLeft = boxLeft + VelofineTheme.PADDING;
        int infoWidth = boxWidth - VelofineTheme.PADDING * 2;

        VelofineTheme.panel(extractor, infoLeft, infoTop, infoWidth, infoHeight - 4,
                0xFF0C0C0C, VelofineTheme.BORDER);

        if (hovered == null) {
            extractor.text(font, "* needs a restart to take effect", infoLeft + VelofineTheme.PADDING,
                    infoTop + 6, VelofineTheme.TEXT_DISABLED);
            return;
        }

        VelofineTheme.marker(extractor, infoLeft + 1, infoTop + 1, infoHeight - 6);

        String badge = hovered.applies().badge();
        String rowLabel = hovered.label().toUpperCase(Locale.ROOT);
        extractor.text(font, rowLabel, infoLeft + VelofineTheme.PADDING + 3, infoTop + 7, VelofineTheme.TEXT);
        if (hovered.isRecommended()) {
            int afterLabelX = infoLeft + VelofineTheme.PADDING + 3 + font.width(rowLabel) + 6;
            extractor.text(font, "RECOMMENDED", afterLabelX, infoTop + 7, VelofineTheme.SUCCESS);
        }
        extractor.text(font, badge, infoLeft + infoWidth - VelofineTheme.PADDING - font.width(badge),
                infoTop + 7, hovered.applies() == Applies.RESTART ? VelofineTheme.WARNING : VelofineTheme.TEXT_DIM);

        int textWidth = infoWidth - VelofineTheme.PADDING * 2 - 3;
        // Grows with infoHeight instead of a hardcoded 2-line cap - a taller panel (more content on
        // this page, more reclaimed space) can now actually show a long description instead of
        // silently truncating it with "...".
        int maxLines = Math.max(2, (infoHeight - 21) / font.lineHeight);
        List<String> lines = TextWrap.wrap(font, hovered.detail(), textWidth, maxLines);
        int lineY = infoTop + 21;
        for (String line : lines) {
            extractor.text(font, line, infoLeft + VelofineTheme.PADDING + 3, lineY, VelofineTheme.TEXT_DIM);
            lineY += font.lineHeight;
        }
    }
}
