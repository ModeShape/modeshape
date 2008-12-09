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

import junit.framework.TestCase;
import org.jboss.dna.common.jdbc.model.DefaultModelFactory;
import org.jboss.dna.common.jdbc.model.api.Reference;
import org.jboss.dna.common.jdbc.model.api.SqlType;
import org.jboss.dna.common.jdbc.model.api.Table;

/**
 * ReferenceBean test
 * 
 * @author <a href="mailto:litsenko_sergey@yahoo.com">Sergiy Litsenko</a>
 */
public class ReferenceBeanTest extends TestCase {

    private Reference bean;

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // create
        bean = new ReferenceBean();
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

    public void testSetSourceTable() {
        Table sourceTable = new DefaultModelFactory().createTable();
        // set
        bean.setSourceTable(sourceTable);
        // check
        assertSame("Unable to set source table", sourceTable, bean.getSourceTable());
    }

    public void testSetSourceDataType() {
        // set
        bean.setSourceDataType(SqlType.VARCHAR);
        // check
        assertSame("Unable to set source data type", SqlType.VARCHAR, bean.getSourceDataType());
    }

}
