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

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Utility for working with Lucene field values.
 */
public class FieldUtil {

    /**
     * Creates a canonical string representation of the supplied {@link BigDecimal} value, whereby all string representations are
     * lexicographically sortable. This makes it possible to store the wide range of values that can be represented by BigDecimal,
     * while still enabling sorting and range queries.
     * <p>
     * This canonical form represents all decimal values using a prescribed format, which is based upon <a
     * href="http://www.mail-archive.com/java-user@lucene.apache.org/msg23632.html">Steven Rowe's suggestion</a> but with
     * modifications to handle variable-length exponents (per his suggestion in the last sentence), use spaces between fields on
     * where required (for minimal length), and utilize an optimized (e.g., shorter) form when the value is '0' or the exponent is
     * '0'. Thus, this format contains only digits (e.g., '0'..'9') and the '-' character.
     * 
     * <pre>
     *     &lt;significand-sign>&lt;exponent-sign>&lt;exponent-length> &lt;exponent>&lt;significand>
     * </pre>
     * 
     * where:
     * <ul>
     * <li>the <b>significand</b> is the part of the number containing the significant figures, and is a (big) integer value
     * obtained from the BigDecimal using {@link BigDecimal#unscaledValue()};</li>
     * <li>the <b>exponent</b> is the integer used to define the number of factors of 10 that are applied to the significand,
     * obtained by computing <code>value.precision() - value.scale() - 1</code>;</li>
     * </ul>
     * Thus the fields are defined as:
     * <ul>
     * <li>the <code>&lt;significand-sign></code> is '-' if the significand is negative, '0' if equal to zero, or '1' if positive;
     * </li>
     * <li>the <code>&lt;exponent-sign></code> is '-' if the exponent is negative, '0' if equal to zero, or '1' if positive; if
     * '0', then the <code>&lt;exponent-length></code> and <code>&lt;exponent></code> fields are not written;</li>
     * <li>the <code>&lt;exponent-length></code> is the postive value representing the length of the <code>&lt;exponent></code>,
     * and is not included when the <code>&lt;exponent-sign></code> is '0';</li>
     * <li>the <code>&lt;exponent></code> is the integer used to define the number of factors of 10 that are applied to the
     * significand, obtained by computing <code>value.precision() - value.scale() - 1</code>;</li>
     * <li>the <code>&lt;significand></code> is the part of the number containing the significant figures, and is a (big) integer
     * value obtained from the BigDecimal using {@link BigDecimal#unscaledValue()};</li>
     * </ul>
     * In the case of a negative significand, the <code>&lt;significand></code> field is negated such that each digit is replaced
     * with <code>(base - digit - 1)</code> and appended by 'A' (which is greater than all other digits) to ensure that
     * significands with greater precision are ordered before those that share significand prefixes but have lesser precision.
     * </p>
     * <p>
     * Thus, the format for a negative BigDecimal value becomes:
     * 
     * <pre>
     *     -&lt;reversed-exponent-sign>&lt;negated-exponent-length> &lt;negated-exponent>&lt;significand>&lt;sentinel>
     * </pre>
     * 
     * where the <code>&lt;sentinel></code> is always 'A'. Note that the exponent length field is also negated.
     * </p>
     * <h3>Examples</h3>
     * <p>
     * Here are several examples that show BigDecimal values and their corresponding canonical string representation:
     * 
     * <pre>
     *    +5.E-3     => 1-1 65
     *    +1.E-2     => 1-1 71
     *    +1.0E-2    => 1-1 71
     *    +1.0000E-2 => 1-1 71
     *    +1.1E-2    => 1-1 711
     *    +1.11E-2   => 1-1 7111
     *    +1.2E-2    => 1-1 712
     *    +5.E-2     => 1-1 75
     *    +7.3E+2    => 111 273
     *    +7.4E+2    => 111 274
     *    +7.45E+2   => 111 2745
     *    +8.7654E+3 => 111 387654
     * </pre>
     * 
     * Here is how a BigDecimal value of {@link BigDecimal#ZERO zero} is represented:
     * 
     * <pre>
     *     0.0E0     => 0
     * </pre>
     * 
     * BigDecimal values with an exponent of '0' are represented as follows:
     * 
     * <pre>
     *    +1.2E0     => 1012
     *    -1.2E0     => -087A
     * </pre>
     * 
     * And here are some negative value examples:
     * 
     * <pre>
     *    -8.7654E+3 => --8 612345A
     *    -7.45E+2   => --8 7254A
     *    -7.4E+2    => --8 725A
     *    -7.3E+2    => --8 726A
     *    -5.E-2     => -18 24A
     *    -1.2E-2    => -18 287A
     *    -1.11E-2   => -18 2888A
     *    -1.1E-2    => -18 288A
     *    -1.0000E-2 => -18 28A
     *    -1.0E-2    => -18 28A
     *    -1.E-2     => -18 28A
     *    -5.E-3     => -18 34A
     *    -5.E-4     => -18 44A
     * </pre>
     * 
     * </p>
     * <p>
     * This canonical form is valid for all values of {@link BigDecimal}.
     * </p>
     * 
     * @param value the value to be converted into its canonical form; may not be null
     * @return the canonical string representation; never null or empty
     * @see #stringToDecimal(String)
     */
    public static String decimalToString( BigDecimal value ) {
        StringBuilder sb = new StringBuilder();
        boolean negate = false;
        // <sigificand-sign> field
        switch (value.signum()) {
            case -1:
                sb.append('-');
                negate = true;
                break;
            case 1:
                sb.append('1');
                break;
            default:
                return "0";
        }

        // <exponent-sign>, <exponent-length> and <exponent> fields
        long exponent = value.precision() - value.scale() - 1;
        if (exponent == 0) {
            sb.append('0');
        } else {
            if (negate) exponent = -exponent;
            String exponentField = String.valueOf(Math.abs(exponent));
            int length = exponentField.length();
            char sign = exponent > 0 ? '1' : '-';
            if (exponent < 0) exponentField = negate(exponentField);
            // <exponent-length>
            String lengthField = String.valueOf(length);
            if (negate) lengthField = negate(lengthField);
            sb.append(sign).append(lengthField).append(' ').append(exponentField);
        }

        // <significand>
        if (negate) value = value.negate();
        StringBuilder significand = new StringBuilder(value.unscaledValue().toString());
        removeTralingZeros(significand);

        // Append the significand (and the sentinel character)...
        sb.append(negate ? negate(significand).append('A') : significand);

        return sb.toString();
    }

