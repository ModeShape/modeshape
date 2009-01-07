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

/**
 * Provides all database table REF column specific metadata.
 * 
 * @author <a href="mailto:litsenko_sergey@yahoo.com">Sergiy Litsenko</a>
 */
public interface Reference extends CoreMetaData {
    /**
     * Returns table name that this the scope of a reference attribure
     * 
     * @return table name that this the scope of a reference attribure
     */
    Table getSourceTable();

    /**
     * Sets table name that this the scope of a reference attribure
     * 
     * @param sourceTable table name that this the scope of a reference attribure
     */
    void setSourceTable( Table sourceTable );

    /**
     * Returns source type of a distinct type or user-generated Ref type.
     * 
     * @return source type of a distinct type or user-generated Ref type,
     */
    SqlType getSourceDataType();

    /**
     * Sets source type of a distinct type or user-generated Ref type.
     * 
     * @param sourceDataType source type of a distinct type or user-generated Ref type,
     */
    void setSourceDataType( SqlType sourceDataType );
}
