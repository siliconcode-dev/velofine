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
import dev.velofine.shaders.format.blending.AlphaTest;
import dev.velofine.shaders.format.blending.AlphaTestFunction;
import dev.velofine.shaders.format.blending.BlendMode;
import dev.velofine.shaders.format.blending.BlendModeFunction;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Consumer;

/**
 * The parsed representation of a shaderpack's {@code shaders.properties}.
 *
 * <p>Adapted from IrisShaders/Iris ({@code common/.../shaderpack/properties/ShaderProperties.java}),
 * LGPL-3.0, with a deliberately narrower v1 scope than Iris's own ~140-directive superset. Covers
 * the directives the Phase 7 pipeline stages (gbuffers takeover, deferred/composite chaining,
 * shadow pass, options screen) actually consume: program enabling, blend/alphaTest/viewport/texture-
 * scale overrides, buffer-flip flags, cloud/shadow/sky/vignette/back-face toggles, profiles,
 * sliders, and screen/sub-screen option layout.
 *
 * <p><b>Explicitly deferred, not silently dropped</b> - real OptiFine/Iris directives this class
 * does not yet parse, tracked as follow-up work once a real pack is shown to need one:
 * {@code bufferObject.*} (SSBOs), {@code indirect.*}, {@code image.*}/{@code customTexture.*}/raw
 * {@code texture.*} (beyond the single {@code texture.noise} path), per-sub-buffer
 * {@code blend.<pass>.<buffer>=} overrides, and {@code variable.*}/{@code uniform.*} custom-uniform
 * expressions (a small expression language of its own - real scope, not a quick add). Neither BSL
 * nor Complementary (this phase's validation targets, per Build_plan) are known to require any of
 * these for their base feature set.
 */
public final class ShaderProperties {

    private final Map<String, String> conditionallyEnabledPrograms = new LinkedHashMap<>();
    private final Map<String, AlphaTest> alphaTestOverrides = new HashMap<>();
    private final Map<String, ViewportData> viewportScaleOverrides = new HashMap<>();
    private final Map<String, TextureScaleOverride> textureScaleOverrides = new HashMap<>();
    private final Map<String, BlendMode> blendModeOverrides = new HashMap<>();
    private final Map<String, Map<String, Boolean>> explicitFlips = new HashMap<>();
    private final Map<String, List<String>> profiles = new LinkedHashMap<>();
    private final Map<String, List<String>> subScreenOptions = new HashMap<>();

    private CloudSetting cloudSetting = CloudSetting.DEFAULT;
    private OptionalBoolean shadowEnabled = OptionalBoolean.UNSET;
    private OptionalBoolean shadowTerrain = OptionalBoolean.UNSET;
    private OptionalBoolean shadowEntities = OptionalBoolean.UNSET;
    private OptionalBoolean shadowPlayer = OptionalBoolean.UNSET;
    private OptionalBoolean shadowBlockEntities = OptionalBoolean.UNSET;
    private OptionalBoolean sun = OptionalBoolean.UNSET;
    private OptionalBoolean moon = OptionalBoolean.UNSET;
    private OptionalBoolean stars = OptionalBoolean.UNSET;
    private OptionalBoolean sky = OptionalBoolean.UNSET;
    private OptionalBoolean vignette = OptionalBoolean.UNSET;
    private String noiseTexturePath;
    private List<String> sliderOptions = new ArrayList<>();
    private List<String> mainScreenOptions;

    private ShaderProperties() {
    }

    public static ShaderProperties empty() {
        return new ShaderProperties();
    }

    /**
     * @param contents raw {@code shaders.properties} text.
     * @param enabledBooleanOptions the pack's currently-enabled boolean options (from
     *                              {@link dev.velofine.shaders.format.option.OptionSet}, resolved
     *                              against user overrides) - drives the file's own {@code #ifdef}
     *                              conditionals before parsing.
     */
    public static ShaderProperties parse(String contents, Iterable<String> enabledBooleanOptions) {
        ShaderProperties result = new ShaderProperties();
        String preprocessed = ShaderPackPreprocessor.preprocessProperties(
                contents, ShaderPackPreprocessor.flagMacros(enabledBooleanOptions));

        Properties properties = new OrderBackedProperties();
        try {
            properties.load(new StringReader(preprocessed));
        } catch (IOException e) {
            VelofineLog.warn("Shaders", "Error loading shaders.properties: " + e);
            return result;
        }

        properties.forEach((keyObject, valueObject) -> result.acceptDirective((String) keyObject, (String) valueObject));
        return result;
    }

