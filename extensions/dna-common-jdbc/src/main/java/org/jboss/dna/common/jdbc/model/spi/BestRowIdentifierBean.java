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
package org.jboss.dna.common.jdbc.model.spi;

import java.util.Set;
import java.util.HashSet;
import org.jboss.dna.common.jdbc.model.api.BestRowIdentifier;
import org.jboss.dna.common.jdbc.model.api.BestRowIdentifierScopeType;
import org.jboss.dna.common.jdbc.model.api.Column;

/**
 * Provides table's best row identifies specific metadata.
 * 
 * @author <a href="mailto:litsenko_sergey@yahoo.com">Sergiy Litsenko</a>
 */
public class BestRowIdentifierBean extends CoreMetaDataBean implements BestRowIdentifier {
    private static final long serialVersionUID = -6031029115876541704L;
    private BestRowIdentifierScopeType scopeType;
    private Set<Column> columns = new HashSet<Column>();

    /**
     * Default constructor
     */
    public BestRowIdentifierBean() {
    }

    /**
     * Return the scope of best row identifier
     * 
     * @return the scope of best row identifier
     */
    public BestRowIdentifierScopeType getScopeType() {
        return scopeType;
    }

    /**
     * Sets the scope of best row identifier
     * 
     * @param scopeType the scope of best row identifier
     */
    public void setScopeType( BestRowIdentifierScopeType scopeType ) {
        this.scopeType = scopeType;
    }

    /**
     * Retrieves best row identifier columns
     * 
     * @return best row identifier columns
     */
    public Set<Column> getColumns() {
        return columns;
    }

    /**
     * Adds column to the best row identifier
     * 
     * @param column the column that part of best row identifier
     */
    public void addColumn( Column column ) {
        columns.add(column);
    }

    /**
     * Deletes column from the best row identifier
     * 
     * @param column the column that no longer part of best row identifier
     */
    public void deleteColumn( Column column ) {
        columns.remove(column);
    }

    /**
     * Searches column by name
     * 
     * @param columnName the column name to search
     * @return column if found, otherwise return null
     */
    public Column findColumnByName( String columnName ) {
        for (Column c : columns) {
            if (c.getName().equals(columnName)) {
                return c;
            }
        }
        // return nothing
        return null;
    }
}
