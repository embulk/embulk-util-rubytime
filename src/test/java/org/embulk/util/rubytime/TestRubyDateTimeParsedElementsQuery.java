/*
 * Copyright 2018 The Embulk project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.embulk.util.rubytime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Tests RubyDateTimeParsedElementsQuery.
 */
public class TestRubyDateTimeParsedElementsQuery {
    @Test
    public void test() {
        final Parsed.Builder builder = Parsed.builder("foo");
        builder.setMillisecondsSinceEpoch(123456789);
        builder.setHour(11);
        builder.setDayOfYear(92);
        builder.setWeekBasedYear(1998);
        builder.setLeftover("foobar");
        final Parsed parsed = builder.build();
        final Map<String, Object> parsedElements =
                parsed.query(RubyDateTimeParsedElementsQuery.withBigDecimal());

        assertEquals(BigDecimal.valueOf(123456).add(BigDecimal.valueOf(789, 3)),
                     parsedElements.get("seconds"));
        assertEquals(11, parsedElements.get("hour"));
        assertEquals(92, parsedElements.get("yday"));
        assertEquals(1998, parsedElements.get("cwyear"));
        assertEquals("foobar", parsedElements.get("leftover"));
    }
}
