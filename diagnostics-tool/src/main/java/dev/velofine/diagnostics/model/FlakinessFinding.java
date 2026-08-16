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

package dev.velofine.diagnostics.model;

import java.util.List;

/**
 * One shader whose compile-success outcome was inconsistent across a {@code pipeline.RepeatRunner}
 * repeat-N-times run - flags flaky/non-deterministic driver behavior, a real possibility on old,
 * buggy drivers, that a single run would silently hide.
 */
public record FlakinessFinding(String shaderName, String stage, String defineVariant, List<Boolean> outcomesPerRun) {

    public boolean consistent() {
        return outcomesPerRun.stream().distinct().count() <= 1;
    }
}
