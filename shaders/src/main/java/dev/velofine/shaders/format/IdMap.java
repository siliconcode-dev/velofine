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

import dev.velofine.core.log.VelofineLog;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Parses {@code block,item,entity}.properties - shader-pack-defined integer IDs (referenced in
 * GLSL as {@code mc_Entity}/{@code mc_Item}-style vertex attributes and the {@code entityId}
 * uniform) mapped onto real game block/item/entity identifiers.
 *
 * <p>Adapted from IrisShaders/Iris ({@code common/.../shaderpack/IdMap.java}), LGPL-3.0, with one
 * scope cut: {@code block.properties} entries with blockstate property predicates (OptiFine's
 * {@code minecraft:redstone_wire:power=0} syntax) have their predicate suffix stripped and the base
 * block ID kept, rather than building full predicate-matching (Iris's {@code BlockEntry}/
 * {@code TagEntry} machinery) - a pack relying on per-state block IDs will get the same integer ID
 * for every state of that block instead. Flagged rather than silently wrong; revisit if a
 * validation pack (BSL/Complementary) is shown to depend on state-level matching.
 */
public final class IdMap {

    private final Map<Integer, List<NamespacedId>> itemIds;
    private final Map<Integer, List<NamespacedId>> entityIds;
    private final Map<Integer, List<NamespacedId>> blockIds;

    private IdMap(Map<Integer, List<NamespacedId>> itemIds, Map<Integer, List<NamespacedId>> entityIds,
            Map<Integer, List<NamespacedId>> blockIds) {
        this.itemIds = itemIds;
        this.entityIds = entityIds;
        this.blockIds = blockIds;
    }

    public static IdMap load(Path shaderPackRoot) {
        return new IdMap(
                parseIdMap(shaderPackRoot, "item.properties", "item."),
                parseIdMap(shaderPackRoot, "entity.properties", "entity."),
                parseBlockIdMap(shaderPackRoot));
    }

    private static Map<Integer, List<NamespacedId>> parseIdMap(Path shaderPackRoot, String fileName, String keyPrefix) {
        Properties properties = readProperties(shaderPackRoot, fileName);
        if (properties == null) {
            return Collections.emptyMap();
        }

        Map<Integer, List<NamespacedId>> result = new HashMap<>();
        properties.forEach((keyObject, valueObject) -> {
            String key = (String) keyObject;
            if (!key.startsWith(keyPrefix)) {
                return;
            }

            int id = parseIntSuffix(key, keyPrefix, fileName);
            if (id == Integer.MIN_VALUE) {
                return;
            }

            List<NamespacedId> ids = new ArrayList<>();
            for (String token : ((String) valueObject).trim().split("\\s+")) {
                if (token.isEmpty()) {
                    continue;
                }
                if (token.contains("=")) {
                    VelofineLog.warn("Shaders", "State properties are not supported in " + fileName + ": " + token);
                    continue;
                }
                ids.add(NamespacedId.parse(token));
            }
            result.put(id, Collections.unmodifiableList(ids));
        });
        return Collections.unmodifiableMap(result);
    }

    private static Map<Integer, List<NamespacedId>> parseBlockIdMap(Path shaderPackRoot) {
        Properties properties = readProperties(shaderPackRoot, "block.properties");
        if (properties == null) {
            return Collections.emptyMap();
        }

        Map<Integer, List<NamespacedId>> result = new HashMap<>();
        properties.forEach((keyObject, valueObject) -> {
            String key = (String) keyObject;
            if (!key.startsWith("block.")) {
                return;
            }

            int id = parseIntSuffix(key, "block.", "block.properties");
            if (id == Integer.MIN_VALUE) {
                return;
            }

            List<NamespacedId> ids = new ArrayList<>();
            for (String token : ((String) valueObject).trim().split("\\s+")) {
                if (!token.isEmpty()) {
                    ids.add(NamespacedId.parse(stripBlockStatePredicate(token)));
                }
            }
            result.put(id, Collections.unmodifiableList(ids));
        });
        return Collections.unmodifiableMap(result);
    }

    /**
     * OptiFine's block.properties entries are {@code [namespace:]path[:property=value[,...]]}.
     * This strips the trailing state predicate, keeping just {@code [namespace:]path} - see this
     * class's javadoc for why full predicate matching isn't built here.
     */
    private static String stripBlockStatePredicate(String token) {
        String[] parts = token.split(":", 3);
        if (parts.length == 3) {
            return parts[0] + ":" + parts[1];
        }
        return token;
    }

    private static int parseIntSuffix(String key, String prefix, String fileName) {
        try {
            return Integer.parseInt(key.substring(prefix.length()));
        } catch (NumberFormatException e) {
            VelofineLog.warn("Shaders", "Invalid key in " + fileName + ": " + key);
            return Integer.MIN_VALUE;
        }
    }

    private static Properties readProperties(Path shaderPackRoot, String fileName) {
        Path file = shaderPackRoot.resolve(fileName);
        try {
            // ID maps are conventionally ISO-8859-1 encoded, matching OptiFine/Iris.
            String contents = Files.readString(file, StandardCharsets.ISO_8859_1);
            Properties properties = new OrderBackedProperties();
            properties.load(new StringReader(contents));
            return properties;
        } catch (NoSuchFileException e) {
            return null;
        } catch (IOException e) {
            VelofineLog.warn("Shaders", "Failed to read " + fileName + ": " + e);
            return null;
        }
    }

    public Map<Integer, List<NamespacedId>> getItemIds() {
        return itemIds;
    }

    public Map<Integer, List<NamespacedId>> getEntityIds() {
        return entityIds;
    }

    public Map<Integer, List<NamespacedId>> getBlockIds() {
        return blockIds;
    }
}
