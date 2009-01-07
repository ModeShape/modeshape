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
package org.jboss.dna.common.jdbc.model.spi;

import junit.framework.TestCase;
import org.jboss.dna.common.jdbc.model.api.TableType;

/**
 * TableTypeBean test
 * 
 * @author <a href="mailto:litsenko_sergey@yahoo.com">Sergiy Litsenko</a>
 */
public class TableTypeBeanTest extends TestCase {

    private TableType bean;

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // create
        bean = new TableTypeBean();
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

    public void testSetName() {
        String tableTypeName = "My type name";
        // set
        bean.setName(tableTypeName);
        // check
        assertSame("Unable to set table type name", tableTypeName, bean.getName());
    }

    /*
     * Class under test for Boolean isTable()
     */
    public void testIsTable() {
        // set
        bean.setName(TableType.DEF_TABLE_TYPE_TABLE);
        // check
        assertTrue("Unable to set table type to table", bean.isTable());
    }

    /*
     * Class under test for Boolean isView()
     */
    public void testIsView() {
        // set
        bean.setName(TableType.DEF_TABLE_TYPE_VIEW);
        // check
        assertTrue("Unable to set table type to view", bean.isView());
    }

    /*
     * Class under test for Boolean isSystemTable()
     */
    public void testIsSystemTable() {
        // set
        bean.setName(TableType.DEF_TABLE_TYPE_SYS_TABLE);
        // check
        assertTrue("Unable to set table type to system table", bean.isSystemTable());
    }

    /*
     * Class under test for Boolean isGlobalTemporary()
     */
    public void testIsGlobalTemporary() {
        // set
        bean.setName(TableType.DEF_TABLE_TYPE_GLOBAL_TEMP);
        // check
        assertTrue("Unable to set table type to global temporary", bean.isGlobalTemporary());
    }

    public void testIsLocalTemporary() {
        // set
        bean.setName(TableType.DEF_TABLE_TYPE_LOCAL_TEMP);
        // check
        assertTrue("Unable to set table type to local temporary", bean.isLocalTemporary());
    }

    /*
     * Class under test for Boolean isAlias()
     */
    public void testIsAlias() {
        // set
        bean.setName(TableType.DEF_TABLE_TYPE_ALIAS);
        // check
        assertTrue("Unable to set table type to alias", bean.isAlias());
    }

    /*
     * Class under test for Boolean isSynonym()
     */
    public void testIsSynonym() {
        // set
        bean.setName(TableType.DEF_TABLE_TYPE_SYNONYM);
        // check
        assertTrue("Unable to set table type to synonym", bean.isSynonym());
    }

}
