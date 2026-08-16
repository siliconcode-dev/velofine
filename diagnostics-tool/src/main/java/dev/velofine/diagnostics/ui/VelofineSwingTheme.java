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

package dev.velofine.diagnostics.ui;

import dev.velofine.diagnostics.ToolVersion;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.font.TextAttribute;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

/**
 * "Tactical Telemetry" styling: dark-mode-exclusive, monospace-dominant, ASCII-bracket framing,
 * visible compartmentalization via full-width rules rather than soft centered prose - the same
 * black/white/red brutalist direction the project CLAUDE.md establishes for the installer and
 * in-game config UI, applied with actual typographic discipline instead of ad hoc centered labels.
 *
 * <p>Applied per-component from each screen's constructor, not a full custom {@code LookAndFeel}.
 * The window's OS-drawn title bar (including its color, which follows the user's Windows accent
 * setting) is native chrome outside Swing content styling's reach - matching it too would need an
 * undecorated {@code JFrame} with a hand-drawn title bar, the bigger lift the in-game config UI
 * took on for its own screens; deliberately not taken on here yet.
 */
public final class VelofineSwingTheme {

    public static final Color BLACK = new Color(0x0A, 0x0A, 0x0A);
    public static final Color WHITE = new Color(0xEA, 0xEA, 0xEA);
    public static final Color DIM_WHITE = new Color(0x8A, 0x8A, 0x8A);
    public static final Color RED = new Color(0xE6, 0x19, 0x19);
    public static final Color DARK_GRAY = new Color(0x1A, 0x1A, 0x1A);
    public static final Color LINE = new Color(0x33, 0x33, 0x33);
    public static final Color TERMINAL_GREEN = new Color(0x4A, 0xF6, 0x26);

    /** Macro-typography: structural headers. Heavy, tight tracking, uppercase. */
    public static final Font MACRO = trackedFont(Font.SANS_SERIF, Font.BOLD, 24, -0.02f);
    /** Micro-typography: metadata, labels, nav - monospace, wide tracking, uppercase. */
    public static final Font MICRO = trackedFont(Font.MONOSPACED, Font.PLAIN, 12, 0.08f);
    public static final Font MICRO_BOLD = trackedFont(Font.MONOSPACED, Font.BOLD, 12, 0.08f);
    /** Body copy: shader names, paths, log/report text - standard monospace, no extra tracking. */
    public static final Font BODY = new Font(Font.MONOSPACED, Font.PLAIN, 13);

    private VelofineSwingTheme() {
    }

    private static Font trackedFont(String family, int style, int size, float tracking) {
        Font base = new Font(family, style, size);
        return base.deriveFont(Map.of(TextAttribute.TRACKING, tracking));
    }

