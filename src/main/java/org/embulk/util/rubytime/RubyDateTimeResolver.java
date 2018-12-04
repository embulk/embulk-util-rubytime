package org.embulk.util.rubytime;

import java.time.temporal.TemporalAccessor;

/**
 * A resolver for {@link java.time.temporal.TemporalAccessor}, date-time data parsed by {@link RubyDateTimeFormatter}.
 */
public interface RubyDateTimeResolver {
    /**
     * Resolves {@link java.time.temporal.TemporalAccessor}, date-time data parsed by {@link RubyDateTimeFormatter}.
     *
     * @param source  the date-time data parsed by {@link RubyDateTimeFormatter}.
     * @return the resolved data-time data
     * @throws RubyTimeResolveException  if the resolution fails
     */
    TemporalAccessor resolve(TemporalAccessor source) throws RubyTimeResolveException;
}
