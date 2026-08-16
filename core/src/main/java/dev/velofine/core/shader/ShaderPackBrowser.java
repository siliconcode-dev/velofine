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

/**
 * What {@code core}'s Utility config page needs to know about shader packs, implemented by the
 * {@code shaders} module and registered via {@link ShaderPackBrowserRegistry} - see that class's
 * javadoc for why this indirection exists.
 */
public interface ShaderPackBrowser {

    /** Names of packs found in {@code shaderpacks/} (folder or zip), sorted. Cheap - just a directory listing, safe to call even when the feature is disabled. */
    List<String> listPackNames();

    /** The currently-selected pack's options, or empty if none is selected/active or it declares none. */
    List<ShaderPackOptionInfo> listOptionsForSelectedPack();

    /** Applies a user's edit to one option of the currently-selected pack. */
    void setOptionValue(String optionName, String value);
}
