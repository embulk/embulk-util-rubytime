package org.embulk.util.rubytime;

import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQuery;

/**
 * Ruby-specific implementations of {@code java.time.temporal.TemporalQuery}.
 */
public final class RubyTemporalQueries {
    private RubyTemporalQueries() {
        // No instantiation.
    }

    public static TemporalQuery<String> rubyTimeZone() {
        return RubyTemporalQueries.RUBY_TIME_ZONE;
    }

    public static TemporalQuery<String> leftover() {
        return RubyTemporalQueries.LEFTOVER;
    }

    static final TemporalQuery<String> RUBY_TIME_ZONE =
            (temporal) -> temporal.query(RubyTemporalQueries.RUBY_TIME_ZONE);
    static final TemporalQuery<String> LEFTOVER =
            (temporal) -> temporal.query(RubyTemporalQueries.LEFTOVER);
}
