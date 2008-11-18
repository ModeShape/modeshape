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
