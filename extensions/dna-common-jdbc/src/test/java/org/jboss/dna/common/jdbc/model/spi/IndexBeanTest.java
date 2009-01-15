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
import org.jboss.dna.common.jdbc.model.api.Index;
import org.jboss.dna.common.jdbc.model.api.IndexColumn;
import org.jboss.dna.common.jdbc.model.api.IndexType;

/**
 * IndexBean test
 * 
 * @author <a href="mailto:litsenko_sergey@yahoo.com">Sergiy Litsenko</a>
 */
public class IndexBeanTest extends TestCase {
    private Index bean;

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // create
        bean = new IndexBean();
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
        // get columns
        Set<IndexColumn> columns = bean.getColumns();
        // check
        assertNotNull("Unable to get columns", columns);
        assertTrue("Column set should be empty by default", columns.isEmpty());
    }

    public void testAddColumn() {
        String COLUMN_NAME = "My column";
        // create column
        IndexColumn column = new DefaultModelFactory().createIndexColumn();
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
        IndexColumn column = new DefaultModelFactory().createIndexColumn();
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
        IndexColumn column = new DefaultModelFactory().createIndexColumn();
        // set name
        column.setName(COLUMN_NAME);
        // add column
        bean.addColumn(column);
        // check
        assertSame("Unable to find column", column, bean.findColumnByName(COLUMN_NAME));
    }

    public void testSetUnique() {
        Boolean unique = Boolean.TRUE;
        // set
        bean.setUnique(unique);
        // check that it was set
        assertSame("Unable to set unique", unique, bean.isUnique());
    }

    public void testSetIndexType() {
        // set
        bean.setIndexType(IndexType.CLUSTERED);
        // check
        assertSame("Unable to set index type", IndexType.CLUSTERED, bean.getIndexType());
    }

    public void testSetCardinality() {
        Integer cardinality = new Integer(1);
        // set
        bean.setCardinality(cardinality);
        // check
        assertSame("Unable to set cardinality", cardinality, bean.getCardinality());
    }

    public void testSetPages() {
        Integer pages = new Integer(1);
        // set
        bean.setPages(pages);
        // check
        assertSame("Unable to set pages", pages, bean.getPages());
    }

    public void testSetFilterCondition() {
        String filterCondition = "IS NOT NULL";
        // set
        bean.setFilterCondition(filterCondition);

        // check
        assertSame("Unable to set filter condition", filterCondition, bean.getFilterCondition());
    }

}
