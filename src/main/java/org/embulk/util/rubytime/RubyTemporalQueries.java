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
