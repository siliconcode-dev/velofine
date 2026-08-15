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

package dev.velofine.core.config;

/**
 * A user override for something Velofine would otherwise decide from hardware detection.
 *
 * <p>{@link #AUTO} is the default for every LegacySupport fix and means "keep letting
 * {@code FixProfileRules} decide". That matters: without it, the only way to express "I don't want
 * to override this" would be to guess the detector's answer and hard-code it, which would silently
 * go stale the moment detection improved or the user changed hardware.
 */
public enum Tri {
    AUTO,
    ON,
    OFF;

    /** Resolves this override against what hardware detection decided on its own. */
    public boolean resolve(boolean detected) {
        return switch (this) {
            case AUTO -> detected;
            case ON -> true;
            case OFF -> false;
        };
    }

    public Tri next() {
        return switch (this) {
            case AUTO -> ON;
            case ON -> OFF;
            case OFF -> AUTO;
        };
    }
}
