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
import org.modeshape.graph.Location;
import org.modeshape.graph.query.QueryResults;
import org.modeshape.graph.query.validate.Schemata;

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
        List<String> columnNames = new LinkedList<String>(graphResults.getColumns().getColumnNames());
        List<String> columnTypes = new LinkedList<String>(graphResults.getColumns().getColumnTypes());
        if (graphResults.getColumns().hasFullTextSearchScores() && !columnNames.contains(JCR_SCORE_COLUMN_NAME)) {
            columnNames.add(0, JCR_SCORE_COLUMN_NAME);
            columnTypes.add(0, JCR_SCORE_COLUMN_TYPE);
        }
        columnNames.add(0, JCR_PATH_COLUMN_NAME);
        columnTypes.add(0, JCR_PATH_COLUMN_TYPE);
        this.columnNames = Collections.unmodifiableList(columnNames);
        this.columnTypes = Collections.unmodifiableList(columnTypes);
    }

    /**
     * {@inheritDoc}
     * 
     * @see JcrQueryResult#getColumnNameList()
     */
    @Override
    public List<String> getColumnNameList() {
        return columnNames;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.query.JcrQueryResult#getColumnTypeList()
     */
    @Override
    public java.util.List<String> getColumnTypeList() {
        return columnTypes;
    }

    /**
     * {@inheritDoc}
     * 
     * @see JcrQueryResult#getRows()
     */
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
                Float score = (Float)tuple[iterator.scoreIndex];
                return ((XPathQueryResultRowIterator)iterator).jcrDouble(score);
            }
            return super.getValue(columnName);
        }
    }
}
