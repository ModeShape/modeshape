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
import org.jboss.dna.common.jdbc.model.DefaultModelFactory;
import org.jboss.dna.common.jdbc.model.api.ColumnPseudoType;
import org.jboss.dna.common.jdbc.model.api.Reference;
import org.jboss.dna.common.jdbc.model.api.TableColumn;

/**
 * TableColumnBean test
 * 
 * @author <a href="mailto:litsenko_sergey@yahoo.com">Sergiy Litsenko</a>
 */
public class TableColumnBeanTest extends TestCase {

    private TableColumn bean;

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // create
        bean = new TableColumnBean();
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

    public void testSetBestRowIdentifier() {
        // set
        bean.setBestRowIdentifier(Boolean.TRUE);
        // check
        assertSame("Unable to set BestRowIdentifier", Boolean.TRUE, bean.isBestRowIdentifier());
    }

    public void testSetPseudoType() {
        // set
        bean.setPseudoType(ColumnPseudoType.NOT_PSEUDO);
        // check
        assertSame("Unable to set column pseudo type", ColumnPseudoType.NOT_PSEUDO, bean.getPseudoType());
    }

    public void testSetReference() {
        Reference reference = new DefaultModelFactory().createReference();
        // set
        bean.setReference(reference);
        // check
        assertSame("Unable to set reference", reference, bean.getReference());
    }

    public void testSetVersionColumn() {
        // set
        bean.setVersionColumn(Boolean.TRUE);
        // check
        assertSame("Unable to set VersionColumn", Boolean.TRUE, bean.isVersionColumn());
    }

    public void testSetPrimaryKeyColumn() {
        // set
        bean.setPrimaryKeyColumn(Boolean.TRUE);
        // check
        assertSame("Unable to set PrimaryKeyColumn", Boolean.TRUE, bean.isPrimaryKeyColumn());
    }

    public void testSetForeignKeyColumn() {
        // set
        bean.setForeignKeyColumn(Boolean.TRUE);
        // check
        assertSame("Unable to set ForeignKeyColumn", Boolean.TRUE, bean.isForeignKeyColumn());
    }

    public void testSetIndexColumn() {
        // set
        bean.setIndexColumn(Boolean.TRUE);
        // check
        assertSame("Unable to set IndexColumn", Boolean.TRUE, bean.isIndexColumn());
    }

}
