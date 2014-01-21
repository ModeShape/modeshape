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
package org.modeshape.jdbc.util;

import java.io.IOException;
import java.io.Reader;
import java.sql.SQLException;

/**
 * Base class for any type of Reader, where data can be read as line by line. The derived classes just need to extend this class
 * and implement the "nextLine()" method to get the full "Reader" functionality.
 */
public abstract class StringLineReader extends Reader {

    // Current line which is being fed to the reader
    String currentLine = null;
    // Current index of where the reading stopped last time
    int currentLineIndex = 0;

    boolean closed = false;

    /**
     * @see java.io.Reader#close()
     */
    @SuppressWarnings( "unused" )
    @Override
    public void close() throws IOException {
        closed = true;
    }

    /**
     * @see java.io.Reader#read(char[], int, int)
     */
    @Override
    public int read( char[] cbuf,
                     int off,
                     int len ) throws IOException {
        if (closed) {
            throw new IllegalStateException("Reader already closed"); //$NON-NLS-1$
        }

        int srcoff = currentLineIndex;
        if (currentLine == null || (currentLine != null && (currentLine.length() - currentLineIndex) <= 0)) {
            try {
                currentLine = nextLine();
            } catch (SQLException e) {
                throw new IOException(e.getLocalizedMessage(), e);
            }
            currentLineIndex = 0;
            srcoff = currentLineIndex;
        }

        // If we have data available then send it.
        if (currentLine != null) {
            // If requested more than one line limit length to one line
            if (len > (currentLine.length() - currentLineIndex)) {
                len = (currentLine.length() - currentLineIndex);
            }

            // Copy the contents to destination.
            System.arraycopy(currentLine.toCharArray(), srcoff, cbuf, off, len);

            // Now move the current index further
            currentLineIndex = currentLineIndex + len;
            return len;
        }
        return -1;
    }

    /**
     * Get the next line of data from the data source.
     * 
     * @return String
     * @throws IOException
     * @throws SQLException
     */
    abstract protected String nextLine() throws IOException, SQLException;
}
