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

package dev.velofine.shaders.pipeline;

import com.mojang.blaze3d.shaders.ShaderType;
import dev.velofine.core.log.VelofineLog;
import dev.velofine.shaders.format.ShaderPack;
import dev.velofine.shaders.format.ShaderPackPreprocessor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;

/**
 * Reads a shader pack's {@code shaders/<program>.vsh}/{@code .fsh} GLSL source. Source text goes
 * through {@link ShaderPackPreprocessor} with the pack's currently-enabled boolean options before
 * being handed back, but does <b>not</b> yet inject Velofine's own uniforms/attributes - that's
 * tracked separately; this class only closes the "read the right file" half.
 */
public final class ShaderProgramLoader {

    private ShaderProgramLoader() {
    }

    /**
     * For {@code gbuffers_*} programs: walks {@link GbuffersFallbackChains} until one actually
     * exists in the pack - the same generic-fallback behavior OptiFine/Iris use so packs that only
     * ship a handful of program files still cover every draw category.
     *
     * @return the resolved GLSL source, or {@code null} if no program in the fallback chain exists.
     */
    public static String load(ShaderPack pack, String gbuffersProgram, ShaderType type) {
        for (String candidate : GbuffersFallbackChains.resolutionOrder(gbuffersProgram)) {
            String source = loadDirect(pack, candidate, type);
            if (source != null) {
                return source;
            }
        }
        return null;
    }

    /**
     * For {@code composite}/{@code deferred}/{@code final}-family programs: no fallback chain -
     * unlike gbuffers programs, a missing composite/deferred stage just means "skip this pass
     * entirely", not "use a more generic one" (OptiFine has no such fallback for these).
     *
     * @return the GLSL source, or {@code null} if this exact program name doesn't exist in the pack.
     */
    public static String loadDirect(ShaderPack pack, String programName, ShaderType type) {
        String extension = type == ShaderType.VERTEX ? ".vsh" : ".fsh";
        Path file = pack.getRoot().resolve("shaders").resolve(programName + extension);

        try {
            String raw = Files.readString(file, StandardCharsets.UTF_8);
            return ShaderPackPreprocessor.preprocessProperties(
                    raw, ShaderPackPreprocessor.flagMacros(enabledOptionNames(pack)));
        } catch (NoSuchFileException e) {
            return null;
        } catch (IOException e) {
            VelofineLog.warn("Shaders", "Failed to read " + file + ": " + e);
            return null;
        }
    }

    private static List<String> enabledOptionNames(ShaderPack pack) {
        // Real option-value resolution (user overrides layered on pack defaults) lands with the
        // options UI (7.7) - until then every discovered boolean option stays at its pack default,
        // and pack.getOptionSet() is empty regardless (see ShaderPack's class javadoc), so this
        // currently always returns an empty list. Kept as its own method so 7.7 has one obvious
        // seam to wire real resolved values into, rather than inlining List.of() at both call sites
        // in this class.
        return List.of();
    }
}
