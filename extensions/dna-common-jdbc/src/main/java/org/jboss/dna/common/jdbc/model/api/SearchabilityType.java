/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008-2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.common.jdbc.model.api;

import java.sql.DatabaseMetaData;

/**
 * Provides RDBMS supported search ability as enumeration set.
 * 
 * @author <a href="mailto:litsenko_sergey@yahoo.com">Sergiy Litsenko</a>
 */
public enum SearchabilityType {

    NOT_SUPPORTED(DatabaseMetaData.typePredNone), // Indicates that <code>WHERE</code> search clauses are not supported for this
                                                  // type.
    WHERE_LIKE(DatabaseMetaData.typePredChar), // Indicates that the only <code>WHERE</code> search clause that can be based on
                                               // this type is <code>WHERE . . . LIKE</code>.
    BASIC(DatabaseMetaData.typePredBasic), // Indicates that one can base all <code>WHERE</code> search clauses except <code>WHERE
                                           // . . . LIKE</code> on this data type.
    SEARCHABLE(DatabaseMetaData.typeSearchable); // Indicates that all <code>WHERE</code> search clauses can be based on this
                                                 // type.

    private final int searchability;

    SearchabilityType( int searchability ) {
        this.searchability = searchability;
    }

    public int getSearchability() {
        return searchability;
    }

    public String getName() {
        return name();
    }
}
