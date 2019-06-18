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

import java.time.ZoneOffset;
import java.time.temporal.TemporalAccessor;

/**
 * A resolver for {@link java.time.temporal.TemporalAccessor}, date-time data parsed by {@link RubyDateTimeFormatter}.
 */
public abstract class RubyDateTimeResolver {
    /**
     * Resolves {@link java.time.temporal.TemporalAccessor}, date-time data parsed by {@link RubyDateTimeFormatter}.
     *
     * @param source  the date-time data parsed by {@link RubyDateTimeFormatter}.
     * @return the resolved data-time data
     * @throws RubyDateTimeParseException  if the resolution fails
     */
    public abstract TemporalAccessor resolve(TemporalAccessor source);

    /**
     * The default resolver similar to Ruby's {@code Time.strptime}.
     *
     * @return the default resolver, not null
     */
    public static RubyDateTimeResolver ofDefault() {
        return DefaultHolder.INSTANCE;
    }

    /**
     * Creates a resolver with default {@link java.time.ZoneOffset} configured.
     *
     * @param defaultZoneOffset  the default {@link java.time.ZoneOffset} if the parsed text does not contain a timezone
     * @return the resolver created, not null
     */
    public static RubyDateTimeResolver withDefaultZoneOffset(final ZoneOffset defaultZoneOffset) {
        return new DefaultRubyTimeResolver(false, defaultZoneOffset, 1970, 1, 1, 0, 0, 0, 0);
    }

    private static class DefaultHolder {  // Initialization-on-demand holder
        static final RubyDateTimeResolver INSTANCE = new DefaultRubyTimeResolver(false, ZoneOffset.UTC, 1970, 1, 1, 0, 0, 0, 0);
    }
}
