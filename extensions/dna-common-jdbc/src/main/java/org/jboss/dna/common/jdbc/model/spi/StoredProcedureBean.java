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
package org.jboss.dna.common.jdbc.model.spi;

import java.util.Set;
import java.util.HashSet;
import org.jboss.dna.common.jdbc.model.api.Parameter;
import org.jboss.dna.common.jdbc.model.api.StoredProcedure;
import org.jboss.dna.common.jdbc.model.api.StoredProcedureResultType;

/**
 * Provides all core database SPe specific metadata.
 * 
 * @author <a href="mailto:litsenko_sergey@yahoo.com">Sergiy Litsenko</a>
 */
public class StoredProcedureBean extends SchemaObjectBean implements StoredProcedure {
    private static final long serialVersionUID = 8530431073036932292L;
    private Set<Parameter> columns = new HashSet<Parameter>();
    private StoredProcedureResultType resultType;

    /**
     * Default constructor
     */
    public StoredProcedureBean() {
    }

    /**
     * Gets stored procedure result type
     * 
     * @return stored procedure result type
     */
    public StoredProcedureResultType getResultType() {
        return resultType;
    }

    /**
     * Sets stored procedure result type
     * 
     * @param resultType the stored procedure result type
     */
    public void setResultType( StoredProcedureResultType resultType ) {
        this.resultType = resultType;
    }

    /**
     * Gets stored procedure columns
     * 
     * @return a set of stored procedure columns.
     */
    public Set<Parameter> getParameters() {
        return columns;
    }

    /**
     * Adds Parameter
     * 
     * @param parameter the Parameter
     */
    public void addParameter( Parameter parameter ) {
        columns.add(parameter);
    }

    /**
     * deletes Parameter
     * 
     * @param parameter the Parameter
     */
    public void deleteParameter( Parameter parameter ) {
        columns.remove(parameter);
    }

    /**
     * Returns stored procedure parameter for specified name or null
     * 
     * @param parameterName the name of parameter
     * @return stored procedure parameter for specified name or null.
     */
    public Parameter findParameterByName( String parameterName ) {
        for (Parameter p : columns) {
            if (p.getName().equals(parameterName)) {
                return p;
            }
        }
        // return nothing
        return null;
    }

}
