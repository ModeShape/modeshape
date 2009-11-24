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
package org.jboss.dna.graph.query.validate;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.jcip.annotations.Immutable;
import org.jboss.dna.graph.query.model.SelectorName;
import org.jboss.dna.graph.query.validate.Schemata.Column;
import org.jboss.dna.graph.query.validate.Schemata.Key;
import org.jboss.dna.graph.query.validate.Schemata.Table;

@Immutable
class ImmutableTable implements Table {
    private final SelectorName name;
    private final Map<String, Column> columnsByName;
    private final List<Column> columns;
    private final Set<Key> keys;

    protected ImmutableTable( SelectorName name,
                              Iterable<Column> columns ) {
        this(name, columns, (Iterable<Column>[])null);
    }

    protected ImmutableTable( SelectorName name,
                              Iterable<Column> columns,
                              Iterable<Column>... keyColumns ) {
        this.name = name;
        // Define the columns ...
        List<Column> columnList = new LinkedList<Column>();
        Map<String, Column> columnMap = new HashMap<String, Column>();
        for (Column column : columns) {
            Column old = columnMap.put(column.getName(), column);
            if (old != null) {
                columnList.set(columnList.indexOf(old), column);
            } else {
                columnList.add(column);
            }
        }
        this.columnsByName = Collections.unmodifiableMap(columnMap);
        this.columns = Collections.unmodifiableList(columnList);
        // Define the keys ...
        if (keyColumns != null) {
            Set<Key> keys = new HashSet<Key>();
            for (Iterable<Column> keyColumnSet : keyColumns) {
                if (keyColumnSet != null) {
                    Key key = new ImmutableKey(keyColumnSet);
                    keys.add(key);
                }
            }
            this.keys = Collections.unmodifiableSet(keys);
        } else {
            this.keys = Collections.emptySet();
        }
    }

    protected ImmutableTable( SelectorName name,
                              Map<String, Column> columnsByName,
                              List<Column> columns,
                              Set<Key> keys ) {
        this.name = name;
        this.columns = columns;
        this.columnsByName = columnsByName;
        this.keys = keys;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.validate.Schemata.Table#getName()
     */
    public SelectorName getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.validate.Schemata.Table#getColumn(java.lang.String)
     */
    public Column getColumn( String name ) {
        return columnsByName.get(name);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.validate.Schemata.Table#getColumns()
     */
    public List<Column> getColumns() {
        return columns;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.validate.Schemata.Table#getColumnsByName()
     */
    public Map<String, Column> getColumnsByName() {
        return columnsByName;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.validate.Schemata.Table#getKeys()
     */
    public Collection<Key> getKeys() {
        return keys;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.validate.Schemata.Table#getKey(org.jboss.dna.graph.query.validate.Schemata.Column[])
     */
    public Key getKey( Column... columns ) {
        for (Key key : keys) {
            if (key.hasColumns(columns)) return key;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.validate.Schemata.Table#getKey(java.lang.Iterable)
     */
    public Key getKey( Iterable<Column> columns ) {
        for (Key key : keys) {
            if (key.hasColumns(columns)) return key;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.validate.Schemata.Table#hasKey(org.jboss.dna.graph.query.validate.Schemata.Column[])
     */
    public boolean hasKey( Column... columns ) {
        return getKey(columns) != null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.validate.Schemata.Table#hasKey(java.lang.Iterable)
     */
    public boolean hasKey( Iterable<Column> columns ) {
        return getKey(columns) != null;
    }

    public ImmutableTable withColumn( String name,
                                      String type ) {
        List<Column> newColumns = new LinkedList<Column>(columns);
        newColumns.add(new ImmutableColumn(name, type));
        return new ImmutableTable(getName(), newColumns);
    }

    public ImmutableTable withColumn( String name,
                                      String type,
                                      boolean fullTextSearchable ) {
        List<Column> newColumns = new LinkedList<Column>(columns);
        newColumns.add(new ImmutableColumn(name, type, fullTextSearchable));
        return new ImmutableTable(getName(), newColumns);
    }

    public ImmutableTable withColumns( Iterable<Column> columns ) {
        List<Column> newColumns = new LinkedList<Column>(this.getColumns());
        for (Column column : columns) {
            newColumns.add(new ImmutableColumn(column.getName(), column.getPropertyType(), column.isFullTextSearchable()));
        }
        return new ImmutableTable(getName(), newColumns);
    }

    public ImmutableTable with( SelectorName name ) {
        return new ImmutableTable(name, columnsByName, columns, keys);
    }

    public ImmutableTable withKey( Iterable<Column> keyColumns ) {
        Set<Key> keys = new HashSet<Key>(this.keys);
        for (Column keyColumn : keyColumns) {
            assert columns.contains(keyColumn);
        }
        if (!keys.add(new ImmutableKey(keyColumns))) return this;
        return new ImmutableTable(name, columnsByName, columns, keys);
    }

    public ImmutableTable withKey( Column... keyColumns ) {
        return withKey(Arrays.asList(keyColumns));
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(name.getName());
        sb.append('(');
        boolean first = true;
        for (Column column : columns) {
            if (first) first = false;
            else sb.append(", ");
            sb.append(column);
        }
        sb.append(')');
        if (!keys.isEmpty()) {
            sb.append(" with keys ");
            first = true;
            for (Key key : keys) {
                if (first) first = false;
                else sb.append(", ");
                sb.append(key);
            }
        }
        return sb.toString();
    }
}
