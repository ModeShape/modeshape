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

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Types;
import javax.jcr.Value;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.hamcrest.core.IsInstanceOf;
import org.modeshape.jdbc.JcrResultSet;
import org.modeshape.jdbc.JcrType;

/**
 * This object wraps/extends a SQL ResultSet object as Reader object. Once the ResultSet can read as reader then it can be
 * persisted, printed or compared easily without lot of code hassle of walking it every time.
 * <p>
 * PS: remember this is a Reader not InputStream, so all the fields read going to be converted to strings before they returned.
 */
public class ResultSetReader extends StringLineReader {

    ResultSet source = null;
    ResultSetMetaData metadata = null;

    // Number of columns in the result set
    int columnCount = 0;

    // delimiter between the fields while reading each row
    String delimiter = null;

    boolean firstTime = true;
    int[] columnTypes = null;

    private int rowCount = 0;

    private boolean compareColumns = true;

    public ResultSetReader( ResultSet in,
                            String delimiter,
                            boolean compareCols ) {
        this.source = in;
        this.delimiter = delimiter;
        this.compareColumns = compareCols;
    }

    /**
     * @see java.io.Reader#close()
     */
    @Override
    public void close() throws IOException {
        try {
            source.close();
        } catch (SQLException e) {

        }
        super.close();
    }

    /**
     * Get the next line of results from the ResultSet. The first line will be the metadata of the resultset and then followed by
     * the result rows. Each row will be returned as one line.
     * 
     * @return next result line from result set.
     * @throws IOException
     */
    @Override
    protected String nextLine() throws IOException, SQLException {
        if (firstTime) {
            firstTime = false;
            this.metadata = source.getMetaData();
            columnCount = metadata.getColumnCount();
            columnTypes = new int[columnCount];
            for (int i = 0; i < columnCount; i++) {
                columnTypes[i] = metadata.getColumnType(i + 1);
            }
            return resultSetMetaDataToString(metadata, delimiter);
        }

        // if you get here then we are ready to read the results.
        if (source.next()) {
            rowCount++;

            final StringBuilder sb = new StringBuilder();
            // Walk through column values in this row
            for (int col = 1; col <= columnCount; col++) {
                // this does not work when database metadata is being queried
                final Object anObj = source.getObject(col);

                if (compareColumns) compareColumn(col, anObj);

                if (columnTypes[col - 1] == Types.CLOB) {
                    sb.append(anObj != null ? anObj : "null"); //$NON-NLS-1$
                } else if (columnTypes[col - 1] == Types.BLOB) {
                    sb.append(anObj != null ? "BLOB" : "null"); //$NON-NLS-1$ //$NON-NLS-2$
                } else if (columnTypes[col - 1] == Types.SQLXML) {
                    final SQLXML xml = (SQLXML)anObj;
                    sb.append(anObj != null ? prettyPrint(xml) : "null"); //$NON-NLS-1$
                } else {
                    sb.append(anObj != null ? anObj : "null"); //$NON-NLS-1$
                }
                if (col != columnCount) {
                    sb.append(delimiter);
                }
            }
            sb.append("\n"); //$NON-NLS-1$
            return sb.toString();
        }

        return null;
    }

    private void compareColumn( int col,
                                Object objIdx ) throws SQLException {

        String colName = metadata.getColumnName(col);
        Object objName = source.getObject(colName);

        assertThat(objIdx, is(objName));

        if (objIdx == null) return;

        if (source instanceof JcrResultSet) {
            Value v = ((JcrResultSet)source).getValue(col);
            JcrType jcrType = JcrType.typeInfo(v.getType());
            assertThat(objIdx, IsInstanceOf.instanceOf(jcrType.getRepresentationClass()));
        }
    }

    public int getRowCount() {
        return rowCount;
    }

    /**
     * Get the first line from the result set. This is the resultset metadata line where we gather the column names and their
     * types.
     * 
     * @param metadata
     * @param delimiter
     * @return String
     * @throws SQLException
     */
    public static String resultSetMetaDataToString( ResultSetMetaData metadata,
                                                    String delimiter ) throws SQLException {

        StringBuilder sb = new StringBuilder();
        int columnCount = metadata.getColumnCount();

        for (int col = 1; col <= columnCount; col++) {
            String colName = metadata.getColumnName(col);
            String colTypeName = metadata.getColumnTypeName(col);

            /**
             * performing specific checks to make sure these are defined as expected.
             */
            if (colName.equalsIgnoreCase("jcr:score")) {
                JcrType jcrType = JcrType.typeInfo(JcrType.DefaultDataTypes.DOUBLE);
                assertThat(colTypeName, is(jcrType.getJcrName()));
            } else if (colName.equalsIgnoreCase("mode:depth")) {
                JcrType jcrType = JcrType.typeInfo(JcrType.DefaultDataTypes.LONG);
                assertThat(colTypeName, is(jcrType.getJcrName()));
            } else if (colName.equalsIgnoreCase("mode:id")) {
                JcrType jcrType = JcrType.typeInfo(JcrType.DefaultDataTypes.STRING);
                assertThat(colTypeName, is(jcrType.getJcrName()));
            }

            sb.append(colName).append("[") //$NON-NLS-1$
              .append(colTypeName).append("]"); //$NON-NLS-1$
            if (col != columnCount) {
                sb.append(delimiter);
            }
        }
        sb.append("\n"); //$NON-NLS-1$
        return sb.toString();
    }

    public static String prettyPrint( SQLXML xml ) throws SQLException {
        try {
            TransformerFactory transFactory = TransformerFactory.newInstance();
            transFactory.setAttribute("indent-number", new Integer(2)); //$NON-NLS-1$

            Transformer tf = transFactory.newTransformer();
            tf.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes"); //$NON-NLS-1$
            tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");//$NON-NLS-1$
            tf.setOutputProperty(OutputKeys.INDENT, "yes");//$NON-NLS-1$
            tf.setOutputProperty(OutputKeys.METHOD, "xml");//$NON-NLS-1$
            tf.setOutputProperty(OutputKeys.STANDALONE, "yes");//$NON-NLS-1$
            tf.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4"); //$NON-NLS-1$ //$NON-NLS-2$

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            StreamResult xmlOut = new StreamResult(new BufferedOutputStream(out));
            tf.transform(xml.getSource(StreamSource.class), xmlOut);

            return out.toString();
        } catch (Exception e) {
            return xml.getString();
        }
    }
}
