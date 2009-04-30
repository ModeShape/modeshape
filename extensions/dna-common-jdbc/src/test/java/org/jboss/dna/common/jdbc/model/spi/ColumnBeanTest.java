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
import org.jboss.dna.common.jdbc.model.api.Column;
import org.jboss.dna.common.jdbc.model.api.NullabilityType;
import org.jboss.dna.common.jdbc.model.api.Privilege;
import org.jboss.dna.common.jdbc.model.api.SqlType;
import org.jboss.dna.common.jdbc.model.api.Table;

/**
 * ColumnBean test
 * 
 * @author <a href="mailto:litsenko_sergey@yahoo.com">Sergiy Litsenko</a>
 */
public class ColumnBeanTest extends TestCase {

    private Column bean;

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // create
        bean = new ColumnBean();
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

    public void testSetOwner() {
        // create table
        Table table = new DefaultModelFactory().createTable();
        // set
        bean.setOwner(table);
        // check
        assertSame("Unable to set owner", table, bean.getOwner());
    }

    public void testSetNullabilityType() {
        // set
        bean.setNullabilityType(NullabilityType.NULLABLE);
        // check
        assertSame("Unable to set nullability type", NullabilityType.NULLABLE, bean.getNullabilityType());
    }

    public void testSetSqlType() {
        // set
        bean.setSqlType(SqlType.VARCHAR);
        // check
        assertSame("Unable to set SQL type", SqlType.VARCHAR, bean.getSqlType());
    }

    public void testSetTypeName() {
        String TYPE_NAME = "My Type";
        // set
        bean.setTypeName(TYPE_NAME);
        // check
        assertEquals("Unable to set type name", TYPE_NAME, bean.getTypeName());
    }

    public void testSetSize() {
        Integer size = new Integer(255);
        // set
        bean.setSize(size);
        // check
        assertSame("Unable to set size", size, bean.getSize());
    }

    public void testSetPrecision() {
        Integer precision = new Integer(5);
        // set
        bean.setPrecision(precision);
        // check
        assertSame("Unable to set precision", precision, bean.getPrecision());
    }

    public void testSetRadix() {
        Integer radix = new Integer(2);
        // set
        bean.setRadix(radix);
        // check
        assertSame("Unable to set radix", radix, bean.getRadix());
    }

    public void testSetDefaultValue() {
        String defaultValue = "Hello";
        // set
        bean.setDefaultValue(defaultValue);
        // check
        assertEquals("Unable to set default value", defaultValue, bean.getDefaultValue());
    }

    public void testSetOrdinalPosition() {
        Integer ordinalPosition = new Integer(1);
        // set
        bean.setOrdinalPosition(ordinalPosition);
        // check
        assertSame("Unable to set ordinalPosition", ordinalPosition, bean.getOrdinalPosition());
    }

    public void testSetCharOctetLength() {
        Integer charOctetLength = new Integer(2);
        // set
        bean.setCharOctetLength(charOctetLength);
        // check
        assertSame("Unable to set char octet length", charOctetLength, bean.getCharOctetLength());
    }

    public void testGetPrivileges() {
        Set<Privilege> privilegeSet = bean.getPrivileges();
        // check
        assertNotNull("Unable to get privileges", privilegeSet);
        assertTrue("Privilege set should be empty by default", privilegeSet.isEmpty());
    }

    public void testAddPrivilege() {
        String PRIVILEGE_NAME = "SELECT";
        // create
        Privilege privilege = new DefaultModelFactory().createPrivilege();
        // set name
        privilege.setName(PRIVILEGE_NAME);
        // add
        bean.addPrivilege(privilege);
        // check
        assertFalse("Privilege set should not be empty", bean.getPrivileges().isEmpty());
    }

    public void testDeletePrivilege() {
        String PRIVILEGE_NAME = "SELECT";
        // create
        Privilege privilege = new DefaultModelFactory().createPrivilege();
        // set name
        privilege.setName(PRIVILEGE_NAME);
        // add
        bean.addPrivilege(privilege);
        // check
        assertFalse("Privilege set should not be empty", bean.getPrivileges().isEmpty());

        // delete
        bean.deletePrivilege(privilege);
        // check
        assertTrue("Privilege set should not be empty", bean.getPrivileges().isEmpty());
    }

    public void testFindPriviledgeByName() {
        String PRIVILEGE_NAME = "SELECT";
        // create
        Privilege privilege = new DefaultModelFactory().createPrivilege();
        // set name
        privilege.setName(PRIVILEGE_NAME);
        // add
        bean.addPrivilege(privilege);
        // check
        assertSame("Unable to find privilege", privilege, bean.findPriviledgeByName(PRIVILEGE_NAME));
    }

}