    /**
     * Converts the canonical string representation of a {@link BigDecimal} value into the object form.
     * <p>
     * See {@link #decimalToString(BigDecimal)} to documentation of the canonical form.
     * </p>
     * 
     * @param value the canonical string representation; may not be null or empty
     * @return the BigDecimal representation; never null
     * @see #decimalToString(BigDecimal)
     */
    public static BigDecimal stringToDecimal( String value ) {
        assert value != null;
        assert value.length() != 0;
        if ("0".equals(value)) return BigDecimal.ZERO;

        boolean negate = false;
        if (value.charAt(0) == '-') {
            // Negative, so remove the trailing sentinel ...
            assert value.charAt(value.length() - 1) == 'A';
            value = value.substring(0, value.length() - 1);
            negate = true;
        }

        // <exponent-sign>, <exponent-length> and <exponent> fields
        long exponent = 0L;
        boolean negateExponent = false;
        int endIndex = 0;
        switch (value.charAt(1)) {
            case '0':
                value = value.substring(2);
                break;
            case '-':
                negateExponent = true;
                // $FALL-THROUGH$
            case '1':
            default:
                // Read in the <exponent-length>
                int indexOfSpace = value.indexOf(' ', 2);
                String lengthField = value.substring(2, indexOfSpace);
                if (negate) lengthField = negate(lengthField);
                int lengthOfExponent = Integer.parseInt(lengthField);
                // Read in the <exponent> (after the space) ...
                int startIndex = indexOfSpace + 1;
                endIndex = startIndex + lengthOfExponent;
                String exponentField = value.substring(startIndex, endIndex);
                exponent = Long.parseLong(negateExponent ? negate(exponentField) : exponentField);
                if (negate) negateExponent = !negateExponent;
                if (negateExponent) exponent = -exponent;
                value = value.substring(endIndex);
        }

        // <significand>
        if (negate) {
            value = negate(value);
        }
        BigInteger significand = new BigInteger(value);
        int scale = (int)(value.length() - exponent - 1);

        // Now create the result ...
        return new BigDecimal(negate ? significand.negate() : significand, scale);
    }

    /**
     * Compute the "negated" string, which replaces the digits (0 becomes 9, 1 becomes 8, ... and 9 becomes 0).
     * 
     * @param value the input string; may not be null
     * @return the negated string; never null
     * @see #negate(StringBuilder)
     */
    protected static String negate( String value ) {
        return negate(new StringBuilder(value)).toString();
    }

    /**
     * Compute the "negated" string, which replaces the digits (0 becomes 9, 1 becomes 8, ... and 9 becomes 0).
     * 
     * @param value the input string; may not be null
     * @return the negated string; never null
     * @see #negate(String)
     */
    protected static StringBuilder negate( StringBuilder value ) {
        for (int i = 0, len = value.length(); i != len; ++i) {
            char c = value.charAt(i);
            if (c == ' ' || c == '-') continue;
            value.setCharAt(i, (char)('9' - c + '0'));
        }
        return value;
    }

    /**
     * Utility to remove the trailing 0's.
     * 
     * @param sb the input string builder; may not be null
     */
    protected static void removeTralingZeros( StringBuilder sb ) {
        int endIndex = sb.length();
        if (endIndex > 0) {
            --endIndex;
            int index = endIndex;
            while (sb.charAt(index) == '0') {
                --index;
            }
            if (index < endIndex) sb.delete(index + 1, endIndex + 1);
        }
    }

    /* Prevent instantiation */
    private FieldUtil() {
    }
}
