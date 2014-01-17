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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * This class can be used as the base class to write Query tests for integration testing. Just like the scripted one this one
 * should provide all those required flexibility in testing.
 */
public class ResultsComparator {
    protected static String DELIMITER = "    "; //$NON-NLS-1$ 

    private ResultSetReader reader = null;

    public boolean compareColumns = true;

    public void assertResultsSetEquals( final ResultSet resultSet,
                                        final String expected ) {
        assertNotNull(resultSet);
        ResultSetReader reader = new ResultSetReader(resultSet, DELIMITER, compareColumns);
        assertReaderEquals(reader, new StringReader(expected));
    }

    public void assertResultsSetEquals( ResultSet resultSet,
                                        String[] expected ) {
        assertNotNull(resultSet);
        ResultSetReader reader = new ResultSetReader(resultSet, DELIMITER, compareColumns);
        assertReaderEquals(reader, new StringArrayReader(expected));
    }

    private void assertReaderEquals( Reader expected,
                                     Reader reader ) {
        BufferedReader resultReader = new BufferedReader(expected);
        BufferedReader expectedReader = new BufferedReader(reader);
        try {
            compareResults(resultReader, expectedReader);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                resultReader.close();
                expectedReader.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    protected String read( BufferedReader r,
                           boolean casesensitive ) throws IOException {
        StringBuilder result = new StringBuilder();
        String s = null;
        try {
            while ((s = r.readLine()) != null) {
                result.append((casesensitive ? s.trim() : s.trim().toLowerCase()));
                result.append("\n"); //$NON-NLS-1$
            }
        } finally {
            r.close();
        }
        return result.toString();
    }

    protected void compareResults( BufferedReader resultReader,
                                   BufferedReader expectedReader ) throws IOException {
        assertEquals(read(expectedReader, compareCaseSensitive()), read(resultReader, compareCaseSensitive()));
    }

    protected boolean compareCaseSensitive() {
        return true;
    }

    public static void printResults( ResultSet results,
                                     boolean compareColumns ) {
        assertNotNull(results);
        try {
            BufferedReader in = new BufferedReader(new ResultSetReader(results, DELIMITER, compareColumns));
            String line = in.readLine();
            String nextline = null;
            System.out.println("String[] expected = {");
            while (line != null) {
                nextline = in.readLine();
                if (nextline != null) {
                    System.out.println("\"" + line + "\",");
                    line = nextline;
                } else {
                    System.out.println("\"" + line + "\"");
                    line = null;
                }

            }
            System.out.println("};");
            in.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void assertRowCount( int expected ) {
        if (reader != null) {
            assertEquals(expected, this.reader.getRowCount());
        }
    }

    public int getRowCount( ResultSet results ) {

        assertNotNull(results);
        // Count all
        try {
            int count = 0;
            while (results.next()) {
                count++;
            }
            return count;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void print( String msg ) {
        System.out.println(msg);
    }

    public void print( Throwable e ) {
        e.printStackTrace();
    }

}

/**
 * Converts a String Array object into a Reader object.
 */
class StringArrayReader extends StringLineReader {
    String[] source = null;
    int index = 0;

    public StringArrayReader( String[] src ) {
        this.source = src;
    }

    @SuppressWarnings( "unused" )
    @Override
    protected String nextLine() throws IOException {
        if (index < this.source.length) {
            return this.source[index++] + "\n"; //$NON-NLS-1$
        }
        return null;
    }
}
