/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.jcr.security.acl;

import javax.jcr.security.Privilege;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author kulikov
 */
public class PrivilegeImplTest {
    //non aggregate privileges
    private PrivilegeImpl p1 = new PrivilegeImpl("p1", new Privilege[]{});
    private PrivilegeImpl p2 = new PrivilegeImpl("p2", new Privilege[]{});

    //aggegate privileges
    private PrivilegeImpl p3 = new PrivilegeImpl("p3", new Privilege[]{p1, p2});
    private PrivilegeImpl p4 = new PrivilegeImpl("p4", new Privilege[]{p3});
    
    //abstract
    private PrivilegeImpl p5 = new PrivilegeImpl("p5", new Privilege[]{}, true);
    
    public PrivilegeImplTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of getName method, of class PrivilegeImpl.
     */
    @Test
    public void testGetName() {
        assertEquals("p1", p1.getName());
    }

    /**
     * Test of isAbstract method, of class PrivilegeImpl.
     */
    @Test
    public void testIsAbstract() {
        assertTrue(!p1.isAbstract());
        assertTrue(p5.isAbstract());
    }

    /**
     * Test of isAggregate method, of class PrivilegeImpl.
     */
    @Test
    public void testIsAggregate() {
        assertTrue(!p1.isAggregate());
        assertTrue(p3.isAggregate());
    }

    /**
     * Test of getDeclaredAggregatePrivileges method, of class PrivilegeImpl.
     */
    @Test
    public void testGetDeclaredAggregatePrivileges() {
        Privilege[] pp = p3.getDeclaredAggregatePrivileges();
        assertEquals(2, pp.length);
        
        assertEquals(p1, pp[0]);
        assertEquals(p2, pp[1]);
                
    }

    /**
     * Test of getAggregatePrivileges method, of class PrivilegeImpl.
     */
    @Test
    public void testGetAggregatePrivileges() {
        Privilege[] pp = p4.getAggregatePrivileges();
        assertEquals(3, pp.length);
        assertTrue(contains(p1, pp));
        assertTrue(contains(p2, pp));
        assertTrue(contains(p3, pp));
    }
    
    private boolean contains(Privilege p, Privilege[] pp) {
        for (int i = 0; i < pp.length; i++) {
            if (pp[i].getName().equals(p.getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Test of contains method, of class PrivilegeImpl.
     */
    @Test
    public void testContains() {
        assertTrue(p4.contains(p3));
        assertTrue(p4.contains(p2));
        assertTrue(p4.contains(p4));
    }


}