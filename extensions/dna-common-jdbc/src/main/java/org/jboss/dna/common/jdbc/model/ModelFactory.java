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
package org.jboss.dna.common.jdbc.model;

import org.jboss.dna.common.jdbc.model.api.*;

/**
 * Database metadata objects creation factory
 * 
 * @author <a href="mailto:litsenko_sergey@yahoo.com">Sergiy Litsenko</a>
 */
public interface ModelFactory {
    // ~ Methods --------------------------------------------------------------------------

    /**
     * Creates Attribute
     * 
     * @return Attribute
     */
    Attribute createAttribute();

    /**
     * Creates BestRowIdentifier
     * 
     * @return BestRowIdentifier object
     */
    BestRowIdentifier createBestRowIdentifier();

    /**
     * Creates Catalog
     * 
     * @return Catalog object
     */
    Catalog createCatalog();

    /**
     * Creates Database
     * 
     * @return Database object
     */
    Database createDatabase();

    /**
     * Creates ForeignKey
     * 
     * @return ForeignKey object
     */
    ForeignKey createForeignKey();

    /**
     * Creates ForeignKeyColumn
     * 
     * @return ForeignKeyColumn object
     */
    ForeignKeyColumn createForeignKeyColumn();

    /**
     * Creates Index
     * 
     * @return Index object
     */
    Index createIndex();

    /**
     * Creates IndexColumn
     * 
     * @return IndexColumn object
     */
    IndexColumn createIndexColumn();

    /**
     * Creates Parameter
     * 
     * @return Parameter object
     */
    Parameter createParameter();

    /**
     * Creates PrimaryKey
     * 
     * @return PrimaryKey object
     */
    PrimaryKey createPrimaryKey();

    /**
     * Creates PrimaryKeyColumn
     * 
     * @return PrimaryKeyColumn object
     */
    PrimaryKeyColumn createPrimaryKeyColumn();

    /**
     * Creates Privilege
     * 
     * @return Privilege object
     */
    Privilege createPrivilege();

    /**
     * Creates Reference
     * 
     * @return Reference object
     */
    Reference createReference();

    /**
     * Creates Schema
     * 
     * @return Schema object
     */
    Schema createSchema();

    /**
     * Creates SqlTypeConversionPair
     * 
     * @return SqlTypeConversionPair object
     */
    SqlTypeConversionPair createSqlTypeConversionPair();

    /**
     * Creates SqlTypeInfo
     * 
     * @return SqlTypeInfo object
     */
    SqlTypeInfo createSqlTypeInfo();

    /**
     * Creates StoredProcedure
     * 
     * @return StoredProcedure object
     */
    StoredProcedure createStoredProcedure();

    /**
     * Creates Table
     * 
     * @return Table object
     */
    Table createTable();

    /**
     * Creates TableColumn
     * 
     * @return TableColumn object
     */
    TableColumn createTableColumn();

    /**
     * Creates TableType
     * 
     * @return TableType object
     */
    TableType createTableType();

    /**
     * Creates UserDefinedType
     * 
     * @return UserDefinedType object
     */
    UserDefinedType createUserDefinedType();

}
