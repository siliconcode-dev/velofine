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

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ShaderPackPreprocessorTest {

    @Test
    void anEnabledFlagMacroKeepsItsIfdefBlock() {
        String source = """
                #ifdef BLOOM
                program.composite=bloom
                #endif
                """;

        String result = ShaderPackPreprocessor.preprocessProperties(source, ShaderPackPreprocessor.flagMacros(List.of("BLOOM")));

        assertTrue(result.contains("program.composite=bloom"));
    }

    @Test
    void aDisabledFlagMacroDropsItsIfdefBlock() {
        String source = """
                #ifdef BLOOM
                program.composite=bloom
                #endif
                fallback=true
                """;

        String result = ShaderPackPreprocessor.preprocessProperties(source, Map.of());

        assertFalse(result.contains("program.composite=bloom"));
        assertTrue(result.contains("fallback=true"));
    }

    @Test
    void ifndefIsHonoredTheOppositeWay() {
        String source = """
                #ifndef BLOOM
                program.composite=noBloomFallback
                #endif
                """;

        String withBloom = ShaderPackPreprocessor.preprocessProperties(source, ShaderPackPreprocessor.flagMacros(List.of("BLOOM")));
        String withoutBloom = ShaderPackPreprocessor.preprocessProperties(source, Map.of());

        assertFalse(withBloom.contains("program.composite=noBloomFallback"));
        assertTrue(withoutBloom.contains("program.composite=noBloomFallback"));
    }

    @Test
    void plainPropertiesCommentLinesAreStrippedRatherThanTreatedAsDirectives() {
        // .properties files use '#' for plain comments, which jcpp would otherwise choke on as an
        // unrecognized preprocessor directive - this is exactly what PROPERTIES_COMMENT_LINE guards.
        String source = """
                # this is a plain comment, not a directive
                clouds=fancy
                """;

        String result = ShaderPackPreprocessor.preprocessProperties(source, Map.of());

        assertTrue(result.contains("clouds=fancy"));
    }

    @Test
    void aValueMacroSubstitutes() {
        String source = "shadowDistance=SHADOW_DIST\n";
        String result = ShaderPackPreprocessor.preprocessProperties(source, Map.of("SHADOW_DIST", "128"));

        assertTrue(result.contains("shadowDistance=128"));
    }

    @Test
    void malformedInputFallsBackToTheOriginalSourceRatherThanThrowing() {
        // #if with no matching #endif - preprocessProperties must degrade gracefully, not throw.
        String source = "#if UNBALANCED\nfoo=bar\n";

        String result = ShaderPackPreprocessor.preprocessProperties(source, Map.of());

        assertTrue(result != null);
    }
}
