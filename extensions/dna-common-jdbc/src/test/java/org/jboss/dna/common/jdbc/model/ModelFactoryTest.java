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

import junit.framework.TestCase;

/**
 * Model factory test
 * 
 * @author <a href="mailto:litsenko_sergey@yahoo.com">Sergiy Litsenko</a>
 */
public class ModelFactoryTest extends TestCase {
    // ~ Instance fields ------------------------------------------------------------------

    private DefaultModelFactory modelFactory;

    // ~ Methods --------------------------------------------------------------------------

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // create
        modelFactory = new DefaultModelFactory();
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        // release
        modelFactory = null;
        super.tearDown();
    }

    /**
     * COMMENT
     */
    public void testCreateAttribute() {
        // check that we're really create database model object
        assertNotNull("Unable to create Attribute", modelFactory.createAttribute());
    }

    /**
     * COMMENT
     */
    public void testCreateBestRowIdentifier() {
        // check that we're really create database model object
        assertNotNull("Unable to create BestRowIdentifier", modelFactory.createBestRowIdentifier());
    }

    /**
     * COMMENT
     */
    public void testCreateCatalog() {
        // check that we're really create database model object
        assertNotNull("Unable to create Catalog", modelFactory.createCatalog());
    }

    /**
     * COMMENT
     */
    public void testCreateDatabase() {
        // check that we're really create database model object
        assertNotNull("Unable to create Database", modelFactory.createDatabase());
    }

    /**
     * COMMENT
     */
    public void testCreateForeignKey() {
        // check that we're really create database model object
        assertNotNull("Unable to create ForeignKey", modelFactory.createForeignKey());
    }

    /**
     * COMMENT
     */
    public void testCreateForeignKeyColumn() {
        // check that we're really create database model object
        assertNotNull("Unable to create ForeignKeyColumn", modelFactory.createForeignKeyColumn());
    }

    /**
     * COMMENT
     */
    public void testCreateIndex() {
        // check that we're really create database model object
        assertNotNull("Unable to create Index", modelFactory.createIndex());
    }

    /**
     * COMMENT
     */
    public void testCreateIndexColumn() {
        // check that we're really create database model object
        assertNotNull("Unable to create IndexColumn", modelFactory.createIndexColumn());
    }

    /**
     * COMMENT
     */
    public void testCreateParameter() {
        // check that we're really create database model object
        assertNotNull("Unable to create Parameter", modelFactory.createParameter());
    }

    /**
     * COMMENT
     */
    public void testCreatePrimaryKey() {
        // check that we're really create database model object
        assertNotNull("Unable to create PrimaryKey", modelFactory.createPrimaryKey());
    }

    /**
     * COMMENT
     */
    public void testCreatePrimaryKeyColumn() {
        // check that we're really create database model object
        assertNotNull("Unable to create PrimaryKeyColumn", modelFactory.createPrimaryKeyColumn());
    }

    /**
     * COMMENT
     */
    public void testCreatePrivilege() {
        // check that we're really create database model object
        assertNotNull("Unable to create Privilege", modelFactory.createPrivilege());
    }

    /**
     * COMMENT
     */
    public void testCreateReference() {
        // check that we're really create database model object
        assertNotNull("Unable to create Reference", modelFactory.createReference());
    }

    /**
     * COMMENT
     */
    public void testCreateSchema() {
        // check that we're really create database model object
        assertNotNull("Unable to create Schema", modelFactory.createSchema());
    }

    /**
     * COMMENT
     */
    public void testCreateSqlTypeConversionPair() {
        // check that we're really create database model object
        assertNotNull("Unable to create SqlTypeConversionPair", modelFactory.createSqlTypeConversionPair());
    }

    /**
     * COMMENT
     */
    public void testCreateSqlTypeInfo() {
        // check that we're really create database model object
        assertNotNull("Unable to create SqlTypeInfo", modelFactory.createSqlTypeInfo());
    }

    /**
     * COMMENT
     */
    public void testCreateStoredProcedure() {
        // check that we're really create database model object
        assertNotNull("Unable to create StoredProcedure", modelFactory.createStoredProcedure());
    }

    /**
     * COMMENT
     */
    public void testCreateTable() {
        // check that we're really create database model object
        assertNotNull("Unable to create Table", modelFactory.createTable());
    }

    /**
     * COMMENT
     */
    public void testCreateTableColumn() {
        // check that we're really create database model object
        assertNotNull("Unable to create TableColumn", modelFactory.createTableColumn());
    }

    /**
     * COMMENT
     */
    public void testCreateTableType() {
        // check that we're really create database model object
        assertNotNull("Unable to create TableType", modelFactory.createTableType());
    }

    /**
     * COMMENT
     */
    public void testCreateUserDefinedType() {
        // check that we're really create database model object
        assertNotNull("Unable to create UserDefinedType", modelFactory.createUserDefinedType());
    }

}
