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
import org.jboss.dna.common.jdbc.model.api.BestRowIdentifier;
import org.jboss.dna.common.jdbc.model.api.BestRowIdentifierScopeType;
import org.jboss.dna.common.jdbc.model.api.Column;

/**
 * BestRowIdentifierBean test
 * 
 * @author <a href="mailto:litsenko_sergey@yahoo.com">Sergiy Litsenko</a>
 */
public class BestRowIdentifierBeanTest extends TestCase {

    BestRowIdentifier bean;

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // create
        bean = new BestRowIdentifierBean();
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

    public void testSetScopeType() {
        // set
        bean.setScopeType(BestRowIdentifierScopeType.SESSION);
        // check
        assertSame("Unable to set scope type", BestRowIdentifierScopeType.SESSION, bean.getScopeType());
    }

    public void testGetColumns() {
        // get
        Set<Column> columnSet = bean.getColumns();
        // check
        assertNotNull("Unable to get columns", columnSet);
        assertTrue("Column set should be empty by default", columnSet.isEmpty());
    }

    public void testAddColumn() {
        String COLUMN_NAME = "My column";
        // create column
        Column column = new DefaultModelFactory().createTableColumn();
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
        Column column = new DefaultModelFactory().createTableColumn();
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
        Column column = new DefaultModelFactory().createTableColumn();
        // set name
        column.setName(COLUMN_NAME);
        // add column
        bean.addColumn(column);
        // check
        assertSame("Unable to find column", column, bean.findColumnByName(COLUMN_NAME));
    }

}
