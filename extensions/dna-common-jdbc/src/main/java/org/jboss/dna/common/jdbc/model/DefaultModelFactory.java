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
package org.jboss.dna.common.jdbc.model;

import org.jboss.dna.common.jdbc.model.api.*;
import org.jboss.dna.common.jdbc.model.spi.*;

/**
 * Database metadata objects creation factory
 * 
 * @author <a href="mailto:litsenko_sergey@yahoo.com">Sergiy Litsenko</a>
 */
public class DefaultModelFactory implements ModelFactory {
    // ~ Constructors ---------------------------------------------------------------------

    /**
     * Default constructor
     */
    public DefaultModelFactory() {
    }

    // ~ Methods --------------------------------------------------------------------------

    /**
     * Creates Attribute
     * 
     * @return Attribute
     */
    public Attribute createAttribute() {
        return new AttributeBean();
    }

    /**
     * Creates BestRowIdentifier
     * 
     * @return BestRowIdentifier object
     */
    public BestRowIdentifier createBestRowIdentifier() {
        return new BestRowIdentifierBean();
    }

    /**
     * Creates Catalog
     * 
     * @return Catalog object
     */
    public Catalog createCatalog() {
        return new CatalogBean();
    }

    /**
     * Creates Database
     * 
     * @return Database object
     */
    public Database createDatabase() {
        return new DatabaseBean();
    }

    /**
     * Creates ForeignKey
     * 
     * @return ForeignKey object
     */
    public ForeignKey createForeignKey() {
        return new ForeignKeyBean();
    }

    /**
     * Creates ForeignKeyColumn
     * 
     * @return ForeignKeyColumn object
     */
    public ForeignKeyColumn createForeignKeyColumn() {
        return new ForeignKeyColumnBean();
    }

    /**
     * Creates Index
     * 
     * @return Index object
     */
    public Index createIndex() {
        return new IndexBean();
    }

    /**
     * Creates IndexColumn
     * 
     * @return IndexColumn object
     */
    public IndexColumn createIndexColumn() {
        return new IndexColumnBean();
    }

    /**
     * Creates Parameter
     * 
     * @return Parameter object
     */
    public Parameter createParameter() {
        return new ParameterBean();
    }

    /**
     * Creates PrimaryKey
     * 
     * @return PrimaryKey object
     */
    public PrimaryKey createPrimaryKey() {
        return new PrimaryKeyBean();
    }

    /**
     * Creates PrimaryKeyColumn
     * 
     * @return PrimaryKeyColumn object
     */
    public PrimaryKeyColumn createPrimaryKeyColumn() {
        return new PrimaryKeyColumnBean();
    }

    /**
     * Creates Privilege
     * 
     * @return Privilege object
     */
    public Privilege createPrivilege() {
        return new PrivilegeBean();
    }

    /**
     * Creates Reference
     * 
     * @return Reference object
     */
    public Reference createReference() {
        return new ReferenceBean();
    }

    /**
     * Creates Schema
     * 
     * @return Schema object
     */
    public Schema createSchema() {
        return new SchemaBean();
    }

    /**
     * Creates SqlTypeConversionPair
     * 
     * @return SqlTypeConversionPair object
     */
    public SqlTypeConversionPair createSqlTypeConversionPair() {
        return new SqlTypeConversionPairBean();
    }

    /**
     * Creates SqlTypeInfo
     * 
     * @return SqlTypeInfo object
     */
    public SqlTypeInfo createSqlTypeInfo() {
        return new SqlTypeInfoBean();
    }

    /**
     * Creates StoredProcedure
     * 
     * @return StoredProcedure object
     */
    public StoredProcedure createStoredProcedure() {
        return new StoredProcedureBean();
    }

    /**
     * Creates Table
     * 
     * @return Table object
     */
    public Table createTable() {
        return new TableBean();
    }

    /**
     * Creates TableColumn
     * 
     * @return TableColumn object
     */
    public TableColumn createTableColumn() {
        return new TableColumnBean();
    }

    /**
     * Creates TableType
     * 
     * @return TableType object
     */
    public TableType createTableType() {
        return new TableTypeBean();
    }

    /**
     * Creates UserDefinedType
     * 
     * @return UserDefinedType object
     */
    public UserDefinedType createUserDefinedType() {
        return new UserDefinedTypeBean();
    }

}
