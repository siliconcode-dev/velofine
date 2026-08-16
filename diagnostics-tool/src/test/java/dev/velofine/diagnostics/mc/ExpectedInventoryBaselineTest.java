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

import dev.velofine.diagnostics.model.InventoryBaselineCheck;
import dev.velofine.diagnostics.model.ShaderInventoryEntry;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ExpectedInventoryBaselineTest {

    @Test
    void unavailableForAnUnknownMcVersion() {
        InventoryBaselineCheck check = ExpectedInventoryBaseline.check("1.19.4", List.of());
        assertFalse(check.baselineAvailable());
    }

    @Test
    void realTerrainAndEndPortalMatchTheBundledBaselineWithNoDiff() {
        // A small, real, always-present subset - a full 34-entry fixture would just duplicate the
        // bundled resource; this exercises the diff logic without needing an exact full match.
        List<ShaderInventoryEntry> discovered = allBundledNames();

        InventoryBaselineCheck check = ExpectedInventoryBaseline.check("26.2", discovered);

        assertTrue(check.baselineAvailable());
        assertTrue(check.unexpectedShaders().isEmpty());
        assertTrue(check.missingExpectedShaders().isEmpty());
    }

    @Test
    void flagsAnUnexpectedShaderNotInTheBaseline() {
        List<ShaderInventoryEntry> discovered = new java.util.ArrayList<>(allBundledNames());
        discovered.add(new ShaderInventoryEntry("mystery_shader", false, true, true));

        InventoryBaselineCheck check = ExpectedInventoryBaseline.check("26.2", discovered);

        assertEquals(List.of("mystery_shader"), check.unexpectedShaders());
        assertTrue(check.missingExpectedShaders().isEmpty());
    }

    @Test
    void flagsAMissingExpectedShader() {
        List<ShaderInventoryEntry> discovered = new java.util.ArrayList<>(allBundledNames());
        discovered.removeIf(e -> e.name().equals("terrain"));

        InventoryBaselineCheck check = ExpectedInventoryBaseline.check("26.2", discovered);

        assertEquals(List.of("terrain"), check.missingExpectedShaders());
    }

    /** The exact 34 real names bundled in {@code expected-shader-inventory.json}. */
    private List<ShaderInventoryEntry> allBundledNames() {
        return List.of(
                "animate_sprite", "animate_sprite_blit", "animate_sprite_interpolate", "blit_screen", "block",
                "debug_point", "entity", "glint", "gui", "item", "lightmap", "panorama", "particle", "position",
                "position_color", "position_tex", "position_tex_color", "rendertype_beacon_beam", "rendertype_clouds",
                "rendertype_crumbling", "rendertype_end_portal", "rendertype_entity_shadow", "rendertype_leash",
                "rendertype_lightning", "rendertype_lines", "rendertype_outline", "rendertype_water_mask",
                "rendertype_world_border", "screenquad", "sky", "stars", "terrain", "text", "text_background")
                .stream()
                .map(name -> new ShaderInventoryEntry(name, false, true, true))
                .toList();
    }
}
