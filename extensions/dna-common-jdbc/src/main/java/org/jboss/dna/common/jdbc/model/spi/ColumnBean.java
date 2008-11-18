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
import org.jboss.dna.common.jdbc.model.api.Column;
import org.jboss.dna.common.jdbc.model.api.NullabilityType;
import org.jboss.dna.common.jdbc.model.api.SqlType;
import org.jboss.dna.common.jdbc.model.api.Privilege;
import org.jboss.dna.common.jdbc.model.api.SchemaObject;

/**
 * Provides all column specific metadata.
 * 
 * @author <a href="mailto:litsenko_sergey@yahoo.com">Sergiy Litsenko</a>
 */
public class ColumnBean extends DatabaseNamedObjectBean implements Column {
    private static final long serialVersionUID = 4797227922671541353L;
    private SchemaObject owner;
    private NullabilityType nullabilityType;
    private SqlType sqlType;
    private String typeName;
    private Integer size;
    private Integer precision;
    private Integer radix;
    private String defaultValue;
    private Integer ordinalPosition;
    private Integer charOctetLength;
    private Set<Privilege> privileges = new HashSet<Privilege>();

    /**
     * Default constructor
     */
    public ColumnBean() {
    }

    /**
     * Returns owner of ColumnMetaData such as Table, or Stored Procedure, UDT, PK, FK, Index, etc. May return NULL
     * 
     * @return owner of ColumnMetaData such as Table, or Stored Procedure, or UDT, PK, FK, Index, etc. May return NULL
     */
    public SchemaObject getOwner() {
        return owner;
    }

    /**
     * Sets the owner of ColumnMetaData
     * 
     * @param owner the owner of ColumnMetaData
     */
    public void setOwner( SchemaObject owner ) {
        this.owner = owner;
    }

    /**
     * Gets column nullability
     * 
     * @return column nullability
     */
    public NullabilityType getNullabilityType() {
        return nullabilityType;
    }

    /**
     * Sets column nullability
     * 
     * @param nullabilityType the column nullability
     */
    public void setNullabilityType( NullabilityType nullabilityType ) {
        this.nullabilityType = nullabilityType;
    }

    /**
     * Gets SQL type from java.sql.Types
     * 
     * @return SQL type from java.sql.Types
     */
    public SqlType getSqlType() {
        return sqlType;
    }

    /**
     * Sets SQL type from java.sql.Types
     * 
     * @param sqlType the SQL type from java.sql.Types
     */
    public void setSqlType( SqlType sqlType ) {
        this.sqlType = sqlType;
    }

    /**
     * Data source dependent type name. For a UDT, the type name is fully qualified. For a REF, the type name is fully qualified
     * and represents the target type of the reference type.
     * 
     * @return data source dependent type name
     */
    public String getTypeName() {
        return typeName;
    }

    /**
     * Data source dependent type name. For a UDT, the type name is fully qualified. For a REF, the type name is fully qualified
     * and represents the target type of the reference type.
     * 
     * @param typeName data source dependent type name
     */
    public void setTypeName( String typeName ) {
        this.typeName = typeName;
    }

    /**
     * Gets column size. For char or date types this is the maximum number of characters, for numeric or decimal types this is
     * precision. For Stored procedure columns it is length in bytes of data
     * 
     * @return column size
     */
    public Integer getSize() {
        return size;
    }

    /**
     * Sets column size. For char or date types this is the maximum number of characters, for numeric or decimal types this is
     * precision. For Stored procedure columns it is length in bytes of data
     * 
     * @param size the column size
     */
    public void setSize( Integer size ) {
        this.size = size;
    }

    /**
     * Gets precision if applicable otherwise 0. For table columns return the number of fractional digits; for stored procedure
     * column - scale.
     * 
     * @return precision if applicable otherwise 0
     */
    public Integer getPrecision() {
        return precision;
    }

    /**
     * Sets precision if applicable otherwise 0. For table columns return the number of fractional digits; for stored procedure
     * column - scale.
     * 
     * @param precision the precision if applicable otherwise 0
     */
    public void setPrecision( Integer precision ) {
        this.precision = precision;
    }

    /**
     * Gets radix if applicable
     * 
     * @return radix if applicable
     */
    public Integer getRadix() {
        return radix;
    }

    /**
     * Sets radix if applicable
     * 
     * @param radix if applicable
     */
    public void setRadix( Integer radix ) {
        this.radix = radix;
    }

    /**
     * Gets default value (may be <code>null</code>)
     * 
     * @return default value (may be <code>null</code>)
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * Sets default value (may be <code>null</code>)
     * 
     * @param defaultValue the default value (may be <code>null</code>)
     */
    public void setDefaultValue( String defaultValue ) {
        this.defaultValue = defaultValue;
    }

    /**
     * Returns index of column starting at 1 - if applicable. Otherwise returns -1.
     * 
     * @return index of column starting at 1 - if applicable. Otherwise returns -1.
     */
    public Integer getOrdinalPosition() {
        return ordinalPosition;
    }

    /**
     * Sets index of column starting at 1 - if applicable. Otherwise returns -1.
     * 
     * @param ordinalPosition the index of column starting at 1 - if applicable. Otherwise returns -1.
     */
    public void setOrdinalPosition( Integer ordinalPosition ) {
        this.ordinalPosition = ordinalPosition;
    }

    /**
     * For char types returns the maximum number of bytes in the column. Otherwise returns -1.
     * 
     * @return For char types returns the maximum number of bytes in the column. Otherwise returns -1.
     */
    public Integer getCharOctetLength() {
        return charOctetLength;
    }

    /**
     * For char types sets the maximum number of bytes in the column. Otherwise -1.
     * 
     * @param charOctetLength For char types sets the maximum number of bytes in the column. Otherwise -1.
     */
    public void setCharOctetLength( Integer charOctetLength ) {
        this.charOctetLength = charOctetLength;
    }

    /**
     * Gets table column privileges.
     * 
     * @return set of table column privileges
     */
    public Set<Privilege> getPrivileges() {
        return privileges;
    }

    /**
     * Adds table column priviledge
     * 
     * @param privilege the table column priviledge
     */
    public void addPrivilege( Privilege privilege ) {
        privileges.add(privilege);
    }

    /**
     * Deletes table column priviledge
     * 
     * @param privilege the table column priviledge
     */
    public void deletePrivilege( Privilege privilege ) {
        privileges.remove(privilege);
    }

    /**
     * Searches priviledge by name
     * 
     * @param priviledgeName the priviledge name to search
     * @return priviledge if found, otherwise return null
     */
    public Privilege findPriviledgeByName( String priviledgeName ) {
        for (Privilege p : privileges) {
            if (p.getName().equals(priviledgeName)) {
                return p;
            }
        }
        // return nothing
        return null;
    }
}
