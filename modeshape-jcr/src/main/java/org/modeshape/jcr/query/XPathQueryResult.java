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
package org.modeshape.jcr.query;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import org.modeshape.jcr.query.QueryResults.Columns;
import org.modeshape.jcr.query.QueryResults.Location;
import org.modeshape.jcr.query.validate.Schemata;

/**
 * {@link QueryResult} implementation for XPath results.
 */
public class XPathQueryResult extends JcrQueryResult {

    public static final String JCR_SCORE_COLUMN_NAME = "jcr:score";
    public static final String JCR_PATH_COLUMN_NAME = "jcr:path";
    /* The TypeFactory.getTypeName() always returns an uppercased type */
    public static final String JCR_SCORE_COLUMN_TYPE = PropertyType.nameFromValue(PropertyType.DOUBLE).toUpperCase();
    public static final String JCR_PATH_COLUMN_TYPE = PropertyType.nameFromValue(PropertyType.STRING).toUpperCase();

    private final List<String> columnNames;
    private final List<String> columnTypes;

    public XPathQueryResult( JcrQueryContext context,
                             String query,
                             QueryResults graphResults,
                             Schemata schemata ) {
        super(context, query, graphResults, schemata);
        Columns resultColumns = graphResults.getColumns();
        List<String> columnNames = new LinkedList<String>(resultColumns.getColumnNames());
        List<String> columnTypes = new LinkedList<String>(resultColumns.getColumnTypes());
        if (resultColumns.hasFullTextSearchScores() && !columnNames.contains(JCR_SCORE_COLUMN_NAME)) {
            columnNames.add(0, JCR_SCORE_COLUMN_NAME);
            columnTypes.add(0, JCR_SCORE_COLUMN_TYPE);
        }
        if (!resultColumns.getColumnNames().contains(JCR_PATH_COLUMN_NAME)) {
            columnNames.add(0, JCR_PATH_COLUMN_NAME);
            columnTypes.add(0, JCR_PATH_COLUMN_TYPE);
        }
        this.columnNames = Collections.unmodifiableList(columnNames);
        this.columnTypes = Collections.unmodifiableList(columnTypes);
    }

    @Override
    public List<String> getColumnNameList() {
        return columnNames;
    }

    @Override
    public java.util.List<String> getColumnTypeList() {
        return columnTypes;
    }

    @Override
    public String[] getSelectorNames() {
        // XPath queries are implicitly selected against all nodes ...
        return new String[] {"nt:base"};
    }

    @Override
    public RowIterator getRows() {
        final int numRows = results.getRowCount();
        final List<Object[]> tuples = results.getTuples();
        return new XPathQueryResultRowIterator(context, queryStatement, results, tuples.iterator(), numRows);
    }

    protected static class XPathQueryResultRowIterator extends SingleSelectorQueryResultRowIterator {

        protected XPathQueryResultRowIterator( JcrQueryContext context,
                                               String query,
                                               QueryResults results,
                                               Iterator<Object[]> tuples,
                                               long numRows ) {
            super(context, query, results, tuples, numRows);
        }

        @Override
        protected Row createRow( Node node,
                                 Object[] tuple ) {
            return new XPathQueryResultRow(this, node, tuple);
        }
    }

    protected static class XPathQueryResultRow extends SingleSelectorQueryResultRow {
        protected XPathQueryResultRow( SingleSelectorQueryResultRowIterator iterator,
                                       Node node,
                                       Object[] tuple ) {
            super(iterator, node, tuple);
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.query.Row#getValue(java.lang.String)
         */
        @Override
        public Value getValue( String columnName ) throws ItemNotFoundException, RepositoryException {
            if (JCR_PATH_COLUMN_NAME.equals(columnName)) {
                Location location = (Location)tuple[iterator.locationIndex];
                return ((XPathQueryResultRowIterator)iterator).jcrPath(location.getPath());
            }
            if (JCR_SCORE_COLUMN_NAME.equals(columnName)) {
                Double score = (Double)tuple[iterator.scoreIndex];
                return ((XPathQueryResultRowIterator)iterator).jcrDouble(score);
            }
            return super.getValue(columnName);
        }
    }
}
