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
package org.modeshape.schematic.internal.document;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.io.StringReader;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.schematic.document.Binary;
import org.modeshape.schematic.document.Bson;
import org.modeshape.schematic.document.Code;
import org.modeshape.schematic.document.CodeWithScope;
import org.modeshape.schematic.document.Document;
import org.modeshape.schematic.document.Null;
import org.modeshape.schematic.document.ObjectId;

public class JsonReaderParserTest {

    protected boolean print;
    protected Object value;
    protected Document bson;
    protected JsonWriter writer;

    @Before
    public void beforeTest() {
        print = false;
        writer = new CompactJsonWriter();
    }

    @After
    public void afterTest() {
        value = null;
        bson = null;
    }

    protected JsonReader.Parser parser( String string ) {
        return new JsonReader.Parser(new JsonReader.Tokenizer(new StringReader(string)), JsonReader.VALUE_FACTORY,
                                     JsonReader.DATE_VALUE_MATCHER);
    }

    @Test
    public void shouldParseValueAsNullGivenNoContent() throws Exception {
        value = parser("").parseValue();
        assertTrue(Null.matches(value));
    }

    @Test
    public void shouldParseValueAsNullGivenNullLiteral() throws Exception {
        value = parser("null").parseValue();
        assertTrue(Null.matches(value));
    }

    @Test
    public void shouldParseValueAsNullGivenNullLiteralWithIncorrectCase() throws Exception {
        value = parser("nULl").parseValue();
        assertTrue(Null.matches(value));
    }

    @Test
    public void shouldParseValueGivenTrueBooleanLiteral() throws Exception {
        value = parser("true").parseValue();
        assertTrue(Boolean.TRUE.equals(value));
    }

    @Test
    public void shouldParseValueGivenFalseBooleanLiteral() throws Exception {
        value = parser("false").parseValue();
        assertTrue(Boolean.FALSE.equals(value));
    }

    @Test
    public void shouldParseValueGivenTrueBooleanLiteralWithIncorrectCase() throws Exception {
        value = parser("TrUe").parseValue();
        assertTrue(Boolean.TRUE.equals(value));
    }

    @Test
    public void shouldParseValueGivenFalseBooleanLiteralWithIncorrectCase() throws Exception {
        value = parser("fAlSE").parseValue();
        assertTrue(Boolean.FALSE.equals(value));
    }

    @Test
    public void shouldParseValueGivenIntegerValues() throws Exception {
        assertEquals(0, parser("0").parseValue());
        assertEquals(-1, parser("-1").parseValue());
        assertEquals(1, parser("1").parseValue());
        assertEquals(123456, parser("123456").parseValue());
        assertEquals(Integer.MAX_VALUE, parser("" + Integer.MAX_VALUE).parseValue());
        assertEquals(Integer.MIN_VALUE, parser("" + Integer.MIN_VALUE).parseValue());
    }

    @Test
    public void shouldParseValueGivenLongValues() throws Exception {
        assertEquals(Integer.MAX_VALUE + 1L, parser("" + (Integer.MAX_VALUE + 1L)).parseValue());
        assertEquals(Integer.MIN_VALUE - 1L, parser("" + (Integer.MIN_VALUE - 1L)).parseValue());
        assertEquals(Long.MAX_VALUE, parser("" + Long.MAX_VALUE).parseValue());
        assertEquals(Long.MIN_VALUE, parser("" + Long.MIN_VALUE).parseValue());
    }

    @Test
    public void shouldParseValueGivenDoubleValues() throws Exception {
        assertEquals(0.1d, parser("0.1").parseValue());
        assertEquals(1.0d, parser("1.0").parseValue());
        assertEquals(-1.0d, parser("-1.0").parseValue());
        assertEquals(-1000.0d, parser("-1.0e3").parseValue());
        assertEquals((double) Float.MAX_VALUE + 1f, parser("" + ((double) Float.MAX_VALUE + 1f)).parseValue());
        assertEquals((double) Float.MIN_VALUE - 1f, parser("" + ((double) Float.MIN_VALUE - 1f)).parseValue());
        assertEquals(Double.MAX_VALUE, parser("" + Double.MAX_VALUE).parseValue());
        assertEquals(Double.MIN_VALUE, parser("" + Double.MIN_VALUE).parseValue());
    }

