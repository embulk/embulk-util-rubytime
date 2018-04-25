package org.embulk.util.rubytime;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.util.Map;
import org.junit.Test;

/**
 * Tests RubyParsedElementsQuery.
 */
public class TestParsedElementsQuery {
    @Test
    public void test() {
        final Parsed.Builder builder = Parsed.builder("foo");
        builder.setInstantMilliseconds(123456789);
        builder.setHour(11);
        builder.setDayOfYear(92);
        builder.setWeekBasedYear(1998);
        builder.setLeftover("foobar");
        final Parsed parsed = builder.build();
        final Map<String, Object> parsedElements = parsed.query(ParsedElementsQuery.withFractionInBigDecimal());

        assertEquals(BigDecimal.valueOf(123456).add(BigDecimal.valueOf(789000000, 9)),
                     parsedElements.get("seconds"));
        assertEquals(11, parsedElements.get("hour"));
        assertEquals(92, parsedElements.get("yday"));
        assertEquals(1998, parsedElements.get("cwyear"));
        assertEquals("foobar", parsedElements.get("leftover"));
    }
}