    private void acceptDirective(String key, String value) {
        if ("texture.noise".equals(key)) {
            noiseTexturePath = value;
            return;
        }

        if ("clouds".equals(key)) {
            switch (value) {
                case "off" -> cloudSetting = CloudSetting.OFF;
                case "fast" -> cloudSetting = CloudSetting.FAST;
                case "fancy" -> cloudSetting = CloudSetting.FANCY;
                default -> VelofineLog.warn("Shaders", "Unrecognized clouds setting: " + value);
            }
            return;
        }

        if (handleBoolean(key, value, "shadow.enabled", v -> shadowEnabled = v)) {
            return;
        }
        if (handleBoolean(key, value, "shadowTerrain", v -> shadowTerrain = v)) {
            return;
        }
        if (handleBoolean(key, value, "shadowEntities", v -> shadowEntities = v)) {
            return;
        }
        if (handleBoolean(key, value, "shadowPlayer", v -> shadowPlayer = v)) {
            return;
        }
        if (handleBoolean(key, value, "shadowBlockEntities", v -> shadowBlockEntities = v)) {
            return;
        }
        if (handleBoolean(key, value, "sun", v -> sun = v)) {
            return;
        }
        if (handleBoolean(key, value, "moon", v -> moon = v)) {
            return;
        }
        if (handleBoolean(key, value, "stars", v -> stars = v)) {
            return;
        }
        if (handleBoolean(key, value, "sky", v -> sky = v)) {
            return;
        }
        if (handleBoolean(key, value, "vignette", v -> vignette = v)) {
            return;
        }

        if (key.startsWith("program.")) {
            String program = key.substring("program.".length());
            int dot = program.indexOf('.');
            if (dot != -1) {
                program = program.substring(0, dot);
            }
            conditionallyEnabledPrograms.put(program, value);
            return;
        }

        if (withPrefix("scale.", key) != null) {
            acceptScaleDirective(withPrefix("scale.", key), value);
            return;
        }

        if (withPrefix("size.buffer.", key) != null) {
            acceptSizeBufferDirective(withPrefix("size.buffer.", key), value);
            return;
        }

        if (withPrefix("alphaTest.", key) != null) {
            acceptAlphaTestDirective(withPrefix("alphaTest.", key), value);
            return;
        }

        if (withPrefix("blend.", key) != null && !withPrefix("blend.", key).contains(".")) {
            acceptBlendDirective(withPrefix("blend.", key), value);
            return;
        }

        if (key.startsWith("flip.")) {
            acceptFlipDirective(key, value);
            return;
        }

        if (key.startsWith("profile.")) {
            profiles.put(key.substring("profile.".length()), splitWhitespace(value));
            return;
        }

        if ("sliders".equals(key)) {
            sliderOptions = splitWhitespace(value);
            return;
        }

        if ("screen".equals(key)) {
            mainScreenOptions = splitWhitespace(value);
            return;
        }

        if (key.startsWith("screen.") && !key.endsWith(".columns")) {
            subScreenOptions.put(key.substring("screen.".length()), splitWhitespace(value));
        }
    }

    private void acceptScaleDirective(String pass, String value) {
        String[] parts = value.split(" ");
        try {
            float scale = Float.parseFloat(parts[0]);
            float offsetX = parts.length > 1 ? Float.parseFloat(parts[1]) : 0.0f;
            float offsetY = parts.length > 2 ? Float.parseFloat(parts[2]) : 0.0f;
            viewportScaleOverrides.put(pass, new ViewportData(scale, offsetX, offsetY));
        } catch (NumberFormatException e) {
            VelofineLog.warn("Shaders", "Unable to parse scale directive for " + pass + ": " + value);
        }
    }

    private void acceptSizeBufferDirective(String pass, String value) {
        String[] parts = value.split(" ");
        if (parts.length != 2) {
            VelofineLog.warn("Shaders", "Unable to parse size.buffer directive for " + pass + ": " + value);
            return;
        }
        textureScaleOverrides.put(pass, new TextureScaleOverride(parts[0], parts[1]));
    }

    private void acceptAlphaTestDirective(String pass, String value) {
        if ("off".equals(value) || "false".equals(value)) {
            alphaTestOverrides.put(pass, AlphaTest.ALWAYS);
            return;
        }

        String[] parts = value.split(" ");
        if (parts.length < 2) {
            VelofineLog.warn("Shaders", "Invalid alpha test directive for " + pass + ": " + value);
            return;
        }

        Optional<AlphaTestFunction> function = AlphaTestFunction.fromString(parts[0]);
        if (function.isEmpty()) {
            VelofineLog.warn("Shaders", "Unknown alpha test function for " + pass + ": " + parts[0]);
            return;
        }

        try {
            alphaTestOverrides.put(pass, new AlphaTest(function.get(), Float.parseFloat(parts[1])));
        } catch (NumberFormatException e) {
            VelofineLog.warn("Shaders", "Unable to parse alpha test reference for " + pass + ": " + value);
        }
    }

