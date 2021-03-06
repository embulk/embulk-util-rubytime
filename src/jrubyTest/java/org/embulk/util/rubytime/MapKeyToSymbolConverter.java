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

import org.jruby.Ruby;
import org.jruby.RubySymbol;

public class MapKeyToSymbolConverter implements RubyDateTimeParsedElementsQuery.MapKeyConverter<RubySymbol> {
    public MapKeyToSymbolConverter(final Ruby ruby) {
        this.ruby = ruby;
    }

    @Override
    public RubySymbol convertMapKey(final String mapKey) {
        return RubySymbol.newSymbol(this.ruby, mapKey);
    }

    private final Ruby ruby;
}
