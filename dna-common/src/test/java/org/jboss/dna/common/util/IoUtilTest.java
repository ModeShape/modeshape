package org.jboss.dna.common.util;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import org.junit.Before;
import org.junit.Test;

public class IoUtilTest {

    @Before
    public void beforeEach() throws Exception {
    }

    @Test
    public void readBytesShouldReturnEmptyByteArrayForNullInputStream() throws Exception {
        assertThat(IoUtil.readBytes((InputStream)null), is(new byte[] {}));
    }

    @Test
    public void readBytesShouldReadInputStreamCorrectlyAndShouldCloseStream() throws Exception {
        // Read content shorter than buffer size ...
        String content = "This is the way to grandma's house.";
        InputStream stream = new ByteArrayInputStream(content.getBytes());
        InputStreamWrapper wrapper = new InputStreamWrapper(stream);
        assertThat(wrapper.isClosed(), is(false));
        byte[] bytes = IoUtil.readBytes(wrapper);
        String output = new String(bytes);
        assertThat(output, is(content));
        assertThat(wrapper.isClosed(), is(true));

        // Read content longer than buffer size ...
        for (int i = 0; i != 10; ++i) {
            content += content; // note this doubles each time!
        }
        stream = new ByteArrayInputStream(content.getBytes());
        wrapper = new InputStreamWrapper(stream);
        assertThat(wrapper.isClosed(), is(false));
        bytes = IoUtil.readBytes(wrapper);
        output = new String(bytes);
        assertThat(output, is(content));
        assertThat(wrapper.isClosed(), is(true));
    }

    @Test
    public void readShouldReturnEmptyStringForNullInputStream() throws Exception {
        assertThat(IoUtil.read((InputStream)null), is(""));
    }

    @Test
    public void readShouldReturnEmptyStringForNullReader() throws Exception {
        assertThat(IoUtil.read((Reader)null), is(""));
    }

    @Test
    public void readShouldReadInputStreamCorrectlyAndShouldCloseStream() throws Exception {
        // Read content shorter than buffer size ...
        String content = "This is the way to grandma's house.";
        InputStream stream = new ByteArrayInputStream(content.getBytes());
        InputStreamWrapper wrapper = new InputStreamWrapper(stream);
        assertThat(wrapper.isClosed(), is(false));
        assertThat(IoUtil.read(wrapper), is(content));
        assertThat(wrapper.isClosed(), is(true));

        // Read content longer than buffer size ...
        for (int i = 0; i != 10; ++i) {
            content += content; // note this doubles each time!
        }
        stream = new ByteArrayInputStream(content.getBytes());
        wrapper = new InputStreamWrapper(stream);
        assertThat(wrapper.isClosed(), is(false));
        assertThat(IoUtil.read(wrapper), is(content));
        assertThat(wrapper.isClosed(), is(true));
    }

    @Test
    public void readShouldReadReaderCorrectlyAndShouldCloseStream() throws Exception {
        // Read content shorter than buffer size ...
        String content = "This is the way to grandma's house.";
        Reader reader = new StringReader(content);
        ReaderWrapper wrapper = new ReaderWrapper(reader);
        assertThat(wrapper.isClosed(), is(false));
        assertThat(IoUtil.read(wrapper), is(content));
        assertThat(wrapper.isClosed(), is(true));

        // Read content longer than buffer size ...
        for (int i = 0; i != 10; ++i) {
            content += content; // note this doubles each time!
        }
        reader = new StringReader(content);
        wrapper = new ReaderWrapper(reader);
        assertThat(wrapper.isClosed(), is(false));
        assertThat(IoUtil.read(wrapper), is(content));
        assertThat(wrapper.isClosed(), is(true));
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
        public int read( char[] cbuf, int off, int len ) throws IOException {
            return reader.read(cbuf, off, len);
        }
    }

    protected class OutputStreamWrapper extends OutputStream {

        private boolean closed = false;
        private final OutputStream stream;

        protected OutputStreamWrapper( OutputStream stream ) {
            this.stream = stream;
        }

        public boolean isClosed() {
            return closed;
        }

        @Override
        public void close() throws IOException {
            stream.close();
            this.closed = true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void write( int b ) throws IOException {
            stream.write(b);
        }

    }

    protected class WriterWrapper extends Writer {

        private boolean closed = false;
        private final Writer writer;

        protected WriterWrapper( Writer writer ) {
            this.writer = writer;
        }

        public boolean isClosed() {
            return closed;
        }

        @Override
        public void close() throws IOException {
            writer.close();
            this.closed = true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void flush() throws IOException {
            writer.flush();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void write( char[] cbuf, int off, int len ) throws IOException {
            writer.write(cbuf, off, len);
        }

    }

}
