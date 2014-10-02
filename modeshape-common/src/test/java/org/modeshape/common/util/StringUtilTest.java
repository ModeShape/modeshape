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
package org.modeshape.common.util;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;
import org.junit.Test;
import org.modeshape.common.FixFor;

public class StringUtilTest {

    public void compareSeparatedLines( Object... lines ) {
        ByteArrayOutputStream content = new ByteArrayOutputStream();
        PrintStream stream = new PrintStream(content);
        for (Object line : lines) {
            stream.println(line);
        }
        List<String> actualLines = StringUtil.splitLines(content.toString());
        assertArrayEquals(lines, actualLines.toArray());
    }

    @Test
    public void splitLinesShouldWorkCorrectly() {
        compareSeparatedLines("Line 1", "Line 2", "Line 3", "Line 4");
    }

    @Test( expected = IllegalArgumentException.class )
    public void createStringShouldFailIfNoPatternSupplied() {
        StringUtil.createString(null, (Object[])null);
    }

    @Test
    public void createStringShouldAllowNoParametersSupplied() {
        assertThat(StringUtil.createString("test", (Object[])null), is("test"));
    }

    @Test
    public void createStringShouldCreateStringFromPattern() {
        String pattern = "This {0} is {1} should {2} not {3} last {4}";
        assertEquals("This one is two should three not four last five", StringUtil.createString(pattern,
                                                                                                "one",
                                                                                                "two",
                                                                                                "three",
                                                                                                "four",
                                                                                                "five"));
    }

    @Test( expected = IllegalArgumentException.class )
    public void createStringShouldFailIfTooFewArgumentsSupplied() {
        String pattern = "This {0} is {1} should {2} not {3} last {4}";
        try {
            StringUtil.createString(pattern, "one", "two", "three", "four");
        } catch (IllegalArgumentException err) {
            System.err.println(err);
            throw err;
        }
    }

    @Test( expected = IllegalArgumentException.class )
    public void createStringShouldFailIfTooManyArgumentsSupplied() {
        String pattern = "This {0} is {1} should {2} not {3} last {4}";
        try {
            StringUtil.createString(pattern, "one", "two", "three", "four", "five", "six");
        } catch (IllegalArgumentException err) {
            System.err.println(err);
            throw err;
        }
    }

    @Test
    public void createStringExceptionMessageShouldbeGrammaticallyCorrect() {
        String pattern = "One = {0}";
        try {
            StringUtil.createString(pattern);
        } catch (IllegalArgumentException err) {
            assertThat(err.getMessage().startsWith("0 parameters supplied, but 1 parameter required"), is(true));
        }
        pattern = "One";
        try {
            StringUtil.createString(pattern, "one");
        } catch (IllegalArgumentException err) {
            assertThat(err.getMessage().startsWith("1 parameter supplied, but 0 parameters required"), is(true));
        }
        pattern = "One = {0}, Two = {1}";
        try {
            StringUtil.createString(pattern);
        } catch (IllegalArgumentException err) {
            assertThat(err.getMessage().startsWith("0 parameters supplied, but 2 parameters required"), is(true));
        }
    }

    @Test
    public void setLengthShouldTruncateStringsThatAreTooLong() {
        assertEquals("This is the st", StringUtil.setLength("This is the string", 14, ' '));
    }

    @Test
    @FixFor( "MODE-2322" )
    public void shouldSupportPrimitiveArrays() {
        String text = StringUtil.createString("Error converting \"{2}\" from {0} to a {1}",
                                              byte[].class.getSimpleName(),
                                              "BinaryValue",
                                              new byte[] {-1});
        assertEquals("Error converting \"[-1]\" from byte[] to a BinaryValue", text);
    }

    @Test
    public void setLengthShouldAppendCharacterForStringsThatAreTooShort() {
        assertEquals("This      ", StringUtil.setLength("This", 10, ' '));
    }

    @Test
    public void setLengthShouldNotRemoveLeadingWhitespace() {
        assertEquals(" This     ", StringUtil.setLength(" This", 10, ' '));
        assertEquals("\tThis     ", StringUtil.setLength("\tThis", 10, ' '));
    }

    @Test
    public void setLengthShouldAppendCharacterForEmptyStrings() {
        assertEquals("          ", StringUtil.setLength("", 10, ' '));
    }

    @Test
    public void setLengthShouldAppendCharacterForNullStrings() {
        assertEquals("          ", StringUtil.setLength(null, 10, ' '));
    }

    @Test
    public void setLengthShouldReturnStringsThatAreTheDesiredLength() {
        assertEquals("This is the string", StringUtil.setLength("This is the string", 18, ' '));
    }

    @Test
    public void justifyLeftShouldTruncateStringsThatAreTooLong() {
        assertEquals("This is the st", StringUtil.justifyLeft("This is the string", 14, ' '));
    }