    @Test
    public void shouldParseDocumentWithOneField() throws Exception {
        bson = (Document)parser("{ \"foo\" : 32 }").parseValue();
        assertField("foo", 32);
    }

    @Test
    public void shouldParseDocumentWithTwoFields() throws Exception {
        bson = (Document)parser("{ \"foo\" : 32 , \"bar\" : \"baz\" }").parseValue();
        assertField("foo", 32);
        assertField("bar", "baz");
    }

    @Test
    public void shouldParseDocumentWithThreeFields() throws Exception {
        bson = (Document)parser("{ \"foo\" : 32 , \"bar\" : \"baz\", \"bom\" : true }").parseValue();
        assertField("foo", 32);
        assertField("bar", "baz");
        assertField("bom", true);
    }

    @Test
    public void shouldParseDocumentWithNestedDocument() throws Exception {
        bson = (Document)parser("{ \"foo\" : 32 , \"nested\" : { \"bar\" : \"baz\", \"bom\" : true }}").parseValue();
        assertField("foo", 32);
        bson = bson.getDocument("nested");
        assertField("bar", "baz");
        assertField("bom", true);
    }

    @Test
    public void shouldParseDocumentWithExtraTrailingFieldDelimiter() throws Exception {
        bson = (Document)parser("{ \"foo\" : 32 , \"bar\" : \"baz\",  }").parseValue();
        assertField("foo", 32);
        assertField("bar", "baz");
    }

    @Test
    public void shouldParseDocumentWithExtraFieldDelimiters() throws Exception {
        bson = (Document)parser("{ \"foo\" : 32 , , \"bar\" : \"baz\",,  }").parseValue();
        assertField("foo", 32);
        assertField("bar", "baz");
    }

    @Test
    public void shouldParseDocumentWithUuid() throws Exception {
        UUID obj = UUID.randomUUID();
        value = parser(writer.write(obj)).parseValue();
        assertEquals(obj, value);
    }

    @Test
    public void shouldParseDocumentWithObjectId() throws Exception {
        ObjectId obj = new ObjectId(300, 200, 9, 15);
        value = parser(writer.write(obj)).parseValue();
        assertEquals(obj, value);
    }

    @Test
    public void shouldParseDocumentWithDate() throws Exception {
        Date obj = now();
        String dateDoc = writer.write(obj);
        value = parser(dateDoc).parseValue();
        assertTrue(value instanceof Date);
    }

    @Test
    public void shouldParseDocumentWithCode() throws Exception {
        Code obj = new Code("foo");
        value = parser(writer.write(obj)).parseValue();
        assertEquals(obj, value);
    }

    @Test
    public void shouldParseDocumentWithCodeAndSscope() throws Exception {
        Document scope = (Document)parser("{ \"foo\" : 32 }").parseValue();
        CodeWithScope obj = new CodeWithScope("foo", scope);
        // String str = writer.write(obj);
        // print = true;
        // print(str);
        value = parser(writer.write(obj)).parseValue();
        assertEquals(obj, value);
    }

    @Test
    public void shouldParseDocumentWithBinary() throws Exception {
        byte[] bytes = new byte[] {0x13, 0x22, 0x53, 0x00};
        Binary obj = new Binary(Bson.BinaryType.MD5, bytes);
        value = parser(writer.write(obj)).parseValue();
        assertEquals(obj, value);
    }

    @Test
    public void shouldParseIsoFormat() throws Exception {
        value = Bson.getDateParsingFormatter().parse("2011-06-14T16:05:11GMT+00:00");
        Date date = (Date)value;
        assertTrue(date.after(date(2011, 06, 13)));
        assertTrue(date.before(date(2011, 06, 16)));
    }

