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
import dev.velofine.shaders.format.option.BooleanOption;
import dev.velofine.shaders.format.option.OptionDiscovery;
import dev.velofine.shaders.format.option.OptionSet;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A fully parsed shaderpack: its {@link ShaderProperties}, {@link IdMap}, and (once discovered -
 * see below) its {@link OptionSet}, ready for the pipeline stages (7.3+) to consume.
 *
 * <p>Tracked follow-up, not yet built: real option discovery. Iris scans every GLSL file in the
 * pack for {@code #define NAME value // [allowed values]}-annotated lines across a full
 * {@code #include}-dependency graph (its {@code ShaderPackOptions}/{@code IncludeGraph}/
 * {@code OptionAnnotatedSource} classes) so an option defined in a commonly-included file is
 * discovered once and applied everywhere it's referenced. {@link OptionDiscovery} (7.7) scans each
 * program file independently instead, as a first cut - see its own class javadoc for the resulting
 * scope cut and when to revisit it.
 */
public final class ShaderPack {

    private final String name;
    private final Path root;
    private final ShaderProperties properties;
    private final IdMap idMap;
    private final OptionSet optionSet;

    private ShaderPack(String name, Path root, ShaderProperties properties, IdMap idMap, OptionSet optionSet) {
        this.name = name;
        this.root = root;
        this.properties = properties;
        this.idMap = idMap;
        this.optionSet = optionSet;
    }

    /**
     * @param userOptionOverrides the user's persisted {@code optionName -> value} choices for this
     *                            pack (from {@code VelofineConfig.ShaderSection.packOptions}) -
     *                            resolved against the pack's own option defaults before {@code
     *                            shaders.properties} is preprocessed, so an overridden option
     *                            actually affects preprocessing rather than only being visible to
     *                            the options screen. Options are discovered first specifically to
     *                            make this resolution possible - {@code shaders.properties}'s own
     *                            preprocessing needs the resolved boolean set, but which booleans
     *                            *exist* is only known after scanning the pack's GLSL.
     */
    public static ShaderPack load(String name, Path root, Map<String, String> userOptionOverrides) {
        OptionSet optionSet = OptionDiscovery.discover(root.resolve("shaders"));
        List<String> enabledBooleanOptions = resolveEnabledBooleanOptions(optionSet, userOptionOverrides);

        ShaderProperties properties = readShaderProperties(root, enabledBooleanOptions);
        IdMap idMap = IdMap.load(root);
        return new ShaderPack(name, root, properties, idMap, optionSet);
    }

    private static List<String> resolveEnabledBooleanOptions(OptionSet optionSet, Map<String, String> overrides) {
        List<String> enabled = new ArrayList<>();
        for (BooleanOption option : optionSet.getBooleanOptions().values()) {
            String override = overrides.get(option.getName());
            boolean value = override != null ? Boolean.parseBoolean(override) : option.getDefaultValue();
            if (value) {
                enabled.add(option.getName());
            }
        }
        return enabled;
    }

    /** The pack's root directory (or mounted zip root) - {@code shaders/} is a subfolder of this. */
    public Path getRoot() {
        return root;
    }

    private static ShaderProperties readShaderProperties(Path root, List<String> enabledBooleanOptions) {
        Path propertiesFile = root.resolve("shaders").resolve("shaders.properties");
        try {
            String contents = Files.readString(propertiesFile, StandardCharsets.ISO_8859_1);
            return ShaderProperties.parse(contents, enabledBooleanOptions);
        } catch (NoSuchFileException e) {
            // A pack with no shaders.properties is valid - every directive simply falls back to
            // its default, matching OptiFine/Iris behavior.
            return ShaderProperties.empty();
        } catch (IOException e) {
            VelofineLog.warn("Shaders", "Failed to read shaders.properties: " + e);
            return ShaderProperties.empty();
        }
    }

    public String getName() {
        return name;
    }

    public ShaderProperties getProperties() {
        return properties;
    }

    public IdMap getIdMap() {
        return idMap;
    }

    public OptionSet getOptionSet() {
        return optionSet;
    }
}
