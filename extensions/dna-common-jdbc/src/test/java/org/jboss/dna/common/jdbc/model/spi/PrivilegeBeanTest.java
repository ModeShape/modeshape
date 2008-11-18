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
import org.jboss.dna.common.jdbc.model.api.Privilege;
import org.jboss.dna.common.jdbc.model.api.PrivilegeType;

/**
 * PrivilegeBean test
 * 
 * @author <a href="mailto:litsenko_sergey@yahoo.com">Sergiy Litsenko</a>
 */
public class PrivilegeBeanTest extends TestCase {

    private Privilege bean;

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        // create
        bean = new PrivilegeBean();
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        bean = null;

        super.tearDown();
    }

    public void testSetPrivilegeType() {
        // set
        bean.setPrivilegeType(PrivilegeType.UPDATE);
        // check
        assertSame("Unable to set priviledge", PrivilegeType.UPDATE, bean.getPrivilegeType());
    }

    public void testSetGrantor() {
        String grantor = "Me";
        // set
        bean.setGrantor(grantor);
        // check
        assertEquals("Unable to set grantor", grantor, bean.getGrantor());
    }

    public void testSetGrantee() {
        String grantee = "You";
        // set
        bean.setGrantee(grantee);
        // check
        assertEquals("Unable to set grantee", grantee, bean.getGrantee());
    }

    public void testSetName() {
        String name = "The name";
        // set
        bean.setName(name);
        // check
        assertEquals("Unable to set name", name, bean.getName());
    }

    public void testSetGrantable() {
        Boolean grantable = Boolean.TRUE;
        // set
        bean.setGrantable(grantable);
        // check
        assertSame("Unable to set grantable", grantable, bean.isGrantable());
    }

    public void testSetUnknownGrantable() {
        Boolean unknownGrantable = Boolean.TRUE;
        // set
        bean.setUnknownGrantable(unknownGrantable);
        // check
        assertSame("Unable to set unknown grantable", unknownGrantable, bean.isUnknownGrantable());
    }

}
