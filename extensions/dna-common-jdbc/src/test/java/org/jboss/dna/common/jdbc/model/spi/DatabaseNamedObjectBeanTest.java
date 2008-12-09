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

import java.util.Map;
import junit.framework.TestCase;
import org.jboss.dna.common.jdbc.model.api.DatabaseNamedObject;

/**
 * DatabaseNamedObjectBean test
 * 
 * @author <a href="mailto:litsenko_sergey@yahoo.com">Sergiy Litsenko</a>
 */
public class DatabaseNamedObjectBeanTest extends TestCase {

    private DatabaseNamedObject bean;

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // create
        bean = new DatabaseNamedObjectBean();
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
        String name = "My Name";
        // set
        bean.setName(name);
        // check
        assertSame("Unable to set name", name, bean.getName());
    }

    public void testSetRemarks() {
        String remarks = "My remarks";
        // set
        bean.setRemarks(remarks);
        // check
        assertSame("Unable to set remarks", remarks, bean.getRemarks());
    }

    public void testGetExtraProperties() {
        // get
        Map<String, Object> properties = bean.getExtraProperties();
        // check
        assertNotNull("ExtraProperties should not be null by default", properties);
        // check
        assertTrue("ExtraProperties should be empty", properties.isEmpty());
    }

    public void testAddExtraProperty() {
        String key = "My key";
        String value = "My Value";
        // set
        bean.addExtraProperty(key, value);
        // check
        assertSame("Unable to set extra property", value, bean.getExtraProperty(key));
    }

    public void testDeleteExtraProperty() {
        String key = "My key";
        String value = "My Value";
        // set
        bean.addExtraProperty(key, value);
        // check
        assertFalse("ExtraProperties should not be empty", bean.getExtraProperties().isEmpty());

        // delete
        bean.deleteExtraProperty(key);
        // check
        assertTrue("ExtraProperties should be empty", bean.getExtraProperties().isEmpty());
    }

}
