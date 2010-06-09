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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import org.modeshape.graph.Location;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.query.QueryResults;
import org.modeshape.graph.query.QueryResults.Columns;
import org.modeshape.graph.query.model.Column;
import org.modeshape.graph.query.validate.Schemata;
import org.modeshape.graph.query.validate.Schemata.Table;

/**
 * 
 */
public class JcrSqlQueryResult extends JcrQueryResult {

    public static final String JCR_SCORE_COLUMN_NAME = "jcr:score";
    public static final String JCR_PATH_COLUMN_NAME = "jcr:path";

    private final List<String> columnNames;
    private boolean addedScoreColumn;
    private boolean addedPathColumn;

    public JcrSqlQueryResult( JcrQueryContext context,
                              String query,
                              QueryResults graphResults,
                              Schemata schemata ) {
        super(context, query, graphResults, schemata);
        List<String> columnNames = new LinkedList<String>(graphResults.getColumns().getColumnNames());
        if (!columnNames.contains(JCR_SCORE_COLUMN_NAME)) {
            columnNames.add(0, JCR_SCORE_COLUMN_NAME);
            addedScoreColumn = true;
        }
        if (!columnNames.contains(JCR_PATH_COLUMN_NAME)) {
            columnNames.add(0, JCR_PATH_COLUMN_NAME);
            addedPathColumn = true;
        }
        this.columnNames = Collections.unmodifiableList(columnNames);
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

    @Override
    protected List<String> loadColumnTypes( Columns columns ) {
        List<String> types = new ArrayList<String>(columns.getColumnCount() + (addedScoreColumn ? 1 : 0)
                                                   + (addedPathColumn ? 1 : 0));
        String stringtype = PropertyType.nameFromValue(PropertyType.STRING);
        if (addedScoreColumn) types.add(0, stringtype);
        if (addedPathColumn) types.add(0, stringtype);

        for (Column column : columns) {
            String typeName = null;
            Table table = schemata.getTable(column.selectorName());
            if (table != null) {
                Schemata.Column typedColumn = table.getColumn(column.propertyName());
                typeName = typedColumn.getPropertyType();
            }
            if (typeName == null) {
                // Might be fabricated column, so just assume string ...
                typeName = stringtype;
            }
            types.add(typeName);
        }

        return types;
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
        return new JcrSqlQueryResultRowIterator(context, queryStatement, results, tuples.iterator(), numRows);
    }

    protected static class JcrSqlQueryResultRowIterator extends SingleSelectorQueryResultRowIterator {

        protected JcrSqlQueryResultRowIterator( JcrQueryContext context,
                                                String query,
                                                QueryResults results,
                                                Iterator<Object[]> tuples,
                                                long numRows ) {
            super(context, query, results, tuples, numRows);
        }

        @Override
        protected Row createRow( Node node,
                                 Object[] tuple ) {
            return new JcrSqlQueryResultRow(this, node, tuple);
        }

        protected Value jcrPath( Path path ) {
            return context.createValue(PropertyType.PATH, path);
        }

        protected Value jcrScore( Float score ) {
            return context.createValue(PropertyType.DOUBLE, score);
        }
    }

    protected static class JcrSqlQueryResultRow extends SingleSelectorQueryResultRow {
        protected JcrSqlQueryResultRow( SingleSelectorQueryResultRowIterator iterator,
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
                return ((JcrSqlQueryResultRowIterator)iterator).jcrPath(location.getPath());
            }
            if (JCR_SCORE_COLUMN_NAME.equals(columnName)) {
                Float score = (Float)tuple[iterator.scoreIndex];
                return ((JcrSqlQueryResultRowIterator)iterator).jcrScore(score);
            }
            return super.getValue(columnName);
        }
    }
}
