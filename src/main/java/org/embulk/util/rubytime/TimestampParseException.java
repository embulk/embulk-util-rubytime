package org.embulk.util.rubytime;

public class TimestampParseException extends DataException {
    public TimestampParseException(String message) {
        super(message);
    }

    public TimestampParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public TimestampParseException(Throwable cause) {
        super(cause);
    }
}
