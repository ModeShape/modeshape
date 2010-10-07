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
import java.util.LinkedList;
import java.util.List;
import javax.jcr.PropertyType;
import javax.jcr.query.RowIterator;
import org.modeshape.graph.query.QueryResults;
import org.modeshape.graph.query.validate.Schemata;

/**
 * 
 */
public class JcrSqlQueryResult extends JcrQueryResult {

    public static final String JCR_SCORE_COLUMN_NAME = "jcr:score";
    public static final String JCR_PATH_COLUMN_NAME = "jcr:path";
    /* The TypeFactory.getTypeName() always returns an uppercased type */
    public static final String JCR_SCORE_COLUMN_TYPE = PropertyType.nameFromValue(PropertyType.DOUBLE).toUpperCase();
    public static final String JCR_PATH_COLUMN_TYPE = PropertyType.nameFromValue(PropertyType.STRING).toUpperCase();

    private final List<String> columnNames;
    private final List<String> columnTypes;

    public JcrSqlQueryResult( JcrQueryContext context,
                              String query,
                              QueryResults graphResults,
                              Schemata schemata ) {
        super(context, query, graphResults, schemata);
        List<String> columnNames = new LinkedList<String>(graphResults.getColumns().getColumnNames());
        List<String> columnTypes = new LinkedList<String>(graphResults.getColumns().getColumnTypes());
        if (!columnNames.contains(JCR_SCORE_COLUMN_NAME)) {
            columnNames.add(0, JCR_SCORE_COLUMN_NAME);
            columnTypes.add(0, JCR_SCORE_COLUMN_TYPE);
        }
        if (!columnNames.contains(JCR_PATH_COLUMN_NAME)) {
            columnNames.add(0, JCR_PATH_COLUMN_NAME);
            columnTypes.add(0, JCR_PATH_COLUMN_TYPE);
        }
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
        return new SingleSelectorQueryResultRowIterator(context, queryStatement, results, tuples.iterator(), numRows);
    }
}
