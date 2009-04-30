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
import junit.framework.TestCase;
import org.jboss.dna.common.jdbc.model.DefaultModelFactory;
import org.jboss.dna.common.jdbc.model.ModelFactory;
import org.jboss.dna.common.jdbc.model.api.BestRowIdentifier;
import org.jboss.dna.common.jdbc.model.api.BestRowIdentifierScopeType;
import org.jboss.dna.common.jdbc.model.api.Catalog;
import org.jboss.dna.common.jdbc.model.api.ForeignKey;
import org.jboss.dna.common.jdbc.model.api.Index;
import org.jboss.dna.common.jdbc.model.api.PrimaryKey;
import org.jboss.dna.common.jdbc.model.api.Privilege;
import org.jboss.dna.common.jdbc.model.api.Schema;
import org.jboss.dna.common.jdbc.model.api.Table;
import org.jboss.dna.common.jdbc.model.api.TableColumn;
import org.jboss.dna.common.jdbc.model.api.TableType;

/**
 * TableBean test
 * 
 * @author <a href="mailto:litsenko_sergey@yahoo.com">Sergiy Litsenko</a>
 */
public class TableBeanTest extends TestCase {

    private Table bean;
    private ModelFactory factory;

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // create
        bean = new TableBean();
        factory = new DefaultModelFactory();
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        // release
        bean = null;
        factory = null;
        super.tearDown();
    }

    public void testSetTableType() {
        TableType tableType = factory.createTableType();
        // set
        bean.setTableType(tableType);

        // check
        assertSame("Unable to set table type", tableType, bean.getTableType());
    }

    public void testSetTypeCatalog() {
        Catalog typeCatalog = factory.createCatalog();
        // set
        bean.setTypeCatalog(typeCatalog);
        // check
        assertSame("Unable to set type catalog", typeCatalog, bean.getTypeCatalog());
    }

    public void testSetTypeSchema() {
        Schema typeSchema = factory.createSchema();
        // set
        bean.setTypeSchema(typeSchema);
        // check
        assertSame("Unable to set type schema", typeSchema, bean.getTypeSchema());
    }

    public void testSetTypeName() {
        String typeName = "My type";
        // set
        bean.setTypeName(typeName);
        // check
        assertSame("Unable to set type name", typeName, bean.getTypeName());
    }

    public void testSetSelfReferencingColumnName() {
        String selfReferencingColumnName = "Self-Ref column name";
        // set
        bean.setSelfReferencingColumnName(selfReferencingColumnName);
        // check
        assertSame("Unable to set self referencing column name", selfReferencingColumnName, bean.getSelfReferencingColumnName());
    }

    public void testSetReferenceGeneration() {
        String referenceGeneration = "Reference generation";
        // set
        bean.setReferenceGeneration(referenceGeneration);
        // check
        assertSame("Unable to set reference generation", referenceGeneration, bean.getReferenceGeneration());
    }

    public void testGetColumns() {
        Set<TableColumn> columns = bean.getColumns();
        // check
        assertNotNull("Unable to get columns", columns);
        assertTrue("Column set should be empty by default", columns.isEmpty());
    }

    public void testAddColumn() {
        String COLUMN_NAME = "My column";
        // create column
        TableColumn column = factory.createTableColumn();
        // set name
        column.setName(COLUMN_NAME);
        // add
        bean.addColumn(column);
        // check
        assertFalse("column set should not be empty", bean.getColumns().isEmpty());
    }

    public void testDeleteColumn() {
        String COLUMN_NAME = "My column";
        // create column
        TableColumn column = factory.createTableColumn();
        // set name
        column.setName(COLUMN_NAME);
        // add
        bean.addColumn(column);
        // check
        assertFalse("column set should not be empty", bean.getColumns().isEmpty());

        // delete
        bean.deleteColumn(column);
        // check
        assertTrue("Parameter set should be empty", bean.getColumns().isEmpty());
    }

    public void testFindColumnByName() {
        String COLUMN_NAME = "My column";
        // create column
        TableColumn column = factory.createTableColumn();
        // set name
        column.setName(COLUMN_NAME);
        // add
        bean.addColumn(column);
        // check
        assertFalse("column set should not be empty", bean.getColumns().isEmpty());
        // check
        assertSame("Unable to find column", column, bean.findColumnByName(COLUMN_NAME));
    }

    public void testSetPrimaryKey() {
        PrimaryKey primaryKey = factory.createPrimaryKey();
        // set
        bean.setPrimaryKey(primaryKey);
        // check
        assertSame("Unable to set primary key", primaryKey, bean.getPrimaryKey());
    }

    public void testGetForeignKeys() {
        Set<ForeignKey> foreignKeys = bean.getForeignKeys();
        // check
        assertNotNull("Unable to get FK list", foreignKeys);
        assertTrue("FK set should be empty by default", foreignKeys.isEmpty());
    }

    public void testAddForeignKey() {
        String NAME = "My FK";
        // create FK
        ForeignKey fk = factory.createForeignKey();
        // set name
        fk.setName(NAME);
        // add
        bean.addForeignKey(fk);
        // check
        assertFalse("FK set should not be empty", bean.getForeignKeys().isEmpty());
    }

    public void testDeleteForeignKey() {
        String NAME = "My FK";
        // create FK
        ForeignKey fk = factory.createForeignKey();
        // set name
        fk.setName(NAME);
        // add
        bean.addForeignKey(fk);
        // check
        assertFalse("FK set should not be empty", bean.getForeignKeys().isEmpty());

        // delete
        bean.deleteForeignKey(fk);
        // check
        assertTrue("FK set should be empty", bean.getForeignKeys().isEmpty());
    }

    public void testFindForeignKeyByName() {
        String NAME = "My FK";
        // create FK
        ForeignKey fk = factory.createForeignKey();
        // set name
        fk.setName(NAME);
        // add
        bean.addForeignKey(fk);
        // check
        assertFalse("FK set should not be empty", bean.getForeignKeys().isEmpty());
        // check
        assertSame("Unable to find FK", fk, bean.findForeignKeyByName(NAME));
    }

    public void testGetIndexes() {
        Set<Index> indexes = bean.getIndexes();
        // check
        assertNotNull("Unable to get Indexes", indexes);
        assertTrue("Index set should be empty by default", indexes.isEmpty());
    }

    public void testAddIndex() {
        String NAME = "My Index";
        // create FK
        Index i = factory.createIndex();
        // set name
        i.setName(NAME);
        // add
        bean.addIndex(i);
        // check
        assertFalse("Index set should not be empty", bean.getIndexes().isEmpty());
    }

    public void testDeleteIndex() {
        String NAME = "My Index";
        // create FK
        Index i = factory.createIndex();
        // set name
        i.setName(NAME);
        // add
        bean.addIndex(i);
        // check
        assertFalse("Index set should not be empty", bean.getIndexes().isEmpty());

        // delete
        bean.deleteIndex(i);
        // check
        assertTrue("*Index set should be empty", bean.getIndexes().isEmpty());
    }

    public void testFindIndexByName() {
        String NAME = "My Index";
        // create FK
        Index i = factory.createIndex();
        // set name
        i.setName(NAME);
        // add
        bean.addIndex(i);
        // check
        assertFalse("Index set should not be empty", bean.getIndexes().isEmpty());
        // check
        assertSame("Unable to find Index", i, bean.findIndexByName(NAME));
    }

    public void testGetVersionColumns() {
        Set<TableColumn> columns = bean.getVersionColumns();
        // check
        assertNotNull("Unable to get version columns", columns);
        assertTrue("Version column set should be empty by default", columns.isEmpty());
    }

    public void testAddVersionColumn() {
        String COLUMN_NAME = "My column";
        // create column
        TableColumn column = factory.createTableColumn();
        // set name
        column.setName(COLUMN_NAME);
        // add
        bean.addVersionColumn(column);
        // check
        assertFalse("column set should not be empty", bean.getVersionColumns().isEmpty());
    }

    public void testDeleteVersionColumn() {
        String COLUMN_NAME = "My column";
        // create column
        TableColumn column = factory.createTableColumn();
        // set name
        column.setName(COLUMN_NAME);
        // add
        bean.addVersionColumn(column);
        // check
        assertFalse("column set should not be empty", bean.getVersionColumns().isEmpty());

        // delete
        bean.deleteVersionColumn(column);
        // check
        assertTrue("Version Column set should be empty", bean.getVersionColumns().isEmpty());
    }

    public void testFindVersionColumnByName() {
        String COLUMN_NAME = "My column";
        // create column
        TableColumn column = factory.createTableColumn();
        // set name
        column.setName(COLUMN_NAME);
        // add
        bean.addVersionColumn(column);
        // check
        assertFalse("column set should not be empty", bean.getVersionColumns().isEmpty());
        // check
        assertSame("Unable to find column", column, bean.findVersionColumnByName(COLUMN_NAME));
    }

    public void testGetPrivileges() {
        Set<Privilege> privileges = bean.getPrivileges();
        // check
        assertNotNull("Unable to get privileges", privileges);
        assertTrue("Privilege set should be empty by default", privileges.isEmpty());
    }

    public void testAddPrivilege() {
        String NAME = "My privilege";
        // create privilege
        Privilege privilege = factory.createPrivilege();
        // set name
        privilege.setName(NAME);
        // add
        bean.addPrivilege(privilege);
        // check
        assertFalse("Privilege set should not be empty", bean.getPrivileges().isEmpty());
    }

    public void testDeletePrivilege() {
        String NAME = "My privilege";
        // create privilege
        Privilege privilege = factory.createPrivilege();
        // set name
        privilege.setName(NAME);
        // add
        bean.addPrivilege(privilege);
        // check
        assertFalse("Privilege set should not be empty", bean.getPrivileges().isEmpty());
        // delete
        bean.deletePrivilege(privilege);
        // check
        assertTrue("Privilege set should be empty", bean.getPrivileges().isEmpty());
    }

    public void testFindPriviledgeByName() {
        String NAME = "My privilege";
        // create privilege
        Privilege privilege = factory.createPrivilege();
        // set name
        privilege.setName(NAME);
        // add
        bean.addPrivilege(privilege);
        // check
        assertFalse("Privilege set should not be empty", bean.getPrivileges().isEmpty());
        // check
        assertSame("Unable to find privilege", privilege, bean.findPriviledgeByName(NAME));
    }

    public void testGetBestRowIdentifiers() {
        Set<BestRowIdentifier> bris = bean.getBestRowIdentifiers();
        // check
        assertNotNull("Unable to get BestRowIdentifiers", bris);
        assertTrue("BestRowIdentifier set should be empty by default", bris.isEmpty());
    }

    public void testAddBestRowIdentifier() {
        // create
        BestRowIdentifier id = factory.createBestRowIdentifier();
        // set scope
        id.setScopeType(BestRowIdentifierScopeType.SESSION);
        // add
        bean.addBestRowIdentifier(id);
        // check
        assertFalse("BestRowIdentifier set should not be empty", bean.getBestRowIdentifiers().isEmpty());
    }

    public void testDeleteBestRowIdentifier() {
        // create privilege
        BestRowIdentifier id = factory.createBestRowIdentifier();
        // set scope
        id.setScopeType(BestRowIdentifierScopeType.SESSION);
        // add
        bean.addBestRowIdentifier(id);
        // check
        assertFalse("BestRowIdentifier set should not be empty", bean.getBestRowIdentifiers().isEmpty());
        // delete
        bean.deleteBestRowIdentifier(id);
        // check
        assertTrue("BestRowIdentifier set should be empty", bean.getBestRowIdentifiers().isEmpty());
    }

    public void testFindBestRowIdentifierByScopeType() {
        // create privilege
        BestRowIdentifier id = factory.createBestRowIdentifier();
        // set scope
        id.setScopeType(BestRowIdentifierScopeType.SESSION);
        // add
        bean.addBestRowIdentifier(id);
        // check
        assertFalse("BestRowIdentifier set should not be empty", bean.getBestRowIdentifiers().isEmpty());
        // check
        assertSame("Unable to find BestRowIdentifier",
                   id,
                   bean.findBestRowIdentifierByScopeType(BestRowIdentifierScopeType.SESSION));
    }

    public void testSetSuperTable() {
        Table superTable = factory.createTable();
        // set
        bean.setSuperTable(superTable);
        // check
        assertSame("Unable to set super table", superTable, bean.getSuperTable());
    }

}
