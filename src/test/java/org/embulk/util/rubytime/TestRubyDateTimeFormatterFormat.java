/*
 * Copyright 2020 The Embulk project
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

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/**
 * Tests formatting by RubyDateTimeFormatter.
 */
public class TestRubyDateTimeFormatterFormat {
    @Test
    public void test() {
        final RubyDateTimeFormatter formatter = RubyDateTimeFormatter.ofPattern("%012% %^a %#A %b %B %^h");
        System.out.println(formatter.format(OffsetDateTime.of(2019, 12, 31, 12, 34, 56, 0, ZoneOffset.UTC)));
    }
}
