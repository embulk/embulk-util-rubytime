package org.embulk.util.rubytime;

import java.time.temporal.TemporalAccessor;

/**
 * Resolves TemporalAccessor parsed by RubyDateTimeFormatter into a meaningful date-time object.
 */
public interface RubyTimeResolver {
    TemporalAccessor resolve(TemporalAccessor source) throws RubyTimeResolveException;
}
