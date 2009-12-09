/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.graph.query.process;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.GraphI18n;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.query.QueryResults.Columns;
import org.jboss.dna.graph.query.model.Column;
import org.jboss.dna.graph.query.model.Constraint;
import org.jboss.dna.graph.query.model.FullTextSearch;
import org.jboss.dna.graph.query.model.Visitors;

/**
 * Defines the columns associated with the results of a query. This definition allows the values to be accessed
 */
@Immutable
public class QueryResultColumns implements Columns {
    private static final long serialVersionUID = 1L;

    protected static final List<Column> NO_COLUMNS = Collections.<Column>emptyList();
    protected static final QueryResultColumns EMPTY = new QueryResultColumns(false, null);

    protected static final String DEFAULT_SELECTOR_NAME = "Results";

    /**
     * Get an empty results column definition.
     * 
     * @return the empty columns definition; never null
     */
    public static QueryResultColumns empty() {
        return EMPTY;
    }

    private final int tupleSize;
    private final List<Column> columns;
    private final List<String> columnNames;
    private final List<String> selectorNames;
    private List<String> tupleValueNames;
    private final Map<String, Column> columnsByName;
    private final Map<String, Integer> columnIndexByColumnName;
    private final Map<String, Integer> locationIndexBySelectorName;
    private final Map<String, Integer> locationIndexByColumnName;
    private final Map<Integer, Integer> locationIndexByColumnIndex;
    private final Map<String, Map<String, Integer>> columnIndexByPropertyNameBySelectorName;
    private final Map<String, Integer> fullTextSearchScoreIndexBySelectorName;

    /**
     * Create a new definition for the query results given the supplied columns.
     * 
     * @param columns the columns that define the results; should never be modified directly
     * @param includeFullTextSearchScores true if room should be made in the tuples for the full-text search scores for each
     *        {@link Location}, or false otherwise
     */
    public QueryResultColumns( List<Column> columns,
                               boolean includeFullTextSearchScores ) {
        this(includeFullTextSearchScores, columns);
        CheckArg.isNotEmpty(columns, "columns");
    }

    /**
     * Create a new definition for the query results given the supplied columns.
     * 
     * @param includeFullTextSearchScores true if room should be made in the tuples for the full-text search scores for each
     *        {@link Location}, or false otherwise
     * @param columns the columns that define the results; should never be modified directly
     */
    protected QueryResultColumns( boolean includeFullTextSearchScores,
                                  List<Column> columns ) {
        this.columns = columns != null ? Collections.<Column>unmodifiableList(columns) : NO_COLUMNS;
        this.columnsByName = new HashMap<String, Column>();
        this.columnIndexByColumnName = new HashMap<String, Integer>();
        Set<String> selectors = new HashSet<String>();
        final int columnCount = this.columns.size();
        Integer selectorIndex = new Integer(columnCount - 1);
        this.locationIndexBySelectorName = new HashMap<String, Integer>();
        this.locationIndexByColumnIndex = new HashMap<Integer, Integer>();
        this.locationIndexByColumnName = new HashMap<String, Integer>();
        this.columnIndexByPropertyNameBySelectorName = new HashMap<String, Map<String, Integer>>();
        List<String> selectorNames = new ArrayList<String>(columnCount);
        List<String> names = new ArrayList<String>(columnCount);
        for (int i = 0, max = this.columns.size(); i != max; ++i) {
            Column column = this.columns.get(i);
            assert column != null;
            String columnName = column.getColumnName();
            assert columnName != null;
            if (columnsByName.put(columnName, column) != null) {
                assert false : "Column names must be unique";
            }
            names.add(columnName);
            columnIndexByColumnName.put(columnName, new Integer(i));
            String selectorName = column.getSelectorName().getName();
            if (selectors.add(selectorName)) {
                selectorNames.add(selectorName);
                selectorIndex = new Integer(selectorIndex.intValue() + 1);
                locationIndexBySelectorName.put(selectorName, selectorIndex);
            }
            locationIndexByColumnIndex.put(new Integer(i), selectorIndex);
            locationIndexByColumnName.put(columnName, selectorIndex);
            // Insert the entry by selector name and property name ...
            Map<String, Integer> byPropertyName = columnIndexByPropertyNameBySelectorName.get(selectorName);
            if (byPropertyName == null) {
                byPropertyName = new HashMap<String, Integer>();
                columnIndexByPropertyNameBySelectorName.put(selectorName, byPropertyName);
            }
            byPropertyName.put(column.getPropertyName(), new Integer(i));
        }
        if (columns != null && selectorNames.isEmpty()) {
            String selectorName = DEFAULT_SELECTOR_NAME;
            selectorNames.add(selectorName);
            locationIndexBySelectorName.put(selectorName, 0);
        }
        this.selectorNames = Collections.unmodifiableList(selectorNames);
        this.columnNames = Collections.unmodifiableList(names);
        if (includeFullTextSearchScores) {
            this.fullTextSearchScoreIndexBySelectorName = new HashMap<String, Integer>();
            int index = columnNames.size() + selectorNames.size();
            for (String selectorName : selectorNames) {
                fullTextSearchScoreIndexBySelectorName.put(selectorName, new Integer(index++));
            }
            this.tupleSize = columnNames.size() + selectorNames.size() + selectorNames.size();
        } else {
            this.fullTextSearchScoreIndexBySelectorName = null;
            this.tupleSize = columnNames.size() + selectorNames.size();
        }
    }

