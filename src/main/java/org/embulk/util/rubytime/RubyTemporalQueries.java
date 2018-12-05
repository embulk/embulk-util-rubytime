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

import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQuery;

/**
 * Ruby-specific implementations of {@link java.time.temporal.TemporalQuery}.
 */
public final class RubyTemporalQueries {
    private RubyTemporalQueries() {
        // No instantiation.
    }

    /**
     * A query for the time zone name in the same manner with {@code Date._strptime}.
     *
     * @return a query that can obtain the time zone name in the same manner with {@code Date._strptime}
     */
    public static TemporalQuery<String> rubyTimeZone() {
        return RubyTemporalQueries.RUBY_TIME_ZONE;
    }

    /**
     * A query for the {@link java.lang.String} leftover from parsing.
     *
     * @return a query that can obtain the {@link java.lang.String} leftover from parsing
     */
    public static TemporalQuery<String> leftover() {
        return RubyTemporalQueries.LEFTOVER;
    }

    static final TemporalQuery<String> RUBY_TIME_ZONE =
            (temporal) -> temporal.query(RubyTemporalQueries.RUBY_TIME_ZONE);
    static final TemporalQuery<String> LEFTOVER =
            (temporal) -> temporal.query(RubyTemporalQueries.LEFTOVER);
}
