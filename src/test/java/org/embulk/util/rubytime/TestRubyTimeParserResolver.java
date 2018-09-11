package org.embulk.util.rubytime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAccessor;
import org.junit.Test;

/**
 * Tests RubyTimeParser and DefaultRubyTimeResolver.
 */
public class TestRubyTimeParserResolver {
    @Test
    public void testEpochWithFraction() throws RubyTimeResolveException {
        assertParsedTime("1500000000.123456789", "%s.%N", Instant.ofEpochSecond(1500000000L, 123456789));
        assertParsedTime("1500000000456.111111111", "%Q.%N", Instant.ofEpochSecond(1500000000L, 567111111));
        assertParsedTime("1500000000.123", "%s.%L", Instant.ofEpochSecond(1500000000L, 123000000));
        assertParsedTime("1500000000456.111", "%Q.%L", Instant.ofEpochSecond(1500000000L, 567000000));
    }

    // Alternative of TestRubyTimeParserWithJRuby#testTestTimeExtension_test_strptime_s_N that is ignored
    // for the precision of the fraction part.
    @Test
    public void test_Ruby_test_time_test_strptime_s_N() throws RubyTimeResolveException {
        assertParsedTime("1.5", "%s.%N", Instant.ofEpochSecond(1L, 500000000));
        assertParsedTime("-1.5", "%s.%N", Instant.ofEpochSecond(-2L, 500000000));
        assertParsedTime("1.000000001", "%s.%N", Instant.ofEpochSecond(1L, 1));
        assertParsedTime("-1.000000001", "%s.%N", Instant.ofEpochSecond(-2L, 999999999));
    }

    @Test
    public void testDateTimeFromInstant() throws RubyTimeResolveException {
        final RubyTimeParser parser = new RubyTimeParser(RubyTimeFormat.compile("%Q.%N"));
        final TemporalAccessor parsed = parser.parse("1500000000456.111111111");

        final RubyTimeResolver resolver = DefaultRubyTimeResolver.of();
        final TemporalAccessor resolved = resolver.resolve(parsed);

        final OffsetDateTime datetime = OffsetDateTime.from(resolved);
        assertEquals(OffsetDateTime.of(2017, 7, 14, 02, 40, 00, 567111111, ZoneOffset.UTC), datetime);
    }

    private static void assertParsedTime(
            final String string,
            final String format,
            final Instant expected)
            throws RubyTimeResolveException {
        final RubyTimeParser parser = new RubyTimeParser(RubyTimeFormat.compile(format));
        final TemporalAccessor parsed = parser.parse(string);

        final RubyTimeResolver resolver = DefaultRubyTimeResolver.of();
        final TemporalAccessor resolved = resolver.resolve(parsed);

        final Instant actualInstant = Instant.from(resolved);
        assertEquals(expected, actualInstant);
    }
}
