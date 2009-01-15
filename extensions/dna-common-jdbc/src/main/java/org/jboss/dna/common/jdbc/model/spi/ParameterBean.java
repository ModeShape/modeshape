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

import org.jboss.dna.common.jdbc.model.api.Parameter;
import org.jboss.dna.common.jdbc.model.api.ParameterIoType;

/**
 * Provides all SP column specific metadata.
 * 
 * @author <a href="mailto:litsenko_sergey@yahoo.com">Sergiy Litsenko</a>
 */
public class ParameterBean extends ColumnBean implements Parameter {
    private static final long serialVersionUID = -154398910715869384L;
    private ParameterIoType parameterIoType;
    private Integer scale;

    /**
     * Default constructor
     */
    public ParameterBean() {
    }

    /**
     * Gets stored procedure parameter I/O type
     * 
     * @return stored procedure parameter I/O type
     */
    public ParameterIoType getIoType() {
        return parameterIoType;
    }

    /**
     * Sets stored procedure parameter I/O type
     * 
     * @param parameterIoType stored procedure parameter I/O type
     */
    public void setIoType( ParameterIoType parameterIoType ) {
        this.parameterIoType = parameterIoType;
    }

    /**
     * Returns parameter scale if appropriate
     * 
     * @return scale if appropriate
     */
    public Integer getScale() {
        return scale;
    }

    /**
     * Sets parameter scale if appropriate
     * 
     * @param scale the scale if appropriate
     */
    public void setScale( Integer scale ) {
        this.scale = scale;
    }
}
