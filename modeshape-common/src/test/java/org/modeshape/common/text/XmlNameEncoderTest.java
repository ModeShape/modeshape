/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.common.text;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import org.junit.Test;

/**
 * Unit test for {@link XmlNameEncoder}
 *
 * @author Randall Hauch
 * @author Horia Chiorean
 */
public class XmlNameEncoderTest {

    private XmlNameEncoder encoder = new XmlNameEncoder();

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

    @Test
    public void shouldEncodeIllegalStartCharacter() {
        checkEncoding("042b4500-a8bc-4b79-8af0-59fb408ecfa5", "_x0030_42b4500-a8bc-4b79-8af0-59fb408ecfa5");
        checkEncoding("-42b4500-a8bc-4b79-8af0-59fb408ecfa5", "_x002d_42b4500-a8bc-4b79-8af0-59fb408ecfa5");
        checkEncoding(".42b4500-a8bc-4b79-8af0-59fb408ecfa5", "_x002e_42b4500-a8bc-4b79-8af0-59fb408ecfa5");
    }

    @Test
    public void shouldDecodeIllegalStartCharacter() {
        checkDecoding("_x0030_42b4500-a8bc-4b79-8af0-59fb408ecfa5", "042b4500-a8bc-4b79-8af0-59fb408ecfa5");
        checkDecoding("_x002d_42b4500-a8bc-4b79-8af0-59fb408ecfa5", "-42b4500-a8bc-4b79-8af0-59fb408ecfa5");
        checkDecoding("_x002e_42b4500-a8bc-4b79-8af0-59fb408ecfa5", ".42b4500-a8bc-4b79-8af0-59fb408ecfa5");
    }

}
