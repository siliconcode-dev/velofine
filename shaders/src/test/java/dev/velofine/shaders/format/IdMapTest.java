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

package dev.velofine.shaders.format;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class IdMapTest {

    @Test
    void loadsItemAndEntityAndBlockPropertiesFromARealPackDirectory(@TempDir Path packRoot) throws IOException {
        Files.writeString(packRoot.resolve("item.properties"), "item.1=minecraft:diamond_sword\n", StandardCharsets.ISO_8859_1);
        Files.writeString(packRoot.resolve("entity.properties"), "entity.10=minecraft:creeper minecraft:skeleton\n", StandardCharsets.ISO_8859_1);
        Files.writeString(packRoot.resolve("block.properties"), "block.100=minecraft:diamond_block\n", StandardCharsets.ISO_8859_1);

        IdMap idMap = IdMap.load(packRoot);

        assertEquals(List.of(new NamespacedId("minecraft", "diamond_sword")), idMap.getItemIds().get(1));
        assertEquals(List.of(new NamespacedId("minecraft", "creeper"), new NamespacedId("minecraft", "skeleton")),
                idMap.getEntityIds().get(10));
        assertEquals(List.of(new NamespacedId("minecraft", "diamond_block")), idMap.getBlockIds().get(100));
    }

    @Test
    void missingPropertyFilesResultInEmptyMapsRatherThanThrowing(@TempDir Path packRoot) {
        IdMap idMap = IdMap.load(packRoot);

        assertEquals(0, idMap.getItemIds().size());
        assertEquals(0, idMap.getEntityIds().size());
        assertEquals(0, idMap.getBlockIds().size());
    }

    @Test
    void blockStatePredicatesAreStrippedToTheBaseBlockId(@TempDir Path packRoot) throws IOException {
        // OptiFine's [namespace:]path[:property=value] syntax - see IdMap's class javadoc on why
        // the predicate suffix is dropped rather than kept for per-state matching.
        Files.writeString(packRoot.resolve("block.properties"),
                "block.50=minecraft:redstone_wire:power=0\n", StandardCharsets.ISO_8859_1);

        IdMap idMap = IdMap.load(packRoot);

        assertEquals(List.of(new NamespacedId("minecraft", "redstone_wire")), idMap.getBlockIds().get(50));
    }

    @Test
    void anUnnamespacedTokenDefaultsToMinecraft(@TempDir Path packRoot) throws IOException {
        Files.writeString(packRoot.resolve("item.properties"), "item.5=diamond_sword\n", StandardCharsets.ISO_8859_1);

        IdMap idMap = IdMap.load(packRoot);

        assertEquals(List.of(new NamespacedId("minecraft", "diamond_sword")), idMap.getItemIds().get(5));
    }

    @Test
    void keysNotUnderAnyRecognizedPrefixAreIgnored(@TempDir Path packRoot) throws IOException {
        Files.writeString(packRoot.resolve("item.properties"), "someUnrelatedKey=value\n", StandardCharsets.ISO_8859_1);

        assertTrue(IdMap.load(packRoot).getItemIds().isEmpty());
    }
}
