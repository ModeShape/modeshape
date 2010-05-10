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
package org.modeshape.jcr.api.query;

import javax.jcr.PropertyType;

/**
 * Replicates some of the methods introduced in JCR 2.0, but also provides an extension that allows accessing the JCR
 * {@link PropertyType} for each of the columns.
 */
public interface QueryResult extends javax.jcr.query.QueryResult {

    /**
     * Returns an array of the {@link PropertyType} name for each of the columns in this result.
     * 
     * @return the array of property type names; never null, never has null elements, and the size always matches
     *         {@link QueryResult#getColumnNames()}.
     */
    public String[] getColumnTypes();

    /**
     * Returns an array of the name of the selector (i.e., table) from which each column originates. Note that the table name will
     * be "" for all fabricated columns.
     * 
     * @return the array of table names; never null, and the size always matches {@link QueryResult#getColumnNames()}
     * @since JCR 2.0
     */
    public String[] getSelectorNames();
}
