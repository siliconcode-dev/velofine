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

import java.util.List;
import java.util.Optional;

/**
 * Holds the one {@link ShaderPackBrowser} implementation (registered by {@code
 * dev.velofine.shaders.ShaderEngine} unconditionally, even when the feature is disabled - pack
 * *discovery* is cheap, safe directory-listing I/O, unlike actually loading/activating one, and a
 * user needs to see what packs exist before they can pick one to enable). {@code core}'s {@code
 * UtilityPage}/{@code ShaderOptionsScreen} read through here rather than depending on the {@code
 * shaders} module directly - see {@link ShaderPackBrowser}'s javadoc for why.
 */
public final class ShaderPackBrowserRegistry {

    private static volatile ShaderPackBrowser browser;

    private ShaderPackBrowserRegistry() {
    }

    public static void register(ShaderPackBrowser instance) {
        browser = instance;
    }

    public static List<String> listPackNames() {
        return browser != null ? browser.listPackNames() : List.of();
    }

    public static List<ShaderPackOptionInfo> listOptionsForSelectedPack() {
        return browser != null ? browser.listOptionsForSelectedPack() : List.of();
    }

    public static void setOptionValue(String optionName, String value) {
        if (browser != null) {
            browser.setOptionValue(optionName, value);
        }
    }

    public static Optional<ShaderPackBrowser> get() {
        return Optional.ofNullable(browser);
    }
}