    private void acceptBlendDirective(String pass, String value) {
        if ("off".equals(value)) {
            blendModeOverrides.put(pass, null);
            return;
        }

        String[] modeNames = value.split(" ");
        if (modeNames.length != 4) {
            VelofineLog.warn("Shaders", "Blend directive for " + pass + " needs 4 factors: " + value);
            return;
        }

        BlendModeFunction[] modes = new BlendModeFunction[4];
        for (int i = 0; i < 4; i++) {
            Optional<BlendModeFunction> mode = BlendModeFunction.fromString(modeNames[i]);
            if (mode.isEmpty()) {
                return;
            }
            modes[i] = mode.get();
        }
        blendModeOverrides.put(pass, BlendMode.of(modes[0], modes[1], modes[2], modes[3]));
    }

    private void acceptFlipDirective(String key, String value) {
        String rest = key.substring("flip.".length());
        int dot = rest.indexOf('.');
        if (dot == -1) {
            return;
        }
        String pass = rest.substring(0, dot);
        String buffer = rest.substring(dot + 1);

        Boolean flip = "true".equals(value) || "1".equals(value) ? Boolean.TRUE
                : "false".equals(value) || "0".equals(value) ? Boolean.FALSE : null;
        if (flip == null) {
            VelofineLog.warn("Shaders", "Unexpected value for flip directive " + key + ": " + value);
            return;
        }
        explicitFlips.computeIfAbsent(pass, p -> new HashMap<>()).put(buffer, flip);
    }

    private static boolean handleBoolean(String key, String value, String expectedKey, Consumer<OptionalBoolean> handler) {
        if (!expectedKey.equals(key)) {
            return false;
        }
        if ("true".equals(value) || "1".equals(value)) {
            handler.accept(OptionalBoolean.TRUE);
        } else if ("false".equals(value) || "0".equals(value)) {
            handler.accept(OptionalBoolean.FALSE);
        } else {
            VelofineLog.warn("Shaders", "Unexpected value for boolean key " + key + ": " + value);
        }
        return true;
    }

    private static String withPrefix(String prefix, String key) {
        return key.startsWith(prefix) ? key.substring(prefix.length()) : null;
    }

    private static List<String> splitWhitespace(String value) {
        return Arrays.asList(value.trim().split("\\s+"));
    }

    public CloudSetting getCloudSetting() {
        return cloudSetting;
    }

    public OptionalBoolean getShadowEnabled() {
        return shadowEnabled;
    }

    public OptionalBoolean getShadowTerrain() {
        return shadowTerrain;
    }

    public OptionalBoolean getShadowEntities() {
        return shadowEntities;
    }

    public OptionalBoolean getShadowPlayer() {
        return shadowPlayer;
    }

    public OptionalBoolean getShadowBlockEntities() {
        return shadowBlockEntities;
    }

    public OptionalBoolean getSun() {
        return sun;
    }

    public OptionalBoolean getMoon() {
        return moon;
    }

    public OptionalBoolean getStars() {
        return stars;
    }

    public OptionalBoolean getSky() {
        return sky;
    }

    public OptionalBoolean getVignette() {
        return vignette;
    }

    public Optional<String> getNoiseTexturePath() {
        return Optional.ofNullable(noiseTexturePath);
    }

    public Map<String, String> getConditionallyEnabledPrograms() {
        return conditionallyEnabledPrograms;
    }

    public Map<String, AlphaTest> getAlphaTestOverrides() {
        return alphaTestOverrides;
    }

    public Map<String, ViewportData> getViewportScaleOverrides() {
        return viewportScaleOverrides;
    }

    public Map<String, TextureScaleOverride> getTextureScaleOverrides() {
        return textureScaleOverrides;
    }

    public Map<String, BlendMode> getBlendModeOverrides() {
        return blendModeOverrides;
    }

    public Map<String, Map<String, Boolean>> getExplicitFlips() {
        return explicitFlips;
    }

    public Map<String, List<String>> getProfiles() {
        return profiles;
    }

    public List<String> getSliderOptions() {
        return sliderOptions;
    }

    public Optional<List<String>> getMainScreenOptions() {
        return Optional.ofNullable(mainScreenOptions);
    }

    public Map<String, List<String>> getSubScreenOptions() {
        return subScreenOptions;
    }
}
