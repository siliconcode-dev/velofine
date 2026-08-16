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

package dev.velofine.shaders.discovery;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * A resolved, opened shaderpack, presenting a uniform {@link Path} to its root regardless of
 * whether it's a plain directory or a {@code .zip} - callers ({@link
 * dev.velofine.shaders.format.IdMap}, {@code ShaderProperties}, and later the GLSL program loader)
 * never need to know which. Zip packs are mounted via the JDK's built-in zip filesystem provider
 * ({@code jar:} scheme), so {@link #close()} must be called once the pack is no longer needed (a
 * directory pack's {@link #close()} is a no-op).
 */
public final class ShaderpackHandle implements Closeable {

    private final Path root;
    private final FileSystem zipFileSystem;

    private ShaderpackHandle(Path root, FileSystem zipFileSystem) {
        this.root = root;
        this.zipFileSystem = zipFileSystem;
    }

    public static ShaderpackHandle open(Path shaderpacksDirectory, String packName) throws IOException {
        Path packPath = shaderpacksDirectory.resolve(packName);

        if (packName.toLowerCase(Locale.ROOT).endsWith(".zip")) {
            FileSystem fs = FileSystems.newFileSystem(packPath);
            Path zipRoot = resolveZipContentRoot(fs);
            return new ShaderpackHandle(zipRoot, fs);
        }

        return new ShaderpackHandle(packPath, null);
    }

    /**
     * Some packs wrap their {@code shaders/}/{@code shaders.properties} inside a single top-level
     * folder within the zip rather than at the archive root (a common author mistake OptiFine/Iris
     * both tolerate) - if the zip root has exactly one directory entry and no {@code shaders}
     * folder of its own, descend into it.
     */
    private static Path resolveZipContentRoot(FileSystem fs) throws IOException {
        Path root = fs.getPath("/");
        if (Files.exists(root.resolve("shaders"))) {
            return root;
        }

        try (Stream<Path> entries = Files.list(root)) {
            List<Path> topLevelDirs = entries.filter(Files::isDirectory).toList();
            if (topLevelDirs.size() == 1 && Files.exists(topLevelDirs.get(0).resolve("shaders"))) {
                return topLevelDirs.get(0);
            }
        }

        return root;
    }

    public Path getRoot() {
        return root;
    }

    @Override
    public void close() throws IOException {
        if (zipFileSystem != null) {
            zipFileSystem.close();
        }
    }
}