    @Test
    public void justifyLeftShouldAppendCharacterForStringsThatAreTooShort() {
        assertEquals("This      ", StringUtil.justifyLeft("This", 10, ' '));
    }

    @Test
    public void justifyLeftShouldRemoveLeadingWhitespace() {
        assertEquals("This      ", StringUtil.justifyLeft(" This", 10, ' '));
        assertEquals("This      ", StringUtil.justifyLeft("\tThis", 10, ' '));
    }

    @Test
    public void justifyLeftShouldAppendCharacterForEmptyStrings() {
        assertEquals("          ", StringUtil.justifyLeft("", 10, ' '));
    }

    @Test
    public void justifyLeftShouldAppendCharacterForNullStrings() {
        assertEquals("          ", StringUtil.justifyLeft(null, 10, ' '));
    }

    @Test
    public void justifyLeftShouldReturnStringsThatAreTheDesiredLength() {
        assertEquals("This is the string", StringUtil.justifyLeft("This is the string", 18, ' '));
    }

    @Test
    public void justifyRightShouldTruncateStringsThatAreTooLong() {
        assertEquals(" is the string", StringUtil.justifyRight("This is the string", 14, ' '));
    }

    @Test
    public void justifyRightShouldPrependCharacterForStringsThatAreTooShort() {
        assertEquals("      This", StringUtil.justifyRight("This", 10, ' '));
    }

    @Test
    public void justifyRightShouldPrependCharacterForEmptyStrings() {
        assertEquals("          ", StringUtil.justifyRight("", 10, ' '));
    }

    @Test
    public void justifyRightShouldPrependCharacterForNullStrings() {
        assertEquals("          ", StringUtil.justifyRight(null, 10, ' '));
    }

    @Test
    public void justifyRightShouldReturnStringsThatAreTheDesiredLength() {
        assertEquals("This is the string", StringUtil.justifyRight("This is the string", 18, ' '));
    }

    @Test
    public void justifyCenterShouldTruncateStringsThatAreTooLong() {
        assertEquals("This is the st", StringUtil.justifyCenter("This is the string", 14, ' '));
    }

    @Test
    public void justifyCenterShouldPrependAndAppendSameNumberOfCharacterForStringsThatAreTooShortButOfAnEvenLength() {
        assertEquals("   This   ", StringUtil.justifyCenter("This", 10, ' '));
    }

    @Test
    public void justifyCenterShouldPrependOneMoreCharacterThanAppendingForStringsThatAreTooShortButOfAnOddLength() {
        assertEquals("   Thing  ", StringUtil.justifyCenter("Thing", 10, ' '));
    }

    @Test
    public void justifyCenterShouldPrependCharacterForEmptyStrings() {
        assertEquals("          ", StringUtil.justifyCenter("", 10, ' '));
    }

    @Test
    public void justifyCenterShouldPrependCharacterForNullStrings() {
        assertEquals("          ", StringUtil.justifyCenter(null, 10, ' '));
    }

    @Test
    public void justifyCenterShouldReturnStringsThatAreTheDesiredLength() {
        assertEquals("This is the string", StringUtil.justifyCenter("This is the string", 18, ' '));
    }

    @Test
    public void truncateShouldReturnEmptyStringIfNullReferenceIsSupplied() {
        assertThat(StringUtil.truncate(null, 0), is(""));
        assertThat(StringUtil.truncate(null, 1), is(""));
        assertThat(StringUtil.truncate(null, 100), is(""));
    }

    @Test( expected = IllegalArgumentException.class )
    public void truncateShouldNotAllowNegativeLength() {
        StringUtil.truncate("some string", -1);
    }

    @Test
    public void truncateShouldReturnEmptyStringForMaximumLengthOfZero() {
        String str = "This is the string with some text";
        assertThat(StringUtil.truncate(str, 0), is(""));
        assertThat(StringUtil.truncate("", 0), is(""));
        assertThat(StringUtil.truncate(str, 0, "123"), is(""));
        assertThat(StringUtil.truncate("", 0, "123"), is(""));
    }

    @Test
    public void truncateShouldNotTruncateStringShorterThanMaximumLength() {
        String str = "This is the string with some text";
        assertThat(StringUtil.truncate(str, str.length() + 2), is(str));
        assertThat(StringUtil.truncate(str, str.length() + 2, null), is(str));
        assertThat(StringUtil.truncate(str, str.length() + 2, "really long suffix"), is(str));
    }

    @Test
    public void truncateShouldNotTruncateStringWithLengthEqualToMaximumLength() {
        String str = "This is the string with some text";
        assertThat(StringUtil.truncate(str, str.length()), is(str));
        assertThat(StringUtil.truncate(str, str.length(), null), is(str));
        assertThat(StringUtil.truncate(str, str.length(), "really long suffix"), is(str));
    }

