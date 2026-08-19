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

package dev.velofine.core.shader;

import com.mojang.blaze3d.shaders.ShaderType;
import dev.velofine.core.log.VelofineLog;
import net.minecraft.resources.Identifier;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Opt-in diagnostic export of the exact GLSL Velofine hands the driver, written only when
 * {@code -Dvelofine.shader.dumpPatched=<directory>} is set. Off by default and never referenced by
 * any shipped code path other than the one guarded call in {@link ShaderSourceInterceptors}.
 *
 * <p>This closes the tester loop the v1.5 Masterdoc's Phase 4 workflow describes. Velofine patches
 * shader text at runtime rather than shipping replacement {@code .fsh} assets (deliberately - it
 * adapts to any Minecraft build and keeps Mojang's proprietary shader source out of this repo), but
 * that means a tester has no artifact to inspect or compile-check independently. With this property
 * set, the patched source lands on disk in a form {@code diagnostic.exe}'s existing CANDIDATE mode
 * can compile against the real driver <em>before</em> anyone has to trust the live rendering path.
 *
 * <p>Never throws: a diagnostic aid must not be able to break a launch, so any I/O failure is logged
 * once per shader and otherwise ignored.
 */
public final class ShaderSourceDump {

    private static final String DUMP_DIR_PROPERTY = "velofine.shader.dumpPatched";

    private ShaderSourceDump() {
    }

    static boolean isEnabled() {
        String dir = System.getProperty(DUMP_DIR_PROPERTY);
        return dir != null && !dir.isBlank();
    }

    /**
     * @param stage short tag distinguishing the pipeline stage the text was captured at (e.g.
     *              {@code "postdefines"}), so a before/after pair for one shader doesn't collide.
     */
    static void dump(Identifier id, ShaderType type, String stage, String source) {
        String dir = System.getProperty(DUMP_DIR_PROPERTY);
        if (dir == null || dir.isBlank() || source == null) {
            return;
        }
        try {
            Path target = Path.of(dir);
            Files.createDirectories(target);
            Files.writeString(target.resolve(fileName(id, type, stage)), source, StandardCharsets.UTF_8);
        } catch (Exception e) {
            VelofineLog.warn("core", "Could not write patched-shader dump for " + id + ": " + e);
        }
    }

    private static String fileName(Identifier id, ShaderType type, String stage) {
        String base = id == null ? "unknown" : (id.getNamespace() + "_" + id.getPath());
        String safe = base.replaceAll("[^A-Za-z0-9._-]", "_");
        String extension = type == ShaderType.VERTEX ? "vsh" : "fsh";
        return safe + "." + stage.toLowerCase(Locale.ROOT) + "." + extension;
    }
}
