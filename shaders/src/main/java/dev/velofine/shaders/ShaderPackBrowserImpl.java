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

package dev.velofine.shaders;

import dev.velofine.core.config.ConfigManager;
import dev.velofine.core.config.VelofineConfig;
import dev.velofine.core.log.VelofineLog;
import dev.velofine.core.shader.ShaderPackBrowser;
import dev.velofine.core.shader.ShaderPackOptionInfo;
import dev.velofine.shaders.discovery.ShaderpackDiscovery;
import dev.velofine.shaders.format.ShaderPack;
import dev.velofine.shaders.format.option.BooleanOption;
import dev.velofine.shaders.format.option.OptionSet;
import dev.velofine.shaders.format.option.StringOption;
import dev.velofine.shaders.pipeline.ShaderPipelineState;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The real {@link ShaderPackBrowser} - see that interface and {@code
 * dev.velofine.core.shader.ShaderPackBrowserRegistry} for why this indirection exists (the GUI
 * lives in {@code core}, which can't depend back on {@code shaders}).
 */
final class ShaderPackBrowserImpl implements ShaderPackBrowser {

    private final Path gameDir;

    ShaderPackBrowserImpl(Path gameDir) {
        this.gameDir = gameDir;
    }

    @Override
    public List<String> listPackNames() {
        return new ShaderpackDiscovery(gameDir).enumerate();
    }

    @Override
    public List<ShaderPackOptionInfo> listOptionsForSelectedPack() {
        ShaderPack pack = ShaderPipelineState.getActivePack();
        if (pack == null) {
            return List.of();
        }

        Map<String, String> overrides = ConfigManager.get().utility.shader.packOptions
                .getOrDefault(pack.getName(), Map.of());
        OptionSet options = pack.getOptionSet();

        List<ShaderPackOptionInfo> result = new ArrayList<>();
        for (BooleanOption option : options.getBooleanOptions().values()) {
            String current = overrides.getOrDefault(option.getName(), Boolean.toString(option.getDefaultValue()));
            result.add(new ShaderPackOptionInfo(option.getName(), true, option.getComment().orElse(""), current, List.of()));
        }
        for (StringOption option : options.getStringOptions().values()) {
            String current = overrides.getOrDefault(option.getName(), option.getDefaultValue());
            result.add(new ShaderPackOptionInfo(option.getName(), false, option.getComment().orElse(""), current, option.getAllowedValues()));
        }
        return result;
    }

    @Override
    public void setOptionValue(String optionName, String value) {
        ShaderPack pack = ShaderPipelineState.getActivePack();
        if (pack == null) {
            return;
        }

        VelofineConfig.ShaderSection config = ConfigManager.get().utility.shader;
        config.packOptions.computeIfAbsent(pack.getName(), n -> new HashMap<>()).put(optionName, value);
        ConfigManager.save();

        // Option edits need the pack's GLSL re-preprocessed with the new value baked in - simplest
        // correct way to do that today is a full re-activation (re-parses shaders.properties/GLSL,
        // rebuilds the gbuffers mapping and composite PostChainConfig). A cheaper "just recompile
        // the affected programs" path is possible later if reload latency turns out to matter.
        VelofineLog.info("Shaders", "Option " + optionName + " = " + value + "; reloading pack " + pack.getName());
        ShaderPipelineState.activate(gameDir, pack.getName());
    }
}
