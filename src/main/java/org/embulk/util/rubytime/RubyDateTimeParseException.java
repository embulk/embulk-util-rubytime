package org.embulk.util.rubytime;

import java.time.format.DateTimeParseException;

public class RubyDateTimeParseException extends DateTimeParseException {
    public RubyDateTimeParseException(
            final String message, final CharSequence parsedData, final int errorIndex) {
        super(message, parsedData, errorIndex);
    }

    public RubyDateTimeParseException(
            final String message, final CharSequence parsedData, final int errorIndex, final Throwable cause) {
        super(message, parsedData, errorIndex, cause);
    }
}
