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
import org.anarres.cpp.LexerException;
import org.anarres.cpp.Preprocessor;
import org.anarres.cpp.StringLexerSource;
import org.anarres.cpp.Token;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Runs a shaderpack's {@code shaders.properties} (or a GLSL source file) through a C-style
 * preprocessor so {@code #ifdef}/{@code #if}/{@code #define} directives - driven by the pack's own
 * currently-active option values - resolve before line-by-line parsing sees the result. Uses
 * {@code org.anarres:jcpp} (Apache-2.0), the same preprocessing library Iris itself depends on -
 * deliberately NOT Iris's other GLSL-processing dependency, {@code io.github.douira:glsl-transformer}
 * (AGPL-3.0), which this project does not use (see {@code shaders/build.gradle.kts} and CLAUDE.md's
 * Phase 7 license-resolution notes). Adapted from IrisShaders/Iris
 * ({@code common/.../shaderpack/preprocessor/PropertiesPreprocessor.java}), LGPL-3.0 - simplified to
 * take a flat macro map instead of Iris's {@code ShaderPackOptions}, since this module does not yet
 * build the {@code #include}-dependency graph Iris uses to discover options across files (see
 * {@link ShaderPack}'s class javadoc for the tracked follow-up).
 */
public final class ShaderPackPreprocessor {

    /** {@code .properties} files use {@code #} for comments; jcpp treats it as a directive marker. */
    private static final Pattern PROPERTIES_COMMENT_LINE = Pattern.compile("^\\s*#(?!(?:if|ifdef|ifndef|else|elif|endif|define|undef)\\b).*$");

    private ShaderPackPreprocessor() {
    }

    /**
     * @param macros boolean-flag macros (mapped from the pack's currently-enabled boolean options)
     *               and value macros (string/enum options), name to replacement text (empty string
     *               for a flag-only macro).
     */
    public static String preprocessProperties(String source, Map<String, String> macros) {
        // String.split("\R") silently drops the trailing line terminator (Java's default
        // zero-limit split trims trailing empty strings), so joining back with "\n" always lost
        // the file's final newline - jcpp's StringLexerSource then raises "No newline before end
        // of file" as a LexerException, which the catch below (correctly) treats as a hard failure
        // and falls back to the raw, un-preprocessed source. That meant every #ifdef/#define in
        // *every* real shaders.properties file silently never resolved - caught by
        // ShaderPackPreprocessorTest/ShaderPropertiesTest, not by inspection. Appending the
        // newline back explicitly is the fix.
        String sanitized = Arrays.stream(source.split("\\R"))
                .map(line -> PROPERTIES_COMMENT_LINE.matcher(line).matches() ? "" : line)
                .collect(Collectors.joining("\n", "", "\n"));

        return process(sanitized, macros);
    }

    private static String process(String source, Map<String, String> macros) {
        try (Preprocessor pp = new Preprocessor()) {
            pp.addInput(new StringLexerSource(source, true));

            for (Map.Entry<String, String> macro : macros.entrySet()) {
                if (macro.getValue() == null || macro.getValue().isEmpty()) {
                    pp.addMacro(macro.getKey());
                } else {
                    pp.addMacro(macro.getKey(), macro.getValue());
                }
            }

            StringBuilder out = new StringBuilder();
            for (;;) {
                Token tok = pp.token();
                if (tok == null || tok.getType() == Token.EOF) {
                    break;
                }
                out.append(tok.getText());
            }
            return out.toString();
        } catch (IOException | LexerException e) {
            VelofineLog.warn("Shaders", "Preprocessing failed, using source as-is: " + e);
            return source;
        }
    }

    /** Flag macros currently enabled, ready for {@link #preprocessProperties}. */
    public static Map<String, String> flagMacros(Iterable<String> enabledBooleanOptions) {
        Map<String, String> macros = new HashMap<>();
        for (String name : enabledBooleanOptions) {
            macros.put(name, "");
        }
        return macros;
    }
}
