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

import dev.velofine.diagnostics.model.ShaderInventoryEntry;
import dev.velofine.diagnostics.shader.DefineVariants;
import dev.velofine.diagnostics.shader.ImportResolutionException;
import dev.velofine.diagnostics.shader.MojImportResolver;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Opens a real Minecraft client jar and reads its {@code assets/minecraft/shaders/core/} and
 * {@code assets/<namespace>/shaders/include/} entries - both the shader inventory this tool
 * discovers generically (not a hardcoded 4-shader list, see {@code DefineVariants}'s javadoc for
 * why), and the {@code #moj_import} include resolution backing {@link MojImportResolver}.
 */
public final class ShaderExtractor implements AutoCloseable, MojImportResolver.ImportSource {

    private static final String CORE_DIR = "assets/minecraft/shaders/core/";

    private final FileSystem jarFs;

    private ShaderExtractor(FileSystem jarFs) {
        this.jarFs = jarFs;
    }

    public static ShaderExtractor open(Path clientJar) throws IOException {
        return new ShaderExtractor(FileSystems.newFileSystem(clientJar, (ClassLoader) null));
    }

    /**
     * Lists every {@code .fsh}/{@code .vsh} pair under {@code shaders/core/} in the real jar,
     * flagged shaders (see {@link DefineVariants#isFlagged}) sorted first, then alphabetical.
     */
    public List<ShaderInventoryEntry> discoverCoreShaders() throws IOException {
        Path coreDir = jarFs.getPath(CORE_DIR);
        Map<String, boolean[]> found = new TreeMap<>();
        if (Files.isDirectory(coreDir)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(coreDir)) {
                for (Path entry : stream) {
                    String fileName = entry.getFileName().toString();
                    boolean isVertex = fileName.endsWith(".vsh");
                    boolean isFragment = fileName.endsWith(".fsh");
                    if (!isVertex && !isFragment) {
                        continue;
                    }
                    String baseName = fileName.substring(0, fileName.length() - 4);
                    boolean[] flags = found.computeIfAbsent(baseName, k -> new boolean[2]);
                    if (isVertex) {
                        flags[0] = true;
                    } else {
                        flags[1] = true;
                    }
                }
            }
        }

        List<ShaderInventoryEntry> result = new ArrayList<>();
        for (Map.Entry<String, boolean[]> entry : found.entrySet()) {
            String name = entry.getKey();
            result.add(new ShaderInventoryEntry(name, DefineVariants.isFlagged(name), entry.getValue()[0], entry.getValue()[1]));
        }
        result.sort(Comparator
                .comparing((ShaderInventoryEntry e) -> !e.flagged())
                .thenComparing(ShaderInventoryEntry::name));
        return result;
    }

    /** Reads {@code shaders/core/<name>.<extension>} ({@code "vsh"} or {@code "fsh"}) if present. */
    public Optional<String> readCoreShaderSource(String name, String extension) throws IOException {
        Path path = jarFs.getPath(CORE_DIR + name + "." + extension);
        if (!Files.isRegularFile(path)) {
            return Optional.empty();
        }
        return Optional.of(Files.readString(path, StandardCharsets.UTF_8));
    }

    @Override
    public String readAngleImport(String namespace, String path) throws ImportResolutionException {
        Path resolved = jarFs.getPath("assets/" + namespace + "/shaders/include/" + path);
        return readOrThrow(resolved);
    }

    @Override
    public String readQuotedImport(String path) throws ImportResolutionException {
        Path resolved = jarFs.getPath(CORE_DIR + path);
        return readOrThrow(resolved);
    }

    private String readOrThrow(Path path) throws ImportResolutionException {
        if (!Files.isRegularFile(path)) {
            throw new ImportResolutionException("not found in client jar: " + path);
        }
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ImportResolutionException("read failed for " + path + ": " + e.getMessage());
        }
    }

    @Override
    public void close() throws IOException {
        jarFs.close();
    }
}
