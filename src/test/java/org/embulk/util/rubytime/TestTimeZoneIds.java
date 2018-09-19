package org.embulk.util.rubytime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

public class TestTimeZoneIds {
    @Test
    public void testParseZoneIdWithJodaAndRubyZoneTab() {
        // TODO: Test more practical equality. (Such as "GMT" v.s. "UTC")
        assertEquals(ZoneOffset.UTC, TimeZoneIds.parseZoneIdWithJodaAndRubyZoneTab("Z"));
        assertEquals(ZoneId.of("Asia/Tokyo"), TimeZoneIds.parseZoneIdWithJodaAndRubyZoneTab("Asia/Tokyo"));
        assertEquals(ZoneId.of("-05:00"), TimeZoneIds.parseZoneIdWithJodaAndRubyZoneTab("EST"));
        assertEquals(ZoneId.of("-10:00"), TimeZoneIds.parseZoneIdWithJodaAndRubyZoneTab("HST"));
        assertEquals(ZoneId.of("Asia/Taipei"), TimeZoneIds.parseZoneIdWithJodaAndRubyZoneTab("ROC"));
    }
}
