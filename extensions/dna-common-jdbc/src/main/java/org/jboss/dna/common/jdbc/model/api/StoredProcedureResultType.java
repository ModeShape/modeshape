/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
 * Provides RDBMS supported stored procedure result types as enumeration set.
 * 
 * @author <a href="mailto:litsenko_sergey@yahoo.com">Sergiy Litsenko</a>
 */
public enum StoredProcedureResultType {

    UNKNOWN(DatabaseMetaData.procedureResultUnknown), // Indicates that it is not known whether the procedure returns a result
    NO_RESULT(DatabaseMetaData.procedureNoResult), // Indicates that the procedure does not return a result.
    RETURNS_RESULT(DatabaseMetaData.procedureReturnsResult); // Indicates that the procedure returns a result.

    private final int type;

    StoredProcedureResultType( int type ) {
        this.type = type;
    }

    public int getType() {
        return type;
    };

    public String getName() {
        return name();
    }
}
