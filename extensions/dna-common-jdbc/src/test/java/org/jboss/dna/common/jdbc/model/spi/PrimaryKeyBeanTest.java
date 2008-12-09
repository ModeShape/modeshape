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
import junit.framework.TestCase;
import org.jboss.dna.common.jdbc.model.DefaultModelFactory;
import org.jboss.dna.common.jdbc.model.api.PrimaryKey;
import org.jboss.dna.common.jdbc.model.api.PrimaryKeyColumn;

/**
 * PrimaryKeyBean test
 * 
 * @author <a href="mailto:litsenko_sergey@yahoo.com">Sergiy Litsenko</a>
 */
public class PrimaryKeyBeanTest extends TestCase {

    private PrimaryKey bean;

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // create
        bean = new PrimaryKeyBean();
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        // release
        bean = null;

        super.tearDown();
    }

    public void testGetColumns() {
        Set<PrimaryKeyColumn> columns = bean.getColumns();
        // check
        assertNotNull("Unable to get columns", columns);
        assertTrue("Column set should be empty by default", columns.isEmpty());
    }

    public void testAddColumn() {
        String COLUMN_NAME = "My column";
        // create column
        PrimaryKeyColumn column = new DefaultModelFactory().createPrimaryKeyColumn();
        // set name
        column.setName(COLUMN_NAME);
        // add column
        bean.addColumn(column);
        // check
        assertFalse("Column set should not be empty", bean.getColumns().isEmpty());
    }

    public void testDeleteColumn() {
        String COLUMN_NAME = "My column";
        // create column
        PrimaryKeyColumn column = new DefaultModelFactory().createPrimaryKeyColumn();
        // set name
        column.setName(COLUMN_NAME);
        // add column
        bean.addColumn(column);
        // check
        assertFalse("Column set should not be empty", bean.getColumns().isEmpty());

        // delete
        bean.deleteColumn(column);
        // check
        assertTrue("Column set should be empty", bean.getColumns().isEmpty());
    }

    public void testFindColumnByName() {
        String COLUMN_NAME = "My column";
        // create column
        PrimaryKeyColumn column = new DefaultModelFactory().createPrimaryKeyColumn();
        // set name
        column.setName(COLUMN_NAME);
        // add column
        bean.addColumn(column);

        // check
        assertSame("Unable to find column", column, bean.findColumnByName(COLUMN_NAME));
    }

}
