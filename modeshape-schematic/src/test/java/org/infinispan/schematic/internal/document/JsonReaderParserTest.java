package org.infinispan.schematic.internal.document;

import java.io.StringReader;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;
import org.infinispan.schematic.document.Binary;
import org.infinispan.schematic.document.Bson;
import org.infinispan.schematic.document.Code;
import org.infinispan.schematic.document.CodeWithScope;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.Null;
import org.infinispan.schematic.document.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
        assert Null.matches(value);
    }

    @Test
    public void shouldParseValueAsNullGivenNullLiteral() throws Exception {
        value = parser("null").parseValue();
        assert Null.matches(value);
    }

    @Test
    public void shouldParseValueAsNullGivenNullLiteralWithIncorrectCase() throws Exception {
        value = parser("nULl").parseValue();
        assert Null.matches(value);
    }

    @Test
    public void shouldParseValueGivenTrueBooleanLiteral() throws Exception {
        value = parser("true").parseValue();
        assert Boolean.TRUE.equals(value);
    }

    @Test
    public void shouldParseValueGivenFalseBooleanLiteral() throws Exception {
        value = parser("false").parseValue();
        assert Boolean.FALSE.equals(value);
    }

    @Test
    public void shouldParseValueGivenTrueBooleanLiteralWithIncorrectCase() throws Exception {
        value = parser("TrUe").parseValue();
        assert Boolean.TRUE.equals(value);
    }

    @Test
    public void shouldParseValueGivenFalseBooleanLiteralWithIncorrectCase() throws Exception {
        value = parser("fAlSE").parseValue();
        assert Boolean.FALSE.equals(value);
    }

    @Test
    public void shouldParseValueGivenIntegerValues() throws Exception {
        assert new Integer(0).equals(parser("0").parseValue());
        assert new Integer(-1).equals(parser("-1").parseValue());
        assert new Integer(1).equals(parser("1").parseValue());
        assert new Integer(123456).equals(parser("123456").parseValue());
        assert new Integer(Integer.MAX_VALUE).equals(parser("" + Integer.MAX_VALUE).parseValue());
        assert new Integer(Integer.MIN_VALUE).equals(parser("" + Integer.MIN_VALUE).parseValue());
    }

    @Test
    public void shouldParseValueGivenLongValues() throws Exception {
        assert new Long(Integer.MAX_VALUE + 1L).equals(parser("" + (Integer.MAX_VALUE + 1L)).parseValue());
        assert new Long(Integer.MIN_VALUE - 1L).equals(parser("" + (Integer.MIN_VALUE - 1L)).parseValue());
        assert new Long(Long.MAX_VALUE).equals(parser("" + Long.MAX_VALUE).parseValue());
        assert new Long(Long.MIN_VALUE).equals(parser("" + Long.MIN_VALUE).parseValue());
    }

    @Test
    public void shouldParseValueGivenDoubleValues() throws Exception {
        assert new Double(0.1d).equals(parser("0.1").parseValue());
        assert new Double(1.0d).equals(parser("1.0").parseValue());
        assert new Double(-1.0d).equals(parser("-1.0").parseValue());
        assert new Double(-1000.0d).equals(parser("-1.0e3").parseValue());
        assert new Double((double)Float.MAX_VALUE + 1f).equals(parser("" + ((double)Float.MAX_VALUE + 1f)).parseValue());
        assert new Double((double)Float.MIN_VALUE - 1f).equals(parser("" + ((double)Float.MIN_VALUE - 1f)).parseValue());
        assert new Double(Double.MAX_VALUE).equals(parser("" + Double.MAX_VALUE).parseValue());
        assert new Double(Double.MIN_VALUE).equals(parser("" + Double.MIN_VALUE).parseValue());
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
        assert obj.equals(value);
    }

    @Test
    public void shouldParseDocumentWithObjectId() throws Exception {
        ObjectId obj = new ObjectId(300, 200, 9, 15);
        value = parser(writer.write(obj)).parseValue();
        assert obj.equals(value);
    }

    @Test
    public void shouldParseDocumentWithDate() throws Exception {
        Date obj = now();
        String dateDoc = writer.write(obj);
        value = parser(dateDoc).parseValue();
        assert value instanceof Date;
    }

    @Test
    public void shouldParseDocumentWithCode() throws Exception {
        Code obj = new Code("foo");
        value = parser(writer.write(obj)).parseValue();
        assert obj.equals(value);
    }

    @Test
    public void shouldParseDocumentWithCodeAndSscope() throws Exception {
        Document scope = (Document)parser("{ \"foo\" : 32 }").parseValue();
        CodeWithScope obj = new CodeWithScope("foo", scope);
        // String str = writer.write(obj);
        // print = true;
        // print(str);
        value = parser(writer.write(obj)).parseValue();
        assert obj.equals(value);
    }

    @Test
    public void shouldParseDocumentWithBinary() throws Exception {
        byte[] bytes = new byte[] {0x13, 0x22, 0x53, 0x00};
        Binary obj = new Binary(Bson.BinaryType.MD5, bytes);
        value = parser(writer.write(obj)).parseValue();
        assert obj.equals(value);
    }

    @Test
    public void shouldParseIsoFormat() throws Exception {
        value = Bson.getDateParsingFormatter().parse("2011-06-14T16:05:11GMT+00:00");
        assert value instanceof Date;
        Date date = (Date)value;
        assert date.after(date(2011, 06, 13));
        assert date.before(date(2011, 06, 16));
    }

    @Test
    public void shouldParseMsDateFormat() throws Exception {
        value = JsonReader.DATE_VALUE_MATCHER.parseValue("\\/Date(2011-06-14T16:05:11GMT+00:00)\\/");
        assert value instanceof Date;
        Date date = (Date)value;
        assert date.after(date(2011, 06, 13));
        assert date.before(date(2011, 06, 16));
    }

    @Test
    public void shouldParseMsDateFormatWithSpaces() throws Exception {
        value = JsonReader.DATE_VALUE_MATCHER.parseValue("\\/Date( 2011-06-14T16:05:11GMT+00:00 )\\/");
        assert value instanceof Date;
        Date date = (Date)value;
        assert date.after(date(2011, 06, 13));
        assert date.before(date(2011, 06, 16));
    }

    @Test
    public void shouldParseEscapedDateFormat() throws Exception {
        value = JsonReader.DATE_VALUE_MATCHER.parseValue("/Date(2011-06-14T16:05:11GMT+00:00)/");
        assert value instanceof Date;
        Date date = (Date)value;
        assert date.after(date(2011, 06, 13));
        assert date.before(date(2011, 06, 16));
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
        assert value instanceof Date;
        Date date = (Date)value;
        assert date.after(date(2011, 06, 13));
        assert date.before(date(2011, 06, 16));
    }

    @Test
    public void shouldEvaluateIsoDateWithGmtTimeZone() throws Exception {
        value = JsonReader.DATE_VALUE_MATCHER.parseValue("2011-06-14T16:05:11GMT+00:00");
        assert value instanceof Date;
        Date date = (Date)value;
        assert date.after(date(2011, 06, 13));
        assert date.before(date(2011, 06, 16));
    }

    @Test
    public void shouldEvaluateIsoDateWithZuluTimeZone() throws Exception {
        value = JsonReader.DATE_VALUE_MATCHER.parseValue("2011-06-14T16:05:11Z");
        assert value instanceof Date;
        Date date = (Date)value;
        assert date.after(date(2011, 06, 13));
        assert date.before(date(2011, 06, 16));
    }

    @Test
    public void shouldParseIsoDateWithZuluTimeZone() throws Exception {
        Date date = now();
        value = JsonReader.DATE_VALUE_MATCHER.parseValue(Bson.getDateFormatter().format(date));
        assert value instanceof Date;
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
            assert Null.matches(actual);
        } else {
            assert value.equals(actual);
        }
    }
}
