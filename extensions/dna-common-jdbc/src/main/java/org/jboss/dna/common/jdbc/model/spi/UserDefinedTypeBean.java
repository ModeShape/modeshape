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
import org.jboss.dna.common.jdbc.model.api.Attribute;
import org.jboss.dna.common.jdbc.model.api.UserDefinedType;
import org.jboss.dna.common.jdbc.model.api.SqlType;

/**
 * Provides User Defined Type (UDT) specific metadata. Retrieves a description of the user-defined types (UDTs) defined in a
 * particular schema. Schema-specific UDTs may have type JAVA_OBJECT, STRUCT, or DISTINCT.
 * 
 * @author <a href="mailto:litsenko_sergey@yahoo.com">Sergiy Litsenko</a>
 * @since 1.2 (JDBC 2.0)
 */
public class UserDefinedTypeBean extends SchemaObjectBean implements UserDefinedType {
    private static final long serialVersionUID = -7493272131759308580L;
    private Set<Attribute> columns = new HashSet<Attribute>();
    private String className;
    private SqlType sqlType;
    private SqlType baseType;
    private UserDefinedType superType;

    /**
     * Default constructor
     */
    public UserDefinedTypeBean() {
    }

    /**
     * Returns JAVA class name for UDT
     * 
     * @return JAVA class name for UDT
     */
    public String getClassName() {
        return className;
    }

    /**
     * Sets JAVA class name for UDT
     * 
     * @param className JAVA class name for UDT
     */
    public void setClassName( String className ) {
        this.className = className;
    }

    /**
     * Gets SQL type from java.sql.Types. One of JAVA_OBJECT, STRUCT, or DISTINCT
     * 
     * @return SQL type from java.sql.Types. One of JAVA_OBJECT, STRUCT, or DISTINCT
     */
    public SqlType getSqlType() {
        return sqlType;
    }

    /**
     * Sets SQL type from java.sql.Types. One of JAVA_OBJECT, STRUCT, or DISTINCT
     * 
     * @param sqlType the SQL type from java.sql.Types. One of JAVA_OBJECT, STRUCT, or DISTINCT
     */
    public void setSqlType( SqlType sqlType ) {
        this.sqlType = sqlType;
    }

    /**
     * Gets SQL base type from java.sql.Types. Type code of the source type of a DISTINCT type or the type that implements the
     * user-generated reference type of the SELF_REFERENCING_COLUMN of a structured type as defined in java.sql.Types (null if
     * DATA_TYPE is not DISTINCT or not STRUCT with REFERENCE_GENERATION = USER_DEFINED)
     * 
     * @return SQL base type from java.sql.Types.
     */
    public SqlType getBaseType() {
        return baseType;
    }

    /**
     * Sets SQL base type from java.sql.Types. Type code of the source type of a DISTINCT type or the type that implements the
     * user-generated reference type of the SELF_REFERENCING_COLUMN of a structured type as defined in java.sql.Types (null if
     * DATA_TYPE is not DISTINCT or not STRUCT with REFERENCE_GENERATION = USER_DEFINED)
     * 
     * @param baseType the SQL base type from java.sql.Types.
     */
    public void setBaseType( SqlType baseType ) {
        this.baseType = baseType;
    }

    /**
     * Gets a set of UDT attributes
     * 
     * @return a set of UDT attributes
     */
    public Set<Attribute> getAttributes() {
        return columns;
    }

    /**
     * adds Attribute
     * 
     * @param attribute the Attribute
     */
    public void addAttribute( Attribute attribute ) {
        columns.add(attribute);
    }

    /**
     * deletes Attribute
     * 
     * @param attribute the Attribute
     */
    public void deleteAttribute( Attribute attribute ) {
        columns.remove(attribute);
    }

    /**
     * Returns UDT attribute for specified attribute name or null
     * 
     * @param attributeName the name of attribute
     * @return UDT attribute for specified attribute name or null.
     */
    public Attribute findAttributeByName( String attributeName ) {
        for (Attribute a : columns) {
            if (a.getName().equals(attributeName)) {
                return a;
            }
        }
        // return nothing
        return null;
    }

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
    public UserDefinedType getSuperType() {
        return superType;
    }

    /**
     * Sets a description of the user-defined type (UDT) hierarchies defined in a particular schema in this database. Only the
     * immediate super type/ sub type relationship is modeled.
     * 
     * @param superType the super type for this UDT if any
     * @since 1.4 (JDBC 3.0)
     */
    public void setSuperType( UserDefinedType superType ) {
        this.superType = superType;
    }
}
