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

import org.jboss.dna.common.jdbc.model.api.SqlType;
import org.jboss.dna.common.jdbc.model.api.SqlTypeConversionPair;

/**
 * Provides RDBMS supported SQL type valid conversion pair.
 * 
 * @author <a href="mailto:litsenko_sergey@yahoo.com">Sergiy Litsenko</a>
 */
public class SqlTypeConversionPairBean extends CoreMetaDataBean implements SqlTypeConversionPair {
    private static final long serialVersionUID = -2111832789576864971L;
    private SqlType srcType;
    private SqlType destType;

    /**
     * Default constructor
     */
    public SqlTypeConversionPairBean() {
    }

    /**
     * Gets valid <from> SQL Type
     * 
     * @return valid <from> SQL Type
     */
    public SqlType getSrcType() {
        return srcType;
    }

    /**
     * Sets valid source SQL Type
     * 
     * @param srcType the source SQL Type
     */
    public void setSrcType( SqlType srcType ) {
        this.srcType = srcType;
    }

    /**
     * Gets valid destination SQL Type
     * 
     * @return valid dewstination SQL Type
     */
    public SqlType getDestType() {
        return destType;
    }

    /**
     * Sets valid destination SQL Type
     * 
     * @param destType the destination SQL Type
     */
    public void setDestType( SqlType destType ) {
        this.destType = destType;
    }
}
