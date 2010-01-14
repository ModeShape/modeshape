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
package org.modeshape.graph.query.validate;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.modeshape.graph.query.validate.Schemata.Column;
import org.modeshape.graph.query.validate.Schemata.Key;

/**
 * 
 */
public class ImmutableKey implements Key {

    private final Set<Column> columns;

    protected ImmutableKey( Iterable<Column> columns ) {
        assert columns != null;
        Set<Column> columnSet = new HashSet<Column>();
        for (Column column : columns) {
            if (column != null) columnSet.add(column);
        }
        assert !columnSet.isEmpty();
        this.columns = Collections.unmodifiableSet(columnSet);
    }

    protected ImmutableKey( Column... columns ) {
        assert columns != null;
        assert columns.length > 0;
        Set<Column> columnSet = new HashSet<Column>();
        for (Column column : columns) {
            if (column != null) columnSet.add(column);
        }
        assert !columnSet.isEmpty();
        this.columns = Collections.unmodifiableSet(columnSet);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.validate.Schemata.Key#getColumns()
     */
    public Set<Column> getColumns() {
        return columns;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.validate.Schemata.Key#hasColumns(org.modeshape.graph.query.validate.Schemata.Column[])
     */
    public boolean hasColumns( Column... columns ) {
        Set<Column> keyColumns = new HashSet<Column>(this.columns);
        for (Column expected : columns) {
            if (!keyColumns.remove(expected)) return false;
        }
        return keyColumns.isEmpty();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.validate.Schemata.Key#hasColumns(java.lang.Iterable)
     */
    public boolean hasColumns( Iterable<Column> columns ) {
        Set<Column> keyColumns = new HashSet<Column>(this.columns);
        for (Column expected : columns) {
            if (!keyColumns.remove(expected)) return false;
        }
        return keyColumns.isEmpty();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        sb.append('[');
        for (Column column : columns) {
            if (first) first = false;
            else sb.append(", ");
            sb.append(column);
        }
        sb.append(']');
        return sb.toString();
    }
}
