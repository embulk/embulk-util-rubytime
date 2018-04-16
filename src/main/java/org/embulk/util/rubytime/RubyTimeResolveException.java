package org.embulk.util.rubytime;

public class RubyTimeResolveException extends Exception {
    public RubyTimeResolveException(final String message) {
        super(message);
    }

    public RubyTimeResolveException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public RubyTimeResolveException(final Throwable cause) {
        super(cause);
    }
}
