/*
 * Copyright 2018 The Embulk project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
