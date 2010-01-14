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
package org.modeshape.common.text;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class XmlNameEncoderTest {

    private XmlNameEncoder encoder = new XmlNameEncoder();

    @Before
    public void beforeEach() {
    }

    protected void checkEncoding( String input,
                                  String expected ) {
        String output = this.encoder.encode(input);
        assertThat(output, is(notNullValue()));
        assertEquals(expected, output);
        assertThat(output.length(), is(expected.length()));
        assertThat(output, is(expected));

        checkDecoding(output, input);
    }

    protected void checkForNoEncoding( String input ) {
        String output = this.encoder.encode(input);
        assertThat(output, is(notNullValue()));
        assertEquals(input, output);
        assertThat(output.length(), is(input.length()));
        assertThat(output, is(input));

        checkForNoDecoding(input);
    }

    protected void checkForNoDecoding( String input ) {
        String output = this.encoder.decode(input);
        assertThat(output, is(notNullValue()));
        assertEquals(input, output);
        assertThat(output.length(), is(input.length()));
        assertThat(output, is(input));
    }

    protected void checkDecoding( String input,
                                  String output ) {
        String decoded = this.encoder.decode(input);
        assertEquals(output, decoded);
        assertThat(decoded.length(), is(output.length()));
        assertThat(decoded, is(output));
    }

    @Test
    public void shouldNotEncodeUnderscoreIfNotFollowedByLowercaseX() {
        checkForNoEncoding("Employee_ID");
        checkForNoEncoding("_Employee_");
        checkForNoEncoding("Employee__ID");
    }

    @Test
    public void shouldEncodeUnderscoreIfFollowedByLowercaseX() {
        checkEncoding("Employee_x", "Employee_x005f_x");
        checkEncoding("Employee_x0", "Employee_x005f_x0");
        checkEncoding("Employee_x0022_", "Employee_x005f_x0022_");
    }

    @Test
    public void shouldNotDecodeIfNotValidHexadecimalValue() {
        checkForNoDecoding("_xH013_");
    }

    @Test
    public void shouldNotDecodeIfNotValidEncodedFormat() {
        checkForNoDecoding("_X0022_"); // No lowercase 'x'
        checkForNoDecoding("x0022_"); // No leading '_'
        checkForNoDecoding("_x0022a"); // No trailing '_'
    }

    @Test
    public void shouldNotEncodeDigits() {
        for (char c = '\u0030'; c <= '\u0039'; c++) { // digit
            checkForNoEncoding("Employee" + c + "xyz");
        }
    }

    @Test
    public void shouldNotEncodeAlphabeticCharacters() {
        for (char c = '\u0041'; c <= '\u005a'; c++) { // digit
            checkForNoEncoding("Employee" + c + "xyz");
        }
        for (char c = '\u0061'; c <= '\u007a'; c++) { // digit
            checkForNoEncoding("Employee" + c + "xyz");
        }
    }

    @Test
    public void shouldNotEncodePeriodOrDashOrUnderscoreCharacters() {
        checkForNoEncoding("Employee.xyz");
        checkForNoEncoding("Employee-xyz");
        checkForNoEncoding("Employee:xyz");
        checkForNoEncoding("Employee_abc");
    }

    @Test
    public void shouldDecodeIfCompleteHexadecimal() {
        checkDecoding("Employee_", "Employee_");
        checkDecoding("Employee_x", "Employee_x");
        checkDecoding("Employee_x0", "Employee_x0");
        checkDecoding("Employee_x00", "Employee_x00");
        checkDecoding("Employee_x002", "Employee_x002");
        checkDecoding("Employee_x0022", "Employee_x0022");
        checkDecoding("_", "_");
        checkDecoding("_x", "_x");
        checkDecoding("_x0", "_x0");
        checkDecoding("_x00", "_x00");
        checkDecoding("_x002", "_x002");
        checkDecoding("_x0022", "_x0022");
    }

    @Test
    public void shouldEncodeUnderscoreOnlyWhenFollowedByX() {
        checkEncoding("Employee_xyz", "Employee_x005f_xyz");
        checkEncoding("Employee_ayz", "Employee_ayz");
    }

    @Test
    public void shouldEncodeNonAlphaNumericCharacters() {
        checkEncoding("Employee!xyz", "Employee_x0021_xyz");
        checkEncoding("Employee\"xyz", "Employee_x0022_xyz");
        checkEncoding("Employee#xyz", "Employee_x0023_xyz");
        checkEncoding("Employee$xyz", "Employee_x0024_xyz");
        checkEncoding("Employee%xyz", "Employee_x0025_xyz");
        checkEncoding("Employee&xyz", "Employee_x0026_xyz");
        checkEncoding("Employee'xyz", "Employee_x0027_xyz");
        checkEncoding("Employee(xyz", "Employee_x0028_xyz");
        checkEncoding("Employee)xyz", "Employee_x0029_xyz");
        checkEncoding("Employee*xyz", "Employee_x002a_xyz");
        checkEncoding("Employee+xyz", "Employee_x002b_xyz");
        checkEncoding("Employee,xyz", "Employee_x002c_xyz");
        checkEncoding("Employee/xyz", "Employee_x002f_xyz");
        checkEncoding("Employee\u0B9Bxyz", "Employee_x0b9b_xyz");
    }

}
