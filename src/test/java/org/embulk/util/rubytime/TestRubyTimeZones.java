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

import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

public class TestRubyTimeZones {
    @Test
    public void testToOffset() {
        assertEquals(ZoneOffset.UTC, RubyTimeZones.toZoneOffset("Z", ZoneOffset.UTC));
        assertEquals(ZoneOffset.of("+02:00:00"), RubyTimeZones.toZoneOffset("+02", ZoneOffset.UTC));
        assertEquals(ZoneOffset.of("+02:15:00"), RubyTimeZones.toZoneOffset("+0215", ZoneOffset.UTC));
        assertEquals(ZoneOffset.of("+02:30:00"), RubyTimeZones.toZoneOffset("+02:30", ZoneOffset.UTC));
        assertEquals(ZoneOffset.of("+02:45:30"), RubyTimeZones.toZoneOffset("+024530", ZoneOffset.UTC));
        assertEquals(ZoneOffset.of("+02:50:10"), RubyTimeZones.toZoneOffset("+02:50:10", ZoneOffset.UTC));
        assertEquals(ZoneOffset.of("-08:00:00"), RubyTimeZones.toZoneOffset("PST", ZoneOffset.UTC));
        assertEquals(ZoneOffset.of("+12:00:00"), RubyTimeZones.toZoneOffset("M", ZoneOffset.UTC));
    }
}
