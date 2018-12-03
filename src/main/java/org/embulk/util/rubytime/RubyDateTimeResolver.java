package org.embulk.util.rubytime;

import java.time.temporal.TemporalAccessor;

/**
 * Resolves {@code java.time.temporal.TemporalAccessor} parsed by {@code RubyDateTimeFormatter} into a meaningful date-time object.
 */
public interface RubyDateTimeResolver {
    TemporalAccessor resolve(TemporalAccessor source) throws RubyTimeResolveException;
}
