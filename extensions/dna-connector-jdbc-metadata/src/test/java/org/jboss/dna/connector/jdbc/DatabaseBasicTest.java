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
package org.jboss.dna.connector.jdbc;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import org.dbunit.IDatabaseTester;
import org.dbunit.Assertion;
import org.dbunit.JdbcDatabaseTester;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Basic test of the HSQLDB database with simple schema
 * 
 * @author <a href="mailto:litsenko_sergey@yahoo.com">Sergiy Litsenko</a>
 */
public class DatabaseBasicTest {
    private IDatabaseTester dbTester;

    private IDataSet getDataSet() throws IOException, DataSetException {
        return new FlatXmlDataSet(new File("src/test/data/insert.xml"));
    }

    @Before
    public void beforeEach() throws Exception {
        dbTester = new JdbcDatabaseTester("org.hsqldb.jdbcDriver", "jdbc:hsqldb:file:target/testdb/db", "sa", "");
        dbTester.setDataSet(getDataSet());
        dbTester.onSetup();
    }

    @After
    public void afterEach() throws Exception {
        dbTester.onTearDown();
    }

    @Test
    public void testTableShallBeLoaded() throws Exception {
        // Fetch live database data
        IDataSet databaseDataSet = dbTester.getConnection().createDataSet();
        ITable actualTable = databaseDataSet.getTable("TEST");
        ITable expectedTable = getDataSet().getTable("TEST");
        Assertion.assertEquals(expectedTable, actualTable);
    }

    @Test
    public void testTableRecordsLoaded() throws Exception {
        // Fetch live database data
        Assert.assertEquals(10, dbTester.getConnection().getRowCount("TEST"));
    }

    @Test
    public void testTableRandomRowColumnId() throws Exception {
        // Fetch live database data
        IDataSet databaseDataSet = dbTester.getConnection().createDataSet();
        ITable actualTable = databaseDataSet.getTable("TEST");
        // get 5th row, column: id
        Object actualId = actualTable.getValue(4, "ID");
        Assert.assertNotNull(actualId);

        BigDecimal expectedId = new BigDecimal(5);
        Assert.assertTrue(expectedId.equals(actualId));
    }

    @Test
    public void testTableRandomRowColumnName() throws Exception {
        // Fetch live database data
        IDataSet databaseDataSet = dbTester.getConnection().createDataSet();
        ITable actualTable = databaseDataSet.getTable("TEST");
        // get 6th row, column: name
        Assert.assertEquals("6@hotmail.com", actualTable.getValue(5, "NAME"));
    }

    @Test
    public void testTableRandomRowColumnDescription() throws Exception {
        // Fetch live database data
        IDataSet databaseDataSet = dbTester.getConnection().createDataSet();
        ITable actualTable = databaseDataSet.getTable("TEST");
        // get 7th row, column: description
        Assert.assertEquals("This is 7", actualTable.getValue(6, "DESCRIPTION"));
    }

    @Test
    public void testDefaultDatabaseSchemaNameIsEmpty() throws Exception {
        Assert.assertNull(dbTester.getConnection().getSchema());
    }

    @Test
    public void salGradeTableShallBeLoaded() throws Exception {
        // Fetch live database data
        IDataSet databaseDataSet = dbTester.getConnection().createDataSet();
        ITable actualTable = databaseDataSet.getTable("SALGRADE");
        ITable expectedTable = getDataSet().getTable("SALGRADE");
        Assertion.assertEquals(expectedTable, actualTable);
    }

    @Test
    public void deptTableShallBeLoaded() throws Exception {
        // Fetch live database data
        IDataSet databaseDataSet = dbTester.getConnection().createDataSet();
        ITable actualTable = databaseDataSet.getTable("DEPT");
        ITable expectedTable = getDataSet().getTable("DEPT");
        Assertion.assertEquals(expectedTable, actualTable);
    }

    @Test
    public void empTableShallBeLoaded() throws Exception {
        // Fetch live database data
        IDataSet databaseDataSet = dbTester.getConnection().createDataSet();
        ITable actualTable = databaseDataSet.getTable("EMP");
        ITable expectedTable = getDataSet().getTable("EMP");
        // Assertion.assertEquals(expectedTable, actualTable);
        Assert.assertEquals(actualTable.getRowCount(), expectedTable.getRowCount());
    }
}
