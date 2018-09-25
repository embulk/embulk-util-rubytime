package org.embulk.util.rubytime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

public class TestTimeZones {
    @Test
    public void testToOffset() {
        assertEquals(ZoneOffset.UTC, TimeZones.toZoneOffset("Z", ZoneOffset.UTC));
        assertEquals(ZoneOffset.of("+02:00:00"), TimeZones.toZoneOffset("+02", ZoneOffset.UTC));
        assertEquals(ZoneOffset.of("+02:15:00"), TimeZones.toZoneOffset("+0215", ZoneOffset.UTC));
        assertEquals(ZoneOffset.of("+02:30:00"), TimeZones.toZoneOffset("+02:30", ZoneOffset.UTC));
        assertEquals(ZoneOffset.of("+02:45:30"), TimeZones.toZoneOffset("+024530", ZoneOffset.UTC));
        assertEquals(ZoneOffset.of("+02:50:10"), TimeZones.toZoneOffset("+02:50:10", ZoneOffset.UTC));
        assertEquals(ZoneOffset.of("-08:00:00"), TimeZones.toZoneOffset("PST", ZoneOffset.UTC));
        assertEquals(ZoneOffset.of("+12:00:00"), TimeZones.toZoneOffset("M", ZoneOffset.UTC));
    }
}
