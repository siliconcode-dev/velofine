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

package dev.velofine.shaders.format.option;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class OptionSetTest {

    @Test
    void booleanOptionCreateReturnsAPlainToggle() {
        BooleanOption option = new BooleanOption(OptionType.DEFINE, "BLOOM", "Enables bloom", true);

        assertEquals("BLOOM", option.getName());
        assertTrue(option.getDefaultValue());
        assertEquals("Enables bloom", option.getComment().orElseThrow());
    }

    @Test
    void stringOptionCreateReturnsNullWhenTheCommentHasNoAllowedValueList() {
        // A plain numeric #define with no [...] list is not a selectable option at all.
        assertNull(StringOption.create(OptionType.DEFINE, "SOME_CONSTANT", "just a comment, no brackets", "5"));
        assertNull(StringOption.create(OptionType.DEFINE, "SOME_CONSTANT", null, "5"));
    }

    @Test
    void stringOptionCreateParsesTheAllowedValueList() {
        StringOption option = StringOption.create(OptionType.DEFINE, "SHADOW_QUALITY", "Shadow quality [0 1 2 3]", "1");

        assertEquals("1", option.getDefaultValue());
        assertEquals(List.of("0", "1", "2", "3"), option.getAllowedValues());
        assertEquals("Shadow quality", option.getComment().orElseThrow());
    }

    @Test
    void stringOptionDefaultIsAddedToAllowedValuesIfMissing() {
        StringOption option = StringOption.create(OptionType.DEFINE, "QUALITY", "[low high]", "medium");

        assertTrue(option.getAllowedValues().contains("medium"));
    }

    @Test
    void builderTracksBooleanAndStringOptionsSeparately() {
        OptionSet.Builder builder = OptionSet.builder();
        builder.addBooleanOption(new BooleanOption(OptionType.DEFINE, "BLOOM", null, true));
        builder.addStringOption(StringOption.create(OptionType.DEFINE, "QUALITY", "[low high]", "low"));
        OptionSet set = builder.build();

        assertTrue(set.isBooleanOption("BLOOM"));
        assertFalse(set.isBooleanOption("QUALITY"));
        assertTrue(set.getStringOptions().containsKey("QUALITY"));
    }

    @Test
    void firstDefinitionWinsOnARedefinitionWithADifferentDefault() {
        OptionSet.Builder builder = OptionSet.builder();
        builder.addBooleanOption(new BooleanOption(OptionType.DEFINE, "BLOOM", null, true));
        builder.addBooleanOption(new BooleanOption(OptionType.DEFINE, "BLOOM", null, false));

        assertTrue(builder.build().getBooleanOptions().get("BLOOM").getDefaultValue());
    }

    @Test
    void anOptionCannotBeBothBooleanAndString() {
        OptionSet.Builder builder = OptionSet.builder();
        builder.addBooleanOption(new BooleanOption(OptionType.DEFINE, "AMBIGUOUS", null, true));
        builder.addStringOption(StringOption.create(OptionType.DEFINE, "AMBIGUOUS", "[a b]", "a"));

        OptionSet set = builder.build();
        assertTrue(set.isBooleanOption("AMBIGUOUS"));
        assertFalse(set.getStringOptions().containsKey("AMBIGUOUS"));
    }
}
