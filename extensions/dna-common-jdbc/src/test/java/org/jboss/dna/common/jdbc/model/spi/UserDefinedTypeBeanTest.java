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

import java.util.Set;
import junit.framework.TestCase;
import org.jboss.dna.common.jdbc.model.DefaultModelFactory;
import org.jboss.dna.common.jdbc.model.api.Attribute;
import org.jboss.dna.common.jdbc.model.api.SqlType;
import org.jboss.dna.common.jdbc.model.api.UserDefinedType;

/**
 * UserDefinedTypeBean test
 * 
 * @author <a href="mailto:litsenko_sergey@yahoo.com">Sergiy Litsenko</a>
 */
public class UserDefinedTypeBeanTest extends TestCase {

    private UserDefinedType bean;

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // create
        bean = new UserDefinedTypeBean();
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

    public void testSetClassName() {
        String className = "My class";
        // set
        bean.setClassName(className);
        // check
        assertSame("Unable to set class name", className, bean.getClassName());
    }

    public void testSetSqlType() {
        // set
        bean.setSqlType(SqlType.VARCHAR);
        // check
        assertSame("Unable to set SQL type", SqlType.VARCHAR, bean.getSqlType());
    }

    public void testSetBaseType() {
        // set
        bean.setBaseType(SqlType.VARCHAR);
        // check
        assertSame("Unable to set base type", SqlType.VARCHAR, bean.getBaseType());
    }

    public void testGetAttributes() {
        Set<Attribute> attributes = bean.getAttributes();
        // check
        assertNotNull("Unable to get attributes", attributes);
        assertTrue("Attribute set should be empty by default", attributes.isEmpty());
    }

    public void testAddAttribute() {
        String NAME = "My name";
        Attribute a = new DefaultModelFactory().createAttribute();
        // set name
        a.setName(NAME);
        // add
        bean.addAttribute(a);
        // check
        assertFalse("Attribute set should not be empty", bean.getAttributes().isEmpty());
    }

    public void testDeleteAttribute() {
        String NAME = "My name";
        Attribute a = new DefaultModelFactory().createAttribute();
        // set name
        a.setName(NAME);
        // add
        bean.addAttribute(a);
        // check
        assertFalse("Attribute set should not be empty", bean.getAttributes().isEmpty());
        // delete
        bean.deleteAttribute(a);
        // check
        assertTrue("Attribute set should be empty", bean.getAttributes().isEmpty());
    }

    public void testFindAttributeByName() {
        String NAME = "My name";
        Attribute a = new DefaultModelFactory().createAttribute();
        // set name
        a.setName(NAME);
        // add
        bean.addAttribute(a);
        // check
        assertSame("Unable to find attribute", a, bean.findAttributeByName(NAME));
    }

    public void testSetSuperType() {
        UserDefinedType st = new DefaultModelFactory().createUserDefinedType();
        // set
        bean.setSuperType(st);
        // check
        assertSame("Unable to set super type", st, bean.getSuperType());
    }

}
