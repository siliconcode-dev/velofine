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

import java.util.ArrayList;
import java.util.List;

/**
 * Greedy word wrapping against the game's own font metrics.
 *
 * <p>Vanilla has {@code textWithWordWrap}, but it takes a {@code FormattedText} and lays out the
 * whole block itself, which gives no control over how many lines are kept. The description box has
 * a fixed height, so Velofine needs to wrap and then truncate with an ellipsis rather than overflow
 * into the button bar.
 */
public final class TextWrap {

    private TextWrap() {
    }

    public static List<String> wrap(Font font, String text, int maxWidth, int maxLines) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty() || maxWidth <= 0 || maxLines <= 0) {
            return lines;
        }

        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            if (line.isEmpty()) {
                line.append(word);
                continue;
            }
            if (font.width(line + " " + word) <= maxWidth) {
                line.append(' ').append(word);
                continue;
            }
            lines.add(line.toString());
            line.setLength(0);
            line.append(word);
            if (lines.size() == maxLines) {
                break;
            }
        }
        if (lines.size() < maxLines && !line.isEmpty()) {
            lines.add(line.toString());
        }

        // If we ran out of room mid-sentence, say so rather than stopping in a way that reads like
        // the description simply ended there.
        if (lines.size() == maxLines && font.width(text) > maxWidth * maxLines) {
            String last = lines.get(maxLines - 1);
            while (!last.isEmpty() && font.width(last + " ...") > maxWidth) {
                last = last.substring(0, last.length() - 1);
            }
            lines.set(maxLines - 1, last + " ...");
        }
        return lines;
    }
}
