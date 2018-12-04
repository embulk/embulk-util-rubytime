package org.embulk.util.rubytime;

import java.time.format.DateTimeParseException;

/**
 * An exception thrown when an error occurs during parsing a date-time string by {@link RubyDateTimeFormatter}.
 */
public class RubyDateTimeParseException extends DateTimeParseException {
    /**
     * Constructs a new exception with the specified message.
     *
     * @param message  the message to use for this exception, may be null
     * @param parsedData  the parsed text, should not be null
     * @param errorIndex  the index in the parsed string that was invalid, should be a valid index
     */
    public RubyDateTimeParseException(
            final String message, final CharSequence parsedData, final int errorIndex) {
        super(message, parsedData, errorIndex);
    }

    /**
     * Constructs a new exception with the specified message and cause.
     *
     * @param message  the message to use for this exception, may be null
     * @param parsedData  the parsed text, should not be null
     * @param errorIndex  the index in the parsed string that was invalid, should be a valid index
     * @param cause  the cause exception, may be null
     */
    public RubyDateTimeParseException(
            final String message, final CharSequence parsedData, final int errorIndex, final Throwable cause) {
        super(message, parsedData, errorIndex, cause);
    }
}
