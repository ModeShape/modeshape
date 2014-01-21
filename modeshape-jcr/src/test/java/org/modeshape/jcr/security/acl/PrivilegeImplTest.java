/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.jcr.security.acl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.security.Privilege;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.jcr.MultiUseAbstractTest;

/**
 * @author kulikov
 */
public class PrivilegeImplTest extends MultiUseAbstractTest {
    // non aggregate privileges
    private PrivilegeImpl p1;
    private PrivilegeImpl p2;

    // aggegate privileges
    private PrivilegeImpl p3;
    private PrivilegeImpl p4;

    // abstract
    private PrivilegeImpl p5;

    public PrivilegeImplTest() {
    }

    @BeforeClass
    public static final void beforeAll() throws Exception {
        MultiUseAbstractTest.beforeAll();

        // Import the node types and the data ...
        registerNodeTypes("cars.cnd");
        importContent("/", "io/cars-system-view-with-uuids.xml", ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);

    }

    @AfterClass
    public static final void afterAll() throws Exception {
        MultiUseAbstractTest.afterAll();
    }

    @Before
    public void setUp() throws Exception {
        p1 = new PrivilegeImpl(session, "{}p1", new Privilege[] {});
        p2 = new PrivilegeImpl(session, "{}p2", new Privilege[] {});

        // aggegate privileges
        p3 = new PrivilegeImpl(session, "{}p3", new Privilege[] {p1, p2});
        p4 = new PrivilegeImpl(session, "{}p4", new Privilege[] {p3});

        // abstract
        p5 = new PrivilegeImpl(session, "{}p5", new Privilege[] {}, true);
    }

    /**
     * Test of getName method, of class PrivilegeImpl.
     */
    @Test
    public void testGetName() {
        assertEquals("jcr:p1", p1.getName());
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

    private boolean contains( Privilege p,
                              Privilege[] pp ) {
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
