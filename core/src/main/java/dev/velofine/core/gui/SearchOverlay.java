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

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Item 8: fuzzy(-ish - real substring matching, not a scored fuzzy algorithm) search across every
 * page's settings, opened with {@code /} or Ctrl+F.
 *
 * <p>Deliberately keyboard-only: selecting a result is Up/Down + Enter, not a mouse click. This
 * overlay is drawn manually on top of everything else in {@code VelofineConfigScreen} rather than
 * through a registered widget, and confirming a safe way to intercept raw mouse clicks for it
 * wasn't attempted this pass (unlike {@code onClick}/{@code onDrag}, which had a confirmed real
 * vanilla hook to build against) - keyboard interaction needed no such hook, since
 * {@code VelofineConfigScreen} already owns {@code keyPressed}/{@code charTyped} directly.
 */
public final class SearchOverlay {

    public record Entry(int pageIndex, String pageTitle, String label, String description) {
    }

    private static final int MAX_VISIBLE_RESULTS = 8;

    private boolean open;
    private final StringBuilder query = new StringBuilder();
    private List<Entry> allEntries = List.of();
    private List<Entry> matches = List.of();
    private int selected;

    public boolean isOpen() {
        return open;
    }

    public void open(List<Entry> allEntries) {
        this.allEntries = allEntries;
        this.open = true;
        query.setLength(0);
        selected = 0;
        refilter();
    }

    public void close() {
        open = false;
    }

    /** @return true if the character was consumed (search was open). */
    public boolean charTyped(int codepoint) {
        if (!open) {
            return false;
        }
        query.appendCodePoint(codepoint);
        refilter();
        return true;
    }

    public void backspace() {
        if (query.length() > 0) {
            query.deleteCharAt(query.length() - 1);
            refilter();
        }
    }

    public void moveSelection(int direction) {
        if (matches.isEmpty()) {
            return;
        }
        selected = Math.floorMod(selected + direction, Math.min(matches.size(), MAX_VISIBLE_RESULTS));
    }

    /** The currently-highlighted result, or {@code null} if there are no matches. */
    public Entry selectedEntry() {
        if (matches.isEmpty() || selected >= matches.size()) {
            return null;
        }
        return matches.get(selected);
    }

    private void refilter() {
        String needle = query.toString().toLowerCase(Locale.ROOT);
        if (needle.isBlank()) {
            matches = allEntries;
        } else {
            List<Entry> found = new ArrayList<>();
            for (Entry entry : allEntries) {
                if (entry.label().toLowerCase(Locale.ROOT).contains(needle)
                        || entry.description().toLowerCase(Locale.ROOT).contains(needle)) {
                    found.add(entry);
                }
            }
            matches = found;
        }
        selected = 0;
    }

    public void render(GuiGraphicsExtractor extractor, Font font, int screenWidth, int screenHeight) {
        if (!open) {
            return;
        }

        int visibleCount = Math.min(matches.size(), MAX_VISIBLE_RESULTS);
        int rowHeight = 14;
        int boxWidth = Math.min(460, screenWidth - 80);
        int headerHeight = 26;
        int boxHeight = headerHeight + Math.max(1, visibleCount) * rowHeight + 6;
        int boxLeft = (screenWidth - boxWidth) / 2;
        int boxTop = 60;

        extractor.fill(0, 0, screenWidth, screenHeight, 0xC0000000);
        VelofineTheme.panel(extractor, boxLeft, boxTop, boxWidth, boxHeight, VelofineTheme.PANEL,
                VelofineTheme.BORDER_BRIGHT);
        VelofineTheme.marker(extractor, boxLeft + 1, boxTop + 1, headerHeight - 2);

        String queryLine = "Search Velofine settings... " + query;
        extractor.text(font, queryLine, boxLeft + VelofineTheme.PADDING + 3, boxTop + 8, VelofineTheme.TEXT);
        VelofineTheme.horizontalRule(extractor, boxLeft, boxTop + headerHeight, boxWidth, VelofineTheme.BORDER);

        if (matches.isEmpty()) {
            extractor.text(font, "No matches", boxLeft + VelofineTheme.PADDING + 3,
                    boxTop + headerHeight + 4, VelofineTheme.TEXT_DISABLED);
            return;
        }

        int y = boxTop + headerHeight + 3;
        for (int i = 0; i < visibleCount; i++) {
            Entry entry = matches.get(i);
            boolean highlighted = i == selected;
            if (highlighted) {
                extractor.fill(boxLeft + 1, y, boxLeft + boxWidth - 1, y + rowHeight, VelofineTheme.PANEL_RAISED);
            }
            String line = entry.pageTitle() + "  ›  " + entry.label();
            extractor.text(font, line, boxLeft + VelofineTheme.PADDING + 3, y + 3,
                    highlighted ? VelofineTheme.TEXT : VelofineTheme.TEXT_DIM);
            y += rowHeight;
        }
    }
}
