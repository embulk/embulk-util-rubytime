package org.embulk.util.rubytime;

import java.time.temporal.TemporalAccessor;

/**
 * Resolves date/time from TemporalAccessor parsed by RubyTimeParser.
 */
public interface RubyTimeResolver {
    TemporalAccessor resolve(TemporalAccessor source) throws RubyTimeResolveException;
}
