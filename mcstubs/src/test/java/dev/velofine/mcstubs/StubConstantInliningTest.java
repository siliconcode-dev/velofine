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

package dev.velofine.mcstubs;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the one mcstubs rule that cannot be caught by any ordinary unit test: a {@code static final}
 * primitive or {@code String} <i>with an initializer</i> is a JLS 4.12.4 constant variable, so javac
 * <b>inlines its value into consumer bytecode</b> and the real Minecraft class is never consulted at
 * runtime.
 *
 * <p>This is not hypothetical. v1.7-Beta shipped {@code GpuTexture.USAGE_COPY_DST = 0} /
 * {@code USAGE_TEXTURE_BINDING = 0} as placeholders; the real values are 1 and 4. Every consumer
 * compiled {@code USAGE_TEXTURE_BINDING | USAGE_COPY_DST} down to a literal {@code 0}, and the
 * animated-texture fix threw {@code IllegalStateException: Color texture must have USAGE_COPY_DST to
 * be a destination for a write} on every invocation on the tester's real hardware - 63 failures per
 * launch - while every JVM-side test stayed green, because nothing in a normal test run ever loads
 * the real class.
 *
 * <p>Scanning source text is deliberate: the failure is invisible in the stub module's own compiled
 * output (the constant is only wrong once <em>inlined into a consumer</em>), so there is nothing on
 * this module's classpath to reflect over. The fix in every case is to declare the field non-final
 * and uninitialized, forcing a {@code getstatic} against the real class - see
 * {@code mcstubs/build.gradle.kts}'s header and {@code GpuTexture}'s javadoc.
 */
final class StubConstantInliningTest {

    /**
     * {@code static final} + a primitive/String type + an {@code =} initializer, on one line. Object-
     * typed constants (e.g. {@code RenderPipelines}' {@code static final RenderPipeline} fields) are
     * intentionally not matched - they are not constant variables and already compile to
     * {@code getstatic}.
     */
    private static final Pattern INLINED_CONSTANT = Pattern.compile(
            "static\\s+final\\s+(boolean|byte|char|short|int|long|float|double|String)\\s+(\\w+)\\s*=");

    @Test
    void noStubDeclaresAnInlinableCompileTimeConstant() throws IOException {
        Path sourceRoot = sourceRoot();
        assertTrue(Files.isDirectory(sourceRoot), "mcstubs source root not found: " + sourceRoot);

        List<String> offenders = new ArrayList<>();
        try (Stream<Path> files = Files.walk(sourceRoot)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file, StandardCharsets.UTF_8);
                Matcher matcher = INLINED_CONSTANT.matcher(source);
                while (matcher.find()) {
                    offenders.add(sourceRoot.relativize(file) + ": " + matcher.group(1) + " " + matcher.group(2));
                }
            }
        }

        assertTrue(offenders.isEmpty(),
                "These mcstubs fields are compile-time constants and would be INLINED into consumer "
                        + "bytecode, so the real Minecraft value is never read at runtime. Declare them "
                        + "non-final and uninitialized instead (see GpuTexture's javadoc for the real "
                        + "bug this caused in v1.7-Beta):\n  " + String.join("\n  ", offenders));
    }

    /** Overridable so the check still works if a future build runs tests from a different directory. */
    private static Path sourceRoot() {
        String override = System.getProperty("velofine.test.stubSourceRoot");
        return override != null ? Path.of(override) : Path.of("src", "main", "java");
    }
}
