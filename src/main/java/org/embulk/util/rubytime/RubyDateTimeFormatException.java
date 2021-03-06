/*
 * Copyright 2020 The Embulk project
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

import java.time.DateTimeException;

/**
 * An exception thrown when an error occurs during formatting a date-time object by {@link RubyDateTimeFormatter}.
 */
public class RubyDateTimeFormatException extends DateTimeException {
    /**
     * Constructs a new exception with the specified message.
     *
     * @param message  the message to use for this exception, may be null
     */
    public RubyDateTimeFormatException(final String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified message and cause.
     *
     * @param message  the message to use for this exception, may be null
     * @param cause  the cause exception, may be null
     */
    public RubyDateTimeFormatException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
