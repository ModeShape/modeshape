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
package org.modeshape.web.jcr.rest.client.domain;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import org.modeshape.common.annotation.Immutable;

@Immutable
public class QueryRow {

    private Map<String, String> queryTypes;
    private Map<String, Object> values;

    public QueryRow( Map<String, String> queryTypes,
                     Map<String, Object> values ) {
        super();
        // queryTypes is expected to already be an unmodifiable map
        this.queryTypes = queryTypes;
        this.values = Collections.unmodifiableMap(values);
    }

    public Collection<String> getColumnNames() {
        return queryTypes != null ? queryTypes.keySet() : values.keySet();
    }

    public Object getValue( String columnName ) {
        return values.get(columnName);
    }

    public String getColumnType( String columnName ) {
        return queryTypes.get(columnName);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return values.toString();
    }
}