    @Test
    public void truncateShouldProperlyTruncateStringWithLengthGreaterThanMaximumLength() {
        String str = "This is the string";
        assertThat(StringUtil.truncate(str, str.length() - 1), is("This is the st..."));
        assertThat(StringUtil.truncate(str, str.length() - 1, null), is("This is the st..."));
        assertThat(StringUtil.truncate(str, str.length() - 1, "X"), is("This is the striX"));
    }

    @Test
    public void truncateShouldProperlyTruncateStringWithLengthGreaterThanMaximumLengthAndMaximumLengthLongerThanPrefixLength() {
        String str = "This is the string";
        assertThat(StringUtil.truncate(str, 2), is(".."));
        assertThat(StringUtil.truncate(str, 2, null), is(".."));
        assertThat(StringUtil.truncate(str, 1, "XX"), is("X"));
    }

    @Test
    public void readShouldReturnEmptyStringForNullInputStream() throws Exception {
        assertThat(StringUtil.read((InputStream)null), is(""));
    }

    @Test
    public void readShouldReturnEmptyStringForNullReader() throws Exception {
        assertThat(StringUtil.read((Reader)null), is(""));
    }

    @Test
    public void readShouldReadInputStreamCorrectlyAndShouldCloseStream() throws Exception {
        // Read content shorter than buffer size ...
        String content = "This is the way to grandma's house.";
        InputStream stream = new ByteArrayInputStream(content.getBytes());
        InputStreamWrapper wrapper = new InputStreamWrapper(stream);
        assertThat(wrapper.isClosed(), is(false));
        assertThat(StringUtil.read(wrapper), is(content));
        assertThat(wrapper.isClosed(), is(true));

        // Read content longer than buffer size ...
        for (int i = 0; i != 10; ++i) {
            content += content; // note this doubles each time!
        }
        stream = new ByteArrayInputStream(content.getBytes());
        wrapper = new InputStreamWrapper(stream);
        assertThat(wrapper.isClosed(), is(false));
        assertThat(StringUtil.read(wrapper), is(content));
        assertThat(wrapper.isClosed(), is(true));
    }

    @Test
    public void readShouldReadReaderCorrectlyAndShouldCloseStream() throws Exception {
        // Read content shorter than buffer size ...
        String content = "This is the way to grandma's house.";
        Reader reader = new StringReader(content);
        ReaderWrapper wrapper = new ReaderWrapper(reader);
        assertThat(wrapper.isClosed(), is(false));
        assertThat(StringUtil.read(wrapper), is(content));
        assertThat(wrapper.isClosed(), is(true));

        // Read content longer than buffer size ...
        for (int i = 0; i != 10; ++i) {
            content += content; // note this doubles each time!
        }
        reader = new StringReader(content);
        wrapper = new ReaderWrapper(reader);
        assertThat(wrapper.isClosed(), is(false));
        assertThat(StringUtil.read(wrapper), is(content));
        assertThat(wrapper.isClosed(), is(true));
    }

    @Test
    public void getStackTraceShouldReturnStackTrace() {
        String msg = "This is the message for a test exception";
        Throwable t = new IllegalArgumentException(msg);
        String trace = StringUtil.getStackTrace(t);
        assertThat(trace, containsString(msg));
        assertThat(trace, containsString(this.getClass().getName()));
    }

    @Test( expected = IllegalArgumentException.class )
    public void normalizeShouldFailIfTextNull() {
        StringUtil.normalize(null);
    }

    @Test
    public void normalizeShouldRemoveLeadingTrailingWhitespace() {
        assertThat(StringUtil.normalize(" \t\n test this \t"), is("test this"));
    }

    @Test
    public void normalizeShouldReduceInternalWhitespace() {
        assertThat(StringUtil.normalize("test \t\n\r this"), is("test this"));
    }

    @Test
    public void normalizeShouldReturnEqualStringIfNothingToNormalize() {
        assertThat(StringUtil.normalize("test this"), is("test this"));
    }

    protected class InputStreamWrapper extends InputStream {

        private boolean closed = false;
        private final InputStream stream;

        protected InputStreamWrapper( InputStream stream ) {
            this.stream = stream;
        }

        public boolean isClosed() {
            return closed;
        }

        @Override
        public int read() throws IOException {
            return stream.read();
        }

        @Override
        public void close() throws IOException {
            stream.close();
            this.closed = true;
        }

    }

    protected class ReaderWrapper extends Reader {

        private boolean closed = false;
        private final Reader reader;

        protected ReaderWrapper( Reader reader ) {
            this.reader = reader;
        }

        public boolean isClosed() {
            return closed;
        }

        @Override
        public void close() throws IOException {
            reader.close();
            this.closed = true;
        }

        @Override
        public int read( char[] cbuf,
                         int off,
                         int len ) throws IOException {
            return reader.read(cbuf, off, len);
        }
    }

}