    public static boolean includeFullTextScores( Iterable<Constraint> constraints ) {
        for (Constraint constraint : constraints) {
            if (includeFullTextScores(constraint)) return true;
        }
        return false;
    }

    public static boolean includeFullTextScores( Constraint constraint ) {
        final AtomicBoolean includeFullTextScores = new AtomicBoolean(false);
        if (constraint != null) {
            Visitors.visitAll(constraint, new Visitors.AbstractVisitor() {
                @Override
                public void visit( FullTextSearch obj ) {
                    includeFullTextScores.set(true);
                }
            });
        }
        return includeFullTextScores.get();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.QueryResults.Columns#subSelect(java.util.List)
     */
    public Columns subSelect( List<Column> columns ) {
        return new QueryResultColumns(columns, this);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.QueryResults.Columns#subSelect(org.jboss.dna.graph.query.model.Column[])
     */
    public Columns subSelect( Column... columns ) {
        return new QueryResultColumns(Arrays.asList(columns), this);
    }

    private QueryResultColumns( List<Column> columns,
                                QueryResultColumns wrappedAround ) {
        assert columns != null;
        this.columns = Collections.unmodifiableList(columns);
        this.columnsByName = new HashMap<String, Column>();
        this.columnIndexByColumnName = new HashMap<String, Integer>();
        this.locationIndexBySelectorName = new HashMap<String, Integer>();
        this.locationIndexByColumnIndex = new HashMap<Integer, Integer>();
        this.locationIndexByColumnName = new HashMap<String, Integer>();
        this.columnIndexByPropertyNameBySelectorName = new HashMap<String, Map<String, Integer>>();
        this.selectorNames = new ArrayList<String>(columns.size());
        List<String> names = new ArrayList<String>(columns.size());
        for (int i = 0, max = this.columns.size(); i != max; ++i) {
            Column column = this.columns.get(i);
            assert column != null;
            String columnName = column.getColumnName();
            assert columnName != null;
            if (columnsByName.put(columnName, column) != null) {
                assert false : "Column names must be unique";
            }
            names.add(columnName);
            Integer columnIndex = new Integer(wrappedAround.getColumnIndexForName(columnName));
            columnIndexByColumnName.put(columnName, columnIndex);
            String selectorName = column.getSelectorName().getName();
            if (!selectorNames.contains(selectorName)) selectorNames.add(selectorName);
            Integer selectorIndex = new Integer(wrappedAround.getLocationIndex(selectorName));
            locationIndexBySelectorName.put(selectorName, selectorIndex);
            locationIndexByColumnIndex.put(new Integer(0), selectorIndex);
            locationIndexByColumnName.put(columnName, selectorIndex);
            // Insert the entry by selector name and property name ...
            Map<String, Integer> byPropertyName = columnIndexByPropertyNameBySelectorName.get(selectorName);
            if (byPropertyName == null) {
                byPropertyName = new HashMap<String, Integer>();
                columnIndexByPropertyNameBySelectorName.put(selectorName, byPropertyName);
            }
            byPropertyName.put(column.getPropertyName(), columnIndex);
        }
        if (selectorNames.isEmpty()) {
            String selectorName = DEFAULT_SELECTOR_NAME;
            selectorNames.add(selectorName);
            locationIndexBySelectorName.put(selectorName, 0);
        }
        this.columnNames = Collections.unmodifiableList(names);
        if (wrappedAround.fullTextSearchScoreIndexBySelectorName != null) {
            this.fullTextSearchScoreIndexBySelectorName = new HashMap<String, Integer>();
            int index = columnNames.size() + selectorNames.size();
            for (String selectorName : selectorNames) {
                fullTextSearchScoreIndexBySelectorName.put(selectorName, new Integer(index++));
            }
            this.tupleSize = columnNames.size() + selectorNames.size() + selectorNames.size();
        } else {
            this.fullTextSearchScoreIndexBySelectorName = null;
            this.tupleSize = columnNames.size() + selectorNames.size();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.QueryResults.Columns#getColumns()
     */
    public List<Column> getColumns() {
        return columns;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.QueryResults.Columns#getColumnNames()
     */
    public List<String> getColumnNames() {
        return columnNames;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.QueryResults.Columns#getColumnCount()
     */
    public int getColumnCount() {
        return columns.size();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.QueryResults.Columns#getLocationCount()
     */
    public int getLocationCount() {
        return selectorNames.size();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.QueryResults.Columns#getSelectorNames()
     */
    public List<String> getSelectorNames() {
        return selectorNames;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.QueryResults.Columns#getTupleSize()
     */
    public int getTupleSize() {
        return tupleSize;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.QueryResults.Columns#getTupleValueNames()
     */
    public List<String> getTupleValueNames() {
        if (this.tupleValueNames == null) {
            // This is idempotent, so no need to lock ...
            List<String> results = new ArrayList<String>(getTupleSize());
            // Add the column names ...
            results.addAll(columnNames);
            // Add the location names ...
            for (String selectorName : selectorNames) {
                String name = "Location(" + selectorName + ")";
                results.add(name);
            }
            // Add the full-text search score names ...
            if (fullTextSearchScoreIndexBySelectorName != null) {
                for (String selectorName : selectorNames) {
                    String name = "Score(" + selectorName + ")";
                    results.add(name);
                }
            }
            this.tupleValueNames = results;
        }
        return this.tupleValueNames;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.QueryResults.Columns#getLocationIndexForColumn(int)
     */
    public int getLocationIndexForColumn( int columnIndex ) {
        if (locationIndexByColumnIndex.isEmpty()) return 0;
        Integer result = locationIndexByColumnIndex.get(new Integer(columnIndex));
        if (result == null) {
            throw new IndexOutOfBoundsException(GraphI18n.columnDoesNotExistInQuery.text(columnIndex));
        }
        return result.intValue();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.QueryResults.Columns#getLocationIndexForColumn(java.lang.String)
     */
    public int getLocationIndexForColumn( String columnName ) {
        if (locationIndexByColumnName.isEmpty()) return 0;
        Integer result = locationIndexByColumnName.get(columnName);
        if (result == null) {
            throw new NoSuchElementException(GraphI18n.columnDoesNotExistInQuery.text(columnName));
        }
        return result.intValue();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.QueryResults.Columns#getLocationIndex(java.lang.String)
     */
    public int getLocationIndex( String selectorName ) {
        Integer result = locationIndexBySelectorName.get(selectorName);
        if (result == null) {
            throw new NoSuchElementException(GraphI18n.selectorDoesNotExistInQuery.text(selectorName));
        }
        return result.intValue();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.QueryResults.Columns#hasSelector(java.lang.String)
     */
    public boolean hasSelector( String selectorName ) {
        return locationIndexBySelectorName.containsKey(selectorName);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.QueryResults.Columns#getPropertyNameForColumn(int)
     */
    public String getPropertyNameForColumn( int columnIndex ) {
        return columns.get(columnIndex).getPropertyName();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.QueryResults.Columns#getPropertyNameForColumn(java.lang.String)
     */
    public String getPropertyNameForColumn( String columnName ) {
        Column result = columnsByName.get(columnName);
        if (result == null) {
            throw new NoSuchElementException(GraphI18n.columnDoesNotExistInQuery.text(columnName));
        }
        return result.getPropertyName();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.QueryResults.Columns#getColumnIndexForName(java.lang.String)
     */
    public int getColumnIndexForName( String columnName ) {
        Integer result = columnIndexByColumnName.get(columnName);
        if (result == null) {
            throw new NoSuchElementException(GraphI18n.columnDoesNotExistInQuery.text(columnName));
        }
        return result.intValue();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.QueryResults.Columns#getColumnIndexForProperty(java.lang.String, java.lang.String)
     */
    public int getColumnIndexForProperty( String selectorName,
                                          String propertyName ) {
        Map<String, Integer> byPropertyName = columnIndexByPropertyNameBySelectorName.get(selectorName);
        if (byPropertyName == null) {
            throw new NoSuchElementException(GraphI18n.selectorDoesNotExistInQuery.text(selectorName));
        }
        Integer result = byPropertyName.get(propertyName);
        if (result == null) {
            throw new NoSuchElementException(GraphI18n.propertyOnSelectorIsNotUsedInQuery.text(propertyName, selectorName));
        }
        return result.intValue();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.QueryResults.Columns#getFullTextSearchScoreIndexFor(java.lang.String)
     */
    public int getFullTextSearchScoreIndexFor( String selectorName ) {
        if (fullTextSearchScoreIndexBySelectorName == null) return -1;
        Integer result = fullTextSearchScoreIndexBySelectorName.get(selectorName);
        if (result == null) {
            throw new NoSuchElementException(GraphI18n.selectorDoesNotExistInQuery.text(selectorName));
        }
        return result.intValue();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.QueryResults.Columns#hasFullTextSearchScores()
     */
    public boolean hasFullTextSearchScores() {
        return fullTextSearchScoreIndexBySelectorName != null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.QueryResults.Columns#includes(org.jboss.dna.graph.query.QueryResults.Columns)
     */
    public boolean includes( Columns other ) {
        if (other == this) return true;
        if (other == null) return false;
        return this.getColumns().containsAll(other.getColumns());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.QueryResults.Columns#isUnionCompatible(org.jboss.dna.graph.query.QueryResults.Columns)
     */
    public boolean isUnionCompatible( Columns other ) {
        if (this == other) return true;
        if (other == null) return false;
        if (this.hasFullTextSearchScores() != other.hasFullTextSearchScores()) return false;
        if (this.getColumnCount() != other.getColumnCount()) return false;
        return this.getColumns().containsAll(other.getColumns()) && other.getColumns().containsAll(this.getColumns());
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof QueryResultColumns) {
            QueryResultColumns that = (QueryResultColumns)obj;
            return this.getColumns().equals(that.getColumns());
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(" [");
        boolean first = true;
        for (Column column : getColumns()) {
            if (first) first = false;
            else sb.append(", ");
            sb.append(column);
        }
        sb.append("] => Locations[");
        first = true;
        for (int i = 0, count = getColumnCount(); i != count; ++i) {
            if (first) first = false;
            else sb.append(", ");
            sb.append(getLocationIndexForColumn(i));
        }
        sb.append(']');
        return sb.toString();
    }
}
