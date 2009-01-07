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

import java.util.Set;

/**
 * Provides User Defined Type (UDT) specific metadata. Retrieves a description of the user-defined types (UDTs) defined in a
 * particular schema. Schema-specific UDTs may have type JAVA_OBJECT, STRUCT, or DISTINCT.
 * 
 * @author <a href="mailto:litsenko_sergey@yahoo.com">Sergiy Litsenko</a>
 * @since 1.2 (JDBC 2.0)
 */
public interface UserDefinedType extends SchemaObject {

    /**
     * Returns JAVA class name for UDT
     * 
     * @return JAVA class name for UDT
     */
    String getClassName();

    /**
     * Sets JAVA class name for UDT
     * 
     * @param className JAVA class name for UDT
     */
    void setClassName( String className );

    /**
     * Gets SQL type from java.sql.Types. One of JAVA_OBJECT, STRUCT, or DISTINCT
     * 
     * @return SQL type from java.sql.Types. One of JAVA_OBJECT, STRUCT, or DISTINCT
     */
    SqlType getSqlType();

    /**
     * Sets SQL type from java.sql.Types. One of JAVA_OBJECT, STRUCT, or DISTINCT
     * 
     * @param sqlType the SQL type from java.sql.Types. One of JAVA_OBJECT, STRUCT, or DISTINCT
     */
    void setSqlType( SqlType sqlType );

    /**
     * Gets SQL base type from java.sql.Types. Type code of the source type of a DISTINCT type or the type that implements the
     * user-generated reference type of the SELF_REFERENCING_COLUMN of a structured type as defined in java.sql.Types (null if
     * DATA_TYPE is not DISTINCT or not STRUCT with REFERENCE_GENERATION = USER_DEFINED)
     * 
     * @return SQL base type from java.sql.Types.
     */
    SqlType getBaseType();

    /**
     * Sets SQL base type from java.sql.Types. Type code of the source type of a DISTINCT type or the type that implements the
     * user-generated reference type of the SELF_REFERENCING_COLUMN of a structured type as defined in java.sql.Types (null if
     * DATA_TYPE is not DISTINCT or not STRUCT with REFERENCE_GENERATION = USER_DEFINED)
     * 
     * @param baseType the SQL base type from java.sql.Types.
     */
    void setBaseType( SqlType baseType );

    /**
     * Gets a set of UDT attributes
     * 
     * @return a set of UDT attributes
     */
    Set<Attribute> getAttributes();

    /**
     * adds Attribute
     * 
     * @param attribute the Attribute
     */
    void addAttribute( Attribute attribute );

    /**
     * deletes Attribute
     * 
     * @param attribute the Attribute
     */
    void deleteAttribute( Attribute attribute );

    /**
     * Returns UDT attribute for specified attribute name or null
     * 
     * @param attributeName the name of attribute
     * @return UDT attribute for specified attribute name or null.
     */
    Attribute findAttributeByName( String attributeName );

    // ===============================================================
    // ------------------- JDBC 3.0 ---------------------------------
    // ===============================================================

    /**
     * Retrieves a description of the user-defined type (UDT) hierarchies defined in a particular schema in this database. Only
     * the immediate super type/ sub type relationship is modeled.
     * 
     * @return super type for this UDT if any
     * @since 1.4 (JDBC 3.0)
     */
    UserDefinedType getSuperType();

    /**
     * Sets a description of the user-defined type (UDT) hierarchies defined in a particular schema in this database. Only the
     * immediate super type/ sub type relationship is modeled.
     * 
     * @param superType the super type for this UDT if any
     * @since 1.4 (JDBC 3.0)
     */
    void setSuperType( UserDefinedType superType );
}
