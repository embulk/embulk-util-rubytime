/*
 * Copyright 2019 The Embulk project
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

import org.jruby.Ruby;
import org.jruby.RubyRational;

public class FractionalSecondToRationalConverter implements RubyDateTimeParsedElementsQuery.FractionalSecondConverter {
    public FractionalSecondToRationalConverter(final Ruby ruby) {
        this.ruby = ruby;
    }

    @Override
    public Object convertFractionalSecond(final long integer, final int nano) {
        return RubyRational.newRationalCanonicalize(
                       this.ruby.getCurrentContext(), ((long) integer * 1_000_000_000L) + (long) nano, 1_000_000_000L);
    }

    private final Ruby ruby;
}
