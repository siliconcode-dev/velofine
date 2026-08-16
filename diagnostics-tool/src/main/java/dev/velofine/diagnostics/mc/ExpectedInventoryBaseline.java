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

package dev.velofine.diagnostics.mc;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.velofine.diagnostics.model.InventoryBaselineCheck;
import dev.velofine.diagnostics.model.ShaderInventoryEntry;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Compares a run's discovered shader inventory against {@code expected-shader-inventory.json}, a
 * bundled resource listing the real 34 shader base names confirmed by extracting the actual 26.2
 * client jar. A mismatch (fewer/more/different names) means the tested jar isn't what this tool
 * expects - a corrupted download, wrong MC version, or a resource-pack-modified jar - and is worth
 * flagging before the rest of a run's results get interpreted as meaningful.
 */
public final class ExpectedInventoryBaseline {

    private static final String RESOURCE_PATH = "/expected-shader-inventory.json";
    private static final Gson GSON = new Gson();

    private ExpectedInventoryBaseline() {
    }

    public static InventoryBaselineCheck check(String mcVersionId, List<ShaderInventoryEntry> discovered) {
        JsonObject baseline = loadBaseline();
        if (baseline == null || !mcVersionId.equals(baseline.get("mcVersionId").getAsString())) {
            return InventoryBaselineCheck.unavailable();
        }

        Set<String> expected = new HashSet<>();
        baseline.getAsJsonArray("shaderNames").forEach(e -> expected.add(e.getAsString()));

        Set<String> found = new HashSet<>();
        discovered.forEach(e -> found.add(e.name()));

        List<String> unexpected = new ArrayList<>(found);
        unexpected.removeAll(expected);
        List<String> missing = new ArrayList<>(expected);
        missing.removeAll(found);

        return new InventoryBaselineCheck(true, unexpected, missing);
    }

    private static JsonObject loadBaseline() {
        try (InputStream in = ExpectedInventoryBaseline.class.getResourceAsStream(RESOURCE_PATH)) {
            if (in == null) {
                return null;
            }
            return GSON.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), JsonObject.class);
        } catch (IOException e) {
            return null;
        }
    }
}