    @Test
    public void shouldParseMsDateFormat() throws Exception {
        value = JsonReader.DATE_VALUE_MATCHER.parseValue("\\/Date(2011-06-14T16:05:11GMT+00:00)\\/");
        assertTrue(value instanceof Date);
        Date date = (Date)value;
        assertTrue(date.after(date(2011, 06, 13)));
        assertTrue(date.before(date(2011, 06, 16)));
    }

    @Test
    public void shouldParseMsDateFormatWithSpaces() throws Exception {
        value = JsonReader.DATE_VALUE_MATCHER.parseValue("\\/Date( 2011-06-14T16:05:11GMT+00:00 )\\/");
        assertTrue(value instanceof Date);
        Date date = (Date)value;
        assertTrue(date.after(date(2011, 06, 13)));
        assertTrue(date.before(date(2011, 06, 16)));
    }

    @Test
    public void shouldParseEscapedDateFormat() throws Exception {
        value = JsonReader.DATE_VALUE_MATCHER.parseValue("/Date(2011-06-14T16:05:11GMT+00:00)/");
        assertTrue(value instanceof Date);
        Date date = (Date)value;
        assertTrue(date.after(date(2011, 06, 13)));
        assertTrue(date.before(date(2011, 06, 16)));
    }

    protected Date date( int year,
                         int month,
                         int day ) {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.setTimeZone(TimeZone.getTimeZone("GMT"));
        cal.set(year, month - 1, day);
        return cal.getTime();
    }

    @Test
    public void shouldParseEscapedDateFormatWithSpaces() throws Exception {
        value = JsonReader.DATE_VALUE_MATCHER.parseValue("/Date( 2011-06-14T16:05:11GMT+00:00 )/");
        assertTrue(value instanceof Date);
        Date date = (Date)value;
        assertTrue(date.after(date(2011, 06, 13)));
        assertTrue(date.before(date(2011, 06, 16)));
    }

    @Test
    public void shouldEvaluateIsoDateWithGmtTimeZone() throws Exception {
        value = JsonReader.DATE_VALUE_MATCHER.parseValue("2011-06-14T16:05:11GMT+00:00");
        assertTrue(value instanceof Date);
        Date date = (Date)value;
        assertTrue(date.after(date(2011, 06, 13)));
        assertTrue(date.before(date(2011, 06, 16)));
    }

    @Test
    public void shouldEvaluateIsoDateWithZuluTimeZone() throws Exception {
        value = JsonReader.DATE_VALUE_MATCHER.parseValue("2011-06-14T16:05:11Z");
        assertTrue(value instanceof Date);
        Date date = (Date)value;
        assertTrue(date.after(date(2011, 06, 13)));
        assertTrue(date.before(date(2011, 06, 16)));
    }

    @Test
    public void shouldParseIsoDateWithZuluTimeZone() throws Exception {
        Date date = now();
        value = JsonReader.DATE_VALUE_MATCHER.parseValue(Bson.getDateFormatter().format(date));
        assertTrue(value instanceof Date);
    }
    
    @Test
    public void dateMatcherShouldPreserveBackslashEscapeChars() throws Exception {
        String value1 = "one\\backslash";
        assertEquals(value1, JsonReader.DATE_VALUE_MATCHER.parseValue(value1));
        String value2 = "two\\\\backslashes";
        assertEquals(value2, JsonReader.DATE_VALUE_MATCHER.parseValue(value2));
        String value3 = "three\\\\\\backslashes";
        assertEquals(value3, JsonReader.DATE_VALUE_MATCHER.parseValue(value3));
    }

    protected void print( Object object ) {
        if (print) {
            System.out.println(object);
            System.out.flush();
        }
    }

    protected Date now() {
        return new Date();
    }

    protected void assertField( String name,
                                Object value ) {
        Object actual = bson.get(name);
        if (value == null) {
            assertTrue(Null.matches(actual));
        } else {
            assertEquals(value, actual);
        }
    }
}
