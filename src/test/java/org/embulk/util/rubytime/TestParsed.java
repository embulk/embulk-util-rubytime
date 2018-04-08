package org.embulk.util.rubytime;

import java.util.Map;
import org.junit.Test;

public class TestParsed {
    @Test
    public void test() {
        final Parsed.Builder builder = Parsed.builder("foo");
        builder.setLeftover("hogehoge");
        builder.setInstantMilliseconds(123456789);
        builder.setHour(11);
        builder.setDayOfYear(92);
        builder.setWeekBasedYear(1998);
        final Parsed parsed = builder.build();
        final Map<String, Object> rubyHash = (ParsedElementsHashQuery.withFractionInBigDecimal()).queryFrom(parsed);
        System.out.println(rubyHash);
    }
}
