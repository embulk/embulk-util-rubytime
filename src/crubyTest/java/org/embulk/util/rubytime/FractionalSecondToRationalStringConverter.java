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

import java.math.BigInteger;

class FractionalSecondToRationalStringConverter implements RubyDateTimeParsedElementsQuery.FractionalSecondConverter {
    FractionalSecondToRationalStringConverter() {}

    @Override
    public Object convertFractionalSecond(final long integer, final int nano) {
        // return BigDecimal.valueOf(integer).add(BigDecimal.valueOf(nano, 9));
        return gcdRationalString(BigInteger.valueOf(integer).multiply(BILLION).add(BigInteger.valueOf(nano)), 1_000_000_000L);
    }

    static String gcdRationalString(final BigInteger dividend, final long divisorInLong) {
        final BigInteger divisor = BigInteger.valueOf(divisorInLong);
        final BigInteger gcd = dividend.gcd(divisor);
        return String.format("%s/%s", dividend.divide(gcd).toString(), divisor.divide(gcd).toString());
    }

    private static final BigInteger BILLION = BigInteger.TEN.pow(9);
}
