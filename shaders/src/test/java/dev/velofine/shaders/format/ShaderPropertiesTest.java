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

import dev.velofine.shaders.format.blending.AlphaTest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ShaderPropertiesTest {

    @Test
    void programEnableDirectivesAreCaptured() {
        ShaderProperties props = parse("""
                program.composite.enabled=BLOOM
                program.deferred1.enabled=true
                """);

        assertEquals("BLOOM", props.getConditionallyEnabledPrograms().get("composite"));
        assertEquals("true", props.getConditionallyEnabledPrograms().get("deferred1"));
    }

    @Test
    void cloudSettingParsesAllThreeRealValues() {
        assertEquals(CloudSetting.OFF, parse("clouds=off").getCloudSetting());
        assertEquals(CloudSetting.FAST, parse("clouds=fast").getCloudSetting());
        assertEquals(CloudSetting.FANCY, parse("clouds=fancy").getCloudSetting());
    }

    @Test
    void unsetShadowFlagsDefaultToUnset() {
        ShaderProperties props = parse("clouds=off");
        assertEquals(OptionalBoolean.UNSET, props.getShadowEnabled());
    }

    @Test
    void shadowBooleanDirectivesParseTrueAndFalseAndOneAndZero() {
        assertEquals(OptionalBoolean.TRUE, parse("shadow.enabled=true").getShadowEnabled());
        assertEquals(OptionalBoolean.FALSE, parse("shadow.enabled=false").getShadowEnabled());
        assertEquals(OptionalBoolean.TRUE, parse("shadowTerrain=1").getShadowTerrain());
        assertEquals(OptionalBoolean.FALSE, parse("sun=0").getSun());
    }

    @Test
    void scaleDirectiveParsesScaleAndOptionalOffsets() {
        ShaderProperties props = parse("scale.composite=0.5 0.1 0.2");

        ViewportData data = props.getViewportScaleOverrides().get("composite");
        assertEquals(0.5f, data.scale());
        assertEquals(0.1f, data.offsetX());
        assertEquals(0.2f, data.offsetY());
    }

    @Test
    void alphaTestOffDisablesTesting() {
        ShaderProperties props = parse("alphaTest.gbuffers_terrain=off");
        assertEquals(AlphaTest.ALWAYS, props.getAlphaTestOverrides().get("gbuffers_terrain"));
    }

    @Test
    void blendOffRecordsANullOverride() {
        ShaderProperties props = parse("blend.composite=off");
        assertTrue(props.getBlendModeOverrides().containsKey("composite"));
    }

    @Test
    void flipDirectiveParsesPassAndBufferAndBooleanValue() {
        ShaderProperties props = parse("flip.composite.colortex0=true");

        assertEquals(Boolean.TRUE, props.getExplicitFlips().get("composite").get("colortex0"));
    }

    @Test
    void profileDirectiveSplitsWhitespaceSeparatedOptionNames() {
        ShaderProperties props = parse("profile.LOW=BLOOM_LOW SHADOW_LOW");
        assertEquals(List.of("BLOOM_LOW", "SHADOW_LOW"), props.getProfiles().get("LOW"));
    }

    @Test
    void slidersAndScreenDirectivesAreCaptured() {
        ShaderProperties props = parse("""
                sliders=SHADOW_DISTANCE BLOOM_STRENGTH
                screen=BLOOM
                screen.shadows=SHADOW_DISTANCE SHADOW_QUALITY
                """);

        assertEquals(List.of("SHADOW_DISTANCE", "BLOOM_STRENGTH"), props.getSliderOptions());
        assertEquals(List.of("BLOOM"), props.getMainScreenOptions().orElseThrow());
        assertEquals(List.of("SHADOW_DISTANCE", "SHADOW_QUALITY"), props.getSubScreenOptions().get("shadows"));
    }

    @Test
    void noiseTexturePathIsCaptured() {
        ShaderProperties props = parse("texture.noise=noise.png");
        assertEquals("noise.png", props.getNoiseTexturePath().orElseThrow());
    }

    @Test
    void anIfdefGuardedDirectiveOnlyAppliesWhenTheOptionIsEnabled() {
        String source = """
                #ifdef BLOOM
                program.composite.enabled=true
                #endif
                """;

        ShaderProperties enabled = ShaderProperties.parse(source, List.of("BLOOM"));
        ShaderProperties disabled = ShaderProperties.parse(source, List.of());

        assertTrue(enabled.getConditionallyEnabledPrograms().containsKey("composite"));
        assertTrue(disabled.getConditionallyEnabledPrograms().isEmpty());
    }

    @Test
    void emptySourceParsesToAnEmptyResultRatherThanThrowing() {
        ShaderProperties props = ShaderProperties.parse("", List.of());
        assertEquals(CloudSetting.DEFAULT, props.getCloudSetting());
    }

    private static ShaderProperties parse(String contents) {
        return ShaderProperties.parse(contents, List.of());
    }
}