    public static void styleFrame(JFrame frame) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Falls back to Swing's default L&F - purely cosmetic, must never block the tool from running.
        }
        frame.getContentPane().setBackground(BLACK);
    }

    public static void stylePanel(JPanel panel) {
        panel.setBackground(BLACK);
        panel.setForeground(WHITE);
    }

    /** A thin full-width rule - the one recurring compartmentalization motif tying screens together. */
    public static JPanel rule(Color color, int thicknessPx) {
        JPanel bar = new JPanel();
        bar.setBackground(color);
        bar.setPreferredSize(new Dimension(10, thicknessPx));
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, thicknessPx));
        return bar;
    }

    /**
     * App-wide top bar: {@code [ VELOFINE DIAGNOSTIC ]} at macro scale on the left, a step
     * indicator + tool revision at micro scale on the right, a red rule beneath - used identically
     * on every screen so the wizard reads as one continuous instrument, not five different screens.
     */
    public static JPanel topBar(String stepLabel) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BLACK);

        JLabel wordmark = new JLabel("[ VELOFINE DIAGNOSTIC ]");
        wordmark.setFont(MACRO);
        wordmark.setForeground(WHITE);

        JLabel meta = new JLabel(stepLabel + "   REV " + ToolVersion.version());
        meta.setFont(MICRO);
        meta.setForeground(DIM_WHITE);
        meta.setHorizontalAlignment(SwingConstants.RIGHT);

        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(BLACK);
        row.setBorder(BorderFactory.createEmptyBorder(18, 20, 14, 20));
        row.add(wordmark, BorderLayout.WEST);
        row.add(meta, BorderLayout.EAST);

        wrapper.add(row, BorderLayout.NORTH);
        wrapper.add(rule(RED, 3), BorderLayout.SOUTH);
        return wrapper;
    }

    /** Small uppercase section heading above a content block, e.g. {@code "TARGET DIRECTORY"}. */
    public static JLabel sectionLabel(String text) {
        JLabel label = new JLabel(text.toUpperCase());
        label.setFont(MICRO_BOLD);
        label.setForeground(WHITE);
        return label;
    }

    /** Dim, small instructional line, e.g. {@code "> folder containing versions/"}. */
    public static JLabel hint(String text) {
        JLabel label = new JLabel("> " + text);
        label.setFont(MICRO);
        label.setForeground(DIM_WHITE);
        return label;
    }

    /**
     * A left label / right value telemetry row with a thin rule beneath - the standard way this
     * tool displays a fact (selected path, status, counts) instead of a centered sentence.
     */
    public static JPanel dataRow(String label, String value, Color valueColor) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(BLACK);
        row.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));

        JLabel labelComponent = new JLabel(label.toUpperCase());
        labelComponent.setFont(MICRO_BOLD);
        labelComponent.setForeground(DIM_WHITE);
        labelComponent.setPreferredSize(new Dimension(180, labelComponent.getPreferredSize().height));

        JLabel valueComponent = new JLabel(value);
        valueComponent.setFont(BODY);
        valueComponent.setForeground(valueColor);

        row.add(labelComponent, BorderLayout.WEST);
        row.add(valueComponent, BorderLayout.CENTER);

        JPanel withRule = new JPanel(new BorderLayout());
        withRule.setBackground(BLACK);
        withRule.add(row, BorderLayout.CENTER);
        withRule.add(rule(LINE, 1), BorderLayout.SOUTH);
        return withRule;
    }

    /** Primary action: {@code >>> TEXT} in red. Secondary: {@code TEXT} in white. Back: {@code <<< TEXT}. */
    public static JButton button(String text, ButtonKind kind) {
        String label = switch (kind) {
            case PRIMARY -> ">>> " + text.toUpperCase();
            case BACK -> "<<< " + text.toUpperCase();
            case SECONDARY -> text.toUpperCase();
        };
        JButton button = new JButton(label);
        Color accent = kind == ButtonKind.PRIMARY ? RED : WHITE;
        button.setForeground(kind == ButtonKind.PRIMARY ? WHITE : WHITE);
        button.setBackground(kind == ButtonKind.PRIMARY ? RED : DARK_GRAY);
        button.setFont(MICRO_BOLD);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(accent, 1),
                BorderFactory.createEmptyBorder(10, 22, 10, 22)));
        button.setFocusPainted(false);
        button.setOpaque(true);
        return button;
    }

    public enum ButtonKind {
        PRIMARY, SECONDARY, BACK
    }

    /** App-wide bottom bar: a white rule, then a button row - back/secondary left, primary right. */
    public static JPanel footerBar(JComponent leftButton, JComponent rightButton) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BLACK);
        wrapper.add(rule(LINE, 1), BorderLayout.NORTH);

        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(BLACK);
        row.setBorder(BorderFactory.createEmptyBorder(16, 20, 20, 20));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setBackground(BLACK);
        if (leftButton != null) {
            left.add(leftButton);
        }

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.setBackground(BLACK);
        if (rightButton != null) {
            right.add(rightButton);
        }

        row.add(left, BorderLayout.WEST);
        row.add(right, BorderLayout.EAST);
        wrapper.add(row, BorderLayout.CENTER);
        return wrapper;
    }

    /** A bordered "compartment" box with a micro-scale title - for the log/report detail panes. */
    public static JPanel compartment(String title, JComponent content) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BLACK);
        panel.setBorder(BorderFactory.createLineBorder(LINE, 1));

        JLabel titleLabel = sectionLabel("[ " + title + " ]");
        titleLabel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(rule(LINE, 1), BorderLayout.NORTH);
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }
}
