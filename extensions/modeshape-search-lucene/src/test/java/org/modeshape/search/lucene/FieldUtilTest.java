/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.search.lucene;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class FieldUtilTest {

    public static final String BIG_INTEGER_NUMBER = "123545678901234567890123545678901234567890";
    public static final String BIG_DECIMAL_NUMBER = "123545678901234567890123545678901234567890.123545678901234567890";
    public static final BigDecimal BIG_NEGATIVE = new BigDecimal("-" + BIG_DECIMAL_NUMBER);
    public static final BigDecimal BIG_POSITIVE = new BigDecimal(BIG_DECIMAL_NUMBER);
    public static final BigDecimal BIGGEST_POSITIVE = new BigDecimal(new BigInteger(BIG_INTEGER_NUMBER), Integer.MAX_VALUE);
    public static final BigDecimal BIGGEST_NEGATIVE = new BigDecimal(new BigInteger(BIG_INTEGER_NUMBER), Integer.MAX_VALUE).negate();
    public static final BigDecimal ZERO = BigDecimal.ZERO;
    public static final BigDecimal TEN = BigDecimal.TEN;
    public static final BigDecimal ONE = BigDecimal.ONE;
    public static final BigDecimal NEGATIVE_TEN = BigDecimal.TEN.negate();
    public static final BigDecimal NEGATIVE_ONE = BigDecimal.ONE.negate();

    @Test
    public void shouldSerializeBigDecimalWithValueOfZero() {
        assertSerializeAndDeserialize(ZERO, "0");
    }

    @Test
    public void shouldSerializeBigDecimalWithValueOfOne() {
        assertSerializeAndDeserialize(ONE, "101");
    }

    @Test
    public void shouldSerializeBigDecimalWithValueOfTen() {
        assertSerializeAndDeserialize(TEN, "111 11");
    }

    @Test
    public void shouldSerializeBigDecimalWithValueOfNegativeOne() {
        assertSerializeAndDeserialize(NEGATIVE_ONE, "-08A");
    }

    @Test
    public void shouldSerializeBigDecimalWithValueOfNegativeTen() {
        assertSerializeAndDeserialize(NEGATIVE_TEN, "--8 88A");
    }

    @Test
    public void shouldSerializeBigDecimalWithBigPositiveValue() {
        assertSerializeAndDeserialize(BIG_POSITIVE, "112 4112354567890123456789012354567890123456789012354567890123456789");
    }

    @Test
    public void shouldSerializeBigDecimalWithBigNegativeValue() {
        assertSerializeAndDeserialize(BIG_NEGATIVE, "--7 5887645432109876543210987645432109876543210987645432109876543210A");
    }

    @Test
    public void shortSerializeLargestPositiveBigDecimalValue() {
        assertSerializeAndDeserialize(BIGGEST_POSITIVE, "1-10 785251639312354567890123456789012354567890123456789");
    }

    @Test
    public void shortSerializeLargestNegativeBigDecimalValue() {
        assertSerializeAndDeserialize(BIGGEST_NEGATIVE, "-189 214748360687645432109876543210987645432109876543210A");
    }

    @Test
    public void shouldSerializeBigDecimalWithZeroAsExponents() {
        assertSerializeAndDeserialize(new BigDecimal("+1.2E0"), "1012");
        assertSerializeAndDeserialize(new BigDecimal("-1.2E0"), "-087A");
    }

    @Test
    public void shouldSerializeBigDecimalPositiveValues() {
        assertSerializeAndDeserialize(new BigDecimal("+5.E-3"), "1-1 65");
        assertSerializeAndDeserialize(new BigDecimal("+1.E-2"), "1-1 71");
        assertSerializeAndDeserialize(new BigDecimal("+1.0E-2"), "1-1 71");
        assertSerializeAndDeserialize(new BigDecimal("+1.0000E-2"), "1-1 71");
        assertSerializeAndDeserialize(new BigDecimal("+1.1E-2"), "1-1 711");
        assertSerializeAndDeserialize(new BigDecimal("+1.11E-2"), "1-1 7111");
        assertSerializeAndDeserialize(new BigDecimal("+1.2E-2"), "1-1 712");
        assertSerializeAndDeserialize(new BigDecimal("+5.E-2"), "1-1 75");
        assertSerializeAndDeserialize(new BigDecimal("+7.3E+2"), "111 273");
        assertSerializeAndDeserialize(new BigDecimal("+7.4E+2"), "111 274");
        assertSerializeAndDeserialize(new BigDecimal("+7.45E+2"), "111 2745");
        assertSerializeAndDeserialize(new BigDecimal("+8.7654E+3"), "111 387654");
    }

    @Test
    public void shouldSerializeBigDecimalNegativeValues() {
        assertSerializeAndDeserialize(new BigDecimal("-8.7654E+3"), "--8 612345A");
        assertSerializeAndDeserialize(new BigDecimal("-7.45E+2"), "--8 7254A");
        assertSerializeAndDeserialize(new BigDecimal("-7.4E+2"), "--8 725A");
        assertSerializeAndDeserialize(new BigDecimal("-7.3E+2"), "--8 726A");
        assertSerializeAndDeserialize(new BigDecimal("-5.E-1"), "-18 14A");
        assertSerializeAndDeserialize(new BigDecimal("-5.E-2"), "-18 24A");
        assertSerializeAndDeserialize(new BigDecimal("-1.2E-2"), "-18 287A");
        assertSerializeAndDeserialize(new BigDecimal("-1.11E-2"), "-18 2888A");
        assertSerializeAndDeserialize(new BigDecimal("-1.1E-2"), "-18 288A");
        assertSerializeAndDeserialize(new BigDecimal("-1.0000E-2"), "-18 28A");
        assertSerializeAndDeserialize(new BigDecimal("-1.0E-2"), "-18 28A");
        assertSerializeAndDeserialize(new BigDecimal("-1.E-2"), "-18 28A");
        assertSerializeAndDeserialize(new BigDecimal("-5.E-3"), "-18 34A");
        assertSerializeAndDeserialize(new BigDecimal("-5.E-4"), "-18 44A");
    }

    @Test
    public void shouldSerializeBigDecimalNegativeValuesWithNegativeExponents() {
        assertSerializeAndDeserialize(new BigDecimal("-5.E-1"), "-18 14A");
        assertSerializeAndDeserialize(new BigDecimal("-5.E-2"), "-18 24A");
        assertSerializeAndDeserialize(new BigDecimal("-5.E-3"), "-18 34A");
        assertSerializeAndDeserialize(new BigDecimal("-5.E-4"), "-18 44A");
    }

    @Test
    public void shouldSerializeBigDecimalNegativeValuesWithPositiveExponents() {
        assertSerializeAndDeserialize(new BigDecimal("-8.7654E+3"), "--8 612345A");
        assertSerializeAndDeserialize(new BigDecimal("-7.45E+2"), "--8 7254A");
        assertSerializeAndDeserialize(new BigDecimal("-7.4E+2"), "--8 725A");
    }

    @Test
    public void shouldSortSerializedFormSameAsBigDecimalValues2() {
        assertSortable("+5.E-3", "+1.E-2");
    }

    @Test
    public void shouldSortSerializedFormSameAsBigDecimalValues() {
        assertSortable("+5.E-3",
                       "+1.E-2",
                       "+1.0E-2",
                       "+1.0000E-2",
                       "+1.1E-2",
                       "+1.11E-2",
                       "+1.2E-2",
                       "+5.E-1",
                       "+5.E-2",
                       "+7.3E+2",
                       "+7.4E+2",
                       "+7.45E+2",
                       "+8.7654E+3",
                       "-8.7654E+3",
                       "-7.45E+2",
                       "-7.4E+2",
                       "-7.3E+2",
                       "-5.E0",
                       "-5.E-1",
                       "-5.E-2",
                       "-5.E-4",
                       "-1.2E-2",
                       "-1.11E-2",
                       "-1.1E-2",
                       "-1.0000E-2",
                       "-1.0E-2",
                       "-1.E-2",
                       "-5.E-3",
                       "-123545678901234567890123545678901234567890.123545678901234567890",
                       "123545678901234567890123545678901234567890.123545678901234567890",
                       "-123545678901234567890123545678901234567890.1235456789012345678901",
                       "123545678901234567890123545678901234567890.1235456789012345678901",
                       BIGGEST_NEGATIVE.toString(),
                       BIG_POSITIVE.toString());
    }

    protected void assertSortable( String... decimals ) {
        // Create the big decimals and string values ...
        List<BigDecimal> bigDecimals = new ArrayList<BigDecimal>();
        List<String> stringValues = new ArrayList<String>();
        for (String decimal : decimals) {
            BigDecimal bigDecimal = new BigDecimal(decimal);
            bigDecimals.add(bigDecimal);
            String stringValue = FieldUtil.decimalToString(bigDecimal);
            stringValues.add(stringValue);
        }
        // Order the two lists ...
        Collections.sort(bigDecimals);
        Collections.sort(stringValues);

        // The strings should be in the same order as the big decimals ...
        for (int i = 0; i != bigDecimals.size(); ++i) {
            BigDecimal decimal = bigDecimals.get(i);
            String ser = stringValues.get(i);
            BigDecimal actual = FieldUtil.stringToDecimal(ser);
            if (decimal.compareTo(actual) != 0) {
                // The order of the strings doesn't match the order of the BigDecimal objects, so
                // Compute the actual decimals from the actual string values ...
                List<BigDecimal> actualDecimals = new ArrayList<BigDecimal>();
                for (String stringValue : stringValues) {
                    actualDecimals.add(FieldUtil.stringToDecimal(stringValue));
                }
                // Compare, which will fail ...
                assertThat(actualDecimals, is(bigDecimals));
            }
        }
    }

    protected void assertSerializeAndDeserialize( BigDecimal decimal ) {
        assertSerializeAndDeserialize(decimal, null);
    }

    protected void assertSerializeAndDeserialize( BigDecimal decimal,
                                                  String expectedSerializedForm ) {
        String ser = FieldUtil.decimalToString(decimal);
        BigDecimal decimal2 = FieldUtil.stringToDecimal(ser);
        assertThat(decimal.compareTo(decimal2), is(0));
        String ser2 = FieldUtil.decimalToString(decimal2);
        assertThat(ser2, is(ser));
        if (expectedSerializedForm != null) assertThat(ser, is(expectedSerializedForm));
    }
}
