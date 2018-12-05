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

import java.time.temporal.TemporalAccessor;

/**
 * A resolver for {@link java.time.temporal.TemporalAccessor}, date-time data parsed by {@link RubyDateTimeFormatter}.
 */
public interface RubyDateTimeResolver {
    /**
     * Resolves {@link java.time.temporal.TemporalAccessor}, date-time data parsed by {@link RubyDateTimeFormatter}.
     *
     * @param source  the date-time data parsed by {@link RubyDateTimeFormatter}.
     * @return the resolved data-time data
     * @throws RubyTimeResolveException  if the resolution fails
     */
    TemporalAccessor resolve(TemporalAccessor source) throws RubyTimeResolveException;
}
